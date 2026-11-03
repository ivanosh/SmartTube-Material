package io.github.ivanosh.stmaterial.update

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liskovsoft.smartyoutubetv2.tv.R
import io.github.ivanosh.stmaterial.logic.DownloadPhase
import io.github.ivanosh.stmaterial.logic.Refusal
import io.github.ivanosh.stmaterial.logic.UpdateEnd
import io.github.ivanosh.stmaterial.logic.UpdateOffer
import io.github.ivanosh.stmaterial.logic.checkDownloaded
import io.github.ivanosh.stmaterial.logic.offerAllowed
import io.github.ivanosh.stmaterial.logic.workEnd
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The one download the Update pill and Settings share. Only one run is on at a time (the [active] guard): a second
 * [start] from either place is ignored, so the two can never run two downloads. The state is observable, so the pill
 * follows a run begun in Settings and the reverse, and a panel can show its progress.
 *
 * The bytes, their verification and the hand-off to the system installer are [UpdateClient], [checkDownloaded] and
 * [UpdateInstaller] — the same code the dialog used before, moved here so both callers share it. The installer opens
 * a system screen, so a run is bound to the activity that began it; that activity cancels the run when it goes
 * ([releaseHost]). A late progress report or result after a cancel changes nothing.
 */
object UpdateDownload {
    var phase by mutableStateOf(DownloadPhase.IDLE)
        private set
    var percent by mutableIntStateOf(0)
        private set

    /** The offer being downloaded, kept for the panel while a run is on. */
    var offer by mutableStateOf<UpdateOffer?>(null)
        private set

    /** The last message to show: a failure, a cancel, or the confirm hint. Read by the pill (on a failure) and Settings. */
    var message by mutableStateOf<String?>(null)
        private set

    private val cancelled = AtomicBoolean(false)
    private var host: Activity? = null
    private var onChange: (() -> Unit)? = null
    private var installer: UpdateInstaller? = null

    /** True while a run owns the flow: a second start is refused, and OK on the pill opens the panel. */
    val active: Boolean get() = phase != DownloadPhase.IDLE && phase != DownloadPhase.FAILED

    /**
     * Starts (or Retries) the single download, bound to [activity] for the installer screen. Ignored while a run is
     * on. [onProgress] is an optional observer for a caller that does not read the state in composition (Settings);
     * it runs on the main thread after every change.
     */
    fun start(activity: Activity, offer: UpdateOffer, onProgress: () -> Unit = {}) {
        if (active) return
        // A Retry leaves the previous run's installer, its receiver inert but still registered; drop it first.
        installer?.unregister()
        installer = null
        host = activity
        onChange = onProgress
        this.offer = offer
        // The channel may have changed since the check: Off downloads nothing, Stable never a prerelease.
        if (!offerAllowed(offer, UpdateStore(activity).channel)) {
            fail(activity.getString(R.string.material_update_channel_changed))
            return
        }
        cancelled.set(false)
        set(DownloadPhase.DOWNLOADING, percent = 0, message = null)
        val app = activity.applicationContext
        val installer = UpdateInstaller(activity) { outcome ->
            // A late result after Back or a cancel changes nothing.
            if (!cancelled.get()) when (outcome) {
                UpdateInstaller.Outcome.CONFIRMING -> set(DownloadPhase.CONFIRMING, message = app.getString(R.string.material_update_confirming))
                UpdateInstaller.Outcome.CANCELLED -> fail(app.getString(R.string.material_update_cancelled))
                UpdateInstaller.Outcome.FAILED -> fail(app.getString(R.string.material_update_install_failed))
            }
        }
        this.installer = installer
        UpdateService.runInBackground {
            val file = UpdateClient.apkFile(app)
            var verified = false
            var committed = false
            var refusal: Refusal? = null
            var downloadFailed = false
            try {
                val client = UpdateClient(app)
                val downloaded = client.download(offer, file, { p -> UpdateService.postMain { if (phase == DownloadPhase.DOWNLOADING) set(DownloadPhase.DOWNLOADING, percent = p) } }, { cancelled.get() })
                if (cancelled.get()) throw java.io.IOException("Cancelled")
                refusal = checkDownloaded(
                    offer, downloaded.sha256, downloaded.size, client.archiveFacts(file),
                    app.packageName, UpdateService.installedVersionCode(app), client.pinnedCert(),
                )
                if (refusal == null && !cancelled.get()) {
                    verified = true
                    UpdateService.postMain { if (phase == DownloadPhase.DOWNLOADING && !cancelled.get()) set(DownloadPhase.PREPARING, percent = 0, message = null) }
                    // The copy into the system's session, off the main thread. install() deletes the file.
                    committed = installer.install(
                        file, offer.asset.sha256.orEmpty(), { cancelled.get() },
                        // Whole percents only, from the copy loop; a late one after a cancel or a result changes nothing.
                        onPercent = { p -> UpdateService.postMain { if (phase == DownloadPhase.PREPARING && !cancelled.get()) set(DownloadPhase.PREPARING, percent = p) } },
                        onFinishing = { UpdateService.postMain { if (phase == DownloadPhase.PREPARING && !cancelled.get()) set(DownloadPhase.PREPARING, percent = 100) } },
                    )
                }
            } catch (e: java.io.IOException) {
                downloadFailed = true
            } catch (e: RuntimeException) {
                downloadFailed = true
            } finally {
                // A verified file belongs to the install, which deletes it; any other file goes now.
                if (!verified) file.delete()
            }
            // Every end but a commit is final, whatever was pressed meanwhile: the flow is never left busy.
            val end = workEnd(committed, cancelled.get(), refusal, downloadFailed)
            UpdateService.postMain {
                // A run the host let go (releaseHost) is already IDLE; a late end leaves it alone.
                if (!active) return@postMain
                when (end) {
                    // The commit waits for the installer's result; the installer callback moves it on.
                    UpdateEnd.WAIT_FOR_SYSTEM -> if (phase == DownloadPhase.PREPARING) set(DownloadPhase.CONFIRMING, percent = 100)
                    UpdateEnd.CANCELLED -> fail(app.getString(R.string.material_update_cancelled))
                    UpdateEnd.REFUSED -> fail(refusal?.message.orEmpty())
                    UpdateEnd.DOWNLOAD_FAILED -> fail(app.getString(R.string.material_update_download_failed))
                    UpdateEnd.INSTALL_FAILED -> fail(app.getString(R.string.material_update_install_failed))
                }
            }
        }
    }

    /** Back on the panel's Cancel, or Back in Settings: the worker sees the flag at its next step and ends the phase. */
    fun cancel() {
        if (!active) return
        cancelled.set(true)
        installer?.abandon()
    }

    /**
     * The activity that began a run is going. A run still on is cancelled (its file goes with it); the installer is
     * let go. Nothing happens for an activity that is not the host, so Settings leaving does not touch a pill run.
     */
    fun releaseHost(activity: Activity) {
        if (host !== activity) return
        cancelled.set(true)
        installer?.let { if (phase == DownloadPhase.CONFIRMING) it.abandon() else it.unregister() }
        if (!active) UpdateClient.deleteLeftovers(activity)
        host = null
        onChange = null
        installer = null
        // A run cut short leaves the state clean for the next time the screen is shown.
        if (active) set(DownloadPhase.IDLE, percent = 0, message = null)
    }

    private fun fail(text: String) = set(DownloadPhase.FAILED, message = text)

    private fun set(phase: DownloadPhase, percent: Int = this.percent, message: String? = this.message) {
        this.phase = phase
        this.percent = percent
        this.message = message
        // The installer is kept past a terminal state: its receiver is unregistered by the next start or by releaseHost.
        onChange?.invoke()
    }
}

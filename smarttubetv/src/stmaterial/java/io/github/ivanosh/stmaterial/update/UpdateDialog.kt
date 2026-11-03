package io.github.ivanosh.stmaterial.update

import android.app.Activity
import com.liskovsoft.smartyoutubetv2.tv.R
import io.github.ivanosh.stmaterial.dialog.ButtonRow
import io.github.ivanosh.stmaterial.dialog.DialogPage
import io.github.ivanosh.stmaterial.dialog.DialogRow
import io.github.ivanosh.stmaterial.dialog.DialogState
import io.github.ivanosh.stmaterial.dialog.TextRow
import io.github.ivanosh.stmaterial.logic.DownloadPhase
import io.github.ivanosh.stmaterial.logic.UpdateOffer

/**
 * The update dialog in the app's own dialog component: the offer, the download, a refusal or the hand-off to the
 * system installer. Used by Settings. The download itself is the shared [UpdateDownload], the same flow the Update
 * pill drives, so the two never run two downloads and each follows the other.
 *
 * The dialog only ever replaces its own page: once the user has left it, late progress or results change nothing.
 * [onBack] is for the owner's Back handler: while a download or a hand-off runs, Back cancels it and stays.
 */
class UpdateDialog(private val activity: Activity, private val state: DialogState, private val close: () -> Unit) {
    private var ownPage = -1

    fun activate(row: DialogRow) {
        (row as? ButtonRow)?.let { (it.ref as? Runnable)?.run() }
    }

    fun offerPage(offer: UpdateOffer): DialogPage {
        val title = activity.getString(R.string.material_update_title, offer.versionName) +
            if (offer.prerelease) " " + activity.getString(R.string.material_update_prerelease_tag) else ""
        val rows = mutableListOf<DialogRow>(
            ButtonRow(1, activity.getString(R.string.material_update_install), Runnable { start(offer) }),
            ButtonRow(2, activity.getString(R.string.material_update_later), Runnable { close() }),
        )
        offer.notes.forEachIndexed { i, line -> rows += TextRow(10 + i, line) }
        return DialogPage.of(title, rows)
    }

    /** True when Back was used up by cancelling something that was running. */
    fun onBack(): Boolean {
        if (!UpdateDownload.active) return false
        UpdateDownload.cancel()
        return true
    }

    private fun start(offer: UpdateOffer) {
        if (UpdateDownload.active) return
        ownPage = state.page?.id ?: -1
        // Each change of the shared download redraws this page, while it is still on top.
        UpdateDownload.start(activity, offer) { render() }
        render()
    }

    private fun onOwnPage() = state.page?.id == ownPage

    /** Turns the shared download's state into this dialog's own page, each time it changes. */
    private fun render() {
        if (!onOwnPage()) return
        when (UpdateDownload.phase) {
            DownloadPhase.DOWNLOADING -> downloadingPage(activity.getString(R.string.material_update_downloading, UpdateDownload.percent))
            DownloadPhase.PREPARING -> showText(activity.getString(R.string.material_update_preparing, UpdateDownload.percent))
            DownloadPhase.CONFIRMING -> show(UpdateDownload.message ?: activity.getString(R.string.material_update_confirming))
            DownloadPhase.FAILED -> refused(UpdateDownload.message.orEmpty())
            DownloadPhase.IDLE -> Unit
        }
    }

    private fun downloadingPage(title: String) {
        state.replace(
            DialogPage.of(
                title,
                listOf(ButtonRow(1, activity.getString(R.string.material_update_cancel), Runnable { UpdateDownload.cancel() })),
            )
        )
    }

    private fun refused(message: String) {
        state.replace(
            DialogPage.of(
                activity.getString(R.string.material_update_not_installed),
                listOf(TextRow(1, message), ButtonRow(2, activity.getString(R.string.material_update_close), Runnable { close() })),
            )
        )
    }

    /** A page with one line and no button: Back cancels. */
    private fun showText(text: String) {
        state.replace(DialogPage.of(activity.getString(R.string.material_update_heading), listOf(TextRow(1, text))))
    }

    private fun show(text: String) {
        state.replace(
            DialogPage.of(
                activity.getString(R.string.material_update_heading),
                listOf(TextRow(1, text), ButtonRow(2, activity.getString(R.string.material_update_close), Runnable { close() })),
            )
        )
    }

    fun release() {
        UpdateDownload.releaseHost(activity)
    }
}

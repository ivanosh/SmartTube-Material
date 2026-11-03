package io.github.ivanosh.stmaterial.logic

/*
 * The decisions behind the Update pill in the top bar, with no Android in them: which phase the one shared download
 * is in, what the pill then shows, whether it is on screen at all, and what OK does. The words and the drawing are in
 * the composable; the download itself is in update/UpdateDownload.kt.
 */

/**
 * The phase of the single download the pill and Settings share. PREPARING is the copy into the system's install
 * session; CONFIRMING is the hand-off, waiting for the system screen. FAILED covers a cancel too: the file is gone,
 * so the next step is a fresh download.
 */
enum class DownloadPhase { IDLE, DOWNLOADING, PREPARING, CONFIRMING, FAILED }

/** The kind of label the pill shows for a phase; the strings themselves are chosen by the composable. */
enum class PillLabel { UPDATE, PERCENT, PREPARING, INSTALL, RETRY }

/** What to draw on the pill: its label, and how full the ring around the icon is (null draws no ring). */
data class PillView(val label: PillLabel, val ring: Float?)

fun pillView(phase: DownloadPhase, percent: Int): PillView = when (phase) {
    DownloadPhase.IDLE -> PillView(PillLabel.UPDATE, null)
    DownloadPhase.DOWNLOADING -> PillView(PillLabel.PERCENT, ringFraction(percent))
    DownloadPhase.PREPARING -> PillView(PillLabel.PREPARING, ringFraction(percent))
    DownloadPhase.CONFIRMING -> PillView(PillLabel.INSTALL, 1f)
    DownloadPhase.FAILED -> PillView(PillLabel.RETRY, null)
}

/** 0 to 1, clamped: a stray percent never over- or under-fills the ring. */
fun ringFraction(percent: Int): Float = percent.coerceIn(0, 100) / 100f

/** On screen while an update is on offer, and while a run is on even if the offer were momentarily gone. */
fun pillVisible(hasOffer: Boolean, phase: DownloadPhase): Boolean = hasOffer || phase != DownloadPhase.IDLE

/** What OK on the pill does: start or Retry a download, or open the panel of the one already running. */
enum class PillAction { START, OPEN_PANEL }

fun pillAction(phase: DownloadPhase): PillAction = when (phase) {
    DownloadPhase.DOWNLOADING, DownloadPhase.PREPARING, DownloadPhase.CONFIRMING -> PillAction.OPEN_PANEL
    DownloadPhase.IDLE, DownloadPhase.FAILED -> PillAction.START
}

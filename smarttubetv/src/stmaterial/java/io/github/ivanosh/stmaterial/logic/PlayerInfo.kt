package io.github.ivanosh.stmaterial.logic

/** The delimiter YouTube's metadata line uses between the author, the view count and the published date. */
private const val META_DELIM = "•"

/** How the metadata parts are shown on the panel, under the title and author. */
const val META_JOIN = " • "

/**
 * The metadata to show under the title and author: the view count and the published date, from the video's second
 * title ("author • views • date"), with the author part dropped so it is not repeated. [author] is the author as the
 * header already shows it. A second title with no author part is kept whole. Blank in, blank out.
 */
fun metadataLine(secondTitle: String?, author: String?): String {
    val parts = secondTitle?.split(META_DELIM)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: return ""
    if (parts.isEmpty()) return ""
    val wanted = author?.trim()?.takeIf { it.isNotEmpty() }
    val kept = if (wanted == null) parts else {
        val at = parts.indexOfFirst { it.equals(wanted, ignoreCase = true) }
        if (at < 0) parts else parts.filterIndexed { i, _ -> i != at }
    }
    return kept.joinToString(META_JOIN)
}

/**
 * The description preview is shown whenever the panel is open and focus is not in the Up next row, which the preview
 * gives way to. It is not shown with the panel hidden.
 */
fun descriptionRowShown(focus: PanelFocus): Boolean =
    focus != PanelFocus.HIDDEN && focus != PanelFocus.UPNEXT

enum class DescriptionStep { None, Close }

/**
 * One remote key in the open full-description overlay: Up and Down scroll it by a step (never below the top), OK and
 * Back close it, Left and Right do nothing. The bottom is clamped by the view, which knows the content height.
 */
fun descriptionKey(scroll: Int, key: PlayerKey): Pair<Int, DescriptionStep> = when (key) {
    PlayerKey.Up -> (scroll - 1).coerceAtLeast(0) to DescriptionStep.None
    PlayerKey.Down -> scroll + 1 to DescriptionStep.None
    PlayerKey.Ok, PlayerKey.Back -> scroll to DescriptionStep.Close
    PlayerKey.Left, PlayerKey.Right -> scroll to DescriptionStep.None
}

/**
 * The description arrived or went: the flag follows it, and focus on a description that is gone goes up to the chapter
 * row or the buttons.
 */
fun withDescription(ui: PlayerUi, present: Boolean): PlayerUi {
    val moved = ui.copy(descriptionPresent = present)
    return if (!present && moved.focus == PanelFocus.DESCRIPTION) {
        moved.copy(focus = if (moved.chapterCount > 0) PanelFocus.CHAPTERS else PanelFocus.BUTTONS)
    } else moved
}

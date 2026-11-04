package io.github.ivanosh.stmaterial.logic

/**
 * One subtitle track as the toggle sees it, free of the engine's FormatItem. [index] is the track's place in the
 * engine's list, which the caller maps back to a FormatItem. [isNone] marks the list's single "off" sentinel
 * (the engine's default, null-format entry); it is not a real track. [isAuto] marks an auto-generated caption.
 * [language] is the track's language, trimmed of region and the auto marker, null when unknown.
 */
data class SubtitleOption(
    val index: Int,
    val language: String?,
    val isNone: Boolean,
    val isAuto: Boolean,
)

/** What a short press of the Subtitles button should do, given what is known about the video and the viewer. */
sealed interface SubtitleToggle {
    /** Turn subtitles on with the track at this [index] in the engine's list. */
    data class Enable(val index: Int) : SubtitleToggle
    /** Turn subtitles off (select the off sentinel). */
    object Disable : SubtitleToggle
    /** The track list is not known yet, so defer to the full picker rather than guess. */
    object OpenPicker : SubtitleToggle
    /** The video genuinely has no subtitle track; the caller shows a short notice. */
    object NoSubtitles : SubtitleToggle
}

private fun norm(language: String?): String? = language?.trim()?.lowercase()?.ifBlank { null }

/**
 * The track a bare toggle enables, when there is one to enable: the first of [lastUsed] languages that the video
 * offers, else the [preferred] language, else the first non-auto track, else the first track. A non-auto track of a
 * language is preferred over an auto one of the same language.
 */
private fun pickSubtitle(real: List<SubtitleOption>, lastUsed: List<String>, preferred: String?): SubtitleOption {
    val queries = (lastUsed.mapNotNull(::norm) + listOfNotNull(norm(preferred)))
    for (query in queries) {
        val matches = real.filter { norm(it.language) == query }
        (matches.firstOrNull { !it.isAuto } ?: matches.firstOrNull())?.let { return it }
    }
    return real.firstOrNull { !it.isAuto } ?: real.first()
}

/**
 * Decides what a short press of the Subtitles button does.
 *
 * On when pressed means turn off. Off means turn on a sensible track with no detour through a menu: the last-used
 * language the video offers, else the viewer's [preferred] language, else the first real track. The full picker is
 * only for when the list is not loaded yet; a video with no real track gives a short notice, not an empty picker.
 *
 * @param options every subtitle format the engine offers, the off sentinel included, or empty when none is known yet.
 * @param currentlyOn whether the CC button is on now.
 * @param lastUsed the languages of the viewer's recently used subtitle formats, most recent first.
 * @param preferred the viewer's preferred subtitle language, null when unset.
 */
fun decideSubtitleToggle(
    options: List<SubtitleOption>,
    currentlyOn: Boolean,
    lastUsed: List<String>,
    preferred: String?,
): SubtitleToggle {
    if (currentlyOn) return SubtitleToggle.Disable
    if (options.isEmpty()) return SubtitleToggle.OpenPicker
    val real = options.filter { !it.isNone }
    if (real.isEmpty()) return SubtitleToggle.NoSubtitles
    return SubtitleToggle.Enable(pickSubtitle(real, lastUsed, preferred).index)
}

package io.github.ivanosh.stmaterial.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleToggleTest {
    private val off = SubtitleOption(0, null, isNone = true, isAuto = false)
    private fun track(index: Int, lang: String, auto: Boolean = false) =
        SubtitleOption(index, lang, isNone = false, isAuto = auto)

    private fun decide(
        options: List<SubtitleOption>,
        currentlyOn: Boolean = false,
        lastUsed: List<String> = emptyList(),
        preferred: String? = null,
    ) = decideSubtitleToggle(options, currentlyOn, lastUsed, preferred)

    @Test
    fun onMeansTurnOff() {
        assertEquals(SubtitleToggle.Disable, decide(listOf(off, track(1, "en")), currentlyOn = true))
        // Even with nothing remembered, an on toggle just turns off.
        assertEquals(SubtitleToggle.Disable, decide(listOf(off), currentlyOn = true))
    }

    @Test
    fun noTracksLoadedYetDefersToThePicker() {
        assertEquals(SubtitleToggle.OpenPicker, decide(emptyList()))
    }

    @Test
    fun aVideoWithNoRealTrackSaysSo() {
        assertEquals(SubtitleToggle.NoSubtitles, decide(listOf(off)))
    }

    @Test
    fun theOnlyRealTrackIsEnabled() {
        assertEquals(SubtitleToggle.Enable(1), decide(listOf(off, track(1, "en"))))
    }

    @Test
    fun theLastUsedLanguageWinsOverOrderAndOverThePreferred() {
        val options = listOf(off, track(1, "en"), track(2, "de"), track(3, "fr"))
        assertEquals(SubtitleToggle.Enable(3), decide(options, lastUsed = listOf("fr", "de"), preferred = "de"))
    }

    @Test
    fun theMostRecentOfSeveralLastUsedLanguagesThatTheVideoOffersWins() {
        val options = listOf(off, track(1, "en"), track(2, "de"))
        // "fr" is not offered, so the next remembered language, "de", is taken.
        assertEquals(SubtitleToggle.Enable(2), decide(options, lastUsed = listOf("fr", "de", "en")))
    }

    @Test
    fun thePreferredLanguageIsUsedWhenNothingWasEverChosen() {
        val options = listOf(off, track(1, "en"), track(2, "de"))
        assertEquals(SubtitleToggle.Enable(2), decide(options, preferred = "de"))
    }

    @Test
    fun aNonAutoTrackWinsOverAnAutoOneOfTheSameLanguage() {
        val options = listOf(off, track(1, "en", auto = true), track(2, "en", auto = false))
        assertEquals(SubtitleToggle.Enable(2), decide(options, preferred = "en"))
    }

    @Test
    fun withNoHintTheFirstRealNonAutoTrackIsEnabled() {
        val options = listOf(off, track(1, "en", auto = true), track(2, "de", auto = false))
        assertEquals(SubtitleToggle.Enable(2), decide(options))
    }

    @Test
    fun withOnlyAutoTracksAndNoHintTheFirstAutoTrackIsEnabled() {
        val options = listOf(off, track(1, "en", auto = true), track(2, "de", auto = true))
        assertEquals(SubtitleToggle.Enable(1), decide(options))
    }

    @Test
    fun languageMatchingIgnoresCaseRegionWhitespaceAndBlanks() {
        val options = listOf(off, track(1, "EN "), track(2, "de"))
        assertEquals(SubtitleToggle.Enable(1), decide(options, lastUsed = listOf("", "en")))
    }
}

package io.github.ivanosh.stmaterial.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerInfoTest {

    // The metadata line under the title and author: views and date, without the author.

    @Test
    fun dropsTheAuthorAndKeepsViewsAndDate() {
        assertEquals("1.2M views • 2 years ago", metadataLine("Some Channel • 1.2M views • 2 years ago", "Some Channel"))
    }

    @Test
    fun aSecondTitleWithoutTheAuthorIsKeptWhole() {
        assertEquals("1.2M views • 2 years ago", metadataLine("1.2M views • 2 years ago", "Some Channel"))
    }

    @Test
    fun matchesTheAuthorIgnoringCaseAndSurroundingSpace() {
        assertEquals("500 views • today", metadataLine("  some channel  •  500 views • today ", "Some Channel"))
    }

    @Test
    fun dropsTheAuthorWhenItFollowsALabel() {
        // Upstream prefixes a label such as 4K; the author is then the second part.
        assertEquals("4K • 10K views • 1 day ago", metadataLine("4K • Some Channel • 10K views • 1 day ago", "Some Channel"))
    }

    @Test
    fun blankOrMissingPartsGiveABlankOrShortLine() {
        assertEquals("", metadataLine(null, "Some Channel"))
        assertEquals("", metadataLine("", "Some Channel"))
        assertEquals("", metadataLine("   ", "Some Channel"))
        // No author known: the whole second title is kept.
        assertEquals("1M views • 2021", metadataLine("1M views • 2021", null))
        // Only the author is there: nothing is left for the line.
        assertEquals("", metadataLine("Some Channel", "Some Channel"))
    }

    // The description preview shows unless focus is in the Up next row.

    @Test
    fun thePreviewShowsOnThePanelButNotHiddenAndNotUnderTheRow() {
        assertTrue(descriptionRowShown(PanelFocus.BUTTONS))
        assertTrue(descriptionRowShown(PanelFocus.PROGRESS))
        assertTrue(descriptionRowShown(PanelFocus.CHAPTERS))
        assertTrue(descriptionRowShown(PanelFocus.DESCRIPTION))
        assertFalse(descriptionRowShown(PanelFocus.UPNEXT))
        assertFalse(descriptionRowShown(PanelFocus.HIDDEN))
    }

    // The full-description overlay keys.

    @Test
    fun theOverlayScrollsDownAndNeverAboveTheTop() {
        assertEquals(1 to DescriptionStep.None, descriptionKey(0, PlayerKey.Down))
        assertEquals(3 to DescriptionStep.None, descriptionKey(2, PlayerKey.Down))
        assertEquals(0 to DescriptionStep.None, descriptionKey(0, PlayerKey.Up))
        assertEquals(1 to DescriptionStep.None, descriptionKey(2, PlayerKey.Up))
    }

    @Test
    fun theOverlayClosesOnOkAndBackAndIgnoresSideways() {
        assertEquals(2 to DescriptionStep.Close, descriptionKey(2, PlayerKey.Ok))
        assertEquals(2 to DescriptionStep.Close, descriptionKey(2, PlayerKey.Back))
        assertEquals(2 to DescriptionStep.None, descriptionKey(2, PlayerKey.Left))
        assertEquals(2 to DescriptionStep.None, descriptionKey(2, PlayerKey.Right))
    }

    // The description flag follows the data, and focus recovers when it goes.

    @Test
    fun aDescriptionThatArrivesSetsTheFlagAndLeavesFocus() {
        val ui = PlayerUi(PanelFocus.BUTTONS, 0, 9)
        val next = withDescription(ui, true)
        assertTrue(next.descriptionPresent)
        assertEquals(PanelFocus.BUTTONS, next.focus)
    }

    @Test
    fun aDescriptionThatGoesWhileFocusedSendsFocusUp() {
        val onDescription = PlayerUi(PanelFocus.DESCRIPTION, 2, 9, descriptionPresent = true)
        assertEquals(PanelFocus.BUTTONS, withDescription(onDescription, false).focus)
        val withChapters = onDescription.copy(chapterCount = 3)
        assertEquals(PanelFocus.CHAPTERS, withDescription(withChapters, false).focus)
        assertFalse(withDescription(onDescription, false).descriptionPresent)
    }

    @Test
    fun aDescriptionThatGoesWhileFocusElsewhereOnlyClearsTheFlag() {
        val onUpNext = PlayerUi(PanelFocus.UPNEXT, 0, 9, upNextCount = 4, descriptionPresent = true)
        val next = withDescription(onUpNext, false)
        assertEquals(PanelFocus.UPNEXT, next.focus)
        assertFalse(next.descriptionPresent)
    }
}

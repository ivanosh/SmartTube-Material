package io.github.ivanosh.stmaterial.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelLayoutTest {
    @Test
    fun theHiddenPanelIsLaidOutAsTheOpenOneSoThatOpeningOnlyChangesLayers() {
        assertTrue(panelLayoutOpen(open = false, seekPreview = false))
    }

    @Test
    fun theSeekPreviewAloneCollapsesTheLayoutToTheBar() {
        assertFalse(panelLayoutOpen(open = false, seekPreview = true))
    }

    @Test
    fun theOpenPanelHasItsFullLayoutWhateverThePreview() {
        assertTrue(panelLayoutOpen(open = true, seekPreview = true))
        assertTrue(panelLayoutOpen(open = true, seekPreview = false))
    }
}

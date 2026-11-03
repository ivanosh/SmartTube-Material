package io.github.ivanosh.stmaterial.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePillTest {
    @Test fun idleOffersAnUpdateWithNoRing() {
        assertEquals(PillView(PillLabel.UPDATE, null), pillView(DownloadPhase.IDLE, 0))
    }

    @Test fun downloadingShowsThePercentAndFillsTheRing() {
        assertEquals(PillView(PillLabel.PERCENT, 0.42f), pillView(DownloadPhase.DOWNLOADING, 42))
    }

    @Test fun preparingShowsItsLabelWithTheCopyPercent() {
        assertEquals(PillView(PillLabel.PREPARING, 0.5f), pillView(DownloadPhase.PREPARING, 50))
    }

    @Test fun confirmingShowsInstallWithAFullRing() {
        assertEquals(PillView(PillLabel.INSTALL, 1f), pillView(DownloadPhase.CONFIRMING, 0))
    }

    @Test fun failedOffersRetryWithNoRing() {
        assertEquals(PillView(PillLabel.RETRY, null), pillView(DownloadPhase.FAILED, 73))
    }

    @Test fun ringFractionIsClamped() {
        assertEquals(0f, ringFraction(-5), 0f)
        assertEquals(1f, ringFraction(200), 0f)
        assertEquals(0.07f, ringFraction(7), 0f)
    }

    @Test fun onlyShownWithAnOfferOrAWhileARunIsOn() {
        assertFalse(pillVisible(hasOffer = false, phase = DownloadPhase.IDLE))
        assertTrue(pillVisible(hasOffer = true, phase = DownloadPhase.IDLE))
        // A run keeps the pill even if the badge offer were cleared under it.
        assertTrue(pillVisible(hasOffer = false, phase = DownloadPhase.DOWNLOADING))
        assertTrue(pillVisible(hasOffer = false, phase = DownloadPhase.FAILED))
    }

    @Test fun okStartsWhenIdleOrFailedAndOpensThePanelWhileRunning() {
        assertEquals(PillAction.START, pillAction(DownloadPhase.IDLE))
        assertEquals(PillAction.START, pillAction(DownloadPhase.FAILED))
        assertEquals(PillAction.OPEN_PANEL, pillAction(DownloadPhase.DOWNLOADING))
        assertEquals(PillAction.OPEN_PANEL, pillAction(DownloadPhase.PREPARING))
        assertEquals(PillAction.OPEN_PANEL, pillAction(DownloadPhase.CONFIRMING))
    }
}

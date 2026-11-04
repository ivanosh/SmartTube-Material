package io.github.ivanosh.stmaterial.player

import io.github.ivanosh.stmaterial.logic.PlayerButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerCommandsTest {
    // Rate joins these: it stands for two Leanback buttons at once, so it has no single id of its own.
    private val withoutId = setOf(PlayerButton.PlayPause, PlayerButton.Next, PlayerButton.Previous, PlayerButton.Rate)

    @Test
    fun onlyPlayPauseNextPreviousAndRateHaveNoLeanbackId() {
        for (button in PlayerButton.values()) {
            if (button in withoutId) assertNull(button.name, viewId(button)) else assertNotNull(button.name, viewId(button))
        }
    }

    @Test
    fun everyButtonWithAnIdHasItsOwn() {
        val ids = PlayerButton.values().mapNotNull { viewId(it) }
        assertEquals(ids.size, ids.toSet().size)
    }
}

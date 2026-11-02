package io.github.ivanosh.stmaterial.browse

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import io.github.ivanosh.stmaterial.browse.VideoAction.BLOCK_CHANNEL
import io.github.ivanosh.stmaterial.browse.VideoAction.CHANNEL
import io.github.ivanosh.stmaterial.browse.VideoAction.MARK_WATCHED
import io.github.ivanosh.stmaterial.browse.VideoAction.NOT_INTERESTED
import io.github.ivanosh.stmaterial.browse.VideoAction.PLAYLIST
import io.github.ivanosh.stmaterial.browse.VideoAction.QUEUE
import io.github.ivanosh.stmaterial.browse.VideoAction.REMOVE_FROM_HISTORY
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoMenuTest {
    private val all = VideoFacts(signedIn = true, hasVideo = true, hasChannel = true, hasAuthor = true, hasFeedback = true, hasHistoryToken = true)

    @Test
    fun `history has remove from history instead of not interested`() {
        assertEquals(
            listOf(QUEUE, PLAYLIST, MARK_WATCHED, CHANNEL, BLOCK_CHANNEL, REMOVE_FROM_HISTORY),
            videoMenuActions(MediaGroup.TYPE_HISTORY)
        )
    }

    @Test
    fun `continue watching row is a history row wherever it is`() {
        assertEquals(videoMenuActions(MediaGroup.TYPE_HISTORY), videoMenuActions(MediaGroup.TYPE_SUBSCRIPTIONS, inContinue = true))
        assertEquals(false, NOT_INTERESTED in videoMenuActions(MediaGroup.TYPE_SUBSCRIPTIONS, inContinue = true))
    }

    @Test
    fun `the rest of the subscriptions tab keeps not interested`() {
        assertEquals(
            listOf(QUEUE, PLAYLIST, MARK_WATCHED, CHANNEL, NOT_INTERESTED, BLOCK_CHANNEL),
            videoMenuActions(MediaGroup.TYPE_SUBSCRIPTIONS, inContinue = false)
        )
    }

    @Test
    fun `other sections keep not interested and have no remove from history`() {
        listOf(MediaGroup.TYPE_HOME, MediaGroup.TYPE_SUBSCRIPTIONS, MediaGroup.TYPE_USER_PLAYLISTS, MediaGroup.TYPE_TRENDING).forEach {
            assertEquals(listOf(QUEUE, PLAYLIST, MARK_WATCHED, CHANNEL, NOT_INTERESTED, BLOCK_CHANNEL), videoMenuActions(it))
        }
    }

    @Test
    fun `signed in video with everything keeps the whole list`() {
        assertEquals(videoMenuActions(MediaGroup.TYPE_HISTORY), availableVideoActions(MediaGroup.TYPE_HISTORY, all))
    }

    @Test
    fun `signed out shows neither mark as watched nor remove from history`() {
        listOf(MediaGroup.TYPE_HISTORY, MediaGroup.TYPE_HOME).forEach {
            val shown = availableVideoActions(it, all.copy(signedIn = false))
            assertEquals(false, MARK_WATCHED in shown)
            assertEquals(false, REMOVE_FROM_HISTORY in shown)
            assertEquals(false, NOT_INTERESTED in shown)
        }
    }

    @Test
    fun `signed out hides what needs an account`() {
        assertEquals(
            listOf(QUEUE, CHANNEL, BLOCK_CHANNEL),
            availableVideoActions(MediaGroup.TYPE_HISTORY, all.copy(signedIn = false))
        )
    }

    @Test
    fun `a card that cannot open a channel hides go to channel only`() {
        assertEquals(
            listOf(QUEUE, PLAYLIST, MARK_WATCHED, BLOCK_CHANNEL, REMOVE_FROM_HISTORY),
            availableVideoActions(MediaGroup.TYPE_HISTORY, all.copy(hasChannel = false))
        )
    }

    /** A search result has no channel id, but an author: the channel can be opened and blocked by name. */
    @Test
    fun `no author hides block channel only`() {
        assertEquals(
            listOf(QUEUE, PLAYLIST, MARK_WATCHED, CHANNEL, REMOVE_FROM_HISTORY),
            availableVideoActions(MediaGroup.TYPE_HISTORY, all.copy(hasAuthor = false))
        )
    }

    @Test
    fun `no feedback tokens hides not interested`() {
        assertEquals(
            listOf(QUEUE, PLAYLIST, MARK_WATCHED, CHANNEL, BLOCK_CHANNEL),
            availableVideoActions(MediaGroup.TYPE_HOME, all.copy(hasFeedback = false))
        )
    }

    @Test
    fun `a card that is not a video cannot be queued or saved`() {
        assertEquals(
            listOf(CHANNEL, NOT_INTERESTED, BLOCK_CHANNEL),
            availableVideoActions(MediaGroup.TYPE_HOME, all.copy(hasVideo = false))
        )
    }

    @Test
    fun `mark as watched needs a video`() {
        assertEquals(false, MARK_WATCHED in availableVideoActions(MediaGroup.TYPE_HOME, all.copy(hasVideo = false)))
    }

    @Test
    fun removeFromHistoryIsHiddenWithoutTheFeedbackTokenSinceUpstreamWouldReportSuccessWithNoCall() {
        val shown = availableVideoActions(MediaGroup.TYPE_HISTORY, all.copy(hasHistoryToken = false))
        assertEquals(false, REMOVE_FROM_HISTORY in shown)
        assertEquals(false, REMOVE_FROM_HISTORY in availableVideoActions(MediaGroup.TYPE_SUBSCRIPTIONS, all.copy(hasHistoryToken = false), inContinue = true))
    }
}

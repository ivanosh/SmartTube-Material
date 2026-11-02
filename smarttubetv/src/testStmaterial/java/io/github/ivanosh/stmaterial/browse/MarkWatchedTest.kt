package io.github.ivanosh.stmaterial.browse

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkWatchedTest {
    private val home = BrowseSection(MediaGroup.TYPE_HOME, "Home", BrowseSection.TYPE_ROW, -1)
    private val subs = BrowseSection(MediaGroup.TYPE_SUBSCRIPTIONS, "Subs", BrowseSection.TYPE_GRID, -1)

    private fun video(id: String, pct: Float = -1f) = Video().apply { videoId = id; percentWatched = pct }

    private fun state() = BrowseState(clock = { 0L }, rowTitle = { "t$it" }, authorOf = { it.author }).apply {
        sections.addAll(listOf(subs, home))
        selectTab(subs.id)
        updateSection(VideoGroup.from(listOf(video("s1"), video("h1", 40f), video("h2", 60f)), subs, 0).apply { action = VideoGroup.ACTION_APPEND })
        updateHistory(VideoGroup.from(listOf(video("h1", 40f), video("h2", 60f)), null, 0), epoch)
    }

    private fun BrowseState.ids(id: String) = rows.firstOrNull { it.id == id }?.videos?.map { it.videoId }

    @Test
    fun leavesContinueButStaysInNewWithAFullBar() {
        val state = state()
        val before = state.rows.first { it.id == "new" }.revision
        state.markWatched(video("h1", 40f))
        assertEquals(listOf("h2"), state.ids("continue"))
        assertEquals(listOf("s1", "h1", "h2"), state.ids("new"))
        assertEquals(100f, state.rows.first { it.id == "new" }.videos.first { it.videoId == "h1" }.percentWatched)
        assertEquals(true, state.rows.first { it.id == "new" }.revision > before)
    }

    @Test
    fun focusMovesToTheNeighbourWhenTheFocusedCardLeavesContinue() {
        val state = state()
        state.onCardFocused(FocusKey("continue", "h1"), 0)
        state.markWatched(video("h1"))
        assertEquals(FocusKey("continue", "h2"), state.pendingFocus)
    }

    @Test
    fun focusStaysWhenTheCardWasFocusedInNew() {
        val state = state()
        state.onCardFocused(FocusKey("new", "h1"), 1)
        state.markWatched(video("h1"))
        assertNull(state.pendingFocus)
    }

    @Test
    fun anEmptiedContinueRowGoesAndFocusFallsToNew() {
        val state = state()
        state.markWatched(video("h1"))
        state.onCardFocused(FocusKey("continue", "h2"), 0)
        state.markWatched(video("h2"))
        assertNull(state.ids("continue"))
        assertEquals("new", state.pendingFocus?.rowId)
    }

    @Test
    fun otherTabsJustShowTheFullBar() {
        val state = BrowseState(clock = { 0L }, rowTitle = { "" }).apply {
            sections.add(home)
            selectTab(home.id)
            updateSection(VideoGroup.from(listOf(video("a", 10f)), home, 0))
        }
        val row = state.rows.first()
        val before = row.revision
        state.markWatched(video("a"))
        assertEquals(100f, row.videos.first().percentWatched)
        assertEquals(true, row.revision > before)
    }
}

package io.github.ivanosh.stmaterial.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.liskovsoft.smartyoutubetv2.tv.R
import io.github.ivanosh.stmaterial.logic.PanelFocus
import io.github.ivanosh.stmaterial.logic.descriptionRowShown
import io.github.ivanosh.stmaterial.ui.Palette
import io.github.ivanosh.stmaterial.ui.focusRing

private val PreviewShape = RoundedCornerShape(16.dp)
private val PreviewTopGap = 24.dp
private val labelStyle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium, color = Palette.Accent)
private val previewStyle = TextStyle(fontSize = 25.sp, lineHeight = 34.sp, color = Palette.OnBackgroundVariant)
private const val PREVIEW_LINES = 2

private val SheetFill = Palette.Tile
private val SheetShape = RoundedCornerShape(28.dp)
private val sheetTitleStyle = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Medium, color = Palette.OnBackground)
private val sheetBodyStyle = TextStyle(fontSize = 28.sp, lineHeight = 42.sp, color = Palette.OnBackground)
/** One Down or Up press scrolls the open description by this many layout units. */
private val ScrollStep = 320.dp

/**
 * The description preview between the chapter row and the Up next row: a label and the first lines of the description,
 * focusable. It gives way (collapses) to the Up next row, so a long description never pushes the recommendations out of
 * reach; OK opens the full, scrollable text in [DescriptionSheet]. Composed only when the video has a description and the
 * panel is open. The focus is read in composition, which is a user action, not a tick, so this does not recompose per frame.
 */
@Composable
fun DescriptionPreview(state: PlayerOverlayState) {
    if (state.description.isBlank() || !state.layoutOpen) return
    val level = animateFloatAsState(if (descriptionRowShown(state.focus)) 1f else 0f, tween(150), label = "descriptionPreview")
    val focused = state.focus == PanelFocus.DESCRIPTION
    Box(
        Modifier
            .descriptionSlot(level)
            .padding(top = PreviewTopGap)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .focusRing(focused, corner = 16.dp, gap = 3.dp)
                .background(if (focused) Palette.TabSelected else Palette.Tile, PreviewShape)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.material_player_description), style = labelStyle)
            Text(state.description, style = previewStyle, maxLines = PREVIEW_LINES, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The slot of the description preview in the panel's column: it animates its height with [level] (0 collapsed, 1 shown),
 * read in layout so a change composes nothing, and clips so the collapsing text draws nothing over the row below it.
 */
private val RingRoom = 8.dp

private fun Modifier.descriptionSlot(level: State<Float>): Modifier = this
    .drawWithContent {
        // Room each side for the focus ring, which draws outside the card; the collapsing height is clipped so the
        // preview draws nothing over the Up next row as it gives way.
        val room = RingRoom.toPx()
        clipRect(-room, -room, size.width + room, size.height + room) { this@drawWithContent.drawContent() }
    }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = (placeable.height * level.value).toInt()
        layout(placeable.width, height) { placeable.place(0, 0) }
    }

/**
 * The full description over the panel: an opaque card (no scrim over the video), scrolled by the Activity through
 * [PlayerOverlayState.descriptionScroll]. Composed only while open. Back or OK closes it; the panel is unchanged behind.
 */
@Composable
fun BoxScope.DescriptionSheet(state: PlayerOverlayState) {
    if (!state.descriptionOpen) return
    val scroll = rememberScrollState()
    val stepPx = with(LocalDensity.current) { ScrollStep.roundToPx() }
    // The target comes from the Activity's step count; animateScrollTo clamps it to the content, so an over-press rests at the bottom.
    LaunchedEffect(state.descriptionScroll) { scroll.animateScrollTo(state.descriptionScroll * stepPx) }
    Column(
        Modifier
            .align(Alignment.Center)
            .width(1180.dp)
            .heightIn(max = 760.dp)
            .background(SheetFill, SheetShape)
            .padding(start = 44.dp, end = 44.dp, top = 36.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val heading = remember(state.title) { state.title.ifBlank { null } }
        Text(heading ?: stringResource(R.string.material_player_description), style = sheetTitleStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        // weight so the body fills the rest of the capped card and scrolls inside it, instead of overflowing.
        Text(
            state.description,
            style = sheetBodyStyle,
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(scroll),
        )
    }
}

package io.github.ivanosh.stmaterial.ui

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.liskovsoft.smartyoutubetv2.tv.R
import io.github.ivanosh.stmaterial.logic.UpdateOffer
import io.github.ivanosh.stmaterial.update.UpdateDownload

private val TitleStyle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Medium)
private val BodyStyle = TextStyle(fontSize = 26.sp, lineHeight = 34.sp)
private const val PANEL_SCRIM_ALPHA = 0.62f

/**
 * The panel of a running download, opened by OK on the pill: the version, its notes, a progress bar, and the two
 * ways out. Back and Hide both leave the download running; Cancel stops it. Nothing else on the screen is reachable
 * while it is up.
 */
@Composable
fun UpdatePanel(offer: UpdateOffer, onHide: () -> Unit, onCancel: () -> Unit) {
    PanelFrame(onBack = onHide) {
        Text(offer.versionName.let { "SmartTube Material $it" }, style = TitleStyle)
        if (offer.notes.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                offer.notes.forEach { Text(it, style = BodyStyle.copy(color = Palette.OnBackgroundVariant)) }
            }
        }
        // The bar is read in its draw, so a percent repaints it and does not recompose the panel.
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(Palette.ProgressTrack, cornerRadius = radius)
            val fraction = UpdateDownload.percent.coerceIn(0, 100) / 100f
            if (fraction > 0f) drawRoundRect(Palette.Accent, size = Size(size.width * fraction, size.height), cornerRadius = radius)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelButton(stringResource(R.string.material_update_panel_hide), focusFirst = true, modifier = Modifier.weight(1f), onClick = onHide)
            PanelButton(stringResource(R.string.material_update_panel_cancel), focusFirst = false, modifier = Modifier.weight(1f), onClick = onCancel)
        }
    }
}

/**
 * The panel shown when the app may not install packages yet: a line of why, and the way to the system screen that
 * grants it. Back or Close leaves it; the user starts the update again once the permission is on.
 */
@Composable
fun InstallPermissionPanel(title: String, message: String, openLabel: String, closeLabel: String, onOpen: () -> Unit, onClose: () -> Unit) {
    PanelFrame(onBack = onClose) {
        Text(title, style = TitleStyle)
        Text(message, style = BodyStyle.copy(color = Palette.OnBackgroundVariant))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelButton(openLabel, focusFirst = true, modifier = Modifier.weight(1f), onClick = onOpen)
            PanelButton(closeLabel, focusFirst = false, modifier = Modifier.weight(1f), onClick = onClose)
        }
    }
}

/**
 * The card and scrim shared by the update panels, with the menu's key rules: the first button has focus, the OK that
 * opened the panel is ignored until the key is pressed again, and Back leaves. Nothing to the sides is reachable.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PanelFrame(onBack: () -> Unit, content: @Composable () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    var backPressed by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                when {
                    native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || native.keyCode == KeyEvent.KEYCODE_ENTER -> {
                        if (event.type == KeyEventType.KeyDown && native.repeatCount == 0 && !native.isLongPress) armed = true
                        !armed || (event.type == KeyEventType.KeyDown && native.repeatCount > 0)
                    }
                    event.key == Key.Back -> {
                        when (event.type) {
                            KeyEventType.KeyDown -> backPressed = true
                            KeyEventType.KeyUp -> if (backPressed) {
                                backPressed = false
                                onBack()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        Box(Modifier.fillMaxSize().background(Palette.Background.copy(alpha = PANEL_SCRIM_ALPHA)))
        Column(
            Modifier
                .align(Alignment.Center)
                .width(900.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Palette.Tile)
                .padding(40.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            content = { content() },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PanelButton(label: String, focusFirst: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val first = remember { FocusRequester() }
    if (focusFirst) LaunchedEffect(Unit) {
        withFrameNanos { }
        first.requestFocus()
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(76.dp)
            .then(if (focusFirst) Modifier.focusRequester(first) else Modifier),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.TabSelected,
            contentColor = Palette.OnBackground,
            focusedContainerColor = Palette.OnBackground,
            focusedContentColor = Palette.Background,
            pressedContainerColor = Palette.OnBackground,
            pressedContentColor = Palette.Background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium))
        }
    }
}

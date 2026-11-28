package com.udyne.notecash

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/* ---------- Tombol liquid ---------- */

/**
 * Pengganti Surface(onClick) untuk tombol. Di tema lain jadi Surface biasa.
 * Di Liquid Glass: kaca dengan bias lensa, diwarnai [color], dan membesar seperti cairan saat ditekan.
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = CircleShape,
    color: Color = Card2,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val backdrop = LocalBackdrop.current
    if (backdrop == null) {
        Surface(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, color = color, border = border, content = content)
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 380f), label = "liquid_press")
    val fx = Build.VERSION.SDK_INT >= 31
    val lensOk = Build.VERSION.SDK_INT >= 33
    val tint = if (color.alpha >= 0.99f) color.copy(alpha = 0.70f) else color
    Box(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    if (fx) {
                        vibrancy()
                        blur(4f.dp.toPx())
                        if (lensOk) lens(6f.dp.toPx(), 12f.dp.toPx())
                    }
                },
                layerBlock = {
                    val s = 1f + 0.06f * press
                    scaleX = s
                    scaleY = s
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = if (fx) 0.08f else 0.18f))
                    drawRect(tint)
                    if (press > 0f) drawRect(Color.White.copy(alpha = 0.12f * press))
                }
            )
            .border(1.dp, GlassEdge, shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.graphicsLayer { val s = 1f + 0.04f * press; scaleX = s; scaleY = s }, Alignment.Center) { content() }
    }
}

/* ---------- Switch gaya iOS ---------- */

@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val backdrop = LocalBackdrop.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val on by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.65f, stiffness = 420f), label = "ios_on")
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "ios_press")
    val fx = Build.VERSION.SDK_INT >= 31
    val lensOk = Build.VERSION.SDK_INT >= 33
    val trackW = 54f
    val trackH = 32f
    val inset = 2f
    val offColor = if (backdrop != null) Color.White.copy(alpha = 0.22f) else Card2
    val trackColor = androidx.compose.ui.graphics.lerp(offColor, Color(0xFF34C759), on)
    Box(
        Modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .size(trackW.dp, trackH.dp)
            .clip(CircleShape)
            .background(trackColor)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, interactionSource = interaction, indication = null, onValueChange = onCheckedChange)
    ) {
        val thumbW = 28f + 8f * press
        val thumbMod = Modifier
            .offset { IntOffset(lerp(inset, trackW - inset - thumbW, on).dp.roundToPx(), inset.dp.roundToPx()) }
            .size(thumbW.dp, (trackH - 2 * inset).dp)
        if (backdrop != null) {
            Box(
                thumbMod.drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        if (fx) {
                            vibrancy()
                            blur(2f.dp.toPx())
                            if (lensOk) lens(6f.dp.toPx(), 10f.dp.toPx())
                        }
                    },
                    // Diam: putih pekat. Ditekan: kaca bening yang membiaskan latar.
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - 0.62f * press)) }
                )
            )
        } else {
            Box(thumbMod.shadow(2.dp, CircleShape).background(Color.White, CircleShape))
        }
    }
}

/* ---------- Selector dengan indikator geser ---------- */

/**
 * Baris pilihan dengan indikator yang bergeser. Bisa diketuk atau digeser dengan jari.
 * Di Liquid Glass indikatornya kaca yang membesar saat ditekan.
 */
@Composable
fun SlidingSelector(
    count: Int, sel: Int, onSel: (Int) -> Unit,
    modifier: Modifier = Modifier, pad: Dp = 4.dp,
    item: @Composable BoxScope.(Int, Boolean) -> Unit
) {
    val backdrop = LocalBackdrop.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0f) }
    var dragX by remember { mutableStateOf<Float?>(null) }
    val press = remember { Animatable(0f) }
    val padPx = with(density) { pad.toPx() }
    val slot = if (count > 0) ((widthPx - 2 * padPx) / count).coerceAtLeast(0f) else 0f
    val dragging = dragX
    val target = if (dragging != null) (dragging - slot / 2).coerceIn(padPx, padPx + slot * (count - 1)) else padPx + slot * sel
    val x by animateFloatAsState(
        target,
        if (dragging != null) snap<Float>() else spring<Float>(dampingRatio = 0.75f, stiffness = 380f),
        label = "selector_x"
    )
    val active = if (dragging != null && slot > 0f) ((dragging - padPx) / slot).toInt().coerceIn(0, count - 1) else sel
    val fx = Build.VERSION.SDK_INT >= 31
    val lensOk = Build.VERSION.SDK_INT >= 33
    val updatedOnSel by rememberUpdatedState(onSel)

    Box(
        modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(count, slot) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    scope.launch { press.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
                    var lastX = down.position.x
                    dragX = lastX
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        lastX = ch.position.x
                        dragX = lastX
                        if (!ch.pressed) break
                        ch.consume()
                    }
                    val idx = if (slot > 0f) ((lastX - padPx) / slot).toInt().coerceIn(0, count - 1) else sel
                    dragX = null
                    scope.launch { press.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
                    updatedOnSel(idx)
                }
            }
    ) {
        val indicatorBase = Modifier
            .padding(vertical = pad)
            .offset { IntOffset(x.roundToInt(), 0) }
            .width(with(density) { slot.toDp() })
            .fillMaxHeight()
        if (backdrop != null) {
            Box(
                indicatorBase
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            if (fx) {
                                vibrancy()
                                blur(3f.dp.toPx())
                                if (lensOk) lens(6f.dp.toPx(), 12f.dp.toPx())
                            }
                        },
                        layerBlock = {
                            val s = 1f + 0.10f * press.value
                            scaleX = s
                            scaleY = s
                        },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.16f + 0.10f * press.value)) }
                    )
                    .border(1.dp, GlassEdge, CircleShape)
            )
        } else {
            Box(indicatorBase.clip(CircleShape).background(BG))
        }
        Row(Modifier.fillMaxSize().padding(horizontal = pad, vertical = pad)) {
            repeat(count) { i ->
                Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) { item(i, i == active) }
            }
        }
    }
}

@Composable
fun GlassTabBar(tab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit, modifier: Modifier) {
    val tabs = listOf(
        Triple(Icons.Rounded.Home, "Beranda", 0),
        Triple(Icons.Rounded.BarChart, "Laporan", 1),
        Triple(Icons.Rounded.Flag, "Target", 2),
        Triple(Icons.Rounded.Settings, "Pengaturan", 3)
    )
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f, fill = false).widthIn(max = 264.dp).height(64.dp).glass(CircleShape, 32f)) {
            SlidingSelector(count = tabs.size, sel = tab, onSel = onTab, modifier = Modifier.fillMaxSize(), pad = 6.dp) { i, active ->
                val (icon, label, _) = tabs[i]
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(icon, label, tint = if (active) White else Muted, modifier = Modifier.size(22.dp))
                    Text(label, color = if (active) White else Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        LiquidButton(onClick = onAdd, modifier = Modifier.size(64.dp), color = Lime) {
            Icon(Icons.Rounded.Add, "Catat", tint = OnLime, modifier = Modifier.size(28.dp))
        }
    }
}


/* ---------- Dialog kaca ---------- */

/** Tempat dialog kaca digambar. Berada di akar layar supaya menutupi seluruh jendela, termasuk bottom bar. */
class DialogHostState {
    var token: Any? by mutableStateOf(null)
    var body: (@Composable () -> Unit)? by mutableStateOf(null)
    fun show(t: Any, b: @Composable () -> Unit) { token = t; body = b }
    fun hide(t: Any) { if (token === t) { token = null; body = null } }
}

val LocalDialogHost = staticCompositionLocalOf<DialogHostState?> { null }

@Composable
fun DialogHost(host: DialogHostState) {
    host.body?.invoke()
}

/**
 * Dialog umum. Liquid Glass: kartu kaca di dalam jendela utama (bisa membiaskan latar).
 * Tema lain: dialog bawaan Android dengan kartu solid.
 */
@Composable
fun AppDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val host = LocalDialogHost.current
    if (LocalBackdrop.current != null && host != null) {
        val token = remember { Any() }
        val dismiss by rememberUpdatedState(onDismiss)
        SideEffect { host.show(token) { GlassDialogFrame({ dismiss() }, content) } }
        DisposableEffect(token) { onDispose { host.hide(token) } }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(28.dp), color = CardC) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), content = content)
            }
        }
    }
}

@Composable
fun GlassDialogFrame(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .systemBarsPadding()
            .imePadding(),
        Alignment.Center
    ) {
        AnimatedVisibility(visible = shown, enter = fadeIn() + scaleIn(initialScale = 0.92f)) {
            Column(
                Modifier
                    .padding(24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(28.dp), 28f)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                content = content
            )
        }
    }
}

/* ---------- Tepi scroll ---------- */

/** Konten memudar lembut di tepi atas dan bawah saat digulir. Hanya aktif di Liquid Glass. */
fun Modifier.scrollEdges(): Modifier = if (!CurPal.glass) this else this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val edge = 36.dp.toPx().coerceAtMost(size.height / 3f)
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = edge),
            size = Size(size.width, edge),
            blendMode = BlendMode.DstIn
        )
        drawRect(
            Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - edge, endY = size.height),
            topLeft = Offset(0f, size.height - edge),
            size = Size(size.width, edge),
            blendMode = BlendMode.DstIn
        )
    }

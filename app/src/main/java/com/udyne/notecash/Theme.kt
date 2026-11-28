package com.udyne.notecash

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CornerBasedShape
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

const val THEME_DARK = 0
const val THEME_LIGHT = 1
const val THEME_AMOLED = 2
const val THEME_GLASS = 3
const val THEME_SYSTEM = 4

val THEME_NAMES = listOf("Gelap", "Terang", "AMOLED hitam", "Liquid Glass", "Ikuti sistem")
val THEME_HINTS = listOf(
    "Biru gelap, tema bawaan",
    "Latar terang untuk siang hari",
    "Hitam pekat, hemat baterai di layar AMOLED",
    "Kaca bening dengan efek bias ala iOS. Efek penuh di Android 13 ke atas",
    "Gelap atau terang mengikuti pengaturan Android"
)

/** Satu set warna lengkap untuk sebuah tema. */
class Pal(
    val bg: Color, val card: Color, val card2: Color,
    val blue: Color, val lime: Color, val green: Color, val red: Color,
    val muted: Color, val text: Color, val onLime: Color,
    val dark: Boolean, val glass: Boolean = false
)

val PalDark = Pal(
    bg = Color(0xFF0D1018), card = Color(0xFF1A1E2A), card2 = Color(0xFF262B3A),
    blue = Color(0xFF0A9BF0), lime = Color(0xFFD4F34A), green = Color(0xFF2ED573), red = Color(0xFFFF4D4F),
    muted = Color(0xFF9AA1B2), text = Color(0xFFFFFFFF), onLime = Color(0xFF0D1018), dark = true
)
val PalAmoled = Pal(
    bg = Color(0xFF000000), card = Color(0xFF0C0D12), card2 = Color(0xFF1B1D26),
    blue = Color(0xFF0A9BF0), lime = Color(0xFFD4F34A), green = Color(0xFF2ED573), red = Color(0xFFFF4D4F),
    muted = Color(0xFF8C93A5), text = Color(0xFFFFFFFF), onLime = Color(0xFF000000), dark = true
)
val PalLight = Pal(
    bg = Color(0xFFF2F4F9), card = Color(0xFFFFFFFF), card2 = Color(0xFFE5E9F2),
    blue = Color(0xFF0A7FD0), lime = Color(0xFFC4E73A), green = Color(0xFF16A34A), red = Color(0xFFE5383B),
    muted = Color(0xFF677085), text = Color(0xFF111522), onLime = Color(0xFF111522), dark = false
)
val PalGlass = Pal(
    bg = Color(0xFF0A1030), card = Color(0xF2182340), card2 = Color(0x33FFFFFF),
    blue = Color(0xFF3FB0FF), lime = Color(0xFFD4F34A), green = Color(0xFF3EE08F), red = Color(0xFFFF6B6E),
    muted = Color(0xFFB9C3E3), text = Color(0xFFFFFFFF), onLime = Color(0xFF0A1030), dark = true, glass = true
)

/** Tema aktif. Dibaca di dalam composable, jadi semua layar ikut berubah saat nilainya diganti. */
object ThemeState {
    var id by mutableStateOf(THEME_DARK)
    var systemDark by mutableStateOf(true)
}

fun palFor(id: Int, systemDark: Boolean): Pal = when (id) {
    THEME_LIGHT -> PalLight
    THEME_AMOLED -> PalAmoled
    THEME_GLASS -> PalGlass
    THEME_SYSTEM -> if (systemDark) PalDark else PalLight
    else -> PalDark
}

val CurPal: Pal get() = palFor(ThemeState.id, ThemeState.systemDark)

val BG: Color get() = CurPal.bg
val CardC: Color get() = CurPal.card
val Card2: Color get() = CurPal.card2
val Blue: Color get() = CurPal.blue
val Lime: Color get() = CurPal.lime
val Green: Color get() = CurPal.green
val Red: Color get() = CurPal.red
val Muted: Color get() = CurPal.muted
val White: Color get() = CurPal.text
val OnLime: Color get() = CurPal.onLime
/** Teks di atas tombol atau kartu biru selalu putih, apa pun temanya. */
val OnBlue: Color = Color.White
val GlassEdge: Color = Color.White.copy(alpha = 0.22f)

fun palScheme(p: Pal): ColorScheme =
    if (p.dark) darkColorScheme(
        primary = p.blue, onPrimary = OnBlue, primaryContainer = Color(0xFF0A5E94), onPrimaryContainer = Color.White,
        secondary = p.lime, onSecondary = p.onLime, secondaryContainer = Color(0xFF14405E), onSecondaryContainer = Color.White,
        background = p.bg, onBackground = p.text, surface = p.bg, onSurface = p.text,
        surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceVariant = p.card2, onSurfaceVariant = p.muted,
        outline = Color(0xFF3A4054), error = p.red
    ) else lightColorScheme(
        primary = p.blue, onPrimary = OnBlue, primaryContainer = Color(0xFFCFE8FA), onPrimaryContainer = p.text,
        secondary = p.lime, onSecondary = p.onLime, secondaryContainer = Color(0xFFE3F0C0), onSecondaryContainer = p.text,
        background = p.bg, onBackground = p.text, surface = p.bg, onSurface = p.text,
        surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceVariant = p.card2, onSurfaceVariant = p.muted,
        outline = Color(0xFFC3C9D8), error = p.red
    )

/* ---------- Liquid glass ---------- */

/** Backdrop yang disampel kartu kaca. Null berarti tema aktif bukan Liquid Glass. */
val LocalBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/** Latar berwarna (gradasi dan gumpalan cahaya) yang dibiaskan oleh kartu kaca. */
fun DrawScope.drawGlassBackdrop() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0A1030), Color(0xFF1A1048), Color(0xFF082B45))))
    val m = maxOf(size.width, size.height)
    fun blob(cx: Float, cy: Float, r: Float, c: Color) {
        val center = Offset(cx, cy)
        drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center, r), r, center)
    }
    blob(size.width * 0.15f, size.height * 0.12f, m * 0.55f, Color(0xAA2F9BFF))
    blob(size.width * 0.95f, size.height * 0.35f, m * 0.50f, Color(0x99B14DFF))
    blob(size.width * 0.10f, size.height * 0.62f, m * 0.45f, Color(0x8800D1B2))
    blob(size.width * 0.85f, size.height * 0.88f, m * 0.55f, Color(0x88FF5CAD))
}

/** Latar layar penuh: warna polos untuk tema biasa, gradasi kaca untuk Liquid Glass. */
fun Modifier.themeBg(): Modifier = if (CurPal.glass) drawBehind { drawGlassBackdrop() } else background(BG)

/**
 * Menjadikan komponen kaca: blur + vibrancy (Android 12+), bias lensa (Android 13+) dan tepi bercahaya.
 * Tanpa backdrop (tema lain) modifier ini tidak melakukan apa-apa.
 * [radiusDp] harus sama dengan radius sudut bentuknya.
 */
@Composable
fun Modifier.glass(shape: CornerBasedShape, radiusDp: Float, useLens: Boolean = true): Modifier {
    val backdrop = LocalBackdrop.current ?: return this
    val fx = Build.VERSION.SDK_INT >= 31
    val lensOk = useLens && Build.VERSION.SDK_INT >= 33
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (fx) {
                    vibrancy()
                    blur(6f.dp.toPx())
                    if (lensOk) lens((radiusDp * 0.5f).dp.toPx(), (radiusDp * 1.0f).dp.toPx())
                }
            },
            onDrawSurface = { drawRect(Color.White.copy(alpha = if (fx) 0.10f else 0.22f)) }
        )
        .border(1.dp, GlassEdge, shape)
}

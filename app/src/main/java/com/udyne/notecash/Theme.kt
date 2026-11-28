package com.udyne.notecash

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

const val THEME_DARK = 0
const val THEME_LIGHT = 1
const val THEME_AMOLED = 2
const val THEME_SYSTEM = 4

/** Urutan saat tombol tema ditekan: auto, siang, malam, amoled. */
val THEME_CYCLE = listOf(THEME_SYSTEM, THEME_LIGHT, THEME_DARK, THEME_AMOLED)

fun themeLabel(id: Int): String = when (id) {
    THEME_LIGHT -> "Siang"
    THEME_DARK -> "Malam"
    THEME_AMOLED -> "Amoled"
    else -> "Auto"
}

fun nextTheme(id: Int): Int {
    val i = THEME_CYCLE.indexOf(if (id in THEME_CYCLE) id else THEME_DARK)
    return THEME_CYCLE[(i + 1) % THEME_CYCLE.size]
}

/** Satu set warna lengkap untuk sebuah tema. */
class Pal(
    val bg: Color, val card: Color, val card2: Color,
    val blue: Color, val lime: Color, val green: Color, val red: Color,
    val muted: Color, val text: Color, val onLime: Color,
    val dark: Boolean
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

/** Tema aktif. Dibaca di dalam composable, jadi semua layar ikut berubah saat nilainya diganti. */
object ThemeState {
    var id by mutableStateOf(THEME_SYSTEM)
    var systemDark by mutableStateOf(true)
}

fun palFor(id: Int, systemDark: Boolean): Pal = when (id) {
    THEME_LIGHT -> PalLight
    THEME_AMOLED -> PalAmoled
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

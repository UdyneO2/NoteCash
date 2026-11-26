@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.udyne.notecash

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ---------- Palet (Orbix dark + blue + lime) ---------- */
val BG = Color(0xFF0D1018)
val CardC = Color(0xFF1A1E2A)
val Card2 = Color(0xFF262B3A)
val Blue = Color(0xFF0A9BF0)
val Lime = Color(0xFFD4F34A)
val Green = Color(0xFF2ED573)
val Red = Color(0xFFFF4D4F)
val Muted = Color(0xFF9AA1B2)
val White = Color(0xFFFFFFFF)

val AppFont: FontFamily = FontFamily.SansSerif

fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge.copy(fontFamily = f), displayMedium.copy(fontFamily = f), displaySmall.copy(fontFamily = f),
    headlineLarge.copy(fontFamily = f), headlineMedium.copy(fontFamily = f), headlineSmall.copy(fontFamily = f),
    titleLarge.copy(fontFamily = f), titleMedium.copy(fontFamily = f), titleSmall.copy(fontFamily = f),
    bodyLarge.copy(fontFamily = f), bodyMedium.copy(fontFamily = f), bodySmall.copy(fontFamily = f),
    labelLarge.copy(fontFamily = f), labelMedium.copy(fontFamily = f), labelSmall.copy(fontFamily = f)
)

const val DEFAULT_SHEET_URL = "https://s.id/notecash"
val USER_RE = Regex("^[a-z0-9_]{2,30}$")
val OUT = listOf("Makan", "Transport", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Lainnya", "Target")
val IN = listOf("Gaji", "Bonus", "Usaha", "Hadiah", "Lainnya")
val AVATARS = listOf("Inisial", "Laki-laki", "Perempuan", "Kacamata", "Anak")
val ID = Locale("id", "ID")

data class Tx(val id: Long, val d: String, val income: Boolean, val c: String, val a: Long, val n: String)
data class Goal(val id: Long, val title: String, val target: Long, val saved: Long, val deadline: String)
data class SyncBundle(val txs: List<Tx>, val goals: List<Goal>)
class Sum(val inc: Long, val exp: Long)

fun rp(n: Long) = (if (n < 0) "-" else "") + "Rp" + "%,d".format(ID, Math.abs(n))
fun totals(l: List<Tx>) = Sum(l.filter { it.income }.sumOf { it.a }, l.filter { !it.income }.sumOf { it.a })
fun within(all: List<Tx>, from: LocalDate, to: LocalDate) = all.filter { it.d >= from.toString() && it.d <= to.toString() }
fun scoreOf(s: Sum): Int = if (s.inc <= 0) 0 else ((s.inc - s.exp) * 100 / s.inc).coerceIn(0L, 100L).toInt()
fun scoreLabel(sc: Int, has: Boolean) = when {
    !has -> "Belum ada data"
    sc >= 80 -> "Sangat baik"
    sc >= 50 -> "Baik"
    sc >= 25 -> "Cukup"
    else -> "Perlu dijaga"
}

fun rangeOf(mode: Int, a: LocalDate): Pair<LocalDate, LocalDate> = when (mode) {
    0 -> { val s = a.with(DayOfWeek.MONDAY); s to s.plusDays(6) }
    1 -> { val s = a.withDayOfMonth(1); s to s.plusMonths(1).minusDays(1) }
    else -> LocalDate.of(a.year, 1, 1) to LocalDate.of(a.year, 12, 31)
}

fun shiftOf(mode: Int, a: LocalDate, n: Long): LocalDate = when (mode) {
    0 -> a.plusWeeks(n)
    1 -> a.withDayOfMonth(1).plusMonths(n)
    else -> a.withDayOfYear(1).plusYears(n)
}

fun labelOf(mode: Int, from: LocalDate, to: LocalDate): String = when (mode) {
    0 -> from.format(DateTimeFormatter.ofPattern("d MMM", ID)) + " – " + to.format(DateTimeFormatter.ofPattern("d MMM yyyy", ID))
    1 -> from.format(DateTimeFormatter.ofPattern("MMMM yyyy", ID))
    else -> from.year.toString()
}

fun formatIdDate(raw: String, pattern: String = "EEEE, d MMMM"): String =
    runCatching { LocalDate.parse(raw).format(DateTimeFormatter.ofPattern(pattern, ID)) }.getOrDefault(raw)

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("catatan", 0)
    var url: String
        get() = p.getString("url", "")?.takeIf { it.isNotBlank() } ?: DEFAULT_SHEET_URL
        set(v) { p.edit().putString("url", v).apply() }
    var last: String?
        get() = p.getString("last", null)
        set(v) { p.edit().putString("last", v).apply() }
    var dev: Boolean
        get() = p.getBoolean("dev", false)
        set(v) { p.edit().putBoolean("dev", v).apply() }
    var user: String?
        get() = p.getString("user", null)
        set(v) { p.edit().putString("user", v).apply() }

    fun avatarOf(u: String): Int = p.getInt("av_$u", 0).coerceIn(0, AVATARS.lastIndex)
    fun setAvatar(u: String, idx: Int) { p.edit().putInt("av_$u", idx.coerceIn(0, AVATARS.lastIndex)).apply() }

    fun load(u: String): List<Tx> = runCatching {
        val j = JSONArray(p.getString("tx_$u", "[]"))
        (0 until j.length()).map {
            val o = j.getJSONObject(it)
            Tx(o.getLong("id"), o.getString("d"), o.getBoolean("i"), o.getString("c"), o.getLong("a"), o.optString("n"))
        }
    }.getOrDefault(emptyList())

    fun save(u: String, l: List<Tx>) {
        val j = JSONArray()
        l.forEach { j.put(JSONObject().put("id", it.id).put("d", it.d).put("i", it.income).put("c", it.c).put("a", it.a).put("n", it.n)) }
        p.edit().putString("tx_$u", j.toString()).apply()
    }

    fun loadGoals(u: String): List<Goal> = runCatching {
        val j = JSONArray(p.getString("goal_$u", "[]"))
        (0 until j.length()).map {
            val o = j.getJSONObject(it)
            Goal(o.getLong("id"), o.getString("t"), o.getLong("tg"), o.getLong("sv"), o.optString("dl"))
        }
    }.getOrDefault(emptyList())

    fun saveGoals(u: String, l: List<Goal>) {
        val j = JSONArray()
        l.forEach { j.put(JSONObject().put("id", it.id).put("t", it.title).put("tg", it.target).put("sv", it.saved).put("dl", it.deadline)) }
        p.edit().putString("goal_$u", j.toString()).apply()
    }
}

/** Sinkron otomatis ke Google Sheets lewat Web App Apps Script. Satu tab per username. */
object Sheet {
    private fun resolve(raw: String): String {
        var cur = if (raw.trim().startsWith("http")) raw.trim() else "https://" + raw.trim()
        repeat(5) {
            val host = URL(cur).host
            if (host.endsWith("google.com") || host.endsWith("googleusercontent.com")) return cur
            val c = (URL(cur).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 10000
                readTimeout = 10000
            }
            val code = c.responseCode
            val loc = c.getHeaderField("Location")
            c.disconnect()
            if (code in 300..399 && loc != null) cur = URI(cur).resolve(loc).toString() else return cur
        }
        return cur
    }

    fun pushAll(url: String, user: String, txs: List<Tx>, goals: List<Goal>) {
        val rows = JSONArray()
        txs.sortedBy { it.d }.forEach {
            rows.put(JSONArray().put(it.id.toString()).put(it.d).put(if (it.income) "Pemasukan" else "Pengeluaran").put(it.c).put(it.a).put(it.n))
        }
        goals.forEach {
            rows.put(JSONArray().put(it.id.toString()).put(it.deadline).put("Target").put(it.title).put(it.target).put(it.saved.toString()))
        }
        val c = (URL(resolve(url)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "text/plain;charset=utf-8")
        }
        c.outputStream.use { it.write(JSONObject().put("user", user).put("rows", rows).toString().toByteArray()) }
        c.inputStream.close()
        c.disconnect()
    }

    fun pullAll(url: String, user: String): SyncBundle {
        val base = resolve(url)
        val sep = if (base.contains("?")) "&" else "?"
        val c = (URL(base + sep + "user=" + URLEncoder.encode(user, "UTF-8")).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
        }
        val raw = c.inputStream.bufferedReader().readText()
        c.disconnect()
        val j = JSONArray(raw)
        val txs = mutableListOf<Tx>()
        val goals = mutableListOf<Goal>()
        for (i in 0 until j.length()) {
            val r = j.getJSONArray(i)
            if (r.optString(0).isBlank()) continue
            val id = r.optString(0).toLongOrNull() ?: System.nanoTime()
            val tipe = r.optString(2)
            if (tipe == "Target") {
                goals.add(Goal(id, r.optString(3).ifBlank { "Target" }, r.optLong(4), r.optString(5).toLongOrNull() ?: 0L, r.optString(1).take(10)))
            } else {
                txs.add(Tx(id, r.optString(1).take(10), tipe == "Pemasukan", r.optString(3).ifBlank { "Lainnya" }, r.optLong(4), r.optString(5)))
            }
        }
        return SyncBundle(txs, goals)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val st = Store(this)
        val scheme = darkColorScheme(
            primary = Blue, onPrimary = White, primaryContainer = Color(0xFF0A5E94), onPrimaryContainer = White,
            secondary = Lime, onSecondary = BG, secondaryContainer = Color(0xFF14405E), onSecondaryContainer = White,
            background = BG, onBackground = White, surface = BG, onSurface = White,
            surfaceContainer = CardC, surfaceContainerHigh = CardC, surfaceVariant = Card2, onSurfaceVariant = Muted,
            outline = Color(0xFF3A4054), error = Red
        )
        setContent {
            MaterialTheme(colorScheme = scheme, typography = Typography().withFont(AppFont)) {
                Surface(Modifier.fillMaxSize(), color = BG) {
                    var user by remember { mutableStateOf(st.user) }
                    AnimatedContent(
                        targetState = user,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(240)) },
                        label = "auth_transition"
                    ) { current ->
                        if (current == null) {
                            LoginScreen(st.last ?: "") { st.user = it; st.last = it; user = it }
                        } else {
                            Main(st, current) { st.user = null; user = null }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Komponen dasar ---------- */
@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = CardC, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = color) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color) {
    Surface(shape = CircleShape, color = bg) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
fun AnimatedBar(fraction: Float, trackColor: Color = Card2, fillColor: Color = Blue, height: Dp = 10.dp, rounded: Boolean = true) {
    val animFrac by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bar_progress"
    )
    val shape = if (rounded) CircleShape else RoundedCornerShape(10.dp)
    Box(Modifier.fillMaxWidth().height(height).clip(shape).background(trackColor)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animFrac).background(fillColor))
    }
}

/** Keyboard angka cepat khusus input nominal (Rp) */
@Composable
fun NominalKeypad(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    fun appendDigits(add: String) {
        val next = (value + add).trimStart('0')
        if (next.length <= 14) onChange(next)
    }

    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "DEL")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardC)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isEmpty()) "Nominal: Rp0" else "Nominal: ${rp(value.toLongOrNull() ?: 0L)}",
                color = Lime,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (value.isNotEmpty()) {
                Text(
                    text = "Bersihkan",
                    color = Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onChange("") }.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val isDel = key == "DEL"
                    Surface(
                        onClick = {
                            if (isDel) {
                                if (value.isNotEmpty()) onChange(value.dropLast(1))
                            } else {
                                appendDigits(key)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDel) Card2 else BG,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            if (isDel) {
                                Icon(Icons.AutoMirrored.Rounded.Backspace, "Hapus digit", tint = White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(key, color = White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Pemilih tanggal kalender Material 3 */
@Composable
fun CalendarDatePicker(
    date: String,
    label: String = "Tanggal",
    onDateSelected: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val parsedDate = remember(date) { runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()) }
    val initialMillis = remember(parsedDate) {
        parsedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    Surface(
        onClick = { open = true },
        shape = RoundedCornerShape(16.dp),
        color = Card2,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatIdDate(parsedDate.toString(), "EEEE, d MMMM yyyy") + " ($parsedDate)",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(Icons.Rounded.CalendarMonth, "Pilih tanggal", tint = Lime, modifier = Modifier.size(22.dp))
        }
    }

    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            onDateSelected(picked.toString())
                        }
                        open = false
                    }
                ) { Text("Pilih", color = Blue) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Batal", color = Muted) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Avatar Vector:
 * 0 = Inisial huruf
 * 1 = Laki-laki
 * 2 = Perempuan
 * 3 = Berkacamata
 * 4 = Anak / Muda
 */
@Composable
fun Avatar(name: String, size: Dp, bg: Color = Card2, type: Int = 0) {
    val ch = name.firstOrNull()?.uppercase() ?: "?"
    Box(Modifier.size(size).clip(CircleShape).background(bg), Alignment.Center) {
        if (type == 0) {
            Text(ch, color = White, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.42f).sp)
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val cx = w / 2f
                val skin = Color(0xFFF3C6A5)
                val darkHair = Color(0xFF1F2430)

                drawCircle(
                    color = when (type) {
                        1 -> Color(0xFF1B3A57)
                        2 -> Color(0xFF4A2545)
                        3 -> Color(0xFF1E3D3B)
                        else -> Color(0xFF3E3A1E)
                    },
                    radius = w * 0.5f
                )

                if (type == 2) {
                    drawRoundRect(
                        color = darkHair,
                        topLeft = Offset(w * 0.22f, h * 0.24f),
                        size = Size(w * 0.56f, h * 0.52f),
                        cornerRadius = CornerRadius(w * 0.24f)
                    )
                }

                val shirt = when (type) {
                    1, 3 -> Color(0xFF0A9BF0)
                    2 -> Color(0xFFEC407A)
                    else -> Color(0xFF2ED573)
                }
                drawRoundRect(
                    color = shirt,
                    topLeft = Offset(w * 0.18f, h * 0.68f),
                    size = Size(w * 0.64f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.30f)
                )

                drawRect(skin, topLeft = Offset(cx - w * 0.07f, h * 0.56f), size = Size(w * 0.14f, h * 0.16f))

                val headR = if (type == 4) w * 0.24f else w * 0.22f
                val headY = if (type == 4) h * 0.44f else h * 0.42f
                drawCircle(skin, radius = headR, center = Offset(cx, headY))

                drawArc(
                    color = darkHair,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - headR * 1.02f, headY - headR * 1.08f),
                    size = Size(headR * 2.04f, headR * 1.35f)
                )

                if (type == 4) {
                    val tuft = Path().apply {
                        moveTo(cx - w * 0.04f, headY - headR * 0.95f)
                        lineTo(cx + w * 0.02f, headY - headR * 1.28f)
                        lineTo(cx + w * 0.06f, headY - headR * 0.92f)
                        close()
                    }
                    drawPath(tuft, darkHair)
                }

                val eyeY = headY + headR * 0.05f
                val eyeOffset = headR * 0.36f
                drawCircle(darkHair, radius = w * 0.024f, center = Offset(cx - eyeOffset, eyeY))
                drawCircle(darkHair, radius = w * 0.024f, center = Offset(cx + eyeOffset, eyeY))

                if (type == 3) {
                    val lensR = headR * 0.30f
                    drawCircle(White, radius = lensR, center = Offset(cx - eyeOffset, eyeY), style = Stroke(w * 0.024f))
                    drawCircle(White, radius = lensR, center = Offset(cx + eyeOffset, eyeY), style = Stroke(w * 0.024f))
                    drawLine(White, Offset(cx - eyeOffset + lensR, eyeY), Offset(cx + eyeOffset - lensR, eyeY), strokeWidth = w * 0.024f)
                }

                drawArc(
                    color = Color(0xFF9E5A4A),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - headR * 0.28f, headY + headR * 0.16f),
                    size = Size(headR * 0.56f, headR * 0.34f),
                    style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun IconCircle(icon: ImageVector, bg: Color = Card2, tint: Color = White) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(bg), Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Money(n: Long, size: TextUnit, color: Color = White, hidden: Boolean = false) {
    val digits = "%,d".format(ID, Math.abs(n))
    Text(
        buildAnnotatedString {
            if (n < 0) append("-")
            withStyle(SpanStyle(fontWeight = FontWeight.Light, fontSize = size * 0.55f)) { append("Rp") }
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(if (hidden) "••••••" else digits) }
        },
        fontSize = size, color = color, letterSpacing = (-0.5).sp
    )
}

@Composable
fun PeriodNav(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        TextButton(onPrev) { Text("‹", fontSize = 24.sp, color = Muted) }
        Text(label, color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        TextButton(onNext) { Text("›", fontSize = 24.sp, color = Muted) }
    }
}

@Composable
fun Segmented(options: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = CircleShape, color = Card2) {
        Row(Modifier.padding(4.dp)) {
            options.forEachIndexed { i, t ->
                Surface(onClick = { onSel(i) }, shape = CircleShape, color = if (i == sel) BG else Color.Transparent, modifier = Modifier.weight(1f)) {
                    Text(t, color = if (i == sel) White else Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
fun TxRow(t: Tx, showDate: Boolean = false, onClick: () -> Unit) {
    val sub = if (showDate) listOf(t.d, t.n).filter { it.isNotBlank() }.joinToString(" · ") else t.n.ifBlank { "Tanpa catatan" }
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = CardC, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(t.c, 42.dp, type = 0)
            Column(Modifier.weight(1f)) {
                Text(t.c, color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(sub, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text((if (t.income) "+ " else "- ") + rp(t.a), color = if (t.income) Green else White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun NavItem(sel: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (sel) BG else Card2) {
        Row(Modifier.height(44.dp).padding(horizontal = if (sel) 12.dp else 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, label, tint = if (sel) White else Muted, modifier = Modifier.size(19.dp))
            AnimatedVisibility(visible = sel, enter = fadeIn(tween(150)), exit = fadeOut(tween(200))) {
                Text(label, color = White, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp), Alignment.Center) {
        Surface(shape = CircleShape, color = Color(0xFF191D29), border = BorderStroke(1.dp, Color(0xFF262B3A)), shadowElevation = 8.dp) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                NavItem(tab == 0, Icons.Rounded.Home, "Beranda") { onTab(0) }
                NavItem(tab == 1, Icons.Rounded.BarChart, "Laporan") { onTab(1) }
                Surface(onClick = onAdd, shape = CircleShape, color = Lime) {
                    Box(Modifier.size(44.dp), Alignment.Center) { Icon(Icons.Rounded.Add, "Catat", tint = BG) }
                }
                NavItem(tab == 2, Icons.Rounded.Flag, "Target") { onTab(2) }
                NavItem(tab == 3, Icons.Rounded.Settings, "Settings") { onTab(3) }
            }
        }
    }
}

@Composable
fun Gauge(score: Int, label: String) {
    Box(Modifier.size(150.dp, 92.dp), Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val dia = size.width - stroke
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(color = Color.White.copy(alpha = 0.22f), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = tl, size = Size(dia, dia), style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color = Lime, startAngle = 180f, sweepAngle = 180f * score / 100f, useCenter = false, topLeft = tl, size = Size(dia, dia), style = Stroke(stroke, cap = StrokeCap.Round))
            val r = dia / 2
            val ang = Math.toRadians(180.0 + 180.0 * score / 100.0)
            val cx = size.width / 2 + r * Math.cos(ang).toFloat()
            val cy = stroke / 2 + r + r * Math.sin(ang).toFloat()
            drawCircle(Color.White, radius = stroke * 0.3f, center = Offset(cx, cy))
        }
        Text(label, color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
fun Chart(bi: List<Long>, be: List<Long>) {
    val mx = maxOf(1L, bi.maxOrNull() ?: 0L, be.maxOrNull() ?: 0L)
    Column {
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val w = size.width / bi.size
            val cr = CornerRadius(4.dp.toPx())
            for (i in bi.indices) {
                val bw = w * 0.36f
                val hi = size.height * bi[i] / mx
                val he = size.height * be[i] / mx
                if (hi > 0f) drawRoundRect(Lime, Offset(i * w + w * 0.09f, size.height - hi), Size(bw, hi), cr)
                if (he > 0f) drawRoundRect(Blue, Offset(i * w + w * 0.09f + bw, size.height - he), Size(bw, he), cr)
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("● Pemasukan", color = Lime, fontSize = 12.sp)
            Text("● Pengeluaran", color = Blue, fontSize = 12.sp)
        }
    }
}

/* ---------- Layar ---------- */
@Composable
fun LoginScreen(last: String, onLogin: (String) -> Unit) {
    var name by remember { mutableStateOf(last) }
    var err by remember { mutableStateOf(false) }
    fun go() { val u = name.trim().lowercase(); if (USER_RE.matches(u)) onLogin(u) else err = true }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Lime), Alignment.Center) {
            Text("NC", color = BG, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Text("Note Cash", fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = White, letterSpacing = (-0.5).sp, modifier = Modifier.padding(top = 16.dp))
        Text("Masukkan username untuk membuka catatanmu", color = Muted, modifier = Modifier.padding(top = 6.dp, bottom = 28.dp))
        OutlinedTextField(
            name, { name = it; err = false }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(20.dp), isError = err,
            label = { Text("Username") },
            supportingText = { Text(if (err) "2-30 karakter: huruf kecil, angka, atau _" else "") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { go() })
        )
        Spacer(Modifier.height(16.dp))
        Surface(onClick = { go() }, shape = CircleShape, color = Blue, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Masuk", color = White, fontWeight = FontWeight.Medium, fontSize = 15.sp) }
        }
    }
}

@Composable
fun Main(st: Store, user: String, onLogout: () -> Unit) {
    var all by remember(user) { mutableStateOf(st.load(user)) }
    var goals by remember(user) { mutableStateOf(st.loadGoals(user)) }
    var avatarType by remember(user) { mutableStateOf(st.avatarOf(user)) }
    var tab by remember { mutableStateOf(0) }
    var edit by remember { mutableStateOf<Tx?>(null) }
    var formIncome by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var hide by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var homeA by remember { mutableStateOf(LocalDate.now()) }
    var repA by remember { mutableStateOf(LocalDate.now()) }
    var repMode by remember { mutableStateOf(1) }
    var url by remember { mutableStateOf(st.url) }
    var dev by remember { mutableStateOf(st.dev) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    fun autoPush(newTxs: List<Tx>, newGoals: List<Goal>) {
        if (url.isBlank()) return
        scope.launch {
            syncing = true
            val r = runCatching { withContext(Dispatchers.IO) { Sheet.pushAll(url, user, newTxs, newGoals) } }
            syncing = false
            if (r.isFailure) snack.showSnackbar("Gagal sinkron otomatis ke Sheet")
        }
    }

    fun commitTx(l: List<Tx>) {
        all = l; st.save(user, l); autoPush(l, goals)
    }

    fun commitGoals(g: List<Goal>) {
        goals = g; st.saveGoals(user, g); autoPush(all, g)
    }

    fun autoPull(silent: Boolean = false) {
        if (url.isBlank()) return
        scope.launch {
            syncing = true
            runCatching { withContext(Dispatchers.IO) { Sheet.pullAll(url, user) } }
                .onSuccess { bundle ->
                    if (bundle.txs.isNotEmpty() || bundle.goals.isNotEmpty()) {
                        all = bundle.txs; st.save(user, bundle.txs)
                        goals = bundle.goals; st.saveGoals(user, bundle.goals)
                    } else if (all.isNotEmpty() || goals.isNotEmpty()) {
                        withContext(Dispatchers.IO) { Sheet.pushAll(url, user, all, goals) }
                    }
                    if (!silent) snack.showSnackbar("Data tersinkron")
                }
                .onFailure { if (!silent) snack.showSnackbar("Gagal sinkron dengan Sheet") }
            syncing = false
        }
    }

    LaunchedEffect(user) { autoPull(silent = true) }

    Box(Modifier.fillMaxSize().background(BG)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { it / 16 * dir }) togetherWith
                    (fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 18 * dir })
            },
            label = "tab_transition"
        ) { activeTab ->
            key(activeTab) {
                when (activeTab) {
                    0 -> HomeScreen(
                        user = user, avatarType = avatarType, syncing = syncing, all = all,
                        anchor = homeA, onAnchor = { homeA = it }, hide = hide, onHide = { hide = !hide },
                        onSync = { autoPull(silent = false) }, onSearch = { searching = true },
                        onAdd = { inc -> edit = null; formIncome = inc; showForm = true },
                        onEdit = { edit = it; showForm = true }
                    )
                    1 -> ReportScreen(all, repMode, { repMode = it }, repA, { repA = it }) { edit = it; showForm = true }
                    2 -> TargetScreen(
                        goals = goals, syncing = syncing,
                        onSaveGoal = { g ->
                            val exists = goals.any { it.id == g.id }
                            commitGoals(if (exists) goals.map { if (it.id == g.id) g else it } else goals + g)
                        },
                        onDeleteGoal = { id -> commitGoals(goals.filter { it.id != id }) }
                    )
                    else -> SettingsScreen(
                        user = user, avatarType = avatarType,
                        onAvatarChange = { avatarType = it; st.setAvatar(user, it) },
                        syncing = syncing, url = url, dev = dev,
                        onUrl = { st.url = it.trim(); url = st.url },
                        onDev = { on -> dev = on; st.dev = on; scope.launch { snack.showSnackbar(if (on) "Mode pengembang aktif" else "Mode pengembang dimatikan") } },
                        onLogout = onLogout
                    )
                }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
        BottomBar(tab, { tab = it }, { edit = null; formIncome = false; showForm = true }, Modifier.align(Alignment.BottomCenter))

        AnimatedVisibility(
            visible = searching,
            enter = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 6 },
            exit = fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 6 }
        ) {
            SearchScreen(all, { searching = false }) { edit = it; showForm = true }
        }

        AnimatedVisibility(
            visible = showForm,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 12 },
            exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 12 }
        ) {
            Form(
                t = edit,
                initialIncome = formIncome,
                onSave = { tx ->
                    commitTx(if (edit == null) all + tx else all.map { if (it.id == tx.id) tx else it })
                    homeA = runCatching { LocalDate.parse(tx.d) }.getOrDefault(homeA)
                    showForm = false
                },
                onDel = edit?.let { e -> { commitTx(all.filter { it.id != e.id }); showForm = false } },
                onClose = { showForm = false }
            )
        }
    }
}

@Composable
fun HomeScreen(
    user: String, avatarType: Int, syncing: Boolean, all: List<Tx>, anchor: LocalDate, onAnchor: (LocalDate) -> Unit,
    hide: Boolean, onHide: () -> Unit, onSync: () -> Unit, onSearch: () -> Unit, onAdd: (Boolean) -> Unit, onEdit: (Tx) -> Unit
) {
    val (from, to) = rangeOf(1, anchor)
    val (pf, pt) = rangeOf(1, shiftOf(1, anchor, -1))
    val list = within(all, from, to).sortedByDescending { it.d }
    val s = totals(list)
    val sp = totals(within(all, pf, pt))
    val total = totals(all)
    val sc = scoreOf(s)
    val scp = scoreOf(sp)
    val net = s.inc - s.exp
    val hour = LocalTime.now().hour
    val greet = when { hour < 11 -> "Selamat pagi"; hour < 15 -> "Selamat siang"; hour < 18 -> "Selamat sore"; else -> "Selamat malam" }
    val dE = s.exp - sp.exp

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(user, 44.dp, type = avatarType)
                Column(Modifier.weight(1f)) {
                    Text(greet, color = Muted, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(user, color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        AnimatedVisibility(visible = syncing, enter = fadeIn(), exit = fadeOut(tween(220))) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Lime)
                        }
                    }
                }
                Surface(onClick = onSync, enabled = !syncing, shape = CircleShape, color = Card2) {
                    Icon(Icons.Rounded.Sync, "Sinkronkan", tint = if (syncing) Lime else White, modifier = Modifier.padding(11.dp).size(22.dp))
                }
                Surface(onClick = onSearch, shape = CircleShape, color = Card2) {
                    Icon(Icons.Rounded.Search, "Cari transaksi", tint = White, modifier = Modifier.padding(11.dp).size(22.dp))
                }
            }
        }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Rounded.AccountBalanceWallet)
                    Spacer(Modifier.width(10.dp))
                    Text("Saldo saya", color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Surface(onClick = onHide, shape = CircleShape, color = Card2) {
                        Icon(if (hide) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Sembunyikan saldo", tint = Muted, modifier = Modifier.padding(9.dp).size(20.dp))
                    }
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), Alignment.Center) { Money(total.inc - total.exp, 40.sp, hidden = hide) }
                Box(Modifier.fillMaxWidth(), Alignment.Center) {
                    Surface(shape = CircleShape, color = Card2) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(if (net >= 0) Green else Red), Alignment.Center) {
                                Icon(if (net >= 0) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null, tint = White, modifier = Modifier.size(14.dp))
                            }
                            Text(if (net >= 0) "Plus " + rp(net) + " bulan ini" else "Minus " + rp(-net) + " bulan ini", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(onClick = { onAdd(true) }, shape = CircleShape, color = Blue, modifier = Modifier.weight(1f).height(52.dp)) {
                        Row(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Add, null, tint = White); Spacer(Modifier.width(6.dp)); Text("Pemasukan", color = White, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(onClick = { onAdd(false) }, shape = CircleShape, color = BG, border = BorderStroke(1.dp, Color(0xFF2E3446)), modifier = Modifier.weight(1f).height(52.dp)) {
                        Row(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ArrowUpward, null, tint = White); Spacer(Modifier.width(6.dp)); Text("Pengeluaran", color = White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Blue) {
                Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Skor", color = White.copy(alpha = 0.85f), fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$sc", color = White, fontSize = 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                            Text("%", color = White.copy(alpha = 0.7f), fontSize = 22.sp, modifier = Modifier.padding(bottom = 8.dp, start = 2.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Pill((if (sc >= scp) "+" else "") + (sc - scp) + " poin", Lime, BG)
                    }
                    Gauge(sc, scoreLabel(sc, s.inc > 0))
                }
            }
        }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Red), Alignment.Center) { Icon(Icons.Rounded.ArrowUpward, null, tint = White, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(10.dp))
                    Text("Pengeluaran", color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Surface(shape = CircleShape, color = Card2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("‹", color = Muted, fontSize = 20.sp, modifier = Modifier.clickable { onAnchor(shiftOf(1, anchor, -1)) }.padding(horizontal = 12.dp, vertical = 6.dp))
                            Text(labelOf(1, from, to), color = White, fontSize = 13.sp)
                            Text("›", color = Muted, fontSize = 20.sp, modifier = Modifier.clickable { onAnchor(shiftOf(1, anchor, 1)) }.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Money(s.exp, 32.sp, hidden = hide)
                    if (sp.exp > 0 || s.exp > 0) Pill((if (dE >= 0) "▲ " else "▼ ") + rp(Math.abs(dE)) + " vs bulan lalu", (if (dE > 0) Red else Green).copy(alpha = 0.15f), if (dE > 0) Red else Green)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (sp.exp > 0) "Pengeluaran " + (if (dE >= 0) "naik " else "turun ") + Math.abs(dE * 100 / sp.exp) + "% dari bulan lalu" else "Belum ada pembanding bulan lalu",
                    color = Muted, fontSize = 13.sp
                )
                Spacer(Modifier.height(14.dp))
                val frac = if (s.inc > 0) s.exp.toFloat() / s.inc else if (s.exp > 0) 1f else 0f
                AnimatedBar(fraction = frac, trackColor = Card2, fillColor = Blue, height = 30.dp, rounded = false)
                Spacer(Modifier.height(6.dp))
                Text(if (s.inc > 0) (s.exp * 100 / s.inc).toString() + "% dari pemasukan bulan ini" else "Belum ada pemasukan bulan ini", color = Muted, fontSize = 12.sp)
            }
        }
        item { Text("Transaksi", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
        if (list.isEmpty()) item { Text("Belum ada transaksi bulan ini. Ketuk tombol hijau untuk mencatat.", color = Muted, fontSize = 14.sp) }
        list.groupBy { it.d }.forEach { (d, l) ->
            item(key = "h$d") { Text(formatIdDate(d), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
            l.forEach { t -> item(key = t.id) { TxRow(t) { onEdit(t) } } }
        }
    }
}

@Composable
fun TargetScreen(goals: List<Goal>, syncing: Boolean, onSaveGoal: (Goal) -> Unit, onDeleteGoal: (Long) -> Unit) {
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var topupGoal by remember { mutableStateOf<Goal?>(null) }

    val totalTarget = goals.sumOf { it.target }
    val totalSaved = goals.sumOf { it.saved }
    val overallPct = if (totalTarget > 0) ((totalSaved * 100) / totalTarget).coerceIn(0L, 100L).toInt() else 0

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Target", color = White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
                        AnimatedVisibility(visible = syncing, enter = fadeIn(), exit = fadeOut(tween(220))) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Lime)
                        }
                    }
                    Surface(onClick = { editingGoal = null; showDialog = true }, shape = CircleShape, color = Blue) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Add, null, tint = White, modifier = Modifier.size(18.dp))
                            Text("Target baru", color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(Icons.Rounded.Flag, bg = Lime, tint = BG)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Total terkumpul", color = Muted, fontSize = 13.sp)
                            Money(totalSaved, 28.sp)
                        }
                        Pill("$overallPct%", Blue, White)
                    }
                    Spacer(Modifier.height(14.dp))
                    AnimatedBar(fraction = if (totalTarget > 0) totalSaved.toFloat() / totalTarget else 0f, fillColor = Lime)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (totalTarget > 0) "Dari total target " + rp(totalTarget) + " (${goals.size} target)" else "Belum ada target tabungan yang dibuat",
                        color = Muted, fontSize = 12.sp
                    )
                }
            }
            if (goals.isEmpty()) {
                item {
                    Panel {
                        Text("Mulai tetapkan tujuanmu", color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("Buat target seperti Dana Darurat, Beli Laptop, atau Liburan dan pantau progresnya secara otomatis.", color = Muted, fontSize = 13.sp)
                    }
                }
            }
            items(goals, key = { it.id }) { g ->
                val pct = if (g.target > 0) ((g.saved * 100) / g.target).coerceIn(0L, 100L).toInt() else 0
                val done = g.saved >= g.target && g.target > 0
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(g.title, color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            if (g.deadline.isNotBlank()) Text("Batas waktu: ${g.deadline}", color = Muted, fontSize = 12.sp)
                        }
                        Pill(if (done) "Tercapai" else "$pct%", if (done) Green else Card2, if (done) BG else White)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(rp(g.saved), color = Lime, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Target " + rp(g.target), color = Muted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    AnimatedBar(fraction = if (g.target > 0) g.saved.toFloat() / g.target else 0f, fillColor = if (done) Green else Blue)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(onClick = { topupGoal = g }, shape = CircleShape, color = Blue, modifier = Modifier.weight(1f).height(42.dp)) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("+ Tambah Dana", color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                        }
                        Surface(onClick = { editingGoal = g; showDialog = true }, shape = CircleShape, color = Card2, modifier = Modifier.height(42.dp)) {
                            Box(Modifier.padding(horizontal = 16.dp), Alignment.Center) { Text("Ubah", color = White, fontSize = 13.sp) }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showDialog,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 12 },
            exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 12 }
        ) {
            GoalDialog(
                goal = editingGoal,
                onSave = { onSaveGoal(it); showDialog = false },
                onDelete = editingGoal?.let { e -> { onDeleteGoal(e.id); showDialog = false } },
                onClose = { showDialog = false }
            )
        }

        AnimatedVisibility(
            visible = topupGoal != null,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 12 },
            exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 12 }
        ) {
            topupGoal?.let { g ->
                TopupGoalDialog(
                    goal = g,
                    onAdd = { addAmt -> onSaveGoal(g.copy(saved = (g.saved + addAmt).coerceAtLeast(0L))); topupGoal = null },
                    onClose = { topupGoal = null }
                )
            }
        }
    }
}

@Composable
fun GoalDialog(goal: Goal?, onSave: (Goal) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var target by remember { mutableStateOf(goal?.target?.toString() ?: "") }
    var saved by remember { mutableStateOf(goal?.saved?.toString() ?: "0") }
    var deadline by remember { mutableStateOf(goal?.deadline?.ifBlank { null } ?: LocalDate.now().plusMonths(3).toString()) }
    var activeNominalField by remember { mutableStateOf(0) }
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(onClick = onClose, shape = CircleShape, color = Card2) {
                    Icon(Icons.Rounded.Close, "Tutup", tint = White, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Text(if (goal == null) "Target baru" else "Ubah target", color = White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, "Hapus", tint = Red)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text("Nama target") },
                singleLine = true
            )
            OutlinedTextField(
                value = target,
                onValueChange = { target = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth().clickable { activeNominalField = 0 },
                shape = RoundedCornerShape(16.dp),
                label = { Text("Nominal target (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = saved,
                onValueChange = { saved = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth().clickable { activeNominalField = 1 },
                shape = RoundedCornerShape(16.dp),
                label = { Text("Sudah terkumpul (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(activeNominalField == 0, { activeNominalField = 0 }, { Text("Keypad: Target") })
                FilterChip(activeNominalField == 1, { activeNominalField = 1 }, { Text("Keypad: Terkumpul") })
            }
            NominalKeypad(
                value = if (activeNominalField == 0) target else saved,
                onChange = { v -> if (activeNominalField == 0) target = v else saved = v }
            )
            CalendarDatePicker(
                date = deadline,
                label = "Batas waktu target",
                onDateSelected = { deadline = it }
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onClose, shape = CircleShape, color = Card2, modifier = Modifier.weight(1f).height(52.dp)) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Batal", color = Muted, fontWeight = FontWeight.Medium) }
            }
            Surface(
                onClick = {
                    val tg = target.toLongOrNull() ?: 0L
                    val sv = saved.toLongOrNull() ?: 0L
                    if (title.isNotBlank() && tg > 0) onSave(Goal(goal?.id ?: System.currentTimeMillis(), title.trim(), tg, sv, deadline))
                },
                shape = CircleShape,
                color = Blue,
                modifier = Modifier.weight(1.4f).height(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Simpan", color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
fun TopupGoalDialog(goal: Goal, onAdd: (Long) -> Unit, onClose: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(onClick = onClose, shape = CircleShape, color = Card2) {
                Icon(Icons.Rounded.Close, "Tutup", tint = White, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Text("Tambah ke ${goal.title}", color = White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Terkumpul saat ini: " + rp(goal.saved) + " / " + rp(goal.target), color = Muted, fontSize = 13.sp)
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text("Nominal tambahan (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            NominalKeypad(value = amount, onChange = { amount = it })
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onClose, shape = CircleShape, color = Card2, modifier = Modifier.weight(1f).height(52.dp)) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Batal", color = Muted, fontWeight = FontWeight.Medium) }
            }
            Surface(
                onClick = { val v = amount.toLongOrNull() ?: 0L; if (v > 0) onAdd(v) },
                shape = CircleShape,
                color = Blue,
                modifier = Modifier.weight(1.4f).height(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Tambah", color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
fun SearchScreen(all: List<Tx>, onClose: () -> Unit, onEdit: (Tx) -> Unit) {
    var q by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(0) }
    val fr = remember { FocusRequester() }
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { fr.requestFocus() }
    val key = q.trim().lowercase()
    val digits = key.filter(Char::isDigit)
    val res = all.filter { t ->
        (type == 0 || (type == 1) == t.income) &&
            (key.isEmpty() || t.c.lowercase().contains(key) || t.n.lowercase().contains(key) || t.d.contains(key) ||
                (digits.isNotEmpty() && t.a.toString().contains(digits)))
    }.sortedByDescending { it.d }
    val s = totals(res)
    Column(Modifier.fillMaxSize().background(BG).statusBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onClose, shape = CircleShape, color = Card2) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", tint = White, modifier = Modifier.padding(12.dp).size(22.dp))
            }
            TextField(
                q, { q = it }, Modifier.weight(1f).focusRequester(fr), singleLine = true, shape = CircleShape,
                placeholder = { Text("Cari kategori, catatan, jumlah, tanggal", color = Muted, fontSize = 14.sp) },
                trailingIcon = if (q.isNotEmpty()) ({ IconButton({ q = "" }) { Icon(Icons.Rounded.Close, "Hapus", tint = Muted) } }) else null,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Card2, unfocusedContainerColor = Card2,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = White, unfocusedTextColor = White, cursorColor = Blue
                )
            )
        }
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Semua", "Pemasukan", "Pengeluaran").forEachIndexed { i, t -> FilterChip(type == i, { type = i }, { Text(t) }) }
        }
        Text(res.size.toString() + " hasil  ·  selisih " + rp(s.inc - s.exp), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (res.isEmpty()) item { Text(if (all.isEmpty()) "Belum ada transaksi." else "Tidak ada transaksi yang cocok.", color = Muted, fontSize = 14.sp) }
            items(res, key = { it.id }) { t -> TxRow(t, showDate = true) { onEdit(t) } }
        }
    }
}

@Composable
fun ReportScreen(all: List<Tx>, mode: Int, onMode: (Int) -> Unit, anchor: LocalDate, onAnchor: (LocalDate) -> Unit, onEdit: (Tx) -> Unit) {
    val (from, to) = rangeOf(mode, anchor)
    val (pf, pt) = rangeOf(mode, shiftOf(mode, anchor, -1))
    val list = within(all, from, to).sortedByDescending { it.d }
    val s = totals(list)
    val sp = totals(within(all, pf, pt))
    val net = s.inc - s.exp
    val netp = sp.inc - sp.exp
    val cats = list.filter { !it.income }.groupBy { it.c }.mapValues { e -> e.value.sumOf { it.a } }.toList().sortedByDescending { it.second }
    val keys = when (mode) {
        0 -> (0..6).map { from.plusDays(it.toLong()).toString() }
        1 -> (0 until from.lengthOfMonth()).map { from.plusDays(it.toLong()).toString() }
        else -> (1..12).map { "%d-%02d".format(from.year, it) }
    }
    fun key(t: Tx) = if (mode == 2) t.d.take(7) else t.d
    val bi = keys.map { k -> list.filter { it.income && key(it) == k }.sumOf { it.a } }
    val be = keys.map { k -> list.filter { !it.income && key(it) == k }.sumOf { it.a } }
    val dI = s.inc - sp.inc
    val dX = s.exp - sp.exp

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)) {
        item { Text("Laporan", color = White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp) }
        item {
            Panel {
                Segmented(listOf("Minggu", "Bulan", "Tahun"), mode, onMode)
                PeriodNav(labelOf(mode, from, to), { onAnchor(shiftOf(mode, anchor, -1)) }, { onAnchor(shiftOf(mode, anchor, 1)) })
                Chart(bi, be)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Card2)
                Spacer(Modifier.height(12.dp))
                Row {
                    Column(Modifier.weight(1f)) { Text("Periode ini", color = Muted, fontSize = 12.sp); Text(rp(net), color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    Column(Modifier.weight(1f)) { Text("Sebelumnya", color = Muted, fontSize = 12.sp); Text(rp(netp), color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    Column(Modifier.weight(1f)) { Text("Selisih", color = Muted, fontSize = 12.sp); Text(rp(net - netp), color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        item {
            Panel {
                Row {
                    Column(Modifier.weight(1f)) {
                        IconCircle(Icons.Rounded.AccountBalanceWallet)
                        Spacer(Modifier.height(14.dp))
                        Text("Pemasukan", color = Muted, fontSize = 13.sp)
                        Money(s.inc, 22.sp)
                        Text((if (dI >= 0) "+" else "") + rp(dI) + " dari periode lalu", color = if (dI >= 0) Green else Red, fontSize = 11.sp)
                    }
                    Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(120.dp).background(Card2))
                    Column(Modifier.weight(1f)) {
                        IconCircle(Icons.Rounded.ShoppingCart)
                        Spacer(Modifier.height(14.dp))
                        Text("Pengeluaran", color = Muted, fontSize = 13.sp)
                        Money(s.exp, 22.sp)
                        Text((if (dX >= 0) "+" else "") + rp(dX) + " dari periode lalu", color = if (dX <= 0) Green else Red, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Panel {
                Text("Pengeluaran per kategori", color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                if (cats.isEmpty()) Text("Belum ada pengeluaran di periode ini.", color = Muted, fontSize = 13.sp)
                cats.forEach { (c, v) ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(c, color = White, fontSize = 14.sp)
                        Text(rp(v) + "  " + (v * 100 / s.exp) + "%", color = Muted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    AnimatedBar(fraction = v.toFloat() / s.exp, fillColor = Blue, height = 8.dp)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        item { Text("Transaksi", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        if (list.isEmpty()) item { Text("Belum ada transaksi di periode ini.", color = Muted, fontSize = 14.sp) }
        list.forEach { t -> item(key = t.id) { TxRow(t) { onEdit(t) } } }
    }
}

@Composable
fun AvatarPopupDialog(user: String, currentType: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = CardC,
        title = { Text("Pilih Avatar", color = White) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AVATARS.forEachIndexed { idx, label ->
                    val selected = currentType == idx
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(idx); onDismiss() }
                            .background(if (selected) Card2 else Color.Transparent)
                            .border(
                                width = if (selected) 1.5.dp else 0.dp,
                                color = if (selected) Lime else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Avatar(user, 50.dp, Blue, type = idx)
                        Spacer(Modifier.height(6.dp))
                        Text(label, color = if (selected) Lime else Muted, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Tutup", color = Blue) } }
    )
}

@Composable
fun SettingsScreen(
    user: String, avatarType: Int, onAvatarChange: (Int) -> Unit,
    syncing: Boolean, url: String, dev: Boolean,
    onUrl: (String) -> Unit, onDev: (Boolean) -> Unit, onLogout: () -> Unit
) {
    var u by remember { mutableStateOf(url) }
    var taps by remember { mutableStateOf(0) }
    var showAvatarPopup by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", color = White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.clip(CircleShape).clickable { showAvatarPopup = true }) {
                    Avatar(user, 64.dp, Blue, type = avatarType)
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(Lime),
                        Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Edit, "Ubah avatar", tint = BG, modifier = Modifier.size(12.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(user, color = White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (syncing) "Menyinkronkan ke tab \"$user\"..." else "Sync aktif"",
                            color = if (syncing) Lime else Muted, fontSize = 13.sp
                        )
                        AnimatedVisibility(visible = syncing, enter = fadeIn(), exit = fadeOut(tween(220))) {
                            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = Lime)
                        }
                    }
                }
            }
        }
        if (dev) Panel {
            Text("Mode pengembang", color = Lime, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(u, { u = it; onUrl(it) }, Modifier.fillMaxWidth(), label = { Text("URL Apps Script atau shortlink") })
            Spacer(Modifier.height(8.dp))
            Text("Bawaan: $DEFAULT_SHEET_URL", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = { u = DEFAULT_SHEET_URL; onUrl("") }, shape = CircleShape, color = Card2, modifier = Modifier.weight(1f).height(48.dp)) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Setel ulang", color = White, fontWeight = FontWeight.Medium) }
                }
                Surface(onClick = { onDev(false) }, shape = CircleShape, color = Card2, modifier = Modifier.weight(1f).height(48.dp)) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Close", color = White, fontWeight = FontWeight.Medium) }
                }
            }
        }
        Surface(onClick = onLogout, shape = CircleShape, color = BG, border = BorderStroke(1.dp, Red.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Keluar", color = Red, fontWeight = FontWeight.Medium) }
        }
        Text(
            "Note Cash v1.2", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable { taps++; if (taps >= 7 && !dev) { taps = 0; onDev(true) } }.padding(16.dp)
        )
    }

    if (showAvatarPopup) {
        AvatarPopupDialog(
            user = user,
            currentType = avatarType,
            onSelect = onAvatarChange,
            onDismiss = { showAvatarPopup = false }
        )
    }
}

@Composable
fun Form(t: Tx?, initialIncome: Boolean, onSave: (Tx) -> Unit, onDel: (() -> Unit)?, onClose: () -> Unit) {
    var inc by remember { mutableStateOf(t?.income ?: initialIncome) }
    var cat by remember { mutableStateOf(t?.c ?: (if (initialIncome) IN[0] else OUT[0])) }
    var amt by remember { mutableStateOf(t?.a?.toString() ?: "") }
    var date by remember { mutableStateOf(t?.d?.ifBlank { null } ?: LocalDate.now().toString()) }
    var note by remember { mutableStateOf(t?.n ?: "") }
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(onClick = onClose, shape = CircleShape, color = Card2) {
                    Icon(Icons.Rounded.Close, "Tutup", tint = White, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Text(
                    if (t == null) "Catat transaksi" else "Ubah transaksi",
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (onDel != null) {
                IconButton(onClick = onDel) {
                    Icon(Icons.Rounded.Delete, "Hapus", tint = Red)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!inc, { inc = false; cat = OUT[0] }, { Text("Pengeluaran") })
                FilterChip(inc, { inc = true; cat = IN[0] }, { Text("Pemasukan") })
            }

            OutlinedTextField(
                value = amt,
                onValueChange = { amt = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                label = { Text("Jumlah (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            NominalKeypad(value = amt, onChange = { amt = it })

            Text("Kategori", color = Muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (if (inc) IN else OUT).forEach {
                    FilterChip(cat == it, { cat = it }, { Text(it) })
                }
            }

            CalendarDatePicker(date = date, label = "Tanggal transaksi", onDateSelected = { date = it })

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                label = { Text("Catatan") },
                singleLine = true
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = Card2,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Batal", color = Muted, fontWeight = FontWeight.Medium)
                }
            }
            Surface(
                onClick = {
                    val a = amt.toLongOrNull() ?: 0L
                    if (a > 0) onSave(Tx(t?.id ?: System.currentTimeMillis(), date, inc, cat, a, note.trim()))
                },
                shape = CircleShape,
                color = Blue,
                modifier = Modifier.weight(1.4f).height(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Simpan", color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

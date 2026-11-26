@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.udyne.notecash

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import java.net.URL
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/* ---------- Palet (mengikuti desain Orbix: gelap, biru, lime) ---------- */
val BG = Color(0xFF0D1018)
val CardC = Color(0xFF1A1E2A)
val Card2 = Color(0xFF262B3A)
val Blue = Color(0xFF0A9BF0)
val Lime = Color(0xFFD4F34A)
val Green = Color(0xFF2ED573)
val Red = Color(0xFFFF4D4F)
val Muted = Color(0xFF9AA1B2)
val White = Color(0xFFFFFFFF)

/** Ganti satu baris ini kalau punya file SF Pro (res/font) -> FontFamily(Font(R.font.sf_pro)). */
val AppFont: FontFamily = FontFamily.SansSerif

fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge.copy(fontFamily = f), displayMedium.copy(fontFamily = f), displaySmall.copy(fontFamily = f),
    headlineLarge.copy(fontFamily = f), headlineMedium.copy(fontFamily = f), headlineSmall.copy(fontFamily = f),
    titleLarge.copy(fontFamily = f), titleMedium.copy(fontFamily = f), titleSmall.copy(fontFamily = f),
    bodyLarge.copy(fontFamily = f), bodyMedium.copy(fontFamily = f), bodySmall.copy(fontFamily = f),
    labelLarge.copy(fontFamily = f), labelMedium.copy(fontFamily = f), labelSmall.copy(fontFamily = f)
)

val USERS = listOf("mnrdnn", "lya")
val OUT = listOf("Makan", "Transport", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Lainnya")
val IN = listOf("Gaji", "Bonus", "Usaha", "Hadiah", "Lainnya")
val ID = Locale("id", "ID")

data class Tx(val id: Long, val d: String, val income: Boolean, val c: String, val a: Long, val n: String)
class Sum(val inc: Long, val exp: Long)

fun rp(n: Long) = (if (n < 0) "-" else "") + "Rp" + "%,d".format(ID, Math.abs(n))
fun totals(l: List<Tx>) = Sum(l.filter { it.income }.sumOf { it.a }, l.filter { !it.income }.sumOf { it.a })
fun within(all: List<Tx>, from: LocalDate, to: LocalDate) = all.filter { it.d >= from.toString() && it.d <= to.toString() }
fun scoreOf(s: Sum): Int = if (s.inc <= 0) 0 else ((s.inc - s.exp) * 100 / s.inc).coerceIn(0L, 100L).toInt()
fun scoreLabel(sc: Int, has: Boolean) = when {
    !has -> "Belum ada data"
    sc >= 60 -> "Sangat baik"
    sc >= 30 -> "Baik"
    sc >= 10 -> "Cukup"
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

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("catatan", 0)
    var url: String
        get() = p.getString("url", "") ?: ""
        set(v) { p.edit().putString("url", v).apply() }
    var user: String?
        get() = p.getString("user", null)
        set(v) { p.edit().putString("user", v).apply() }
    var chart: Boolean
        get() = p.getBoolean("chart", false)
        set(v) { p.edit().putBoolean("chart", v).apply() }

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
}

/** Sinkron ke Google Sheets lewat Web App Apps Script. Satu tab per username. */
object Sheet {
    fun push(url: String, user: String, l: List<Tx>) {
        val rows = JSONArray()
        l.sortedBy { it.d }.forEach {
            rows.put(JSONArray().put(it.id.toString()).put(it.d).put(if (it.income) "Pemasukan" else "Pengeluaran").put(it.c).put(it.a).put(it.n))
        }
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "text/plain;charset=utf-8")
        c.outputStream.use { it.write(JSONObject().put("user", user).put("rows", rows).toString().toByteArray()) }
        c.inputStream.close(); c.disconnect()
    }

    fun pull(url: String, user: String): List<Tx> {
        val sep = if (url.contains("?")) "&" else "?"
        val c = URL(url + sep + "user=" + URLEncoder.encode(user, "UTF-8")).openConnection() as HttpURLConnection
        val s = c.inputStream.bufferedReader().readText(); c.disconnect()
        val j = JSONArray(s)
        return (0 until j.length()).map { j.getJSONArray(it) }.filter { it.optString(0).isNotBlank() }.map {
            Tx(it.optString(0).toLongOrNull() ?: System.nanoTime(), it.optString(1).take(10), it.optString(2) == "Pemasukan", it.optString(3), it.optLong(4), it.optString(5))
        }
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
                    val u = user
                    if (u == null) LoginScreen { st.user = it; user = it }
                    else Main(st, u) { st.user = null; user = null }
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
fun Avatar(name: String, size: Dp, bg: Color = Card2) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), Alignment.Center) {
        Text(name.first().uppercase(), color = White, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.42f).sp)
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
    val sub = if (showDate) listOf(t.d, t.n).filter { it.isNotBlank() }.joinToString(" · ") else if (t.n.isBlank()) "Tanpa catatan" else t.n
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = CardC, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(t.c, 42.dp)
            Column(Modifier.weight(1f)) {
                Text(t.c, color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(sub, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text((if (t.income) "+" else "-") + rp(t.a), color = if (t.income) Green else White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun NavItem(sel: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (sel) BG else Card2) {
        Row(Modifier.height(48.dp).padding(horizontal = if (sel) 18.dp else 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, label, tint = if (sel) White else Muted)
            if (sel) Text(label, color = White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp), Alignment.Center) {
        Surface(shape = CircleShape, color = Color(0xFF191D29), border = BorderStroke(1.dp, Color(0xFF262B3A)), shadowElevation = 8.dp) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NavItem(tab == 0, Icons.Rounded.Home, "Beranda") { onTab(0) }
                NavItem(tab == 1, Icons.Rounded.BarChart, "Laporan") { onTab(1) }
                Surface(onClick = onAdd, shape = CircleShape, color = Lime) {
                    Box(Modifier.size(48.dp), Alignment.Center) { Icon(Icons.Rounded.Add, "Catat", tint = BG) }
                }
                NavItem(tab == 2, Icons.Rounded.Person, "Profil") { onTab(2) }
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
fun LoginScreen(onPick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("NoteCash", fontSize = 38.sp, fontWeight = FontWeight.SemiBold, color = White, letterSpacing = (-0.5).sp)
        Text("Siapa yang mencatat hari ini?", color = Muted, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))
        USERS.forEach { u ->
            Surface(onClick = { onPick(u) }, shape = RoundedCornerShape(28.dp), color = CardC, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u, 52.dp, Blue)
                    Column {
                        Text(u, color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Tab sheet: $u", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Main(st: Store, user: String, onLogout: () -> Unit) {
    var all by remember(user) { mutableStateOf(st.load(user)) }
    var tab by remember { mutableStateOf(0) }
    var edit by remember { mutableStateOf<Tx?>(null) }
    var formIncome by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var confirmPull by remember { mutableStateOf(false) }
    var hide by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var homeA by remember { mutableStateOf(LocalDate.now()) }
    var repA by remember { mutableStateOf(LocalDate.now()) }
    var repMode by remember { mutableStateOf(1) }
    var chart by remember { mutableStateOf(st.chart) }
    var url by remember { mutableStateOf(st.url) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    fun commit(l: List<Tx>) {
        all = l; st.save(user, l)
        if (url.isNotBlank()) scope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Sheet.push(url, user, l) } }
            if (r.isFailure) snack.showSnackbar("Gagal sinkron ke Sheet")
        }
    }
    fun pushNow() {
        if (url.isBlank()) { scope.launch { snack.showSnackbar("Isi URL Apps Script dulu") }; return }
        scope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Sheet.push(url, user, all) } }
            snack.showSnackbar(if (r.isSuccess) "Terkirim ke tab $user" else "Gagal mengirim. Cek URL.")
        }
    }
    fun pullNow() {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { Sheet.pull(url, user) } }
                .onSuccess { all = it; st.save(user, it); snack.showSnackbar("Diambil: ${it.size} transaksi") }
                .onFailure { snack.showSnackbar("Gagal mengambil. Cek URL dan akses Anyone.") }
        }
    }

    Box(Modifier.fillMaxSize().background(BG)) {
        when (tab) {
            0 -> HomeScreen(user, all, homeA, { homeA = it }, hide, { hide = !hide }, { searching = true }, { inc -> edit = null; formIncome = inc; showForm = true }) { edit = it; showForm = true }
            1 -> ReportScreen(all, repMode, { repMode = it }, repA, { repA = it }, chart) { edit = it; showForm = true }
            else -> ProfileScreen(user, url, { url = it; st.url = it }, chart, { chart = it; st.chart = it }, { pushNow() },
                { if (url.isBlank()) scope.launch { snack.showSnackbar("Isi URL Apps Script dulu") } else confirmPull = true }, onLogout)
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
        BottomBar(tab, { tab = it }, { edit = null; formIncome = false; showForm = true }, Modifier.align(Alignment.BottomCenter))
        if (searching) SearchScreen(all, { searching = false }) { edit = it; showForm = true }
    }

    if (showForm) Form(
        t = edit, initialIncome = formIncome,
        onSave = { tx -> commit(if (edit == null) all + tx else all.map { if (it.id == tx.id) tx else it }); homeA = LocalDate.parse(tx.d); showForm = false },
        onDel = edit?.let { e -> { commit(all.filter { it.id != e.id }); showForm = false } },
        onClose = { showForm = false }
    )
    if (confirmPull) AlertDialog(
        onDismissRequest = { confirmPull = false },
        title = { Text("Ambil dari Sheet?") },
        text = { Text("Data $user di HP akan diganti isi tab \"$user\".") },
        confirmButton = { TextButton({ confirmPull = false; pullNow() }) { Text("Ambil") } },
        dismissButton = { TextButton({ confirmPull = false }) { Text("Batal") } }
    )
}

@Composable
fun HomeScreen(
    user: String, all: List<Tx>, anchor: LocalDate, onAnchor: (LocalDate) -> Unit,
    hide: Boolean, onHide: () -> Unit, onSearch: () -> Unit, onAdd: (Boolean) -> Unit, onEdit: (Tx) -> Unit
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
                Avatar(user, 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(greet, color = Muted, fontSize = 13.sp)
                    Text(user, color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
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
                            Text(if (net >= 0) "Surplus " + rp(net) + " bulan ini" else "Defisit " + rp(-net) + " bulan ini", color = Muted, fontSize = 13.sp)
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
                        Text("Skor keuangan", color = White.copy(alpha = 0.85f), fontSize = 13.sp)
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
                val frac = if (s.inc > 0) (s.exp.toFloat() / s.inc).coerceIn(0f, 1f) else if (s.exp > 0) 1f else 0f
                Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(10.dp)).background(Card2)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Blue))
                }
                Spacer(Modifier.height(6.dp))
                Text(if (s.inc > 0) (s.exp * 100 / s.inc).toString() + "% dari pemasukan bulan ini" else "Belum ada pemasukan bulan ini", color = Muted, fontSize = 12.sp)
            }
        }
        item { Text("Transaksi", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
        if (list.isEmpty()) item { Text("Belum ada transaksi bulan ini. Ketuk tombol hijau untuk mencatat.", color = Muted, fontSize = 14.sp) }
        list.groupBy { it.d }.forEach { (d, l) ->
            item(key = "h$d") {
                Text(LocalDate.parse(d).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
            l.forEach { t -> item(key = t.id) { TxRow(t) { onEdit(t) } } }
        }
    }
}

@Composable
fun SearchScreen(all: List<Tx>, onClose: () -> Unit, onEdit: (Tx) -> Unit) {
    var q by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(0) } // 0 semua, 1 pemasukan, 2 pengeluaran
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (res.isEmpty()) item { Text(if (all.isEmpty()) "Belum ada transaksi." else "Tidak ada transaksi yang cocok.", color = Muted, fontSize = 14.sp) }
            items(res, key = { it.id }) { t -> TxRow(t, showDate = true) { onEdit(t) } }
        }
    }
}

@Composable
fun ReportScreen(all: List<Tx>, mode: Int, onMode: (Int) -> Unit, anchor: LocalDate, onAnchor: (LocalDate) -> Unit, showChart: Boolean, onEdit: (Tx) -> Unit) {
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
        item { Text("Cashflow", color = White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp) }
        item {
            Panel {
                Segmented(listOf("Minggu", "Bulan", "Tahun"), mode, onMode)
                PeriodNav(labelOf(mode, from, to), { onAnchor(shiftOf(mode, anchor, -1)) }, { onAnchor(shiftOf(mode, anchor, 1)) })
                if (showChart) Chart(bi, be)
                else Text("Grafik dimatikan. Nyalakan di Profil.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
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
                    Box(Modifier.padding(top = 6.dp, bottom = 12.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(Card2)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(v.toFloat() / s.exp).background(Blue))
                    }
                }
            }
        }
        item { Text("Transaksi", color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        if (list.isEmpty()) item { Text("Belum ada transaksi di periode ini.", color = Muted, fontSize = 14.sp) }
        list.forEach { t -> item(key = t.id) { TxRow(t) { onEdit(t) } } }
    }
}

@Composable
fun ProfileScreen(
    user: String, url: String, onUrl: (String) -> Unit,
    chart: Boolean, onChart: (Boolean) -> Unit, onPush: () -> Unit, onPull: () -> Unit, onLogout: () -> Unit
) {
    var u by remember { mutableStateOf(url) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Profil", color = White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(user, 64.dp, Blue)
                Column {
                    Text(user, color = White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tab sheet: $user", color = Muted, fontSize = 13.sp)
                }
            }
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tampilkan grafik", color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Di halaman Laporan", color = Muted, fontSize = 13.sp)
                }
                Switch(chart, onChart, colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = White))
            }
        }
        Panel {
            Text("Google Sheets", color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(u, { u = it; onUrl(it.trim()) }, Modifier.fillMaxWidth(), label = { Text("URL Apps Script (/exec)") })
            Spacer(Modifier.height(8.dp))
            Text("Perubahan terkirim otomatis ke tab \"$user\".", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = onPush, shape = CircleShape, color = Blue, modifier = Modifier.weight(1f).height(48.dp)) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Kirim ke Sheet", color = White, fontWeight = FontWeight.Medium) }
                }
                Surface(onClick = onPull, shape = CircleShape, color = Card2, modifier = Modifier.weight(1f).height(48.dp)) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Ambil dari Sheet", color = White, fontWeight = FontWeight.Medium) }
                }
            }
        }
        Surface(onClick = onLogout, shape = CircleShape, color = BG, border = BorderStroke(1.dp, Red.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Keluar", color = Red, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
fun Form(t: Tx?, initialIncome: Boolean, onSave: (Tx) -> Unit, onDel: (() -> Unit)?, onClose: () -> Unit) {
    var inc by remember { mutableStateOf(t?.income ?: initialIncome) }
    var cat by remember { mutableStateOf(t?.c ?: (if (initialIncome) IN[0] else OUT[0])) }
    var amt by remember { mutableStateOf(t?.a?.toString() ?: "") }
    var date by remember { mutableStateOf(t?.d ?: LocalDate.now().toString()) }
    var note by remember { mutableStateOf(t?.n ?: "") }
    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (t == null) "Catat transaksi" else "Ubah transaksi", color = White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!inc, { inc = false; cat = OUT[0] }, { Text("Pengeluaran") })
                    FilterChip(inc, { inc = true; cat = IN[0] }, { Text("Pemasukan") })
                }
                OutlinedTextField(amt, { amt = it.filter(Char::isDigit) }, label = { Text("Jumlah (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (if (inc) IN else OUT).forEach { FilterChip(cat == it, { cat = it }, { Text(it) }) } }
                OutlinedTextField(date, { date = it }, label = { Text("Tanggal (yyyy-MM-dd)") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Catatan") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton({
                val a = amt.toLongOrNull() ?: 0L
                val d = runCatching { LocalDate.parse(date).toString() }.getOrNull()
                if (a > 0 && d != null) onSave(Tx(t?.id ?: System.currentTimeMillis(), d, inc, cat, a, note.trim()))
            }) { Text("Simpan", color = Blue) }
        },
        dismissButton = {
            Row {
                if (onDel != null) IconButton(onDel) { Icon(Icons.Rounded.Delete, "Hapus", tint = Red) }
                TextButton(onClose) { Text("Batal", color = Muted) }
            }
        }
    )
}

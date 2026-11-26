@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.udyne.notecash

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val USERS = listOf("mnrdnn", "lya")
val OUT = listOf("Makan", "Transport", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Lainnya")
val IN = listOf("Gaji", "Bonus", "Usaha", "Hadiah", "Lainnya")
val ID = Locale("id", "ID")

data class Tx(val id: Long, val d: String, val income: Boolean, val c: String, val a: Long, val n: String)
class Sum(val inc: Long, val exp: Long)

fun rp(n: Long) = (if (n < 0) "-" else "") + "Rp" + "%,d".format(ID, Math.abs(n))
fun totals(l: List<Tx>) = Sum(l.filter { it.income }.sumOf { it.a }, l.filter { !it.income }.sumOf { it.a })
fun within(all: List<Tx>, from: LocalDate, to: LocalDate) = all.filter { it.d >= from.toString() && it.d <= to.toString() }

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
        enableEdgeToEdge()
        val st = Store(this)
        setContent {
            val dark = isSystemInDarkTheme()
            val cs = when {
                Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = cs) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var user by remember { mutableStateOf(st.user) }
                    val u = user
                    if (u == null) LoginScreen { st.user = it; user = it }
                    else Main(st, u) { st.user = null; user = null }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onPick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("NoteCash", fontSize = 38.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text("Siapa yang mencatat hari ini?", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))
        USERS.forEach { u ->
            Card(onClick = { onPick(u) }, Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Text(u.first().uppercase(), fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimary) }
                    }
                    Column {
                        Text(u, style = MaterialTheme.typography.titleLarge)
                        Text("Tab sheet: $u", style = MaterialTheme.typography.labelMedium)
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
    var showForm by remember { mutableStateOf(false) }
    var confirmPull by remember { mutableStateOf(false) }
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

    val tabs = listOf("Beranda" to Icons.Default.Home, "Laporan" to Icons.Default.DateRange, "Profil" to Icons.Default.Person)
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = { TopAppBar(title = { Text(if (tab == 0) "NoteCash" else tabs[tab].first) }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (n, ic) ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(ic, null) }, label = { Text(n) })
                }
            }
        },
        floatingActionButton = {
            if (tab == 0) ExtendedFloatingActionButton(onClick = { edit = null; showForm = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Catat") })
        }
    ) { pad ->
        when (tab) {
            0 -> HomeScreen(pad, user, all, homeA, { homeA = it }) { edit = it; showForm = true }
            1 -> ReportScreen(pad, all, repMode, { repMode = it }, repA, { repA = it }, chart)
            else -> ProfileScreen(pad, user, url, { url = it; st.url = it }, chart, { chart = it; st.chart = it }, { pushNow() }, { if (url.isBlank()) scope.launch { snack.showSnackbar("Isi URL Apps Script dulu") } else confirmPull = true }, onLogout)
        }
    }

    if (showForm) Form(
        t = edit,
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
fun PeriodNav(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        TextButton(onPrev) { Text("‹", fontSize = 24.sp) }
        Text(label, style = MaterialTheme.typography.titleMedium)
        TextButton(onNext) { Text("›", fontSize = 24.sp) }
    }
}

@Composable
fun Hero(title: String, s: Sum) {
    val cI = MaterialTheme.colorScheme.tertiary
    val cE = MaterialTheme.colorScheme.error
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(22.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(rp(s.inc - s.exp), fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.weight(1f), MaterialTheme.shapes.medium) { Column(Modifier.padding(12.dp)) { Text("Pemasukan", style = MaterialTheme.typography.labelSmall); Text(rp(s.inc), color = cI, fontWeight = FontWeight.SemiBold) } }
                Surface(Modifier.weight(1f), MaterialTheme.shapes.medium) { Column(Modifier.padding(12.dp)) { Text("Pengeluaran", style = MaterialTheme.typography.labelSmall); Text(rp(s.exp), color = cE, fontWeight = FontWeight.SemiBold) } }
            }
        }
    }
}

@Composable
fun TxRow(t: Tx, onClick: () -> Unit) {
    val cI = MaterialTheme.colorScheme.tertiary
    val cE = MaterialTheme.colorScheme.error
    ListItem(
        modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        headlineContent = { Text(t.c) },
        supportingContent = if (t.n.isNotBlank()) ({ Text(t.n) }) else null,
        trailingContent = { Text((if (t.income) "+" else "-") + rp(t.a), color = if (t.income) cI else cE, fontWeight = FontWeight.SemiBold) }
    )
}

@Composable
fun HomeScreen(pad: PaddingValues, user: String, all: List<Tx>, anchor: LocalDate, onAnchor: (LocalDate) -> Unit, onEdit: (Tx) -> Unit) {
    val (from, to) = rangeOf(1, anchor)
    val list = within(all, from, to).sortedByDescending { it.d }
    val s = totals(list)
    LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item { Text("Halo, $user", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp)) }
        item { PeriodNav(labelOf(1, from, to), { onAnchor(shiftOf(1, anchor, -1)) }, { onAnchor(shiftOf(1, anchor, 1)) }) }
        item { Hero("Saldo bulan ini", s) }
        item { Text("Riwayat", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
        if (list.isEmpty()) item { Text("Belum ada transaksi bulan ini. Ketuk Catat untuk menambah.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        list.groupBy { it.d }.forEach { (d, l) ->
            item(key = "h$d") {
                Text(LocalDate.parse(d).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ID)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
            }
            items(l, key = { it.id }) { t -> TxRow(t) { onEdit(t) } }
        }
    }
}

@Composable
fun RowScope.Stat(label: String, value: String) {
    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun Chart(bi: List<Long>, be: List<Long>) {
    val cI = MaterialTheme.colorScheme.tertiary
    val cE = MaterialTheme.colorScheme.error
    val mx = maxOf(1L, bi.maxOrNull() ?: 0L, be.maxOrNull() ?: 0L)
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width / bi.size
                for (i in bi.indices) {
                    val bw = w * 0.38f
                    val hi = size.height * bi[i] / mx
                    val he = size.height * be[i] / mx
                    drawRect(cI, Offset(i * w + w * 0.08f, size.height - hi), Size(bw, hi))
                    drawRect(cE, Offset(i * w + w * 0.08f + bw, size.height - he), Size(bw, he))
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("● Pemasukan", color = cI, style = MaterialTheme.typography.labelMedium)
                Text("● Pengeluaran", color = cE, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ReportScreen(pad: PaddingValues, all: List<Tx>, mode: Int, onMode: (Int) -> Unit, anchor: LocalDate, onAnchor: (LocalDate) -> Unit, showChart: Boolean) {
    val (from, to) = rangeOf(mode, anchor)
    val list = within(all, from, to)
    val s = totals(list)
    val days = ChronoUnit.DAYS.between(from, to) + 1
    val today = LocalDate.now()
    val elapsed = if (today.isBefore(from) || today.isAfter(to)) days else ChronoUnit.DAYS.between(from, today) + 1
    val saving = if (s.inc > 0) ((s.inc - s.exp) * 100 / s.inc).toString() + "%" else "–"
    val cats = list.filter { !it.income }.groupBy { it.c }.mapValues { e -> e.value.sumOf { it.a } }.toList().sortedByDescending { it.second }
    val keys = when (mode) {
        0 -> (0..6).map { from.plusDays(it.toLong()).toString() }
        1 -> (0 until from.lengthOfMonth()).map { from.plusDays(it.toLong()).toString() }
        else -> (1..12).map { "%d-%02d".format(from.year, it) }
    }
    fun key(t: Tx) = if (mode == 2) t.d.take(7) else t.d
    val bi = keys.map { k -> list.filter { it.income && key(it) == k }.sumOf { it.a } }
    val be = keys.map { k -> list.filter { !it.income && key(it) == k }.sumOf { it.a } }

    LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                listOf("Minggu", "Bulan", "Tahun").forEachIndexed { i, t ->
                    SegmentedButton(mode == i, { onMode(i) }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(t) }
                }
            }
        }
        item { PeriodNav(labelOf(mode, from, to), { onAnchor(shiftOf(mode, anchor, -1)) }, { onAnchor(shiftOf(mode, anchor, 1)) }) }
        item { Hero("Saldo periode ini", s) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Rata-rata keluar per hari", rp(s.exp / elapsed))
                Stat("Rasio tabungan", saving)
            }
        }
        if (showChart) item { Chart(bi, be) }
        else item { Text("Grafik dimatikan. Nyalakan di Profil.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        item { Text("Pengeluaran per kategori", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        if (cats.isEmpty()) item { Text("Belum ada pengeluaran di periode ini.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(cats) { (c, v) ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(c); Text(rp(v) + "  " + (v * 100 / s.exp) + "%") }
                LinearProgressIndicator(progress = { v.toFloat() / s.exp }, Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
fun ProfileScreen(
    pad: PaddingValues, user: String, url: String, onUrl: (String) -> Unit,
    chart: Boolean, onChart: (Boolean) -> Unit, onPush: () -> Unit, onPull: () -> Unit, onLogout: () -> Unit
) {
    var u by remember { mutableStateOf(url) }
    Column(Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { Text(user.first().uppercase(), fontSize = 28.sp, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Column {
                Text(user, style = MaterialTheme.typography.titleLarge)
                Text("Tab sheet: $user", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                headlineContent = { Text("Tampilkan grafik") },
                supportingContent = { Text("Di halaman Laporan") },
                trailingContent = { Switch(chart, onChart) }
            )
        }
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Google Sheets", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(u, { u = it; onUrl(it.trim()) }, Modifier.fillMaxWidth(), label = { Text("URL Apps Script (/exec)") })
                Text("Perubahan terkirim otomatis ke tab \"$user\".", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onPush) { Text("Kirim ke Sheet") }
                    OutlinedButton(onPull) { Text("Ambil dari Sheet") }
                }
            }
        }
        OutlinedButton(onLogout, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Keluar") }
    }
}

@Composable
fun Form(t: Tx?, onSave: (Tx) -> Unit, onDel: (() -> Unit)?, onClose: () -> Unit) {
    var inc by remember { mutableStateOf(t?.income ?: false) }
    var cat by remember { mutableStateOf(t?.c ?: OUT[0]) }
    var amt by remember { mutableStateOf(t?.a?.toString() ?: "") }
    var date by remember { mutableStateOf(t?.d ?: LocalDate.now().toString()) }
    var note by remember { mutableStateOf(t?.n ?: "") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (t == null) "Catat transaksi" else "Ubah transaksi") },
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
            }) { Text("Simpan") }
        },
        dismissButton = {
            Row {
                if (onDel != null) IconButton(onDel) { Icon(Icons.Default.Delete, "Hapus") }
                TextButton(onClose) { Text("Batal") }
            }
        }
    )
}

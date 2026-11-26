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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val OUT = listOf("Makan", "Transport", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Lainnya")
val IN = listOf("Gaji", "Bonus", "Usaha", "Hadiah", "Lainnya")
val ID = Locale("id", "ID")

data class Tx(val id: Long, val d: String, val income: Boolean, val c: String, val a: Long, val n: String)

fun rp(n: Long) = "Rp" + "%,d".format(ID, n)

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("catatan", 0)
    var url: String
        get() = p.getString("url", "") ?: ""
        set(v) { p.edit().putString("url", v).apply() }

    fun load(): List<Tx> = runCatching {
        val j = JSONArray(p.getString("tx", "[]"))
        (0 until j.length()).map {
            val o = j.getJSONObject(it)
            Tx(o.getLong("id"), o.getString("d"), o.getBoolean("i"), o.getString("c"), o.getLong("a"), o.optString("n"))
        }
    }.getOrDefault(emptyList())

    fun save(l: List<Tx>) {
        val j = JSONArray()
        l.forEach { j.put(JSONObject().put("id", it.id).put("d", it.d).put("i", it.income).put("c", it.c).put("a", it.a).put("n", it.n)) }
        p.edit().putString("tx", j.toString()).apply()
    }
}

/** Sinkron ke Google Sheets lewat Web App Apps Script. */
object Sheet {
    fun push(url: String, l: List<Tx>) {
        val rows = JSONArray()
        l.sortedBy { it.d }.forEach {
            rows.put(JSONArray().put(it.id.toString()).put(it.d).put(if (it.income) "Pemasukan" else "Pengeluaran").put(it.c).put(it.a).put(it.n))
        }
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "text/plain;charset=utf-8")
        c.outputStream.use { it.write(JSONObject().put("rows", rows).toString().toByteArray()) }
        c.inputStream.close(); c.disconnect()
    }

    fun pull(url: String): List<Tx> {
        val c = URL(url).openConnection() as HttpURLConnection
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
            MaterialTheme(colorScheme = cs) { App(st) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(st: Store) {
    var all by remember { mutableStateOf(st.load()) }
    var mode by remember { mutableStateOf(1) } // 0 minggu, 1 bulan, 2 tahun
    var anchor by remember { mutableStateOf(LocalDate.now()) }
    var edit by remember { mutableStateOf<Tx?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var showSet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    fun commit(l: List<Tx>) {
        all = l; st.save(l)
        if (st.url.isNotBlank()) scope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Sheet.push(st.url, l) } }
            if (r.isFailure) snack.showSnackbar("Gagal sinkron ke Sheet")
        }
    }

    val (from, to) = when (mode) {
        0 -> { val s = anchor.with(DayOfWeek.MONDAY); s to s.plusDays(6) }
        1 -> { val s = anchor.withDayOfMonth(1); s to s.plusMonths(1).minusDays(1) }
        else -> LocalDate.of(anchor.year, 1, 1) to LocalDate.of(anchor.year, 12, 31)
    }
    val list = all.filter { it.d >= from.toString() && it.d <= to.toString() }.sortedByDescending { it.d }
    val inc = list.filter { it.income }.sumOf { it.a }
    val exp = list.filter { !it.income }.sumOf { it.a }
    val cats = list.filter { !it.income }.groupBy { it.c }.mapValues { e -> e.value.sumOf { it.a } }.toList().sortedByDescending { it.second }
    val label = when (mode) {
        0 -> from.format(DateTimeFormatter.ofPattern("d MMM", ID)) + " – " + to.format(DateTimeFormatter.ofPattern("d MMM yyyy", ID))
        1 -> from.format(DateTimeFormatter.ofPattern("MMMM yyyy", ID))
        else -> from.year.toString()
    }
    fun shift(n: Long) {
        anchor = when (mode) {
            0 -> anchor.plusWeeks(n)
            1 -> anchor.withDayOfMonth(1).plusMonths(n)
            else -> anchor.withDayOfYear(1).plusYears(n)
        }
    }
    val keys = when (mode) {
        0 -> (0..6).map { from.plusDays(it.toLong()).toString() }
        1 -> (0 until from.lengthOfMonth()).map { from.plusDays(it.toLong()).toString() }
        else -> (1..12).map { "%d-%02d".format(from.year, it) }
    }
    fun key(t: Tx) = if (mode == 2) t.d.take(7) else t.d
    val bi = keys.map { k -> list.filter { it.income && key(it) == k }.sumOf { it.a } }
    val be = keys.map { k -> list.filter { !it.income && key(it) == k }.sumOf { it.a } }
    val mx = maxOf(1L, bi.maxOrNull() ?: 0, be.maxOrNull() ?: 0)
    val cI = MaterialTheme.colorScheme.tertiary
    val cE = MaterialTheme.colorScheme.error

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(title = { Text("NoteCash") }, actions = {
                IconButton({ showSet = true }) { Icon(Icons.Default.Settings, "Pengaturan") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { edit = null; showForm = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Catat") })
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Minggu", "Bulan", "Tahun").forEachIndexed { i, t ->
                        SegmentedButton(mode == i, { mode = i }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(t) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ shift(-1) }) { Text("‹", fontSize = 24.sp) }
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    TextButton({ shift(1) }) { Text("›", fontSize = 24.sp) }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Saldo periode ini", style = MaterialTheme.typography.labelLarge)
                        Text(rp(inc - exp), fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(Modifier.weight(1f), MaterialTheme.shapes.medium) { Column(Modifier.padding(12.dp)) { Text("Pemasukan", style = MaterialTheme.typography.labelSmall); Text(rp(inc), color = cI, fontWeight = FontWeight.SemiBold) } }
                            Surface(Modifier.weight(1f), MaterialTheme.shapes.medium) { Column(Modifier.padding(12.dp)) { Text("Pengeluaran", style = MaterialTheme.typography.labelSmall); Text(rp(exp), color = cE, fontWeight = FontWeight.SemiBold) } }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
                    Canvas(Modifier.fillMaxWidth().height(110.dp).padding(16.dp)) {
                        val w = size.width / keys.size
                        keys.indices.forEach { i ->
                            val bw = w * 0.38f
                            val hi = size.height * bi[i] / mx
                            val he = size.height * be[i] / mx
                            drawRect(cI, Offset(i * w + w * 0.08f, size.height - hi), Size(bw, hi))
                            drawRect(cE, Offset(i * w + w * 0.08f + bw, size.height - he), Size(bw, he))
                        }
                    }
                }
            }
            item { Text("Pengeluaran per kategori", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            if (cats.isEmpty()) item { Text("Belum ada pengeluaran di periode ini.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(cats) { (c, v) ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(c); Text(rp(v) + "  " + (v * 100 / exp) + "%") }
                    LinearProgressIndicator(progress = { v.toFloat() / exp }, Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
            }
            item { Text("Riwayat", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            if (list.isEmpty()) item { Text("Belum ada transaksi. Ketuk Catat untuk menambah.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(list, key = { it.id }) { t ->
                ListItem(
                    modifier = Modifier.clickable { edit = t; showForm = true },
                    colors = ListItemDefaults.colors(MaterialTheme.colorScheme.surfaceContainer),
                    headlineContent = { Text(t.c) },
                    supportingContent = { Text(t.d + if (t.n.isNotBlank()) "  ·  " + t.n else "") },
                    trailingContent = { Text((if (t.income) "+" else "-") + rp(t.a), color = if (t.income) cI else cE, fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    }

    if (showForm) Form(
        t = edit,
        onSave = { tx -> commit(if (edit == null) all + tx else all.map { if (it.id == tx.id) tx else it }); anchor = LocalDate.parse(tx.d); showForm = false },
        onDel = edit?.let { e -> { commit(all.filter { it.id != e.id }); showForm = false } },
        onClose = { showForm = false }
    )
    if (showSet) SettingsDialog(
        st,
        onPush = { scope.launch { val r = runCatching { withContext(Dispatchers.IO) { Sheet.push(st.url, all) } }; snack.showSnackbar(if (r.isSuccess) "Terkirim ke Sheet" else "Gagal mengirim") } },
        onPull = { u -> scope.launch {
            runCatching { withContext(Dispatchers.IO) { Sheet.pull(u) } }
                .onSuccess { all = it; st.save(it); snack.showSnackbar("Diambil: ${it.size} transaksi") }
                .onFailure { snack.showSnackbar("Gagal mengambil. Cek URL dan akses Anyone.") }
        } },
        onClose = { showSet = false }
    )
}

@OptIn(ExperimentalLayoutApi::class)
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

@Composable
fun SettingsDialog(st: Store, onPush: () -> Unit, onPull: (String) -> Unit, onClose: () -> Unit) {
    var url by remember { mutableStateOf(st.url) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Sinkron Google Sheets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(url, { url = it }, label = { Text("URL Apps Script (/exec)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ st.url = url.trim(); onPush() }) { Text("Kirim") }
                    OutlinedButton({ st.url = url.trim(); onPull(st.url) }) { Text("Ambil") }
                }
            }
        },
        confirmButton = { TextButton({ st.url = url.trim(); onClose() }) { Text("Selesai") } }
    )
}

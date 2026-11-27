// Kosongkan ALLOWED agar semua username valid boleh membuat tab sendiri.
// Isi mis. ['mnrdnn','lya'] untuk membatasi.
const ALLOWED = [];

function tab_(u) {
  u = String(u || '').trim().toLowerCase();
  if (!/^[a-z0-9_]{2,30}$/.test(u)) throw new Error('username tidak valid');
  if (ALLOWED.length && ALLOWED.indexOf(u) < 0) throw new Error('username tidak diizinkan');
  const s = SpreadsheetApp.getActive();
  return s.getSheetByName(u) || s.insertSheet(u);
}

function doPost(e) {
  const d = JSON.parse(e.postData.contents);
  const rows = Array.isArray(d.rows) ? d.rows : [];
  const sh = tab_(d.user);
  // Kunci mencegah dua kiriman bersamaan saling menimpa tab yang sama.
  const lock = LockService.getScriptLock();
  lock.waitLock(20000);
  try {
    sh.clear();
    sh.getRange('A:B').setNumberFormat('@');
    sh.getRange(1, 1, 1, 6).setValues([['ID', 'Tanggal', 'Tipe', 'Kategori', 'Jumlah', 'Catatan']]).setFontWeight('bold');
    if (rows.length) sh.getRange(2, 1, rows.length, 6).setValues(rows);
    sh.setFrozenRows(1);
  } finally {
    lock.releaseLock();
  }
  return ContentService.createTextOutput('ok');
}

function doGet(e) {
  const v = tab_(e.parameter.user).getDataRange().getValues();
  v.shift();
  return ContentService.createTextOutput(JSON.stringify(v)).setMimeType(ContentService.MimeType.JSON);
}

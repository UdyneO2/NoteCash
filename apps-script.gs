const ALLOWED = ['mnrdnn', 'lya'];

function tab_(u) {
  u = String(u || '').trim().toLowerCase();
  if (ALLOWED.indexOf(u) < 0) throw new Error('user tidak dikenal');
  const s = SpreadsheetApp.getActive();
  return s.getSheetByName(u) || s.insertSheet(u);
}

function doPost(e) {
  const d = JSON.parse(e.postData.contents);
  const sh = tab_(d.user);
  sh.clear();
  sh.getRange('A:B').setNumberFormat('@');
  sh.getRange(1, 1, 1, 6).setValues([['ID', 'Tanggal', 'Tipe', 'Kategori', 'Jumlah', 'Catatan']]).setFontWeight('bold');
  if (d.rows.length) sh.getRange(2, 1, d.rows.length, 6).setValues(d.rows);
  sh.setFrozenRows(1);
  return ContentService.createTextOutput('ok');
}

function doGet(e) {
  const v = tab_(e.parameter.user).getDataRange().getValues();
  v.shift();
  return ContentService.createTextOutput(JSON.stringify(v)).setMimeType(ContentService.MimeType.JSON);
}

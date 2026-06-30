package mk.com.snt.kc.warehouse.boundary;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ExcelExporterService {

    @PersistenceContext(unitName = "warehouse_PU")
    private EntityManager em;

    private Connection openConnection() throws Exception {
        return em.unwrap(Connection.class);
    }

    public void exportInventoryEXLSX(String whsCod, Date dateFrom, Date dateTo, List<String> headers, OutputStream outputStream) throws Exception {
        boolean filterByWhs = (whsCod != null && !whsCod.isEmpty());

        String sql;
        if (filterByWhs) {
            sql = "SELECT REGIME, IDE_CUO_COD, IN_REG_NBR, IDE_REG_DAT, WHS_TIM, DEC_COD, CMP_CON_COD, WHS_COD, KEY_ITM_NBR, TAR_PRC_EXT, TARIK, VIT_WGT_NETI, VIT_WGT_NET, VIT_STV, GDS_ORG_CTY, QTY_I, QTY_R, TAR_SUP_COD, TAR_PRF, GDS_DS3 FROM WHS_SAD_INVENTORY_E WHERE (WHS_COD = ? OR WHS_OWNER = ?) AND IDE_REG_DAT BETWEEN ? AND ? ORDER BY IDE_REG_DAT, IN_REG_NBR, KEY_ITM_NBR";
        } else {
            sql = "SELECT REGIME, IDE_CUO_COD, IN_REG_NBR, IDE_REG_DAT, WHS_TIM, DEC_COD, CMP_CON_COD, WHS_COD, KEY_ITM_NBR, TAR_PRC_EXT, TARIK, VIT_WGT_NETI, VIT_WGT_NET, VIT_STV, GDS_ORG_CTY, QTY_I, QTY_R, TAR_SUP_COD, TAR_PRF, GDS_DS3 FROM WHS_SAD_INVENTORY_E WHERE IDE_REG_DAT BETWEEN ? AND ? ORDER BY IDE_REG_DAT, IN_REG_NBR, KEY_ITM_NBR";
        }

        List<ExportColumn> exportColumns = getExportColumns(headers);

        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (filterByWhs) {
                ps.setString(paramIndex++, whsCod);
                ps.setString(paramIndex++, whsCod);
            }
            ps.setDate(paramIndex++, new java.sql.Date(dateFrom.getTime()));
            ps.setDate(paramIndex++, new java.sql.Date(dateTo.getTime()));

            try (ResultSet rs = ps.executeQuery()) {
                java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(outputStream);

                // 1. [Content_Types].xml
                zos.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(zos, "UTF-8"));
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                pw.println("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
                pw.println("  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
                pw.println("  <Default Extension=\"xml\" ContentType=\"application/xml\"/>");
                pw.println("  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
                pw.println("  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                pw.println("</Types>");
                pw.flush();
                zos.closeEntry();

                // 2. _rels/.rels
                zos.putNextEntry(new java.util.zip.ZipEntry("_rels/.rels"));
                pw = new PrintWriter(new OutputStreamWriter(zos, "UTF-8"));
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                pw.println("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
                pw.println("  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>");
                pw.println("</Relationships>");
                pw.flush();
                zos.closeEntry();

                // 3. xl/workbook.xml
                zos.putNextEntry(new java.util.zip.ZipEntry("xl/workbook.xml"));
                pw = new PrintWriter(new OutputStreamWriter(zos, "UTF-8"));
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                pw.println("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
                pw.println("  <sheets>");
                pw.println("    <sheet name=\"WHS Inventory\" sheetId=\"1\" r:id=\"rId1\"/>");
                pw.println("  </sheets>");
                pw.println("</workbook>");
                pw.flush();
                zos.closeEntry();

                // 4. xl/_rels/workbook.xml.rels
                zos.putNextEntry(new java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels"));
                pw = new PrintWriter(new OutputStreamWriter(zos, "UTF-8"));
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                pw.println("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
                pw.println("  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>");
                pw.println("</Relationships>");
                pw.flush();
                zos.closeEntry();

                // 5. xl/worksheets/sheet1.xml (contains the table)
                zos.putNextEntry(new java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"));
                pw = new PrintWriter(new OutputStreamWriter(zos, "UTF-8"));
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                pw.println("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
                pw.println("  <cols>");
                for (int colIdx = 0; colIdx < exportColumns.size(); colIdx++) {
                    pw.println("    <col min=\"" + (colIdx + 1) + "\" max=\"" + (colIdx + 1) + "\" width=\"" + getColumnWidth(colIdx) + "\" customWidth=\"1\"/>");
                }
                pw.println("  </cols>");
                pw.println("  <sheetData>");

                // Header Row (Row 1)
                pw.println("    <row r=\"1\">");
                for (ExportColumn exportColumn : exportColumns) {
                    String colRef = getColName(exportColumns.indexOf(exportColumn) + 1) + "1";
                    pw.println("      <c r=\"" + colRef + "\" t=\"inlineStr\"><is><t>" + escapeXml(exportColumn.header) + "</t></is></c>");
                }
                pw.println("    </row>");

                // Data Rows (Starting at Row 2)
                ResultSetMetaData rsmd = rs.getMetaData();
                int rowIdx = 2;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy");

                while (rs.next()) {
                    pw.println("    <row r=\"" + rowIdx + "\">");
                    for (int cellIdx = 0; cellIdx < exportColumns.size(); cellIdx++) {
                        ExportColumn exportColumn = exportColumns.get(cellIdx);
                        String colRef = getColName(cellIdx + 1) + rowIdx;
                        Object val = rs.getObject(exportColumn.sqlIndex);
                        if (val == null) {
                            continue;
                        }

                        int colType = rsmd.getColumnType(exportColumn.sqlIndex);
                        boolean isNumber = (colType == java.sql.Types.NUMERIC
                                         || colType == java.sql.Types.DECIMAL
                                         || colType == java.sql.Types.INTEGER
                                         || colType == java.sql.Types.BIGINT
                                         || colType == java.sql.Types.DOUBLE
                                         || colType == java.sql.Types.FLOAT
                                         || colType == java.sql.Types.REAL
                                         || colType == java.sql.Types.SMALLINT
                                         || colType == java.sql.Types.TINYINT);

                        if (val instanceof java.util.Date || val instanceof java.sql.Date || val instanceof java.sql.Timestamp) {
                            String dateStr = sdf.format((java.util.Date) val);
                            pw.println("      <c r=\"" + colRef + "\" t=\"inlineStr\"><is><t>" + escapeXml(dateStr) + "</t></is></c>");
                        } else if (isNumber) {
                            pw.println("      <c r=\"" + colRef + "\"><v>" + val.toString() + "</v></c>");
                        } else {
                            pw.println("      <c r=\"" + colRef + "\" t=\"inlineStr\"><is><t>" + escapeXml(val.toString()) + "</t></is></c>");
                        }
                    }
                    pw.println("    </row>");
                    rowIdx++;
                }

                pw.println("  </sheetData>");
                pw.println("</worksheet>");
                pw.flush();
                zos.closeEntry();

                zos.finish();
                zos.flush();
            }
        }
    }

    public List<Map<String, String>> previewInventoryE(String whsCod, Date dateFrom, Date dateTo, List<String> headers) throws Exception {
        boolean filterByWhs = (whsCod != null && !whsCod.isEmpty());
        String sql;
        if (filterByWhs) {
            sql = "SELECT REGIME, IDE_CUO_COD, IN_REG_NBR, IDE_REG_DAT, WHS_TIM, DEC_COD, CMP_CON_COD, WHS_COD, KEY_ITM_NBR, TAR_PRC_EXT, TARIK, VIT_WGT_NETI, VIT_WGT_NET, VIT_STV, GDS_ORG_CTY, QTY_I, QTY_R, TAR_SUP_COD, TAR_PRF, GDS_DS3 FROM WHS_SAD_INVENTORY_E WHERE (WHS_COD = ? OR WHS_OWNER = ?) AND IDE_REG_DAT BETWEEN ? AND ? ORDER BY IDE_REG_DAT, IN_REG_NBR, KEY_ITM_NBR";
        } else {
            sql = "SELECT REGIME, IDE_CUO_COD, IN_REG_NBR, IDE_REG_DAT, WHS_TIM, DEC_COD, CMP_CON_COD, WHS_COD, KEY_ITM_NBR, TAR_PRC_EXT, TARIK, VIT_WGT_NETI, VIT_WGT_NET, VIT_STV, GDS_ORG_CTY, QTY_I, QTY_R, TAR_SUP_COD, TAR_PRF, GDS_DS3 FROM WHS_SAD_INVENTORY_E WHERE IDE_REG_DAT BETWEEN ? AND ? ORDER BY IDE_REG_DAT, IN_REG_NBR, KEY_ITM_NBR";
        }

        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy");
        List<ExportColumn> exportColumns = getExportColumns(headers);

        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (filterByWhs) {
                ps.setString(paramIndex++, whsCod);
                ps.setString(paramIndex++, whsCod);
            }
            ps.setDate(paramIndex++, new java.sql.Date(dateFrom.getTime()));
            ps.setDate(paramIndex++, new java.sql.Date(dateTo.getTime()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<String, String>();
                    for (ExportColumn exportColumn : exportColumns) {
                        Object val = rs.getObject(exportColumn.sqlIndex);
                        if (val == null) {
                            row.put(exportColumn.header, "");
                            continue;
                        }

                        int colType = rs.getMetaData().getColumnType(exportColumn.sqlIndex);
                        if (val instanceof java.util.Date || val instanceof java.sql.Date || val instanceof java.sql.Timestamp) {
                            row.put(exportColumn.header, sdf.format((java.util.Date) val));
                        } else {
                            row.put(exportColumn.header, val.toString());
                        }
                    }
                    row.put("indexId", String.valueOf(row.hashCode()));
                    list.add(row);
                }
            }
        }
        return list;
    }

    private List<ExportColumn> getExportColumns(List<String> headers) {
        List<ExportColumn> exportColumns = new ArrayList<ExportColumn>();
        if (headers == null) {
            return exportColumns;
        }

        int headerIndex = 0;
        for (int sqlIndex = 1; sqlIndex <= 20; sqlIndex++) {
            if (sqlIndex == 13 || sqlIndex == 17) {
                continue;
            }
            if (headerIndex < headers.size()) {
                exportColumns.add(new ExportColumn(headers.get(headerIndex), sqlIndex));
                headerIndex++;
            }
        }
        return exportColumns;
    }

    private int getColumnWidth(int colIdx) {
        if (colIdx >= 8) {
            return 24;
        }
        return 18;
    }

    private String getColName(int col) {
        StringBuilder sb = new StringBuilder();
        while (col > 0) {
            int rem = (col - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            col = (col - 1) / 26;
        }
        return sb.toString();
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static class ExportColumn {
        private final String header;
        private final int sqlIndex;

        private ExportColumn(String header, int sqlIndex) {
            this.header = header;
            this.sqlIndex = sqlIndex;
        }
    }
}

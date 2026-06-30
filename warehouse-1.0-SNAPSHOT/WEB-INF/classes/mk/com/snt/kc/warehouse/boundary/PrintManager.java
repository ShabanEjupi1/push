/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.Stateless
 *  javax.inject.Inject
 *  net.sf.jasperreports.engine.JRDataSource
 *  net.sf.jasperreports.engine.JRException
 *  net.sf.jasperreports.engine.JRExporterParameter
 *  net.sf.jasperreports.engine.JasperExportManager
 *  net.sf.jasperreports.engine.JasperFillManager
 *  net.sf.jasperreports.engine.JasperPrint
 *  net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
 *  net.sf.jasperreports.engine.export.JRPdfExporter
 *  net.sf.jasperreports.engine.export.JRPdfExporterParameter
 *  org.omnifaces.util.Faces
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import mk.com.snt.kc.warehouse.boundary.PdfReportFilter;
import mk.com.snt.kc.warehouse.boundary.TraderManager;
import mk.com.snt.kc.warehouse.boundary.WarehouseExitsFilter;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData;
import mk.com.snt.kc.warehouse.boundary.WarehouseManager;
import mk.com.snt.kc.warehouse.domain.Trader;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.util.Utils;
import mk.com.snt.kc.warehouse.view.SessionData;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;
import org.omnifaces.util.Faces;

@Stateless
public class PrintManager {
    private static final String UNDECLARED_EXITS_REPORT = "/mk/com/snt/kc/warehouse/print/undeclared_exits.jasper";
    private static final String UNDECLARED_EXITS_REPORT1 = "/mk/com/snt/kc/warehouse/print/undeclared_exits1.jasper";
    private static final String DAILY_EXITS_REPORT = "/mk/com/snt/kc/warehouse/print/daily_exits.jasper";
    private static final int UNDECLARED_EXITS_REPORT1_ROWS_LIMIT = 99;
    @Inject
    WarehouseManager warehouseManager;
    @Inject
    TraderManager traderManager;
    @Inject
    SessionData sessionData;
    @Resource(mappedName="WHS_REPO")
    String WHS_REPO;

    public byte[] printPdf(PdfReportFilter filter) {
        Trader trader = this.traderManager.getTrader(filter.getTrader());
        filter.setFirst(0);
        filter.setPageSize(0);
        List<WarehouseInventoryData> results = this.warehouseManager.getRemainingData(filter);
        HashMap<String, Object> parms = new HashMap<String, Object>();
        parms.put("trader", trader);
        parms.put("month", filter.getMonth());
        byte[] result = this.printPdf(UNDECLARED_EXITS_REPORT, parms, results);
        this.savePdf(result);
        return result;
    }

    public byte[] printPdf(String traderFiscalNumber, String month, List<WarehouseInventoryData> results) {
        try {
            Trader trader = this.traderManager.getTrader(traderFiscalNumber);
            HashMap<String, Object> parms = new HashMap<String, Object>();
            parms.put("trader", trader);
            parms.put("month", month);
            parms.put("whsCode", results != null && !results.isEmpty() ? results.get(0).getWi().getWhsCode() : null);
            ArrayList<JasperPrint> jasperPrintList = new ArrayList<JasperPrint>();
            for (int i = 0; i < results.size(); i += 99) {
                List<WarehouseInventoryData> reportResults = results.subList(i, Math.min(results.size(), i + 99));
                JasperPrint jp = this.createJasperPrint(UNDECLARED_EXITS_REPORT1, reportResults, parms);
                jasperPrintList.add(jp);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT_LIST, jasperPrintList);
            exporter.setParameter((JRExporterParameter)JRPdfExporterParameter.IS_CREATING_BATCH_MODE_BOOKMARKS, (Object)Boolean.TRUE);
            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, (Object)baos);
            exporter.exportReport();
            byte[] result = baos.toByteArray();
            this.savePdf(result);
            return result;
        }
        catch (JRException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private void savePdf(byte[] result) {
        File userFolder = this.createUserFolder();
        this.saveFile(userFolder, result);
    }

    private void saveFile(File userFolder, byte[] result) {
        StringBuilder filePath = new StringBuilder();
        filePath.append(userFolder.getAbsolutePath()).append(File.separator).append(Utils.convert(new Date(), "yyyy-MM-dd HH-mm-ss")).append(".pdf");
        try (FileOutputStream stream = new FileOutputStream(filePath.toString());){
            stream.write(result);
        }
        catch (IOException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private File createUserFolder() {
        StringBuilder folderPath = new StringBuilder();
        folderPath.append(this.WHS_REPO).append(File.separator).append(this.sessionData.getCurrentUsername());
        File userFolder = new File(folderPath.toString());
        if (!userFolder.exists()) {
            userFolder.mkdir();
        }
        return userFolder;
    }

    public byte[] printPdfExits(WarehouseExitsFilter filter) {
        Trader trader = this.traderManager.getTrader(filter.getTrader());
        filter.setPageSize(0);
        filter.setFirst(0);
        List<WarehouseExit> results = this.warehouseManager.getExits(filter);
        HashMap<String, Serializable> parms = new HashMap<String, Serializable>();
        parms.put("trader", trader);
        parms.put("filter", filter);
        return this.printPdf(DAILY_EXITS_REPORT, parms, results);
    }

    private InputStream getReport(String reportName) {
        InputStream reportXMLStream = this.getClass().getResourceAsStream(reportName);
        return reportXMLStream;
    }

    private byte[] printPdf(String reportName, Map parms, List results) {
        try {
            JasperPrint jasperPrint = this.createJasperPrint(reportName, results, parms);
            return JasperExportManager.exportReportToPdf((JasperPrint)jasperPrint);
        }
        catch (JRException ex) {
            Logger.getLogger(PrintManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private JasperPrint createJasperPrint(String reportName, List results, Map parms) throws JRException {
        InputStream reportXMLStream = this.getReport(reportName);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource((Collection)results);
        parms.put("REPORT_LOCALE", Faces.getLocale());
        return JasperFillManager.fillReport((InputStream)reportXMLStream, (Map)parms, (JRDataSource)dataSource);
    }
}

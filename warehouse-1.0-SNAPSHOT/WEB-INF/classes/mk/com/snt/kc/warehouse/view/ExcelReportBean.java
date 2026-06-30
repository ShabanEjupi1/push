package mk.com.snt.kc.warehouse.view;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import mk.com.snt.kc.warehouse.boundary.ExcelExporterService;

@Named
@SessionScoped
public class ExcelReportBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private ExcelExporterService excelExporterService;

    @Inject
    private SessionData sessionData;

    private Date dateFrom;
    private Date dateTo;
    private List<Map<String, String>> previewData;
    private List<String> columnNames;
    private boolean previewLoaded = false;

    public void init() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getExternalContext().isResponseCommitted() || context.isPostback()) {
            return;
        }
        if (dateFrom == null) {
            dateFrom = new Date();
        }
        if (dateTo == null) {
            dateTo = new Date();
        }
        previewData = null;
        columnNames = null;
        previewLoaded = false;
    }

    private List<String> getExportHeaders() {
        FacesContext context = FacesContext.getCurrentInstance();
        java.util.ResourceBundle bundle = context.getApplication().getResourceBundle(context, "msgs");
        List<String> headers = new ArrayList<String>();
        String[] keys = {
            "regime", "officeCode", "registerNumber", "registrationDate",
            "endDate", "declarantCode", "traderFiscalNumber", "whsCode",
            "sadItemNbr", "procedure", "tariff", "inputNetWeight",
            "customsValue", "countryOfOrigin", "inputQuantity",
            "measurementUnit", "cefta", "goodsDescription"
        };
        for (String key : keys) {
            String val = key;
            try {
                val = bundle.getString(key);
            } catch (Exception e) {
                // fallback
            }
            headers.add(val);
        }
        return headers;
    }

    public void exportExcel() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            excelExporterService.exportInventoryEXLSX(sessionData.getTrader(), dateFrom, dateTo, getExportHeaders(), buffer);
            byte[] xlsxBytes = buffer.toByteArray();

            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"WHS Inventory.xlsx\"");
            response.setContentLength(xlsxBytes.length);
            response.getOutputStream().write(xlsxBytes);
            response.getOutputStream().flush();

            facesContext.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Excel export failed: " + msg, null));
        }
    }

    public void loadPreview() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        try {
            previewData = excelExporterService.previewInventoryE(sessionData.getTrader(), dateFrom, dateTo, getExportHeaders());
            if (previewData != null && !previewData.isEmpty()) {
                columnNames = getExportHeaders();
                previewLoaded = true;
            } else {
                previewLoaded = false;
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "No records found for the selected range.", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Preview failed: " + msg, null));
        }
    }

    public Date getDateFrom() { return dateFrom; }
    public void setDateFrom(Date dateFrom) { this.dateFrom = dateFrom; }

    public Date getDateTo() { return dateTo; }
    public void setDateTo(Date dateTo) { this.dateTo = dateTo; }

    public List<Map<String, String>> getPreviewData() { return previewData; }
    public void setPreviewData(List<Map<String, String>> previewData) { this.previewData = previewData; }

    public List<String> getColumnNames() { return columnNames; }
    public void setColumnNames(List<String> columnNames) { this.columnNames = columnNames; }

    public boolean isPreviewLoaded() { return previewLoaded; }
    public void setPreviewLoaded(boolean previewLoaded) { this.previewLoaded = previewLoaded; }
}

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
            "regime", "officeCode", "officeName", "registerNumber", "registrationDate",
            "endDate", "declarantCode", "decRefYear", "traderFiscalNumber", "whsCode",
            "sadItemNbr", "procedure", "tariff", "inputNetWeight", "remainingNetWeight",
            "customsValue", "countryOfOrigin", "inputQuantity", "remainingQuantity",
            "measurementUnit", "goodsDescription", "cefta"
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
            response.setHeader("Content-Disposition", "attachment; filename=\"WHS_SAD_INVENTORY_E.xlsx\"");
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

    public Date getDateFrom() { return dateFrom; }
    public void setDateFrom(Date dateFrom) { this.dateFrom = dateFrom; }

    public Date getDateTo() { return dateTo; }
    public void setDateTo(Date dateTo) { this.dateTo = dateTo; }
}

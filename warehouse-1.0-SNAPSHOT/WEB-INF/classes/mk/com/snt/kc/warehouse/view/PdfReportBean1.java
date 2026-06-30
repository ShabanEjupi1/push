/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.inject.Inject
 *  javax.inject.Named
 *  org.omnifaces.util.Faces
 *  org.primefaces.model.DefaultStreamedContent
 *  org.primefaces.model.StreamedContent
 */
package mk.com.snt.kc.warehouse.view;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.boundary.PdfReportFilter1;
import mk.com.snt.kc.warehouse.boundary.PrintManager;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData;
import mk.com.snt.kc.warehouse.boundary.WarehouseManager;
import mk.com.snt.kc.warehouse.view.SessionData;
import org.omnifaces.util.Faces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@SessionScoped
@Named(value="pdfBean1")
public class PdfReportBean1
implements Serializable {
    @Inject
    WarehouseManager warehouseManager;
    @Inject
    PrintManager printManager;
    @Inject
    SessionData sessionData;
    private List<WarehouseInventoryData> data;
    private PdfReportFilter1 filter;

    public void init() {
        if (Faces.isAjaxRequest()) {
            return;
        }
        this.setupFilter();
        this.clearData();
    }

    private void setupData() {
        this.data = this.warehouseManager.getRemainingData1(this.filter);
    }

    public void search() {
        this.setupData();
    }

    public void cancel() {
        this.setupFilter();
        this.clearData();
    }

    protected void clearData() {
        this.data = null;
    }

    public StreamedContent getPdf() {
        return new DefaultStreamedContent((InputStream)new ByteArrayInputStream(this.printManager.printPdf(this.filter.getTrader(), this.filter.getMonth(), this.data)), "application/pdf", "report.pdf");
    }

    public List<WarehouseInventoryData> getData() {
        return this.data;
    }

    public void setupFilter() {
        this.filter = new PdfReportFilter1();
        this.filter.setupMonth();
        this.filter.setTrader(this.sessionData.getTrader());
    }

    public PdfReportFilter1 getFilter() {
        return this.filter;
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.inject.Inject
 *  javax.inject.Named
 *  org.omnifaces.util.Faces
 *  org.primefaces.model.DefaultStreamedContent
 *  org.primefaces.model.LazyDataModel
 *  org.primefaces.model.SortOrder
 *  org.primefaces.model.StreamedContent
 */
package mk.com.snt.kc.warehouse.view;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.boundary.PdfReportFilter;
import mk.com.snt.kc.warehouse.boundary.PrintManager;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData;
import mk.com.snt.kc.warehouse.boundary.WarehouseManager;
import mk.com.snt.kc.warehouse.view.SessionData;
import org.omnifaces.util.Faces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.StreamedContent;

@SessionScoped
@Named(value="pdfBean")
public class PdfReportBean
implements Serializable {
    @Inject
    WarehouseManager warehouseManager;
    @Inject
    PrintManager printManager;
    @Inject
    SessionData sessionData;
    private LazyDataModel<WarehouseInventoryData> data;
    private PdfReportFilter filter;

    public void init() {
        if (Faces.isAjaxRequest()) {
            return;
        }
        this.setupFilter();
        this.setupData();
    }

    private void setupData() {
        this.data = new LazyDataModel<WarehouseInventoryData>(){

            public List<WarehouseInventoryData> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                PdfReportBean.this.filter.setFirst(first);
                PdfReportBean.this.filter.setPageSize(pageSize);
                return PdfReportBean.this.warehouseManager.getRemainingData(PdfReportBean.this.filter);
            }
        };
        this.data.setRowCount(this.warehouseManager.countRemainingData(this.filter));
        this.data.setPageSize(10);
    }

    public void search() {
        this.setupData();
    }

    public void cancel() {
        this.setupFilter();
        this.setupData();
    }

    public StreamedContent getPdf() {
        return new DefaultStreamedContent((InputStream)new ByteArrayInputStream(this.printManager.printPdf(this.filter)), "application/pdf", "report.pdf");
    }

    public LazyDataModel<WarehouseInventoryData> getData() {
        return this.data;
    }

    private void setupFilter() {
        this.filter = new PdfReportFilter();
        this.filter.setTrader(this.sessionData.getCurrentUsername());
    }

    public PdfReportFilter getFilter() {
        return this.filter;
    }
}

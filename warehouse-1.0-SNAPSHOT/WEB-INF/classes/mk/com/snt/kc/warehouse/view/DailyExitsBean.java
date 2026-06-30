/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.inject.Inject
 *  javax.inject.Named
 *  org.omnifaces.util.Faces
 *  org.omnifaces.util.Messages
 *  org.primefaces.context.RequestContext
 *  org.primefaces.event.SelectEvent
 *  org.primefaces.model.DefaultStreamedContent
 *  org.primefaces.model.LazyDataModel
 *  org.primefaces.model.SortOrder
 *  org.primefaces.model.StreamedContent
 */
package mk.com.snt.kc.warehouse.view;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.InvalidDataException;
import mk.com.snt.kc.warehouse.boundary.PrintManager;
import mk.com.snt.kc.warehouse.boundary.TraderManager;
import mk.com.snt.kc.warehouse.boundary.WarehouseExitsFilter;
import mk.com.snt.kc.warehouse.boundary.WarehouseManager;
import mk.com.snt.kc.warehouse.domain.Trader;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.util.Utils;
import mk.com.snt.kc.warehouse.view.SessionData;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.StreamedContent;

@SessionScoped
@Named
public class DailyExitsBean
implements Serializable {
    @Inject
    WarehouseManager warehouseManager;
    @Inject
    SessionData sessionData;
    @Inject
    PrintManager printManager;
    private LazyDataModel<WarehouseExit> data;
    private WarehouseExitsFilter filter;
    @Inject
    TraderManager traderManager;
    private Trader trader;
    private WarehouseExit warehouseExit;
    private boolean storn;

    public void init() {
        if (Faces.isAjaxRequest()) {
            return;
        }
        this.setupFilter();
    }

    public void setupFilter() {
        this.filter = new WarehouseExitsFilter();
        if (this.storn) {
            this.trader = null;
        } else {
            this.filter.setTrader(this.sessionData.getTrader());
        }
        this.filter.setExitDateFrom(new Date());
        this.filter.setExitDateTo(new Date());
    }

    protected void setupData() {
        this.setData(new LazyDataModel<WarehouseExit>(){

            public List<WarehouseExit> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                DailyExitsBean.this.filter.setFirst(first);
                DailyExitsBean.this.filter.setSortField(sortField);
                if (sortField != null) {
                    DailyExitsBean.this.filter.setSortOrder(Utils.convert(sortOrder));
                }
                DailyExitsBean.this.data.setRowCount(DailyExitsBean.this.warehouseManager.countExits(DailyExitsBean.this.filter));
                return DailyExitsBean.this.warehouseManager.getExits(DailyExitsBean.this.filter);
            }
        });
        this.data.setPageSize(5);
    }

    public String search() {
        if (!this.storn) {
            this.filter.setTrader(this.sessionData.getTrader());
        }
        this.setupData();
        return "";
    }

    public String cancel() {
        this.setupFilter();
        this.data = null;
        this.trader = null;
        this.warehouseExit = null;
        return "";
    }

    public WarehouseExitsFilter getFilter() {
        return this.filter;
    }

    public void setFilter(WarehouseExitsFilter filter) {
        this.filter = filter;
    }

    public LazyDataModel<WarehouseExit> getData() {
        return this.data;
    }

    public void setData(LazyDataModel<WarehouseExit> data) {
        this.data = data;
    }

    public List<WarehouseExit.Status> getStatuses() {
        return Arrays.asList(WarehouseExit.Status.values());
    }

    public StreamedContent getPdfExits() {
        return new DefaultStreamedContent((InputStream)new ByteArrayInputStream(this.printManager.printPdfExits(this.filter)), "application/pdf", "report_exits.pdf");
    }

    public boolean isStorn() {
        return this.storn;
    }

    public void setStorn(boolean storn) {
        this.storn = storn;
    }

    public List<Trader> completeTrader(String fiscalNumber) {
        return this.traderManager.getValidTraders(new Date(), fiscalNumber);
    }

    public void onSelectTrader(SelectEvent event) {
        if (this.trader != null) {
            this.filter.setTrader(this.trader.getFiscalNumber());
        }
    }

    public Trader getTrader() {
        return this.trader;
    }

    public void setTrader(Trader trader) {
        this.trader = trader;
    }

    public void stornExit(WarehouseExit exit) {
        this.warehouseExit = exit;
    }

    public void stornExitAction() {
        try {
            this.warehouseManager.stornExit(this.warehouseExit);
            Messages.addGlobalInfo((String)"WarehouseExit.storn.success", (Object[])new Object[0]);
            this.warehouseExit = null;
            this.setupData();
        }
        catch (InvalidDataException ide) {
            RequestContext.getCurrentInstance().addCallbackParam("validationFailed", (Object)true);
            Messages.addGlobalError((String)ide.getMessage(), (Object[])ide.getParams());
        }
    }

    public void cancelStorning() {
        this.warehouseExit = null;
    }

    public WarehouseExit getWarehouseExit() {
        return this.warehouseExit;
    }

    public void setWarehouseExit(WarehouseExit warehouseExit) {
        this.warehouseExit = warehouseExit;
    }
}

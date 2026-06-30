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
 *  org.primefaces.model.LazyDataModel
 *  org.primefaces.model.SortOrder
 */
package mk.com.snt.kc.warehouse.view;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.InvalidDataException;
import mk.com.snt.kc.warehouse.boundary.UnresolvedExitsFilter;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData;
import mk.com.snt.kc.warehouse.boundary.WarehouseManager;
import mk.com.snt.kc.warehouse.boundary.WrittenOffWithoutExitsFilter;
import mk.com.snt.kc.warehouse.domain.Trader;
import mk.com.snt.kc.warehouse.domain.UnresolvedExit;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.util.Utils;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

@SessionScoped
@Named
public class RelinkBean
implements Serializable {
    @Inject
    WarehouseManager warehouseManager;
    private LazyDataModel<WarehouseInventoryData> im7Data;
    private WrittenOffWithoutExitsFilter filter;
    private Trader trader;
    private LazyDataModel<UnresolvedExit> exitsData;
    private UnresolvedExitsFilter unresolvedFilter;
    private WarehouseInventoryData selectedItem;

    public void init() {
        if (Faces.isAjaxRequest()) {
            return;
        }
        this.selectedItem = null;
        this.setupFilter();
        this.setupData();
    }

    protected void setupFilter() {
        this.filter = new WrittenOffWithoutExitsFilter();
        this.trader = null;
    }

    private void setupData() {
        this.im7Data = new LazyDataModel<WarehouseInventoryData>(){

            public List<WarehouseInventoryData> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                RelinkBean.this.filter.setFirst(first);
                RelinkBean.this.filter.setPageSize(pageSize);
                RelinkBean.this.filter.setSortField(sortField);
                RelinkBean.this.filter.setSortOrder(Utils.convert(sortOrder));
                System.out.println("COUNT IM7");
                RelinkBean.this.im7Data.setRowCount(RelinkBean.this.warehouseManager.countWrittenOffIM7WithoutExits(RelinkBean.this.filter));
                System.out.println("COUNT IM7 END");
                System.out.println("GET IM7");
                List<WarehouseInventoryData> results = RelinkBean.this.warehouseManager.getWrittenOffIM7WithoutExits(RelinkBean.this.filter);
                System.out.println("GET IM7 END");
                return results;
            }
        };
        this.im7Data.setPageSize(10);
    }

    public void onSelectTrader(SelectEvent event) {
        if (this.trader != null) {
            this.filter.setTrader(this.trader.getFiscalNumber());
        }
    }

    public void search() {
    }

    public void cancel() {
        this.setupFilter();
    }

    public void selectItem(WarehouseInventoryData item) {
        this.selectedItem = item;
        this.setupExitsData(item);
    }

    private void setupExitsData(WarehouseInventoryData item) {
        this.unresolvedFilter = new UnresolvedExitsFilter();
        this.unresolvedFilter.setTrader(item.getWi().getTraderFiscalNumber());
        this.exitsData = new LazyDataModel<UnresolvedExit>(){

            public List<UnresolvedExit> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                RelinkBean.this.unresolvedFilter.setFirst(first);
                RelinkBean.this.unresolvedFilter.setPageSize(pageSize);
                RelinkBean.this.unresolvedFilter.setSortField(sortField);
                RelinkBean.this.unresolvedFilter.setSortOrder(Utils.convert(sortOrder));
                System.out.println("COUNT EXITS");
                RelinkBean.this.exitsData.setRowCount(RelinkBean.this.warehouseManager.countUnresolvedExits(RelinkBean.this.unresolvedFilter));
                System.out.println("COUNT EXITS END");
                System.out.println("GET EXITS");
                List<UnresolvedExit> results = RelinkBean.this.warehouseManager.getUnresolvedExits(RelinkBean.this.unresolvedFilter);
                System.out.println("GET EXITS END");
                return results;
            }
        };
        this.exitsData.setPageSize(5);
    }

    public void relinkExit(UnresolvedExit unresolvedExit) {
        try {
            WarehouseExit exit = this.warehouseManager.getExit(unresolvedExit.getId());
            this.warehouseManager.relinkExit(exit, this.selectedItem.getWi());
            Messages.addGlobalInfo((String)"RelinkBean.relink.success", (Object[])new Object[0]);
        }
        catch (InvalidDataException ide) {
            Messages.addGlobalError((String)ide.getMessage(), (Object[])ide.getParams());
            RequestContext.getCurrentInstance().addCallbackParam("validationFailed", (Object)true);
        }
    }

    public LazyDataModel<WarehouseInventoryData> getIm7Data() {
        return this.im7Data;
    }

    public WrittenOffWithoutExitsFilter getFilter() {
        return this.filter;
    }

    public Trader getTrader() {
        return this.trader;
    }

    public void setTrader(Trader trader) {
        this.trader = trader;
    }

    public LazyDataModel<UnresolvedExit> getExitsData() {
        return this.exitsData;
    }

    public WarehouseInventoryData getSelectedItem() {
        return this.selectedItem;
    }
}

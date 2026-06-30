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
 *  org.primefaces.model.LazyDataModel
 *  org.primefaces.model.SortOrder
 */
package mk.com.snt.kc.warehouse.view;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.InvalidDataException;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryFilter;
import mk.com.snt.kc.warehouse.boundary.WarehouseManager;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.domain.WarehouseInventory;
import mk.com.snt.kc.warehouse.util.Utils;
import mk.com.snt.kc.warehouse.view.SessionData;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;
import org.primefaces.context.RequestContext;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

@SessionScoped
@Named
public class HomeBean
implements Serializable {
    @Inject
    WarehouseManager warehouseManager;
    @Inject
    SessionData sessionData;
    private static final int PAGE_SIZE = 5;
    private LazyDataModel<WarehouseInventoryData> data;
    private WarehouseInventoryData selected;
    private WarehouseInventoryFilter filter;
    private WarehouseExit warehouseExit;
    private LazyDataModel<WarehouseExit> exits;
    private LazyDataModel<WarehouseExit> dailyExits;

    public void init() {
        if (Faces.isAjaxRequest()) {
            return;
        }
        this.selected = null;
        this.exits = null;
        this.warehouseExit = new WarehouseExit();
        this.filter = new WarehouseInventoryFilter();
        this.loadDailyExits();
        this.setupData();
    }

    private void loadDailyExits() {
        this.dailyExits = new LazyDataModel<WarehouseExit>(){

            public List<WarehouseExit> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                return HomeBean.this.warehouseManager.getLatestExits(HomeBean.this.sessionData.getTrader(), first, pageSize);
            }
        };
        this.dailyExits.setRowCount(this.warehouseManager.countLatestExits(this.sessionData.getTrader()));
    }

    private void setupData() {
        this.data = new LazyDataModel<WarehouseInventoryData>(){

            public List<WarehouseInventoryData> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                filters.put("trader", HomeBean.this.sessionData.getTrader());
                if (HomeBean.this.filter.getRegistrationDate() != null) {
                    filters.put("registrationDate", Utils.convert(HomeBean.this.filter.getRegistrationDate()));
                }
                HomeBean.this.filter.setupFilters(filters);
                HomeBean.this.filter.setFirst(first);
                HomeBean.this.filter.setPageSize(pageSize);
                HomeBean.this.filter.setSortField(sortField);
                HomeBean.this.filter.setSortOrder(Utils.convert(sortOrder));
                HomeBean.this.data.setRowCount(HomeBean.this.warehouseManager.countInventory(HomeBean.this.filter));
                return HomeBean.this.warehouseManager.getInventory(HomeBean.this.filter);
            }
        };
        this.data.setPageSize(5);
    }

    public void filterRegistrationDate() {
        this.setupData();
    }

    public void clearRegistrationDate() {
        this.filter.setRegistrationDate(null);
        this.setupData();
    }

    public void save() {
        try {
            this.warehouseManager.saveExit(this.warehouseExit);
            this.cancel();
            Messages.addGlobalInfo((String)"WarehouseExit.save.success", (Object[])new Object[0]);
            this.loadDailyExits();
            this.setupData();
        }
        catch (InvalidDataException ide) {
            RequestContext.getCurrentInstance().addCallbackParam("validationFailed", (Object)true);
            Messages.addGlobalError((String)ide.getMessage(), (Object[])ide.getParams());
        }
    }

    public void cancel() {
        this.warehouseExit = new WarehouseExit();
        this.selected = null;
        this.exits = null;
        this.loadDailyExits();
        this.setupData();
    }

    public void selectItem(WarehouseInventoryData selected) {
        this.selected = selected;
        this.warehouseExit = new WarehouseExit();
        this.warehouseExit.setExitDate(new Date());
        this.warehouseExit.setWarehouseInventory(selected.getWi());
    }

    public void onChangeQuantityOut() {
        this.isQuantityValid();
    }

    private boolean isQuantityValid() {
        WarehouseInventoryData wiData = this.getWarehouseInventoryData();
        if (!wiData.isQuantityValid(this.warehouseExit)) {
            Messages.addGlobalError((String)"WarehouseExit.quantityOut.notEnough", (Object[])new Object[0]);
            this.warehouseExit.clearAmounts();
            return false;
        }
        this.warehouseExit.calculateNetWeight();
        return true;
    }

    private WarehouseInventoryData getWarehouseInventoryData() {
        WarehouseInventoryData wiData = this.warehouseManager.getInventory(this.warehouseExit);
        WarehouseExit exit = this.warehouseExit;
        if (this.warehouseExit.getId() != null) {
            exit = this.warehouseManager.getExit(this.warehouseExit.getId());
        }
        wiData.excludeExit(exit);
        return wiData;
    }

    public void onChangeNetWeightOut() {
        this.isNetWeightValid();
    }

    private boolean isNetWeightValid() {
        WarehouseInventoryData wiData = this.getWarehouseInventoryData();
        if (!wiData.isNetWeightValid(this.warehouseExit)) {
            Messages.addGlobalError((String)"WarehouseExit.netWeightOut.notEnough", (Object[])new Object[0]);
            this.warehouseExit.clearAmounts();
            return false;
        }
        this.warehouseExit.calculateGrossWeight();
        this.warehouseExit.calculateValue();
        return true;
    }

    public void onChangeStockValue() {
    }

    public void editExit(WarehouseExit exit) {
        this.warehouseExit = exit;
        this.selected = this.warehouseManager.getInventory(exit);
    }

    public void deleteExit(WarehouseExit exit) {
        this.warehouseManager.deleteExit(exit);
        Messages.addGlobalInfo((String)"WarehouseExit.delete.success", (Object[])new Object[0]);
        this.loadDailyExits();
        this.setupData();
    }

    public void loadExits(WarehouseInventoryData item) {
        final WarehouseInventory wi = item.getWi();
        this.exits = new LazyDataModel<WarehouseExit>(){

            public List<WarehouseExit> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                return HomeBean.this.warehouseManager.getExits(wi, first, pageSize);
            }
        };
        this.exits.setRowCount(this.warehouseManager.countExits(item.getWi()));
    }

    public boolean hasTodaysExits(WarehouseInventoryData item) {
        int count = this.warehouseManager.countTodaysExits(item.getWi());
        return count > 0;
    }

    public LazyDataModel<WarehouseInventoryData> getData() {
        return this.data;
    }

    public WarehouseInventoryData getSelected() {
        return this.selected;
    }

    public void setSelected(WarehouseInventoryData selected) {
        this.selected = selected;
    }

    public WarehouseExit getWarehouseExit() {
        return this.warehouseExit;
    }

    public WarehouseInventoryFilter getFilter() {
        return this.filter;
    }

    public LazyDataModel<WarehouseExit> getExits() {
        return this.exits;
    }

    public LazyDataModel<WarehouseExit> getDailyExits() {
        return this.dailyExits;
    }
}

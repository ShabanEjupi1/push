/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.inject.Inject
 *  javax.inject.Named
 *  org.omnifaces.util.Faces
 *  org.primefaces.event.SelectEvent
 *  org.primefaces.model.LazyDataModel
 *  org.primefaces.model.SortOrder
 */
package mk.com.snt.kc.warehouse.view;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.audit.AuditFilter;
import mk.com.snt.kc.warehouse.audit.AuditLog;
import mk.com.snt.kc.warehouse.boundary.ReportManager;
import mk.com.snt.kc.warehouse.boundary.TraderManager;
import mk.com.snt.kc.warehouse.domain.Trader;
import mk.com.snt.kc.warehouse.util.Utils;
import mk.com.snt.kc.warehouse.view.SessionData;
import org.omnifaces.util.Faces;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

@SessionScoped
@Named(value="alBean")
public class AuditLogBean
implements Serializable {
    @Inject
    ReportManager reportManager;
    @Inject
    TraderManager traderManager;
    @Inject
    SessionData sessionData;
    private AuditFilter filter;
    private LazyDataModel<AuditLog> data;
    private Trader trader;

    public void init() {
        if (Faces.isAjaxRequest()) {
            return;
        }
        this.setupFilter();
        this.setupData();
    }

    private void setupFilter() {
        this.filter = new AuditFilter();
        this.filter.setUsername(this.sessionData.getCurrentUsername());
        this.trader = null;
    }

    private void setupData() {
        this.data = new LazyDataModel<AuditLog>(){

            public List<AuditLog> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                AuditLogBean.this.filter.setFirst(first);
                AuditLogBean.this.filter.setPageSize(pageSize);
                AuditLogBean.this.filter.setSortField(sortField);
                AuditLogBean.this.filter.setSortOrder(Utils.isNullOrEmpty(sortField) ? null : Utils.convert(sortOrder));
                AuditLogBean.this.data.setRowCount(AuditLogBean.this.reportManager.count(AuditLogBean.this.filter));
                return AuditLogBean.this.reportManager.getData(AuditLogBean.this.filter);
            }
        };
        this.data.setPageSize(this.filter.getPageSize());
    }

    public String cancel() {
        this.setupFilter();
        return "";
    }

    public List<Trader> completeTrader(String fiscalNumber) {
        return this.traderManager.getValidTraders(new Date(), fiscalNumber);
    }

    public void onSelectTrader(SelectEvent event) {
        if (this.trader != null) {
            this.filter.setTraderFiscalNumber(this.trader.getFiscalNumber());
        }
    }

    public List<AuditLog.EntityType> getEntityTypes() {
        return Arrays.asList(AuditLog.EntityType.values());
    }

    public List<AuditLog.ActionType> getActionTypes() {
        return Arrays.asList(AuditLog.ActionType.values());
    }

    public LazyDataModel<AuditLog> getData() {
        return this.data;
    }

    public AuditFilter getFilter() {
        return this.filter;
    }

    public Trader getTrader() {
        return this.trader;
    }

    public void setTrader(Trader trader) {
        this.trader = trader;
    }
}

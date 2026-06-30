/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.inject.Inject
 *  javax.inject.Named
 */
package mk.com.snt.kc.warehouse.view;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.boundary.WarehouseManager;
import mk.com.snt.kc.warehouse.view.SessionData;

@SessionScoped
@Named
public class WarehouseBalanceBean
implements Serializable {
    @Inject
    WarehouseManager warehouseManager;
    @Inject
    SessionData sessionData;
    private BigDecimal balance;

    @PostConstruct
    public void init() {
        this.balance = this.warehouseManager.getBalance(this.sessionData.getTrader());
    }

    public BigDecimal getBalance() {
        return this.balance;
    }
}

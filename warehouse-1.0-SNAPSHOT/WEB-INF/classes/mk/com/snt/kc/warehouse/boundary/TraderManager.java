/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.Stateless
 *  javax.inject.Inject
 *  javax.validation.constraints.NotNull
 */
package mk.com.snt.kc.warehouse.boundary;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import mk.com.snt.kc.warehouse.domain.Trader;
import mk.com.snt.kc.warehouse.persistence.CrudService;
import mk.com.snt.kc.warehouse.persistence.QueryParameter;

@Stateless
public class TraderManager {
    @Inject
    CrudService crudService;

    public Trader getTrader(@NotNull String fiscalNumber) {
        return this.crudService.findSingleWithNamedQuery("Trader.byFiscalNumber", QueryParameter.with("fiscalNumber", fiscalNumber).parameters(), Trader.class);
    }

    public Trader getValidTrader(String fiscalNumber, Date date) {
        return this.crudService.findSingleWithNamedQuery("Trader.getValidByFiscalNumber", QueryParameter.with("fiscalNumber", fiscalNumber).and("date", date).parameters(), Trader.class);
    }

    public Trader getTrader(@NotNull Long id) {
        return this.crudService.find(Trader.class, id);
    }

    public List<Trader> getValidTraders(Date date, String fiscalNumber) {
        return this.crudService.findWithNamedQuery("Trader.getValidLikeFiscalNumber", QueryParameter.with("date", date).and("fiscalNumber", fiscalNumber + "%").parameters(), 0, 30);
    }
}

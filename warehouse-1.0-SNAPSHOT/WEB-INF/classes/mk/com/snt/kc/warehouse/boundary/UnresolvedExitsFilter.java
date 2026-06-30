/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.Serializable;
import mk.com.snt.kc.warehouse.persistence.SearchCriteria;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;

public class UnresolvedExitsFilter
extends SearchFilter
implements Serializable {
    private static final String QUERY = " SELECT ue FROM UnresolvedExit ue";
    private static final String COUNT_QUERY = "SELECT COUNT(ue.id) FROM UnresolvedExit ue";
    private String trader;

    @Override
    public SearchCriteria createSearchCriteria() {
        return new SearchCriteria().addEqualsCriteria("ue.traderFiscalNumber", this.trader);
    }

    @Override
    public String getStartingSql() {
        return QUERY;
    }

    @Override
    public String getDefaultSortField() {
        return "id";
    }

    @Override
    public SearchFilter.SortOrder getDefaultSortOrder() {
        return SearchFilter.SortOrder.DESC;
    }

    @Override
    public String getSortFieldPrefix() {
        return "ue.";
    }

    @Override
    public String getCountStartingSql() {
        return COUNT_QUERY;
    }

    @Override
    public boolean startsWithWhere() {
        return true;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }

    public String getTrader() {
        return this.trader;
    }
}

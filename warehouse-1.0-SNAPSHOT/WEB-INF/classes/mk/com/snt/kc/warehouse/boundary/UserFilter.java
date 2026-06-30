/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.Serializable;
import mk.com.snt.kc.warehouse.persistence.SearchCriteria;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;

public class UserFilter
extends SearchFilter
implements Serializable {
    private static final String QUERY = "SELECT u FROM User u";
    private static final String COUNT_QUERY = "SELECT COUNT(u.id) FROM User u";
    private String username;
    private String name;
    private String trader;
    private String lang;

    @Override
    public SearchCriteria createSearchCriteria() {
        return new SearchCriteria().addLikeCriteria("u.username", this.username).addLikeCriteria("u.name", this.name).addLikeCriteria("u.traderFiscalNumber", this.trader).addLikeCriteria("u.lang", this.lang);
    }

    @Override
    public String getStartingSql() {
        return QUERY;
    }

    @Override
    public String getDefaultSortField() {
        return "username";
    }

    @Override
    public SearchFilter.SortOrder getDefaultSortOrder() {
        return SearchFilter.SortOrder.ASC;
    }

    @Override
    public String getSortFieldPrefix() {
        return "u.";
    }

    @Override
    public String getCountStartingSql() {
        return COUNT_QUERY;
    }

    @Override
    public boolean startsWithWhere() {
        return true;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTrader() {
        return this.trader;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}

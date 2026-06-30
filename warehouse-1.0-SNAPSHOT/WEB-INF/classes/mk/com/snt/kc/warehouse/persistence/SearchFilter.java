/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.persistence;

import mk.com.snt.kc.warehouse.persistence.SearchCriteria;

public abstract class SearchFilter {
    private int first;
    private int pageSize;
    private String sortField;
    private SortOrder sortOrder;

    public SearchFilter() {
        this.first = 0;
        this.pageSize = 10;
    }

    public SearchFilter(int first, int pageSize, String sortField, SortOrder sortOrder) {
        this.first = first;
        this.pageSize = pageSize;
        this.sortField = sortField;
        this.sortOrder = sortOrder;
    }

    public int getFirst() {
        return this.first;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public String getSortField() {
        return this.sortField == null ? this.getSortFieldPrefix() + this.getDefaultSortField() : this.getSortFieldPrefix() + this.sortField;
    }

    public SortOrder getSortOrder() {
        return this.sortOrder == null ? this.getDefaultSortOrder() : this.sortOrder;
    }

    public abstract SearchCriteria createSearchCriteria();

    public void setFirst(int first) {
        this.first = first;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public abstract String getStartingSql();

    public abstract String getDefaultSortField();

    public abstract SortOrder getDefaultSortOrder();

    public abstract String getSortFieldPrefix();

    public abstract String getCountStartingSql();

    public String getGroupBySql() {
        return "";
    }

    public abstract boolean startsWithWhere();

    public String getSql() {
        SearchCriteria searchCriteria = this.createSearchCriteria();
        searchCriteria.addOrderByCriteria(this.getSortField(), this.getSortOrder());
        String finalSQL = this.getStartingSql() + searchCriteria.getCriteriaString(this.startsWithWhere()) + this.groupBy() + searchCriteria.getOrderByString();
        return finalSQL;
    }

    public String getCountSql() {
        SearchCriteria searchCriteria = this.createSearchCriteria();
        String finalSQL = this.getCountStartingSql() + searchCriteria.getCriteriaString(this.startsWithWhere());
        return finalSQL;
    }

    private String groupBy() {
        return this.getGroupBySql().length() == 0 ? "" : " GROUP BY " + this.getGroupBySql();
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public static enum SortOrder {
        ASC,
        DESC;

    }
}

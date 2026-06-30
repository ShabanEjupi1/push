/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.audit;

import java.io.Serializable;
import java.util.Date;
import mk.com.snt.kc.warehouse.audit.AuditLog;
import mk.com.snt.kc.warehouse.persistence.SearchCriteria;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;

public class AuditFilter
extends SearchFilter
implements Serializable {
    private static final String QUERY = "SELECT al FROM AuditLog al, User u WHERE al.eventBy = u.username";
    private static final String COUNT_QUERY = "SELECT COUNT(al.id) FROM AuditLog al, User u WHERE al.eventBy = u.username";
    private Date dateFrom;
    private Date dateTo;
    private String username;
    private String traderFiscalNumber;
    private AuditLog.ActionType actionType;
    private AuditLog.EntityType entityType;

    @Override
    public SearchCriteria createSearchCriteria() {
        return new SearchCriteria().addBetweenCriteria("al.eventAt", this.dateFrom, this.dateTo).addLikeCriteria("al.eventBy", this.username).addEqualsCriteria("al.actionType", (Object)this.actionType).addEqualsCriteria("u.traderFiscalNumber", this.traderFiscalNumber).addEqualsCriteria("al.entityType", (Object)this.entityType);
    }

    @Override
    public String getStartingSql() {
        return QUERY;
    }

    @Override
    public String getDefaultSortField() {
        return "eventAt";
    }

    @Override
    public SearchFilter.SortOrder getDefaultSortOrder() {
        return SearchFilter.SortOrder.DESC;
    }

    @Override
    public String getSortFieldPrefix() {
        return "al.";
    }

    @Override
    public String getCountStartingSql() {
        return COUNT_QUERY;
    }

    @Override
    public boolean startsWithWhere() {
        return false;
    }

    public Date getDateFrom() {
        return this.dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return this.dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AuditLog.ActionType getActionType() {
        return this.actionType;
    }

    public void setActionType(AuditLog.ActionType actionType) {
        this.actionType = actionType;
    }

    public AuditLog.EntityType getEntityType() {
        return this.entityType;
    }

    public void setEntityType(AuditLog.EntityType entityType) {
        this.entityType = entityType;
    }

    public void setTraderFiscalNumber(String traderFiscalNumber) {
        this.traderFiscalNumber = traderFiscalNumber;
    }

    public String getTraderFiscalNumber() {
        return this.traderFiscalNumber;
    }
}

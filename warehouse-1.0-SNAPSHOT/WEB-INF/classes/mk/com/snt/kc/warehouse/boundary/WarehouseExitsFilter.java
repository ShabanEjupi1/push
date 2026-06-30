/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.Serializable;
import java.util.Date;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.persistence.SearchCriteria;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;

public class WarehouseExitsFilter
extends SearchFilter
implements Serializable {
    private static final String QUERY = "SELECT DISTINCT we FROM WarehouseExit we JOIN FETCH we.warehouseInventory";
    private static final String COUNT_QUERY = "SELECT COUNT(DISTINCT we.id) FROM WarehouseExit we JOIN FETCH we.warehouseInventory";
    private String trader;
    private String officeName;
    private String registerNumber;
    private Date registrationDate;
    private Long sadItemNbr;
    private String tariff;
    private String countryOfOrigin;
    private String goodsDescription;
    private Date exitDateFrom;
    private Date exitDateTo;
    private String exitReference;
    private WarehouseExit.Status status;

    @Override
    public SearchCriteria createSearchCriteria() {
        return new SearchCriteria().addEqualsCriteria("we.warehouseInventory.traderFiscalNumber", this.trader).addEqualsCriteria("we.warehouseInventory.registrationDate", this.registrationDate).addLikeCriteria("we.warehouseInventory.officeName", this.officeName).addLikeCriteria("CONCAT(we.warehouseInventory.signR, '-', we.warehouseInventory.registerNumber)", this.registerNumber).addEqualsCriteria("we.warehouseInventory.sadItemNbr", this.sadItemNbr).addLikeCriteria("we.warehouseInventory.tariff", this.tariff).addLikeCriteria("we.warehouseInventory.countryOfOrigin", this.countryOfOrigin).addLikeCriteria("we.warehouseInventory.goodsDescription", this.goodsDescription).addGreaterThanAndEqualsCriteria("we.exitDate", this.exitDateFrom).addLessThanAndEqualsCriteria("we.exitDate", this.exitDateTo).addLikeCriteria("we.exitReference", this.exitReference).addEqualsCriteria("we.status", (Object)this.status);
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
        return "we.";
    }

    @Override
    public String getCountStartingSql() {
        return COUNT_QUERY;
    }

    @Override
    public boolean startsWithWhere() {
        return true;
    }

    public String getTrader() {
        return this.trader;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }

    public String getRegisterNumber() {
        return this.registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public Date getRegistrationDate() {
        return this.registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getOfficeName() {
        return this.officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }

    public Long getSadItemNbr() {
        return this.sadItemNbr;
    }

    public void setSadItemNbr(Long sadItemNbr) {
        this.sadItemNbr = sadItemNbr;
    }

    public String getTariff() {
        return this.tariff;
    }

    public void setTariff(String tariff) {
        this.tariff = tariff;
    }

    public String getCountryOfOrigin() {
        return this.countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public String getGoodsDescription() {
        return this.goodsDescription;
    }

    public void setGoodsDescription(String goodsDescription) {
        this.goodsDescription = goodsDescription;
    }

    public String getExitReference() {
        return this.exitReference;
    }

    public void setExitReference(String exitReference) {
        this.exitReference = exitReference;
    }

    public Date getExitDateFrom() {
        return this.exitDateFrom;
    }

    public void setExitDateFrom(Date exitDateFrom) {
        this.exitDateFrom = exitDateFrom;
    }

    public Date getExitDateTo() {
        return this.exitDateTo;
    }

    public void setExitDateTo(Date exitDateTo) {
        this.exitDateTo = exitDateTo;
    }

    public WarehouseExit.Status getStatus() {
        return this.status;
    }

    public void setStatus(WarehouseExit.Status status) {
        this.status = status;
    }
}

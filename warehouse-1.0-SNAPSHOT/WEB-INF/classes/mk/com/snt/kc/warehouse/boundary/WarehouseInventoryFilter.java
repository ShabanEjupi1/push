/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mk.com.snt.kc.warehouse.persistence.SearchCriteria;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;
import mk.com.snt.kc.warehouse.util.Utils;

public class WarehouseInventoryFilter
extends SearchFilter
implements Serializable {
    private static final String QUERY = "SELECT NEW mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData(wi, SUM(exits.netWeightOut), SUM(exits.grossWeightOut), SUM(exits.stockValue), SUM(exits.quantityOut)) FROM WarehouseInventory wi LEFT JOIN wi.exits exits ON exits.status = mk.com.snt.kc.warehouse.domain.WarehouseExit.Status.VALID";
    private static final String COUNT_QUERY = "SELECT COUNT(DISTINCT CONCAT(wi.instanceId, wi.sadItemNbr)) FROM WarehouseInventory wi LEFT JOIN wi.exits exits ON exits.status = mk.com.snt.kc.warehouse.domain.WarehouseExit.Status.VALID";
    private static final String[] FIELDS = new String[]{"trader", "officeCode", "registerNumber", "registrationDate", "sadItemNbr", "tariff", "tscCode", "countryOfOrigin", "tarPrf", "goodsDescription"};
    private String trader;
    private String officeCode;
    private String registerNumber;
    private Date registrationDate;
    private Long sadItemNbr;
    private String tariff;
    private String tscCode;
    private String tarPrf;
    private String countryOfOrigin;
    private String goodsDescription;
    private Long instanceId;

    @Override
    public SearchCriteria createSearchCriteria() {
        return new SearchCriteria().addGreaterThanCriteria("wi.remainingNetWeight", BigDecimal.ZERO).addOrEqualsCriteria("wi.whsCode", "wi.whsOwner", this.trader).addEqualsCriteria("wi.registrationDate", this.registrationDate).addLikeCriteria("wi.officeCode", this.officeCode).addLikeCriteria("CONCAT(wi.signR, '-', wi.registerNumber)", this.registerNumber).addEqualsCriteria("wi.sadItemNbr", this.sadItemNbr).addEqualsCriteria("wi.instanceId", this.instanceId).addLikeCriteria("wi.tariff", this.tariff).addLikeCriteria("wi.tscCode", this.tscCode).addLikeCriteria("wi.tarPrf", this.tarPrf).addLikeCriteria("wi.countryOfOrigin", this.countryOfOrigin).addLikeCriteria("wi.goodsDescription", this.goodsDescription);
    }

    @Override
    public String getStartingSql() {
        return QUERY;
    }

    @Override
    public String getDefaultSortField() {
        return "instanceId";
    }

    @Override
    public SearchFilter.SortOrder getDefaultSortOrder() {
        return SearchFilter.SortOrder.ASC;
    }

    @Override
    public String getSortFieldPrefix() {
        return "wi.";
    }

    @Override
    public String getCountStartingSql() {
        return COUNT_QUERY;
    }

    @Override
    public boolean startsWithWhere() {
        return true;
    }

    @Override
    public String getGroupBySql() {
        return "wi";
    }

    public String getTrader() {
        return this.trader;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }

    public String getOfficeCode() {
        return this.officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
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

    public Long getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getTscCode() {
        return this.tscCode;
    }

    public void setTscCode(String tscCode) {
        this.tscCode = tscCode;
    }

    public String getTarPrf() {
        return this.tarPrf;
    }

    public void setTarPrf(String tarPrf) {
        this.tarPrf = tarPrf;
    }

    public void setupFilters(Map<String, String> filters) {
        for (String field : FIELDS) {
            String setter = "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
            try {
                Method m = this.getClass().getMethod(setter, this.getClass().getDeclaredField(field).getType());
                m.invoke(this, this.getValue(filters, field));
            }
            catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                Logger.getLogger(WarehouseInventoryFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Object getValue(Map<String, String> filters, String field) {
        if (filters != null && filters.containsKey(field)) {
            switch (field) {
                case "sadItemNbr": {
                    return Long.valueOf(filters.get(field));
                }
                case "registrationDate": {
                    return Utils.parse(filters.get(field));
                }
            }
            return filters.get(field);
        }
        return null;
    }
}

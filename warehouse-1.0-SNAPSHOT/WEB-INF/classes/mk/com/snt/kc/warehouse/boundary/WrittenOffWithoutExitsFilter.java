/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.Serializable;
import java.util.Map;
import mk.com.snt.kc.warehouse.persistence.QueryParameter;
import mk.com.snt.kc.warehouse.persistence.SearchCriteria;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;
import mk.com.snt.kc.warehouse.util.Utils;

public class WrittenOffWithoutExitsFilter
extends SearchFilter
implements Serializable {
    private static final String QUERY = "SELECT NEW mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData(wi, SUM(exits.netWeightOut), SUM(exits.grossWeightOut), SUM(exits.stockValue), SUM(exits.quantityOut)) FROM WarehouseInventory wi LEFT JOIN wi.exits exits ON exits.status = mk.com.snt.kc.warehouse.domain.WarehouseExit.Status.VALID";
    private static final String GROUP_BY = " wi HAVING wi.im4NetWeight > SUM(exits.netWeightOut) OR (wi.im4NetWeight > 0 AND SUM(exits.netWeightOut) IS NULL)";
    private static final String COUNT_QUERY = "SELECT COUNT(*) FROM (SELECT t0.INSTANCEID, t0.KEY_ITM_NBR FROM KCUSTOMS.WHS_SAD_INVENTORY t0 LEFT OUTER JOIN KCUSTOMS.WHS_STOCK_OUT t1 ON \n(((t1.INSTANCEID = t0.INSTANCEID) AND (t1.KEY_ITM_NBR = t0.KEY_ITM_NBR)) AND (t1.STATUS = 'VALID')) \n";
    private static final String COUNT_GROUP_BY = "GROUP BY t0.KEY_ITM_NBR, t0.INSTANCEID, t0.GDS_ORG_CTY, t0.VIT_STV, t0.DEC_REF_NBR, t0.DEC_REF_YER, t0.DEC_COD, t0.WHS_TIM, t0.GDS_DS3, t0.IM4_NET, \nt0.IM4_QTY, t0.VIT_WGT_GRSI, t0.VIT_WGT_NETI, t0.PCK_NBRI, t0.QTY_I, t0.TAR_SUP_COD, t0.IDE_CUO_COD, t0.OFFICE_NAME, t0.PCK_TYP_COD, t0.PCK_TYP_NAM, \nt0.TAR_PRC_EXT, t0.REGIME, t0.IN_REG_NBR, t0.IDE_REG_DAT, t0.VIT_WGT_NET, t0.QTY_R, t0.IDE_REG_SER, t0.TARIK, t0.CMP_CON_COD, t0.WHS_COD \nHAVING (t0.IM4_NET > SUM(t1.VIT_WGT_NETO) OR (t0.IM4_NET > 0 AND SUM(t1.VIT_WGT_NETO) IS NULL))";
    private String trader;

    @Override
    public SearchCriteria createSearchCriteria() {
        return new SearchCriteria().addEqualsCriteria("wi.traderFiscalNumber", this.trader);
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
    public String getCountSql() {
        String finalSQL = this.getCountStartingSql() + (Utils.isNullOrEmpty(this.trader) ? "" : " WHERE t0.CMP_CON_COD = ?1 ") + COUNT_GROUP_BY + ")";
        return finalSQL;
    }

    public Map<String, Object> getCountParameters() {
        return QueryParameter.with("1", this.trader).parameters();
    }

    @Override
    public boolean startsWithWhere() {
        return true;
    }

    @Override
    public String getGroupBySql() {
        return GROUP_BY;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }

    public String getTrader() {
        return this.trader;
    }
}

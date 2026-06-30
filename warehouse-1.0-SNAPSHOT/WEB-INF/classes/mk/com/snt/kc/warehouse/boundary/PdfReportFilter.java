/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.Serializable;
import java.util.Map;
import mk.com.snt.kc.warehouse.persistence.QueryParameter;
import mk.com.snt.kc.warehouse.util.Utils;

public class PdfReportFilter
implements Serializable {
    public static final String NOT_WRITTEN_OFF_QUERY_TEMPLATE = "SELECT NEW mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData(wi, SUM(exits.netWeightOut), SUM(exits.quantityOut)) FROM WarehouseInventory wi JOIN wi.exits exits WHERE wi.traderFiscalNumber = :trader AND wi.remainingNetWeight > 0 AND exits.status != mk.com.snt.kc.warehouse.domain.WarehouseExit.Status.STORNED %sGROUP BY wi HAVING (SUM(exits.netWeightOut) - wi.im4NetWeight > 0)ORDER BY wi.registrationDate, wi.registerNumber, wi.sadItemNbr";
    public static final String COUNT_NOT_WRITTEN_OFF_QUERY_TEMPLATE = "SELECT COUNT(*) FROM (SELECT t1.INSTANCEID, t1.KEY_ITM_NBR FROM KCUSTOMS.WHS_STOCK_OUT t2, KCUSTOMS.WHS_SAD_INVENTORY t1 WHERE t1.CMP_CON_COD = ? AND t1.VIT_WGT_NET > 0 AND t2.STATUS <> 'STORNED' AND t2.KEY_ITM_NBR = t1.KEY_ITM_NBR AND t2.INSTANCEID = t1.INSTANCEID  %sGROUP BY t1.KEY_ITM_NBR, t1.INSTANCEID, t1.GDS_ORG_CTY, t1.VIT_STV, t1.DEC_REF_NBR, t1.DEC_REF_YER, t1.DEC_COD, t1.WHS_TIM, t1.GDS_DS3, t1.IM4_NET, t1.IM4_QTY, t1.VIT_WGT_GRSI, t1.VIT_WGT_NETI, t1.PCK_NBRI, t1.QTY_I, t1.TAR_SUP_COD, t1.IDE_CUO_COD, t1.OFFICE_NAME, t1.PCK_TYP_COD, t1.PCK_TYP_NAM, t1.TAR_PRC_EXT, t1.REGIME, t1.IN_REG_NBR, t1.IDE_REG_DAT, t1.VIT_WGT_NET, t1.QTY_R, t1.IDE_REG_SER, t1.TARIK, t1.CMP_CON_COD, t1.WHS_COD \nHAVING ((SUM(t2.VIT_WGT_NETO) - t1.IM4_NET) > 0))";
    public static final String BY_MONTH = "AND exits.exitDate <= :lastDay ";
    public static final String COUNT_BY_MONTH = "AND t2.EXIT_DATE <= ? ";
    public static final String NOT_WRITTEN_OFF_QUERY = String.format("SELECT NEW mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData(wi, SUM(exits.netWeightOut), SUM(exits.quantityOut)) FROM WarehouseInventory wi JOIN wi.exits exits WHERE wi.traderFiscalNumber = :trader AND wi.remainingNetWeight > 0 AND exits.status != mk.com.snt.kc.warehouse.domain.WarehouseExit.Status.STORNED %sGROUP BY wi HAVING (SUM(exits.netWeightOut) - wi.im4NetWeight > 0)ORDER BY wi.registrationDate, wi.registerNumber, wi.sadItemNbr", "");
    public static final String COUNT_NOT_WRITTEN_OFF_QUERY = String.format("SELECT COUNT(*) FROM (SELECT t1.INSTANCEID, t1.KEY_ITM_NBR FROM KCUSTOMS.WHS_STOCK_OUT t2, KCUSTOMS.WHS_SAD_INVENTORY t1 WHERE t1.CMP_CON_COD = ? AND t1.VIT_WGT_NET > 0 AND t2.STATUS <> 'STORNED' AND t2.KEY_ITM_NBR = t1.KEY_ITM_NBR AND t2.INSTANCEID = t1.INSTANCEID  %sGROUP BY t1.KEY_ITM_NBR, t1.INSTANCEID, t1.GDS_ORG_CTY, t1.VIT_STV, t1.DEC_REF_NBR, t1.DEC_REF_YER, t1.DEC_COD, t1.WHS_TIM, t1.GDS_DS3, t1.IM4_NET, t1.IM4_QTY, t1.VIT_WGT_GRSI, t1.VIT_WGT_NETI, t1.PCK_NBRI, t1.QTY_I, t1.TAR_SUP_COD, t1.IDE_CUO_COD, t1.OFFICE_NAME, t1.PCK_TYP_COD, t1.PCK_TYP_NAM, t1.TAR_PRC_EXT, t1.REGIME, t1.IN_REG_NBR, t1.IDE_REG_DAT, t1.VIT_WGT_NET, t1.QTY_R, t1.IDE_REG_SER, t1.TARIK, t1.CMP_CON_COD, t1.WHS_COD \nHAVING ((SUM(t2.VIT_WGT_NETO) - t1.IM4_NET) > 0))", "");
    public static final String MONTHLY_NOT_WRITTEN_OFF_QUERY = String.format("SELECT NEW mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData(wi, SUM(exits.netWeightOut), SUM(exits.quantityOut)) FROM WarehouseInventory wi JOIN wi.exits exits WHERE wi.traderFiscalNumber = :trader AND wi.remainingNetWeight > 0 AND exits.status != mk.com.snt.kc.warehouse.domain.WarehouseExit.Status.STORNED %sGROUP BY wi HAVING (SUM(exits.netWeightOut) - wi.im4NetWeight > 0)ORDER BY wi.registrationDate, wi.registerNumber, wi.sadItemNbr", "AND exits.exitDate <= :lastDay ");
    public static final String MONTHLY_COUNT_NOT_WRITTEN_OFF_QUERY = String.format("SELECT COUNT(*) FROM (SELECT t1.INSTANCEID, t1.KEY_ITM_NBR FROM KCUSTOMS.WHS_STOCK_OUT t2, KCUSTOMS.WHS_SAD_INVENTORY t1 WHERE t1.CMP_CON_COD = ? AND t1.VIT_WGT_NET > 0 AND t2.STATUS <> 'STORNED' AND t2.KEY_ITM_NBR = t1.KEY_ITM_NBR AND t2.INSTANCEID = t1.INSTANCEID  %sGROUP BY t1.KEY_ITM_NBR, t1.INSTANCEID, t1.GDS_ORG_CTY, t1.VIT_STV, t1.DEC_REF_NBR, t1.DEC_REF_YER, t1.DEC_COD, t1.WHS_TIM, t1.GDS_DS3, t1.IM4_NET, t1.IM4_QTY, t1.VIT_WGT_GRSI, t1.VIT_WGT_NETI, t1.PCK_NBRI, t1.QTY_I, t1.TAR_SUP_COD, t1.IDE_CUO_COD, t1.OFFICE_NAME, t1.PCK_TYP_COD, t1.PCK_TYP_NAM, t1.TAR_PRC_EXT, t1.REGIME, t1.IN_REG_NBR, t1.IDE_REG_DAT, t1.VIT_WGT_NET, t1.QTY_R, t1.IDE_REG_SER, t1.TARIK, t1.CMP_CON_COD, t1.WHS_COD \nHAVING ((SUM(t2.VIT_WGT_NETO) - t1.IM4_NET) > 0))", "AND t2.EXIT_DATE <= ? ");
    private String month;
    private String trader;
    private int first;
    private int pageSize;

    public String getQuery() {
        return !this.hasMonth() ? NOT_WRITTEN_OFF_QUERY : MONTHLY_NOT_WRITTEN_OFF_QUERY;
    }

    public String getCountQuery() {
        return !this.hasMonth() ? COUNT_NOT_WRITTEN_OFF_QUERY : MONTHLY_COUNT_NOT_WRITTEN_OFF_QUERY;
    }

    public Map<String, Object> getParameters() {
        QueryParameter qp = QueryParameter.with("trader", this.trader);
        if (this.hasMonth()) {
            int iMonth = Integer.parseInt(this.month.substring(0, 2));
            int iYear = Integer.parseInt(this.month.substring(3));
            qp.and("lastDay", Utils.lastDay(iMonth, iYear));
        }
        return qp.parameters();
    }

    public Map<String, Object> getCountParameters() {
        QueryParameter qp = QueryParameter.with("1", this.trader);
        if (this.hasMonth()) {
            int iMonth = Integer.parseInt(this.month.substring(0, 2));
            int iYear = Integer.parseInt(this.month.substring(3));
            qp.and("2", Utils.lastDay(iMonth, iYear));
        }
        return qp.parameters();
    }

    public boolean hasMonth() {
        return !Utils.isNullOrEmpty(this.month);
    }

    public String getMonth() {
        return this.month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getFirst() {
        return this.first;
    }

    public void setFirst(int first) {
        this.first = first;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getTrader() {
        return this.trader;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }
}

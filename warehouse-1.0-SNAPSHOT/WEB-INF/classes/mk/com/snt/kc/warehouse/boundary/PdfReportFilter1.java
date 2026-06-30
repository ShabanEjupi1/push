/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import mk.com.snt.kc.warehouse.persistence.QueryParameter;
import mk.com.snt.kc.warehouse.util.Utils;

public class PdfReportFilter1
implements Serializable {
    public static final String NOT_WRITTEN_OFF_QUERY = "            with all_im7 as\n             -- Najdi gi site IM7 koi imaat nedeklarirani kolicini \n             (SELECT t0.INSTANCEID, t0.KEY_ITM_NBR,  t0.GDS_ORG_CTY, t0.VIT_STV, t0.DEC_REF_NBR, t0.DEC_REF_YER, t0.DEC_COD, t0.WHS_TIM, t0.GDS_DS3, t0.IM4_QTY, t0.VIT_WGT_GRSI, \n              t0.VIT_WGT_NETI, t0.PCK_NBRI, t0.QTY_I, t0.TAR_SUP_COD, t0.IDE_CUO_COD, t0.OFFICE_NAME, t0.PCK_TYP_COD, t0.PCK_TYP_NAM, t0.TAR_PRC_EXT, t0.REGIME, t0.IN_REG_NBR, t0.IDE_REG_DAT, t0.VIT_WGT_NET,\n             t0.QTY_R, t0.IDE_REG_SER, t0.TARIK, t0.CMP_CON_COD, t0.WHS_COD, t0.TSC_CODE, t0.TAR_PRF, SUM(t1.VIT_WGT_NETO) - nvl(t0.im4_net,0)  net_r, SUM(t1.QTY_O) - nvl(t0.im4_qty,0) qty_mr\n             FROM KCUSTOMS.WHS_SAD_INVENTORY t0, KCUSTOMS.WHS_STOCK_OUT t1 \n              WHERE \n              ((t1.INSTANCEID = t0.INSTANCEID) AND (t1.KEY_ITM_NBR = t0.KEY_ITM_NBR)) AND t0.cmp_con_cod=?1 AND\n             ( t0.VIT_WGT_NET > 0 AND t1.STATUS <> 'STORNED' AND t1.EXIT_DATE <= ?2 )\n              GROUP BY t0.INSTANCEID, t0.KEY_ITM_NBR,  t0.GDS_ORG_CTY, t0.VIT_STV, t0.DEC_REF_NBR, t0.DEC_REF_YER, t0.DEC_COD, t0.WHS_TIM, t0.GDS_DS3, t0.IM4_NET, t0.IM4_QTY, t0.VIT_WGT_GRSI, \n              t0.VIT_WGT_NETI, t0.PCK_NBRI, t0.QTY_I, t0.TAR_SUP_COD, t0.IDE_CUO_COD, t0.OFFICE_NAME, t0.PCK_TYP_COD, t0.PCK_TYP_NAM, t0.TAR_PRC_EXT, t0.REGIME, t0.IN_REG_NBR, t0.IDE_REG_DAT, t0.VIT_WGT_NET,\n             t0.QTY_R, t0.IDE_REG_SER, t0.TARIK, t0.CMP_CON_COD, t0.WHS_COD,t0.TSC_CODE,t0.TAR_PRF\n             HAVING (SUM(t1.VIT_WGT_NETO) - nvl(t0.IM4_NET,0)) > 0) \n             --- Izracunaj kolku od mesecnite izlezi se nedeklarirani\n             select i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3, i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, net_r, qty_r, exit_month, exit_qmonth,\n             CASE WHEN NET_R>=EXIT_MONTH THEN EXIT_MONTH ELSE NET_R END undecl_month ,\n             CASE WHEN QTY_MR>=EXIT_QMONTH THEN EXIT_QMONTH ELSE QTY_MR END undecl_qmonth\n             from\n             -- Za site IM7 koi imaat nedeklarirani kolicini, izracunaj ja sumata na izlezite za izbraniot mesec -  exit_month i exit_qmonth\n             (select i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3,  i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, decode( sign(i1.net_r), -1,0,i1.net_r) net_r, decode( sign(i1.qty_mr), -1,0,i1.qty_mr) qty_mr, sum(I2.VIT_WGT_NETO) exit_month, SUM(i2.QTY_O) exit_qmonth\n             from all_im7 i1, KCUSTOMS.WHS_STOCK_OUT i2\n             where i1.INSTANCEID = i2.INSTANCEID AND i1.KEY_ITM_NBR = i2.KEY_ITM_NBR\n             and i2.exit_date between ?3 and  ?2 and i2.STATUS <> 'STORNED'\n             group by i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3,  i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, i1.net_r, i1.qty_mr) i1\n             where net_r>0 order by IDE_CUO_COD, IDE_REG_DAT, IN_REG_NBR, KEY_ITM_NBR\n";
    public static final String COUNT_NOT_WRITTEN_OFF_QUERY = "SELECT COUNT(*) FROM (            with all_im7 as\n             -- Najdi gi site IM7 koi imaat nedeklarirani kolicini \n             (SELECT t0.INSTANCEID, t0.KEY_ITM_NBR,  t0.GDS_ORG_CTY, t0.VIT_STV, t0.DEC_REF_NBR, t0.DEC_REF_YER, t0.DEC_COD, t0.WHS_TIM, t0.GDS_DS3, t0.IM4_QTY, t0.VIT_WGT_GRSI, \n              t0.VIT_WGT_NETI, t0.PCK_NBRI, t0.QTY_I, t0.TAR_SUP_COD, t0.IDE_CUO_COD, t0.OFFICE_NAME, t0.PCK_TYP_COD, t0.PCK_TYP_NAM, t0.TAR_PRC_EXT, t0.REGIME, t0.IN_REG_NBR, t0.IDE_REG_DAT, t0.VIT_WGT_NET,\n             t0.QTY_R, t0.IDE_REG_SER, t0.TARIK, t0.CMP_CON_COD, t0.WHS_COD, t0.TSC_CODE, t0.TAR_PRF, SUM(t1.VIT_WGT_NETO) - nvl(t0.im4_net,0)  net_r, SUM(t1.QTY_O) - nvl(t0.im4_qty,0) qty_mr\n             FROM KCUSTOMS.WHS_SAD_INVENTORY t0, KCUSTOMS.WHS_STOCK_OUT t1 \n              WHERE \n              ((t1.INSTANCEID = t0.INSTANCEID) AND (t1.KEY_ITM_NBR = t0.KEY_ITM_NBR)) AND t0.cmp_con_cod=?1 AND\n             ( t0.VIT_WGT_NET > 0 AND t1.STATUS <> 'STORNED' AND t1.EXIT_DATE <= ?2 )\n              GROUP BY t0.INSTANCEID, t0.KEY_ITM_NBR,  t0.GDS_ORG_CTY, t0.VIT_STV, t0.DEC_REF_NBR, t0.DEC_REF_YER, t0.DEC_COD, t0.WHS_TIM, t0.GDS_DS3, t0.IM4_NET, t0.IM4_QTY, t0.VIT_WGT_GRSI, \n              t0.VIT_WGT_NETI, t0.PCK_NBRI, t0.QTY_I, t0.TAR_SUP_COD, t0.IDE_CUO_COD, t0.OFFICE_NAME, t0.PCK_TYP_COD, t0.PCK_TYP_NAM, t0.TAR_PRC_EXT, t0.REGIME, t0.IN_REG_NBR, t0.IDE_REG_DAT, t0.VIT_WGT_NET,\n             t0.QTY_R, t0.IDE_REG_SER, t0.TARIK, t0.CMP_CON_COD, t0.WHS_COD,t0.TSC_CODE,t0.TAR_PRF\n             HAVING (SUM(t1.VIT_WGT_NETO) - nvl(t0.IM4_NET,0)) > 0) \n             --- Izracunaj kolku od mesecnite izlezi se nedeklarirani\n             select i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3, i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, net_r, qty_r, exit_month, exit_qmonth,\n             CASE WHEN NET_R>=EXIT_MONTH THEN EXIT_MONTH ELSE NET_R END undecl_month ,\n             CASE WHEN QTY_MR>=EXIT_QMONTH THEN EXIT_QMONTH ELSE QTY_MR END undecl_qmonth\n             from\n             -- Za site IM7 koi imaat nedeklarirani kolicini, izracunaj ja sumata na izlezite za izbraniot mesec -  exit_month i exit_qmonth\n             (select i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3,  i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, decode( sign(i1.net_r), -1,0,i1.net_r) net_r, decode( sign(i1.qty_mr), -1,0,i1.qty_mr) qty_mr, sum(I2.VIT_WGT_NETO) exit_month, SUM(i2.QTY_O) exit_qmonth\n             from all_im7 i1, KCUSTOMS.WHS_STOCK_OUT i2\n             where i1.INSTANCEID = i2.INSTANCEID AND i1.KEY_ITM_NBR = i2.KEY_ITM_NBR\n             and i2.exit_date between ?3 and  ?2 and i2.STATUS <> 'STORNED'\n             group by i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3,  i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, i1.net_r, i1.qty_mr) i1\n             where net_r>0 order by IDE_CUO_COD, IDE_REG_DAT, IN_REG_NBR, KEY_ITM_NBR\n)";
    private String month;
    private String trader;
    private int first;
    private int pageSize;

    public String getQuery() {
        return NOT_WRITTEN_OFF_QUERY;
    }

    public String getCountQuery() {
        return COUNT_NOT_WRITTEN_OFF_QUERY;
    }

    public Map<String, Object> getParameters() {
        QueryParameter qp = QueryParameter.with("1", this.trader);
        if (!this.hasMonth()) {
            this.setupMonth();
        }
        int iMonth = Integer.parseInt(this.month.substring(0, 2));
        int iYear = Integer.parseInt(this.month.substring(3));
        qp.and("2", Utils.lastDay(iMonth, iYear)).and("3", Utils.firstDay(iMonth, iYear));
        return qp.parameters();
    }

    public void setupMonth() {
        this.month = Utils.convert(new Date(), "MM.yyyy");
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

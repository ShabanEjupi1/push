/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.Stateless
 *  javax.enterprise.event.Event
 *  javax.inject.Inject
 */
package mk.com.snt.kc.warehouse.boundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import mk.com.snt.kc.warehouse.audit.AuditEvent;
import mk.com.snt.kc.warehouse.audit.AuditLog;
import mk.com.snt.kc.warehouse.boundary.PdfReportFilter;
import mk.com.snt.kc.warehouse.boundary.PdfReportFilter1;
import mk.com.snt.kc.warehouse.boundary.UnresolvedExitsFilter;
import mk.com.snt.kc.warehouse.boundary.WarehouseExitsFilter;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryFilter;
import mk.com.snt.kc.warehouse.boundary.WrittenOffWithoutExitsFilter;
import mk.com.snt.kc.warehouse.domain.UnresolvedExit;
import mk.com.snt.kc.warehouse.domain.WarehouseBalance;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.domain.WarehouseInventory;
import mk.com.snt.kc.warehouse.persistence.CrudService;
import mk.com.snt.kc.warehouse.persistence.QueryParameter;
import mk.com.snt.kc.warehouse.util.Utils;

@Stateless
public class WarehouseManager {
    @Inject
    CrudService crudService;
    @Inject
    Event<AuditEvent> auditEvent;

    public List<WarehouseInventoryData> getInventory(WarehouseInventoryFilter filter) {
        return this.crudService.find(filter);
    }

    public int countInventory(WarehouseInventoryFilter filter) {
        return this.crudService.count(filter);
    }

    public List<WarehouseExit> getExits(WarehouseExitsFilter filter) {
        return this.crudService.find(filter);
    }

    public int countExits(WarehouseExitsFilter filter) {
        return this.crudService.count(filter);
    }

    public WarehouseInventoryData getInventory(WarehouseExit exit) {
        WarehouseInventoryFilter filter = new WarehouseInventoryFilter();
        filter.setInstanceId(exit.getWarehouseInventory().getInstanceId());
        filter.setSadItemNbr(exit.getWarehouseInventory().getSadItemNbr());
        List<WarehouseInventoryData> data = this.getInventory(filter);
        return data.get(0);
    }

    public void saveExit(WarehouseExit exit) {
        WarehouseExit existing = null;
        if (exit.getId() == null) {
            exit.setStatus(WarehouseExit.Status.VALID);
            exit.setCreatedAt(new Date());
        } else {
            existing = this.crudService.find(WarehouseExit.class, exit.getId());
            Utils.validateCondition(existing.isStorned(), "WarehouseExit.edit.stornedNotAllowed", new Object[0]);
            Utils.validateCondition(!existing.isEditable(), "WarehouseExit.edit.notAllowed", new Object[0]);
        }
        exit.validate();
        WarehouseInventoryData inventoryData = this.getInventory(exit);
        if (exit.getId() != null) {
            inventoryData.excludeExit(existing);
        }
        Utils.validateCondition(!inventoryData.isNetWeightValid(exit), "WarehouseExit.netWeightOut.notEnough", new Object[0]);
        Utils.validateCondition(!inventoryData.isQuantityValid(exit), "WarehouseExit.quantityOut.notEnough", new Object[0]);
        this.auditEvent.fire(new AuditEvent(exit, exit.getId() == null ? AuditLog.ActionType.CREATION : AuditLog.ActionType.UPDATE, AuditLog.EntityType.WAREHOUSE_EXIT));
        this.crudService.save(exit);
    }

    public List<WarehouseExit> getExits(WarehouseInventory warehouseInventory) {
        return this.crudService.findWithNamedQuery("WarehouseExit.byInventory", QueryParameter.with("warehouseInventory", warehouseInventory).parameters());
    }

    public List<WarehouseExit> getExits(WarehouseInventory warehouseInventory, int first, int pageSize) {
        return this.crudService.findWithNamedQuery("WarehouseExit.byInventory", QueryParameter.with("warehouseInventory", warehouseInventory).parameters(), first, pageSize);
    }

    public int countExits(WarehouseInventory warehouseInventory) {
        return this.crudService.findSingleWithNamedQuery("WarehouseExit.countByInventory", QueryParameter.with("warehouseInventory", warehouseInventory).parameters(), Long.class).intValue();
    }

    public int countLatestExits(String trader) {
        return this.crudService.findSingleWithNamedQuery("WarehouseExit.countLatestByTrader", QueryParameter.with("date", Utils.minusDays(new Date(), 1)).and("trader", trader).parameters(), Long.class).intValue();
    }

    public List<WarehouseExit> getLatestExits(String trader, int first, int pageSize) {
        return this.crudService.findWithNamedQuery("WarehouseExit.latestByTrader", QueryParameter.with("date", Utils.minusDays(new Date(), 1)).and("trader", trader).parameters(), first, pageSize);
    }

    public int countTodaysExits(WarehouseInventory warehouseInventory) {
        return this.crudService.findSingleWithNamedQuery("WarehouseExit.countTodaysInventory", QueryParameter.with("date", new Date()).and("warehouseInventory", warehouseInventory).parameters(), Long.class).intValue();
    }

    public void deleteExit(WarehouseExit exit) {
        WarehouseExit existing = this.crudService.find(WarehouseExit.class, exit.getId());
        Utils.validateCondition(existing.isStorned(), "WarehouseExit.delete.stornedNotAllowed", new Object[0]);
        Utils.validateCondition(!exit.isEditable(), "WarehouseExit.delete.notAllowed", new Object[0]);
        this.auditEvent.fire(new AuditEvent(exit, AuditLog.ActionType.DELETE, AuditLog.EntityType.WAREHOUSE_EXIT));
        this.crudService.delete(exit);
    }

    public WarehouseExit getExit(Long id) {
        return this.crudService.find(WarehouseExit.class, id);
    }

    public List<WarehouseInventoryData> getRemainingData(PdfReportFilter filter) {
        return this.crudService.findWithQuery(filter.getQuery(), filter.getParameters(), filter.getFirst(), filter.getPageSize());
    }

    public int countRemainingData(PdfReportFilter filter) {
        List results = this.crudService.findWithNativeQuery(filter.getCountQuery(), filter.getCountParameters());
        if (results == null || results.isEmpty()) {
            return 0;
        }
        BigDecimal count = (BigDecimal)results.get(0);
        return count.intValue();
    }

    public void stornExit(WarehouseExit exit) {
        exit.setStatus(WarehouseExit.Status.STORNED);
        this.auditEvent.fire(new AuditEvent(exit, AuditLog.ActionType.STORNING, AuditLog.EntityType.WAREHOUSE_EXIT));
        exit.validateStorningExit();
        this.crudService.save(exit);
    }

    public List<WarehouseInventoryData> getWrittenOffIM7WithoutExits(WrittenOffWithoutExitsFilter filter) {
        return this.crudService.find(filter);
    }

    public int countWrittenOffIM7WithoutExits(WrittenOffWithoutExitsFilter filter) {
        List results = this.crudService.findWithNativeQuery(filter.getCountSql(), filter.getCountParameters());
        if (results == null || results.isEmpty()) {
            return 0;
        }
        BigDecimal count = (BigDecimal)results.get(0);
        return count.intValue();
    }

    public List<UnresolvedExit> getUnresolvedExits(UnresolvedExitsFilter filter) {
        return this.crudService.find(filter);
    }

    public int countUnresolvedExits(UnresolvedExitsFilter filter) {
        return this.crudService.count(filter);
    }

    public void relinkExit(WarehouseExit exit, WarehouseInventory wi) {
        Utils.validateCondition(exit.isStorned(), "WarehouseExit.relink.notAllowed", new Object[0]);
        WarehouseInventoryData inventoryData = this.getInventory(wi);
        Utils.validateCondition(!inventoryData.isNetWeightValid(exit), "WarehouseExit.netWeightOut.notEnough", new Object[0]);
        Utils.validateCondition(!inventoryData.isQuantityValid(exit), "WarehouseExit.quantityOut.notEnough", new Object[0]);
        exit.setWarehouseInventory(wi);
        this.auditEvent.fire(new AuditEvent(exit, AuditLog.ActionType.RELINK, AuditLog.EntityType.WAREHOUSE_EXIT));
        this.crudService.save(exit);
    }

    public WarehouseInventoryData getInventory(WarehouseInventory wi) {
        WarehouseInventoryFilter filter = new WarehouseInventoryFilter();
        filter.setInstanceId(wi.getInstanceId());
        filter.setSadItemNbr(wi.getSadItemNbr());
        List<WarehouseInventoryData> data = this.getInventory(filter);
        return data.get(0);
    }

    public List<WarehouseInventoryData> getRemainingData1(PdfReportFilter1 filter) {
        List result = this.crudService.findWithNativeQuery("            with all_im7 as\n             -- Najdi gi site IM7 koi imaat nedeklarirani kolicini \n             (SELECT t0.INSTANCEID, t0.KEY_ITM_NBR,  t0.GDS_ORG_CTY, t0.VIT_STV, t0.DEC_REF_NBR, t0.DEC_REF_YER, t0.DEC_COD, t0.WHS_TIM, t0.GDS_DS3, t0.IM4_QTY, t0.VIT_WGT_GRSI, \n              t0.VIT_WGT_NETI, t0.PCK_NBRI, t0.QTY_I, t0.TAR_SUP_COD, t0.IDE_CUO_COD, t0.OFFICE_NAME, t0.PCK_TYP_COD, t0.PCK_TYP_NAM, t0.TAR_PRC_EXT, t0.REGIME, t0.IN_REG_NBR, t0.IDE_REG_DAT, t0.VIT_WGT_NET,\n             t0.QTY_R, t0.IDE_REG_SER, t0.TARIK, t0.CMP_CON_COD, t0.WHS_COD, t0.TSC_CODE, t0.TAR_PRF, SUM(t1.VIT_WGT_NETO) - nvl(t0.im4_net,0)  net_r, SUM(t1.QTY_O) - nvl(t0.im4_qty,0) qty_mr\n             FROM KCUSTOMS.WHS_SAD_INVENTORY t0, KCUSTOMS.WHS_STOCK_OUT t1 \n              WHERE \n              ((t1.INSTANCEID = t0.INSTANCEID) AND (t1.KEY_ITM_NBR = t0.KEY_ITM_NBR)) AND t0.cmp_con_cod=?1 AND\n             ( t0.VIT_WGT_NET > 0 AND t1.STATUS <> 'STORNED' AND t1.EXIT_DATE <= ?2 )\n              GROUP BY t0.INSTANCEID, t0.KEY_ITM_NBR,  t0.GDS_ORG_CTY, t0.VIT_STV, t0.DEC_REF_NBR, t0.DEC_REF_YER, t0.DEC_COD, t0.WHS_TIM, t0.GDS_DS3, t0.IM4_NET, t0.IM4_QTY, t0.VIT_WGT_GRSI, \n              t0.VIT_WGT_NETI, t0.PCK_NBRI, t0.QTY_I, t0.TAR_SUP_COD, t0.IDE_CUO_COD, t0.OFFICE_NAME, t0.PCK_TYP_COD, t0.PCK_TYP_NAM, t0.TAR_PRC_EXT, t0.REGIME, t0.IN_REG_NBR, t0.IDE_REG_DAT, t0.VIT_WGT_NET,\n             t0.QTY_R, t0.IDE_REG_SER, t0.TARIK, t0.CMP_CON_COD, t0.WHS_COD,t0.TSC_CODE,t0.TAR_PRF\n             HAVING (SUM(t1.VIT_WGT_NETO) - nvl(t0.IM4_NET,0)) > 0) \n             --- Izracunaj kolku od mesecnite izlezi se nedeklarirani\n             select i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3, i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, net_r, qty_r, exit_month, exit_qmonth,\n             CASE WHEN NET_R>=EXIT_MONTH THEN EXIT_MONTH ELSE NET_R END undecl_month ,\n             CASE WHEN QTY_MR>=EXIT_QMONTH THEN EXIT_QMONTH ELSE QTY_MR END undecl_qmonth\n             from\n             -- Za site IM7 koi imaat nedeklarirani kolicini, izracunaj ja sumata na izlezite za izbraniot mesec -  exit_month i exit_qmonth\n             (select i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3,  i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, decode( sign(i1.net_r), -1,0,i1.net_r) net_r, decode( sign(i1.qty_mr), -1,0,i1.qty_mr) qty_mr, sum(I2.VIT_WGT_NETO) exit_month, SUM(i2.QTY_O) exit_qmonth\n             from all_im7 i1, KCUSTOMS.WHS_STOCK_OUT i2\n             where i1.INSTANCEID = i2.INSTANCEID AND i1.KEY_ITM_NBR = i2.KEY_ITM_NBR\n             and i2.exit_date between ?3 and  ?2 and i2.STATUS <> 'STORNED'\n             group by i1.INSTANCEID, i1.KEY_ITM_NBR,  i1.GDS_ORG_CTY, i1.VIT_STV, i1.DEC_REF_NBR, i1.DEC_REF_YER, i1.DEC_COD, i1.WHS_TIM, i1.GDS_DS3,  i1.IM4_QTY, i1.VIT_WGT_GRSI, \n              i1.VIT_WGT_NETI, i1.PCK_NBRI, i1.QTY_I, i1.TAR_SUP_COD, i1.IDE_CUO_COD, i1.OFFICE_NAME, i1.PCK_TYP_COD, i1.PCK_TYP_NAM, i1.TAR_PRC_EXT, i1.REGIME, i1.IN_REG_NBR, i1.IDE_REG_DAT, i1.VIT_WGT_NET,\n             i1.QTY_R, i1.IDE_REG_SER, i1.TARIK, i1.CMP_CON_COD, i1.WHS_COD, i1.TSC_CODE, i1.TAR_PRF, i1.net_r, i1.qty_mr) i1\n             where net_r>0 order by IDE_CUO_COD, IDE_REG_DAT, IN_REG_NBR, KEY_ITM_NBR\n", filter.getParameters());
        ArrayList<WarehouseInventoryData> items = new ArrayList<WarehouseInventoryData>();
        for (Object object : result) {
            Object[] fields = (Object[])object;
            WarehouseInventoryData item = new WarehouseInventoryData();
            WarehouseInventory wi = new WarehouseInventory();
            wi.setInstanceId(((BigDecimal)fields[0]).longValue());
            wi.setSadItemNbr(((BigDecimal)fields[1]).longValue());
            wi.setCountryOfOrigin((String)fields[2]);
            wi.setCustomsValue((BigDecimal)fields[3]);
            wi.setDecRefNbr((String)fields[4]);
            wi.setDecRefYear(((BigDecimal)fields[5]).longValue());
            wi.setDeclarantCode((String)fields[6]);
            wi.setEndDate((Date)fields[7]);
            wi.setGoodsDescription((String)fields[8]);
            wi.setIm4Quantity((BigDecimal)fields[9]);
            wi.setInputGrossWeight((BigDecimal)fields[10]);
            wi.setInputNetWeight((BigDecimal)fields[11]);
            wi.setInputNumberOfPackages(((BigDecimal)fields[12]).longValue());
            wi.setInputQuantity((BigDecimal)fields[13]);
            wi.setMeasurementUnit((String)fields[14]);
            wi.setOfficeCode((String)fields[15]);
            wi.setOfficeName((String)fields[16]);
            wi.setPackagesTypeCode((String)fields[17]);
            wi.setPackagesTypeName((String)fields[18]);
            wi.setProcedure((String)fields[19]);
            wi.setRegime((String)fields[20]);
            wi.setRegisterNumber((String)fields[21]);
            wi.setRegistrationDate((Date)fields[22]);
            wi.setRemainingNetWeight((BigDecimal)fields[23]);
            wi.setRemainingQuantity((BigDecimal)fields[24]);
            wi.setSignR((String)fields[25]);
            wi.setTariff((String)fields[26]);
            wi.setTraderFiscalNumber((String)fields[27]);
            wi.setWhsCode((String)fields[28]);
            wi.setTscCode((String)fields[29]);
            wi.setTarPrf((String)fields[30]);
            item.setWi(wi);
            item.setRemainingNetWeight((BigDecimal)fields[33]);
            item.setRemainingQuantity((BigDecimal)fields[34]);
            item.setNotWrittenOffNetWeight((BigDecimal)fields[35]);
            item.setNotWrittenOffQuantity((BigDecimal)fields[36]);
            items.add(item);
        }
        return items;
    }

    public int countRemainingData1(PdfReportFilter1 filter) {
        List results = this.crudService.findWithNativeQuery(filter.getCountQuery(), filter.getParameters());
        if (results == null || results.isEmpty()) {
            return 0;
        }
        BigDecimal count = (BigDecimal)results.get(0);
        return count.intValue();
    }

    public BigDecimal getBalance(String traderFiscalNumber) {
        WarehouseBalance balance = this.crudService.find(WarehouseBalance.class, traderFiscalNumber);
        return balance != null ? balance.getBalance() : null;
    }
}

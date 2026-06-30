/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import mk.com.snt.kc.warehouse.boundary.WarehouseInventoryData;
import mk.com.snt.kc.warehouse.domain.WarehouseInventory;

public class WarehousePDFReport1DataSource {
    public static Collection<WarehouseInventoryData> list() {
        ArrayList<WarehouseInventoryData> result = new ArrayList<WarehouseInventoryData>();
        for (int i = 0; i < 78; ++i) {
            WarehouseInventory wi = new WarehouseInventory();
            wi.setRegisterNumber(1000 + i + "");
            wi.setRegistrationDate(new Date());
            wi.setTariff("0000000000");
            wi.setOfficeCode("1234");
            wi.setTscCode("1111");
            wi.setCountryOfOrigin("MK");
            wi.setTarPrf("XSCEFTA");
            wi.setMeasurementUnit("LTR");
            for (int j = 0; j < 3; ++j) {
                WarehouseInventoryData item = new WarehouseInventoryData();
                wi.setSadItemNbr(Long.valueOf(j));
                item.setWi(wi);
                item.setRemainingNetWeight(new BigDecimal("12345678.90"));
                item.setNotWrittenOffNetWeight(new BigDecimal("12345678.90"));
                result.add(item);
            }
        }
        return result;
    }
}

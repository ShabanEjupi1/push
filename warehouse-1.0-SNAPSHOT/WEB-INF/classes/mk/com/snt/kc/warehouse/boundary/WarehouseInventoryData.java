/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.boundary;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.domain.WarehouseInventory;
import mk.com.snt.kc.warehouse.util.Utils;

public class WarehouseInventoryData
implements Serializable {
    private WarehouseInventory wi;
    private BigDecimal inputNetWeight;
    private BigDecimal inputGrossWeight;
    private BigDecimal remainingNetWeight;
    private BigDecimal inputQuantity;
    private BigDecimal remainingQuantity;
    private BigDecimal value;
    private BigDecimal notWrittenOffNetWeight;
    private BigDecimal notWrittenOffQuantity;
    private BigDecimal netWeightOut;
    private BigDecimal quantityOut;

    public WarehouseInventoryData() {
    }

    public WarehouseInventoryData(WarehouseInventory wi, BigDecimal netWeightOut, BigDecimal quantityOut) {
        this.wi = wi;
        this.remainingNetWeight = netWeightOut;
        this.remainingQuantity = quantityOut;
        this.notWrittenOffNetWeight = Utils.prepare(netWeightOut).subtract(Utils.prepare(wi.getIm4NetWeight()));
        this.notWrittenOffQuantity = Utils.prepare(quantityOut).subtract(Utils.prepare(wi.getIm4Quantity()));
    }

    public WarehouseInventoryData(WarehouseInventory wi, BigDecimal netWeightOut, BigDecimal grossWeightOut, BigDecimal stockValue, BigDecimal quantityOut) {
        this.wi = wi;
        this.inputNetWeight = wi.getInputNetWeight();
        this.inputGrossWeight = wi.getInputGrossWeight();
        this.inputQuantity = wi.getInputQuantity();
        this.value = wi.getCustomsValue();
        this.remainingNetWeight = Utils.subtract(wi.getRemainingNetWeight(), netWeightOut);
        this.inputGrossWeight = Utils.subtract(wi.getInputGrossWeight(), grossWeightOut);
        this.remainingQuantity = Utils.subtract(wi.getRemainingQuantity(), quantityOut);
        this.netWeightOut = netWeightOut;
        this.quantityOut = quantityOut;
    }

    public WarehouseInventory getWi() {
        return this.wi;
    }

    public void setWi(WarehouseInventory wi) {
        this.wi = wi;
    }

    public BigDecimal getInputNetWeight() {
        return this.inputNetWeight;
    }

    public void setInputNetWeight(BigDecimal inputNetWeight) {
        this.inputNetWeight = inputNetWeight;
    }

    public BigDecimal getInputGrossWeight() {
        return this.inputGrossWeight;
    }

    public void setInputGrossWeight(BigDecimal inputGrossWeight) {
        this.inputGrossWeight = inputGrossWeight;
    }

    public BigDecimal getRemainingNetWeight() {
        return this.remainingNetWeight;
    }

    public void setRemainingNetWeight(BigDecimal remainingNetWeight) {
        this.remainingNetWeight = remainingNetWeight;
    }

    public BigDecimal getInputQuantity() {
        return this.inputQuantity;
    }

    public void setInputQuantity(BigDecimal inputQuantity) {
        this.inputQuantity = inputQuantity;
    }

    public BigDecimal getRemainingQuantity() {
        return this.remainingQuantity;
    }

    public void setRemainingQuantity(BigDecimal remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public BigDecimal getValue() {
        return this.value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getNotWrittenOffNetWeight() {
        return this.notWrittenOffNetWeight;
    }

    public BigDecimal getNotWrittenOffQuantity() {
        return this.notWrittenOffQuantity;
    }

    public void setNotWrittenOffNetWeight(BigDecimal notWrittenOffNetWeight) {
        this.notWrittenOffNetWeight = notWrittenOffNetWeight;
    }

    public void setNotWrittenOffQuantity(BigDecimal notWrittenOffQuantity) {
        this.notWrittenOffQuantity = notWrittenOffQuantity;
    }

    public boolean isNetWeightValid(WarehouseExit exit) {
        return Utils.prepare(this.getRemainingNetWeight()).compareTo(Utils.prepare(exit.getNetWeightOut())) >= 0;
    }

    public boolean isQuantityValid(WarehouseExit exit) {
        return Utils.prepare(this.getRemainingQuantity()).compareTo(Utils.prepare(exit.getQuantityOut())) >= 0;
    }

    public BigDecimal getQuantityOut() {
        return this.quantityOut;
    }

    public BigDecimal getNetWeightOut() {
        return this.netWeightOut;
    }

    public void excludeExit(WarehouseExit exit) {
        if (exit.getId() != null) {
            if (this.getWi().isQuantityEnabled()) {
                this.remainingQuantity = this.remainingQuantity.add(exit.getQuantityOut());
            }
            this.remainingNetWeight = this.remainingNetWeight.add(exit.getNetWeightOut());
        }
    }

    public boolean isExitAllowed() {
        return this.getWi().isQuantityEnabled() && Utils.isPositive(this.remainingQuantity) || !this.getWi().isQuantityEnabled() && Utils.isPositive(this.remainingNetWeight);
    }

    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.wi);
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        WarehouseInventoryData other = (WarehouseInventoryData)obj;
        return Objects.equals(this.wi, other.wi);
    }

    public String toString() {
        return "WarehouseInventoryData{wi=" + this.wi + ", inputNetWeight=" + this.inputNetWeight + ", inputGrossWeight=" + this.inputGrossWeight + ", remainingNetWeight=" + this.remainingNetWeight + ", inputQuantity=" + this.inputQuantity + ", remainingQuantity=" + this.remainingQuantity + ", value=" + this.value + '}';
    }
}

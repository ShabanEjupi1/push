/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.domain;

import java.io.Serializable;
import java.util.Objects;

public class WarehouseInventoryId
implements Serializable {
    private Long sadItemNbr;
    private Long instanceId;

    public WarehouseInventoryId() {
    }

    public WarehouseInventoryId(Long instanceId, Long sadItemNbr) {
        this.instanceId = instanceId;
        this.sadItemNbr = sadItemNbr;
    }

    public Long getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getSadItemNbr() {
        return this.sadItemNbr;
    }

    public void setSadItemNbr(Long sadItemNbr) {
        this.sadItemNbr = sadItemNbr;
    }

    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.instanceId);
        hash = 19 * hash + Objects.hashCode(this.sadItemNbr);
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        WarehouseInventoryId other = (WarehouseInventoryId)obj;
        if (!Objects.equals(this.instanceId, other.instanceId)) {
            return false;
        }
        return Objects.equals(this.sadItemNbr, other.sadItemNbr);
    }

    public String toString() {
        return super.toString() + "{instanceId=" + this.instanceId + ", sadItemNbr=" + this.sadItemNbr + '}';
    }
}

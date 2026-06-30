/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package mk.com.snt.kc.warehouse.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="WHS_BALANCE", schema="KCUSTOMS")
public class WarehouseBalance
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name="CMP_CON_COD")
    @Id
    private String traderFiscalNumber;
    @Column(name="ACC_COD")
    private String whsCode;
    @Column(name="REMAINS")
    private BigDecimal balance;

    public String getTraderFiscalNumber() {
        return this.traderFiscalNumber;
    }

    public void setTraderFiscalNumber(String traderFiscalNumber) {
        this.traderFiscalNumber = traderFiscalNumber;
    }

    public String getWhsCode() {
        return this.whsCode;
    }

    public void setWhsCode(String whsCode) {
        this.whsCode = whsCode;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.traderFiscalNumber);
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        WarehouseBalance other = (WarehouseBalance)obj;
        return Objects.equals(this.traderFiscalNumber, other.traderFiscalNumber);
    }

    public String toString() {
        return "WarehouseBalance{traderFiscalNumber=" + this.traderFiscalNumber + ", whsCode=" + this.whsCode + ", balance=" + this.balance + '}';
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.NamedQueries
 *  javax.persistence.NamedQuery
 *  javax.persistence.Table
 *  javax.persistence.Temporal
 *  javax.persistence.TemporalType
 */
package mk.com.snt.kc.warehouse.domain;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="D_TRADERS", schema="KC_DWH")
@NamedQueries(value={@NamedQuery(name="Trader.byFiscalNumber", query="SELECT t FROM Trader t WHERE t.fiscalNumber = :fiscalNumber"), @NamedQuery(name="Trader.getValidLikeFiscalNumber", query="SELECT t FROM Trader t WHERE t.fiscalNumber LIKE :fiscalNumber AND t.validFrom <= :date AND (t.validTo IS NULL OR t.validTo >= :date) ORDER BY t.fiscalNumber ASC"), @NamedQuery(name="Trader.getValidByFiscalNumber", query="SELECT t FROM Trader t WHERE t.fiscalNumber = :fiscalNumber AND t.validFrom <= :date AND (t.validTo IS NULL OR t.validTo >= :date)")})
public class Trader
implements Serializable {
    public static final String BY_FISCAL_NUMBER = "Trader.byFiscalNumber";
    public static final String GET_VALID_BY_FISCAL_NUMBER = "Trader.getValidByFiscalNumber";
    public static final String GET_VALID_LIKE_FISCAL_NUMBER = "Trader.getValidLikeFiscalNumber";
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name="PK_TRADERS")
    private Long id;
    @Column(name="CMP_COD")
    private String fiscalNumber;
    @Column(name="VALID_FROM")
    @Temporal(value=TemporalType.DATE)
    private Date validFrom;
    @Column(name="VALID_TO")
    @Temporal(value=TemporalType.DATE)
    private Date validTo;
    @Column(name="CMP_NAM")
    private String name;
    @Column(name="CMP_ADR")
    private String address;
    @Column(name="CMP_TEL")
    private String phone;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFiscalNumber() {
        return this.fiscalNumber;
    }

    public void setFiscalNumber(String fiscalNumber) {
        this.fiscalNumber = fiscalNumber;
    }

    public Date getValidFrom() {
        return this.validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return this.validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDisplay() {
        return this.getName() + " " + this.getAddress();
    }

    public int hashCode() {
        int hash = 0;
        return hash += this.id != null ? this.id.hashCode() : 0;
    }

    public boolean equals(Object object) {
        if (!(object instanceof Trader)) {
            return false;
        }
        Trader other = (Trader)object;
        return !(this.id == null && other.id != null || this.id != null && !this.id.equals(other.id));
    }

    public String toString() {
        return "Trader{id=" + this.id + ", fiscalNumber=" + this.fiscalNumber + ", validFrom=" + this.validFrom + ", validTo=" + this.validTo + ", name=" + this.name + ", address=" + this.address + ", phone=" + this.phone + '}';
    }
}

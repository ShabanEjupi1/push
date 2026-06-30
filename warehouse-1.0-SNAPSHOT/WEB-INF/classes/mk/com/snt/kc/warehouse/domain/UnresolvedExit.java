/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.EnumType
 *  javax.persistence.Enumerated
 *  javax.persistence.Id
 *  javax.persistence.Table
 *  javax.persistence.Temporal
 *  javax.persistence.TemporalType
 */
package mk.com.snt.kc.warehouse.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.persistence.Persistable;

@Entity
@Table(name="WHS_UNRESOLVED_EXITS", schema="KCUSTOMS")
public class UnresolvedExit
implements Serializable,
Persistable {
    private static final long serialVersionUID = 1L;
    @Id
    private Long id;
    @Column(name="EXIT_DATE")
    @Temporal(value=TemporalType.DATE)
    private Date exitDate;
    @Column(name="VIT_WGT_NETO")
    private BigDecimal netWeightOut;
    @Column(name="VIT_WGT_GRSO")
    private BigDecimal grossWeightOut;
    @Column(name="VIT_STV")
    private BigDecimal stockValue;
    @Column(name="QTY_O")
    private BigDecimal quantityOut;
    @Column(name="EXIT_REF")
    private String exitReference;
    @Column(name="NOTES")
    private String note;
    @Column(name="COMMENTS")
    private String comment;
    @Column(name="STATUS")
    @Enumerated(value=EnumType.STRING)
    private WarehouseExit.Status status;
    @Column(name="INSTANCEID_D")
    private Long writeoffSADId;
    @Column(name="KEY_ITM_D")
    private Long writeoffSADItemId;
    @Column(name="DATE_CREATED")
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name="KEY_ITM_NBR")
    private Long sadItemNbr;
    @Column(name="INSTANCEID")
    private Long instanceId;
    @Column(name="IDE_CUO_COD")
    private String officeCode;
    @Column(name="OFFICE_NAME")
    private String officeName;
    @Column(name="IDE_REG_SER")
    private String signR;
    @Column(name="IN_REG_NBR")
    private String registerNumber;
    @Temporal(value=TemporalType.DATE)
    @Column(name="IDE_REG_DAT")
    private Date registrationDate;
    @Column(name="CMP_CON_COD")
    private String traderFiscalNumber;
    @Column(name="TARIK")
    private String tariff;
    @Column(name="GDS_ORG_CTY")
    private String countryOfOrigin;
    @Column(name="GDS_DS3")
    private String goodsDescription;

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getExitDate() {
        return this.exitDate;
    }

    public void setExitDate(Date exitDate) {
        this.exitDate = exitDate;
    }

    public BigDecimal getNetWeightOut() {
        return this.netWeightOut;
    }

    public void setNetWeightOut(BigDecimal netWeightOut) {
        this.netWeightOut = netWeightOut;
    }

    public BigDecimal getGrossWeightOut() {
        return this.grossWeightOut;
    }

    public void setGrossWeightOut(BigDecimal grossWeightOut) {
        this.grossWeightOut = grossWeightOut;
    }

    public BigDecimal getStockValue() {
        return this.stockValue;
    }

    public void setStockValue(BigDecimal stockValue) {
        this.stockValue = stockValue;
    }

    public BigDecimal getQuantityOut() {
        return this.quantityOut;
    }

    public void setQuantityOut(BigDecimal quantityOut) {
        this.quantityOut = quantityOut;
    }

    public String getExitReference() {
        return this.exitReference;
    }

    public void setExitReference(String exitReference) {
        this.exitReference = exitReference;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getWriteoffSADId() {
        return this.writeoffSADId;
    }

    public void setWriteoffSADId(Long writeoffSADId) {
        this.writeoffSADId = writeoffSADId;
    }

    public Long getWriteoffSADItemId() {
        return this.writeoffSADItemId;
    }

    public void setWriteoffSADItemId(Long writeoffSADItemId) {
        this.writeoffSADItemId = writeoffSADItemId;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public WarehouseExit.Status getStatus() {
        return this.status;
    }

    public void setStatus(WarehouseExit.Status status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSadItemNbr() {
        return this.sadItemNbr;
    }

    public void setSadItemNbr(Long sadItemNbr) {
        this.sadItemNbr = sadItemNbr;
    }

    public Long getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getOfficeCode() {
        return this.officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public String getSignR() {
        return this.signR;
    }

    public void setSignR(String signR) {
        this.signR = signR;
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

    public String getTraderFiscalNumber() {
        return this.traderFiscalNumber;
    }

    public void setTraderFiscalNumber(String traderFiscalNumber) {
        this.traderFiscalNumber = traderFiscalNumber;
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

    public String getOfficeName() {
        return this.officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }

    public String getRegisterNumberDisplay() {
        return this.signR + "-" + this.registerNumber;
    }

    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.id);
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        UnresolvedExit other = (UnresolvedExit)obj;
        return Objects.equals(this.id, other.id);
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.IdClass
 *  javax.persistence.OneToMany
 *  javax.persistence.Table
 *  javax.persistence.Temporal
 *  javax.persistence.TemporalType
 */
package mk.com.snt.kc.warehouse.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import mk.com.snt.kc.warehouse.domain.WarehouseExit;
import mk.com.snt.kc.warehouse.domain.WarehouseInventoryId;
import mk.com.snt.kc.warehouse.persistence.Displayable;
import mk.com.snt.kc.warehouse.util.Utils;

@Entity
@IdClass(value=WarehouseInventoryId.class)
@Table(name="WHS_SAD_INVENTORY", schema="KCUSTOMS")
public class WarehouseInventory
implements Serializable,
Displayable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name="KEY_ITM_NBR")
    private Long sadItemNbr;
    @Id
    @Column(name="INSTANCEID")
    private Long instanceId;
    @Column(name="REGIME")
    private String regime;
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
    @Temporal(value=TemporalType.DATE)
    @Column(name="WHS_TIM")
    private Date endDate;
    @Column(name="DEC_COD")
    private String declarantCode;
    @Column(name="DEC_REF_YER")
    private Long decRefYear;
    @Column(name="DEC_REF_NBR")
    private String decRefNbr;
    @Column(name="CMP_CON_COD")
    private String traderFiscalNumber;
    @Column(name="WHS_COD")
    private String whsCode;
    @Column(name="TAR_PRC_EXT")
    private String procedure;
    @Column(name="TARIK")
    private String tariff;
    @Column(name="VIT_WGT_NETI")
    private BigDecimal inputNetWeight;
    @Column(name="VIT_WGT_NET")
    private BigDecimal remainingNetWeight;
    @Column(name="VIT_WGT_GRSI")
    private BigDecimal inputGrossWeight;
    @Column(name="VIT_STV")
    private BigDecimal customsValue;
    @Column(name="GDS_ORG_CTY")
    private String countryOfOrigin;
    @Column(name="PCK_NBRI")
    private Long inputNumberOfPackages;
    @Column(name="PCK_TYP_COD")
    private String packagesTypeCode;
    @Column(name="PCK_TYP_NAM")
    private String packagesTypeName;
    @Column(name="QTY_I")
    private BigDecimal inputQuantity;
    @Column(name="QTY_R")
    private BigDecimal remainingQuantity;
    @Column(name="TAR_SUP_COD")
    private String measurementUnit;
    @Column(name="GDS_DS3")
    private String goodsDescription;
    @Column(name="IM4_NET")
    private BigDecimal im4NetWeight;
    @Column(name="IM4_QTY")
    private BigDecimal im4Quantity;
    @OneToMany(mappedBy="warehouseInventory")
    private List<WarehouseExit> exits;
    @Column(name="TSC_CODE")
    private String tscCode;
    @Column(name="TAR_PRF")
    private String tarPrf;
    @Column(name="WGT_NET_ASY")
    private BigDecimal wgtNetAsy;
    @Column(name="WHS_OWNER")
    private String whsOwner;

    public Long getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getRegime() {
        return this.regime;
    }

    public void setRegime(String regime) {
        this.regime = regime;
    }

    public String getOfficeCode() {
        return this.officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public String getOfficeName() {
        return this.officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
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

    public Date getEndDate() {
        return this.endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDeclarantCode() {
        return this.declarantCode;
    }

    public void setDeclarantCode(String declarantCode) {
        this.declarantCode = declarantCode;
    }

    public Long getDecRefYear() {
        return this.decRefYear;
    }

    public void setDecRefYear(Long decRefYear) {
        this.decRefYear = decRefYear;
    }

    public String getDecRefNbr() {
        return this.decRefNbr;
    }

    public void setDecRefNbr(String decRefNbr) {
        this.decRefNbr = decRefNbr;
    }

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

    public Long getSadItemNbr() {
        return this.sadItemNbr;
    }

    public void setSadItemNbr(Long sadItemNbr) {
        this.sadItemNbr = sadItemNbr;
    }

    public String getProcedure() {
        return this.procedure;
    }

    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    public String getTariff() {
        return this.tariff;
    }

    public void setTariff(String tariff) {
        this.tariff = tariff;
    }

    public BigDecimal getInputNetWeight() {
        return this.inputNetWeight;
    }

    public void setInputNetWeight(BigDecimal inputNetWeight) {
        this.inputNetWeight = inputNetWeight;
    }

    public BigDecimal getRemainingNetWeight() {
        return this.remainingNetWeight;
    }

    public void setRemainingNetWeight(BigDecimal remainingNetWeight) {
        this.remainingNetWeight = remainingNetWeight;
    }

    public BigDecimal getInputGrossWeight() {
        return this.inputGrossWeight;
    }

    public void setInputGrossWeight(BigDecimal inputGrossWeight) {
        this.inputGrossWeight = inputGrossWeight;
    }

    public BigDecimal getCustomsValue() {
        return this.customsValue;
    }

    public void setCustomsValue(BigDecimal customsValue) {
        this.customsValue = customsValue;
    }

    public String getCountryOfOrigin() {
        return this.countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public Long getInputNumberOfPackages() {
        return this.inputNumberOfPackages;
    }

    public void setInputNumberOfPackages(Long inputNumberOfPackages) {
        this.inputNumberOfPackages = inputNumberOfPackages;
    }

    public String getPackagesTypeCode() {
        return this.packagesTypeCode;
    }

    public void setPackagesTypeCode(String packagesTypeCode) {
        this.packagesTypeCode = packagesTypeCode;
    }

    public String getPackagesTypeName() {
        return this.packagesTypeName;
    }

    public void setPackagesTypeName(String packagesTypeName) {
        this.packagesTypeName = packagesTypeName;
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

    public String getMeasurementUnit() {
        return this.measurementUnit;
    }

    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public String getGoodsDescription() {
        return this.goodsDescription;
    }

    public void setGoodsDescription(String goodsDescription) {
        this.goodsDescription = goodsDescription;
    }

    public BigDecimal getIm4NetWeight() {
        return this.im4NetWeight;
    }

    public void setIm4NetWeight(BigDecimal im4NetWeight) {
        this.im4NetWeight = im4NetWeight;
    }

    public BigDecimal getIm4Quantity() {
        return this.im4Quantity;
    }

    public void setIm4Quantity(BigDecimal im4Quantity) {
        this.im4Quantity = im4Quantity;
    }

    public List<WarehouseExit> getExits() {
        return this.exits;
    }

    public void setExits(List<WarehouseExit> exits) {
        this.exits = exits;
    }

    public String getTscCode() {
        return this.tscCode;
    }

    public void setTscCode(String tscCode) {
        this.tscCode = tscCode;
    }

    public String getTarPrf() {
        return this.tarPrf;
    }

    public void setTarPrf(String tarPrf) {
        this.tarPrf = tarPrf;
    }

    public BigDecimal getWgtNetAsy() {
        return this.wgtNetAsy;
    }

    public void setWgtNetAsy(BigDecimal wgtNetAsy) {
        this.wgtNetAsy = wgtNetAsy;
    }

    public String getWhsOwner() {
        return this.whsOwner;
    }

    public void setWhsOwner(String whsOwner) {
        this.whsOwner = whsOwner;
    }

    public void addExit(WarehouseExit exit) {
        if (this.exits == null) {
            this.exits = new ArrayList<WarehouseExit>();
        }
        this.exits.add(exit);
    }

    public void removeExit(WarehouseExit exit) {
        if (this.exits != null) {
            this.exits.remove(exit);
        }
    }

    public String getRegisterNumberDisplay() {
        return this.signR + "-" + this.registerNumber;
    }

    public WarehouseInventoryId getId() {
        return new WarehouseInventoryId(this.getInstanceId(), this.getSadItemNbr());
    }

    public boolean isQuantityEnabled() {
        return this.getInputQuantity() != null;
    }

    @Override
    public String getDisplay() {
        String pattern = "%s: %s/%s/%s-%s %s %s";
        return String.format(pattern, this.getTraderFiscalNumber(), this.getRegisterNumberDisplay(), Utils.convert(this.getRegistrationDate()), this.getOfficeCode(), this.getSadItemNbr(), this.getTariff(), this.getCountryOfOrigin());
    }

    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.instanceId);
        hash = 41 * hash + Objects.hashCode(this.sadItemNbr);
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        WarehouseInventory other = (WarehouseInventory)obj;
        if (!Objects.equals(this.instanceId, other.instanceId)) {
            return false;
        }
        return Objects.equals(this.sadItemNbr, other.sadItemNbr);
    }
}

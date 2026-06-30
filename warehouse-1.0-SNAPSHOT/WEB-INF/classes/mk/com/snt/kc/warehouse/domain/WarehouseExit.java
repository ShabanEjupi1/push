/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.EnumType
 *  javax.persistence.Enumerated
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.JoinColumn
 *  javax.persistence.JoinColumns
 *  javax.persistence.ManyToOne
 *  javax.persistence.NamedQueries
 *  javax.persistence.NamedQuery
 *  javax.persistence.SequenceGenerator
 *  javax.persistence.Table
 *  javax.persistence.Temporal
 *  javax.persistence.TemporalType
 */
package mk.com.snt.kc.warehouse.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import mk.com.snt.kc.warehouse.domain.WarehouseInventory;
import mk.com.snt.kc.warehouse.persistence.Persistable;
import mk.com.snt.kc.warehouse.util.Utils;

@Entity
@Table(name="WHS_STOCK_OUT", schema="KCUSTOMS")
@NamedQueries(value={@NamedQuery(name="WarehouseExit.byInventory", query="SELECT we FROM WarehouseExit we WHERE we.warehouseInventory = :warehouseInventory ORDER BY we.exitDate"), @NamedQuery(name="WarehouseExit.countByInventory", query="SELECT COUNT(we.id) FROM WarehouseExit we WHERE we.warehouseInventory = :warehouseInventory"), @NamedQuery(name="WarehouseExit.latestByTrader", query="SELECT we FROM WarehouseExit we WHERE we.createdAt >= :date AND (we.warehouseInventory.whsCode = :trader OR we.warehouseInventory.whsOwner = :whsOwner) ORDER BY we.id DESC"), @NamedQuery(name="WarehouseExit.countLatestByTrader", query="SELECT COUNT(we.id) FROM WarehouseExit we WHERE we.createdAt >= :date AND (we.warehouseInventory.whsCode = :trader OR we.warehouseInventory.whsOwner = :whsOwner)"), @NamedQuery(name="WarehouseExit.countTodaysInventory", query="SELECT COUNT(we.id) FROM WarehouseExit we WHERE we.exitDate = :date AND we.warehouseInventory = :warehouseInventory")})
public class WarehouseExit
implements Serializable,
Persistable {
    private static final long serialVersionUID = 1L;
    public static final String BY_INVENTORY = "WarehouseExit.byInventory";
    public static final String COUNT_BY_INVENTORY = "WarehouseExit.countByInventory";
    public static final String COUNT_TODAYS_INVENTORY = "WarehouseExit.countTodaysInventory";
    public static final String LATEST_BY_TRADER = "WarehouseExit.latestByTrader";
    public static final String COUNT_LATEST_BY_TRADER = "WarehouseExit.countLatestByTrader";
    private static final String SEQ_NAME = "WHS_STOCK_OUT_SEQ";
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="WHS_STOCK_OUT_SEQ")
    @SequenceGenerator(name="WHS_STOCK_OUT_SEQ", sequenceName="WHS_STOCK_OUT_SEQ", allocationSize=1, initialValue=1, schema="KCUSTOMS")
    private Long id;
    @Column(name="EXIT_DATE")
    @Temporal(value=TemporalType.DATE)
    private Date exitDate;
    @ManyToOne
    @JoinColumns(value={@JoinColumn(name="KEY_ITM_NBR", referencedColumnName="KEY_ITM_NBR"), @JoinColumn(name="INSTANCEID", referencedColumnName="INSTANCEID")})
    private WarehouseInventory warehouseInventory;
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
    private Status status;
    @Column(name="INSTANCEID_D")
    private Long writeoffSADId;
    @Column(name="KEY_ITM_D")
    private Long writeoffSADItemId;
    @Column(name="DATE_CREATED")
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date createdAt;

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

    public WarehouseInventory getWarehouseInventory() {
        return this.warehouseInventory;
    }

    public void setWarehouseInventory(WarehouseInventory warehouseInventory) {
        this.warehouseInventory = warehouseInventory;
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

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void validate() {
        Utils.validateCondition(this.getWarehouseInventory() == null, "WarehouseExit.warehouseInventory.required", new Object[0]);
        Utils.validateCondition(this.getExitDate() == null, "WarehouseExit.exitDate.required", new Object[0]);
        Utils.validateCondition(this.getNetWeightOut() == null, "WarehouseExit.netWeightOut.required", new Object[0]);
        Utils.validateCondition(this.getStockValue() == null, "WarehouseExit.stockValue.required", new Object[0]);
        Utils.validateCondition(this.getGrossWeightOut() == null, "WarehouseExit.grossWeightOut.required", new Object[0]);
        Utils.validateCondition(Utils.isNullOrEmpty(this.getExitReference()), "WarehouseExit.exitReference.required", new Object[0]);
        Utils.validateCondition(this.isQuantityEnabled() && this.getQuantityOut() == null, "WarehouseExit.quantityOut.required", new Object[0]);
    }

    public void validateStorningExit() {
        Utils.validateCondition(Utils.isNullOrEmpty(this.comment), "WarehouseExit.comment.required", new Object[0]);
    }

    public boolean isQuantityEnabled() {
        return this.getWarehouseInventory() != null && this.getWarehouseInventory().isQuantityEnabled();
    }

    public void calculateNetWeight() {
        this.netWeightOut = this.getWarehouseInventory().getInputNetWeight().multiply(this.getQuantityOut()).divide(this.getWarehouseInventory().getInputQuantity(), 2, RoundingMode.HALF_UP);
        this.calculateGrossWeight();
        this.calculateValue();
    }

    public void calculateGrossWeight() {
        this.grossWeightOut = this.getWarehouseInventory().getInputGrossWeight().multiply(this.getNetWeightOut()).divide(this.getWarehouseInventory().getInputNetWeight(), 2, RoundingMode.HALF_UP);
    }

    public void calculateValue() {
        this.stockValue = this.getWarehouseInventory().getCustomsValue().multiply(this.getNetWeightOut()).divide(this.getWarehouseInventory().getInputNetWeight(), 2, RoundingMode.HALF_UP);
    }

    public void clearAmounts() {
        this.setQuantityOut(null);
        this.setNetWeightOut(null);
        this.setGrossWeightOut(null);
        this.setStockValue(null);
    }

    public boolean isEditable() {
        return Status.VALID.equals((Object)this.getStatus()) && this.isCurrent();
    }

    public boolean isStorned() {
        return Status.STORNED.equals((Object)this.getStatus());
    }

    private boolean isCurrent() {
        return Utils.isBefore(Utils.minusDays(new Date(), 1), this.getCreatedAt());
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
        WarehouseExit other = (WarehouseExit)obj;
        return Objects.equals(this.id, other.id);
    }

    public String getValue(String key) {
        switch (key) {
            case "registerNumber": {
                return this.getWarehouseInventory().getRegisterNumberDisplay();
            }
            case "regDate": {
                return Utils.convert(this.getWarehouseInventory().getRegistrationDate());
            }
            case "offName": {
                return this.getWarehouseInventory().getOfficeName();
            }
            case "sadItemNbr": {
                return this.getWarehouseInventory().getSadItemNbr().toString();
            }
            case "tarEx": {
                return this.getWarehouseInventory().getTariff();
            }
            case "origin": {
                return this.getWarehouseInventory().getCountryOfOrigin();
            }
            case "netWeightOut": {
                return Utils.convertForPrint(this.netWeightOut);
            }
            case "grossWeightOut": {
                return Utils.convertForPrint(this.grossWeightOut);
            }
            case "quantity": {
                return Utils.convertForPrint(this.quantityOut);
            }
            case "stockValue": {
                return Utils.convertForPrint(this.stockValue);
            }
            case "exitReference": {
                return this.exitReference;
            }
            case "exitDate": {
                return Utils.convert(this.exitDate);
            }
            case "im4NetWeight": {
                return Utils.convertForPrint(this.getWarehouseInventory().getIm4NetWeight());
            }
            case "im4Quantity": {
                return Utils.convertForPrint(this.getWarehouseInventory().getIm4Quantity());
            }
            case "stat": {
                return this.status.equals((Object)Status.VALID) ? "V" : "S";
            }
        }
        return "";
    }

    public static enum Status {
        VALID,
        STORNED;

    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.JoinColumn
 *  javax.persistence.ManyToOne
 *  javax.persistence.SequenceGenerator
 *  javax.persistence.Table
 */
package mk.com.snt.kc.warehouse.audit;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import mk.com.snt.kc.warehouse.audit.AuditLog;
import mk.com.snt.kc.warehouse.persistence.Persistable;

@Entity
@Table(name="WHS_AUDIT_ENTRIES", schema="KCUSTOMS")
public class AuditEntry
implements Serializable,
Persistable {
    private static final long serialVersionUID = 1L;
    private static final String SEQ_NAME = "WHS_AUDIT_ENTRIES_SEQ";
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="WHS_AUDIT_ENTRIES_SEQ")
    @SequenceGenerator(name="WHS_AUDIT_ENTRIES_SEQ", sequenceName="WHS_AUDIT_ENTRIES_SEQ", allocationSize=1, initialValue=1, schema="KCUSTOMS")
    private Long id;
    @Column(name="FIELD", nullable=false, updatable=false)
    private String field;
    @Column(name="OLD_VALUE", updatable=false)
    private String oldValue;
    @Column(name="NEW_VALUE", updatable=false)
    private String newValue;
    @ManyToOne
    @JoinColumn(name="AUDIT_LOG_ID", nullable=false, updatable=false)
    private AuditLog auditLog;

    public AuditEntry() {
    }

    public AuditEntry(String field, String oldValue, String newValue) {
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getField() {
        return this.field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOldValue() {
        return this.oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return this.newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public AuditLog getAuditLog() {
        return this.auditLog;
    }

    public void setAuditLog(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public int hashCode() {
        int hash = 0;
        return hash += this.id != null ? this.id.hashCode() : 0;
    }

    public boolean equals(Object object) {
        if (!(object instanceof AuditEntry)) {
            return false;
        }
        AuditEntry other = (AuditEntry)object;
        return !(this.id == null && other.id != null || this.id != null && !this.id.equals(other.id));
    }

    public String toString() {
        return "AuditEntry{id=" + this.id + ", field=" + this.field + ", oldValue=" + this.oldValue + ", newValue=" + this.newValue + ", auditLog=" + this.auditLog + '}';
    }
}

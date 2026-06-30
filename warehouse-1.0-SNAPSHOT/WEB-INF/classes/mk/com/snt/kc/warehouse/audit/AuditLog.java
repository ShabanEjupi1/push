/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.CascadeType
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.EnumType
 *  javax.persistence.Enumerated
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.OneToMany
 *  javax.persistence.SequenceGenerator
 *  javax.persistence.Table
 *  javax.persistence.Temporal
 *  javax.persistence.TemporalType
 */
package mk.com.snt.kc.warehouse.audit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import mk.com.snt.kc.warehouse.audit.AuditEntry;
import mk.com.snt.kc.warehouse.persistence.Persistable;

@Entity
@Table(name="WHS_AUDIT_LOG", schema="KCUSTOMS")
public class AuditLog
implements Serializable,
Persistable {
    private static final long serialVersionUID = 1L;
    private static final String SEQ_NAME = "WHS_AUDIT_LOG_SEQ";
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="WHS_AUDIT_LOG_SEQ")
    @SequenceGenerator(name="WHS_AUDIT_LOG_SEQ", sequenceName="WHS_AUDIT_LOG_SEQ", allocationSize=1, initialValue=1, schema="KCUSTOMS")
    private Long id;
    @Column(name="EVENT_AT", nullable=false, updatable=false)
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date eventAt;
    @Column(name="EVENT_BY", nullable=false, updatable=false)
    private String eventBy;
    @Column(name="ENTITY_TYPE", nullable=false, updatable=false)
    @Enumerated(value=EnumType.STRING)
    private EntityType entityType;
    @Column(name="ENTITY_ID", updatable=false)
    private Long entityId;
    @Column(name="ACTION_TYPE", nullable=false, updatable=false)
    @Enumerated(value=EnumType.STRING)
    private ActionType actionType;
    @OneToMany(mappedBy="auditLog", cascade={CascadeType.ALL})
    private List<AuditEntry> auditEntries;

    public AuditLog() {
    }

    public AuditLog(String eventBy, EntityType entityType, Long entityId, ActionType actionType) {
        this.eventAt = new Date();
        this.eventBy = eventBy;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actionType = actionType;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public Date getEventAt() {
        return this.eventAt;
    }

    public String getEventBy() {
        return this.eventBy;
    }

    public EntityType getEntityType() {
        return this.entityType;
    }

    public Long getEntityId() {
        return this.entityId;
    }

    public ActionType getActionType() {
        return this.actionType;
    }

    public List<AuditEntry> getAuditEntries() {
        return this.auditEntries;
    }

    public void addEntry(AuditEntry entry) {
        if (this.auditEntries == null) {
            this.auditEntries = new ArrayList<AuditEntry>();
        }
        entry.setAuditLog(this);
        this.auditEntries.add(entry);
    }

    public int hashCode() {
        int hash = 0;
        return hash += this.id != null ? this.id.hashCode() : 0;
    }

    public boolean equals(Object object) {
        if (!(object instanceof AuditLog)) {
            return false;
        }
        AuditLog other = (AuditLog)object;
        return !(this.id == null && other.id != null || this.id != null && !this.id.equals(other.id));
    }

    public String toString() {
        return "AuditLog{id=" + this.id + ", eventAt=" + this.eventAt + ", eventBy=" + this.eventBy + ", entityType=" + (Object)((Object)this.entityType) + ", entityId=" + this.entityId + ", actionType=" + (Object)((Object)this.actionType) + '}';
    }

    public static enum ActionType {
        CREATION,
        UPDATE,
        DELETE,
        ACTIVATION,
        DEACTIVATION,
        STORNING,
        RELINK;

    }

    public static enum EntityType {
        WAREHOUSE_EXIT,
        USER;

    }
}

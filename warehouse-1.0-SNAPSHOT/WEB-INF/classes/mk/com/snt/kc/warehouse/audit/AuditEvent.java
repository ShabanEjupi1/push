/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.audit;

import java.io.Serializable;
import mk.com.snt.kc.warehouse.audit.AuditLog;
import mk.com.snt.kc.warehouse.persistence.Persistable;

public class AuditEvent
implements Serializable {
    private Persistable entity;
    private AuditLog.ActionType actionType;
    private AuditLog.EntityType entityType;

    public AuditEvent(Persistable entity, AuditLog.ActionType actionType, AuditLog.EntityType entityType) {
        this.entity = entity;
        this.actionType = actionType;
        this.entityType = entityType;
    }

    public Persistable getEntity() {
        return this.entity;
    }

    public AuditLog.ActionType getActionType() {
        return this.actionType;
    }

    public AuditLog.EntityType getEntityType() {
        return this.entityType;
    }
}

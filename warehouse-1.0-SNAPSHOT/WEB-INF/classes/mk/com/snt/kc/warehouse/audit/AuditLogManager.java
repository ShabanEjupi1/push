/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.Stateless
 *  javax.enterprise.event.Observes
 *  javax.inject.Inject
 *  org.apache.shiro.SecurityUtils
 *  org.apache.shiro.UnavailableSecurityManagerException
 */
package mk.com.snt.kc.warehouse.audit;

import java.lang.reflect.Field;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import mk.com.snt.kc.warehouse.audit.AuditEntry;
import mk.com.snt.kc.warehouse.audit.AuditEvent;
import mk.com.snt.kc.warehouse.audit.AuditLog;
import mk.com.snt.kc.warehouse.persistence.CrudService;
import mk.com.snt.kc.warehouse.persistence.Persistable;
import mk.com.snt.kc.warehouse.util.Utils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;

@Stateless
public class AuditLogManager {
    @Inject
    CrudService crudService;

    public void save(AuditLog log) {
        this.crudService.save(log);
    }

    public void logEvent(@Observes AuditEvent event) {
        AuditLog log = new AuditLog(this.getUsername(), event.getEntityType(), event.getEntity().getId(), event.getActionType());
        switch (event.getActionType()) {
            case DELETE: {
                break;
            }
            default: {
                Field[] fields;
                Persistable existingEntity = this.loadExistingEntity(event);
                for (Field field : fields = event.getEntity().getClass().getDeclaredFields()) {
                    if (!Utils.isJPAField(field)) continue;
                    this.handleField(event, log, field, existingEntity);
                }
            }
        }
        this.save(log);
    }

    private Persistable loadExistingEntity(AuditEvent event) {
        Persistable existingEntity = null;
        if (event.getEntity().getId() != null) {
            existingEntity = (Persistable)this.crudService.find(event.getEntity().getClass(), event.getEntity().getId());
            this.crudService.detach(existingEntity);
        }
        return existingEntity;
    }

    private void handleField(AuditEvent event, AuditLog log, Field field, Persistable existingEntity) {
        String newValue = Utils.convertFieldValue(field, event.getEntity());
        String oldValue = existingEntity != null ? Utils.convertFieldValue(field, existingEntity) : null;
        log.addEntry(new AuditEntry(field.getName(), oldValue, newValue));
    }

    private String getUsername() {
        try {
            return (String)SecurityUtils.getSubject().getPrincipal();
        }
        catch (UnavailableSecurityManagerException ex) {
            return "system";
        }
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.Stateless
 *  javax.enterprise.event.Event
 *  javax.inject.Inject
 *  org.apache.shiro.crypto.hash.Sha256Hash
 */
package mk.com.snt.kc.warehouse.boundary;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import mk.com.snt.kc.warehouse.audit.AuditEvent;
import mk.com.snt.kc.warehouse.audit.AuditLog;
import mk.com.snt.kc.warehouse.domain.User;
import mk.com.snt.kc.warehouse.persistence.CrudService;
import mk.com.snt.kc.warehouse.persistence.QueryParameter;
import mk.com.snt.kc.warehouse.util.Utils;
import org.apache.shiro.crypto.hash.Sha256Hash;

@Stateless
public class UserManager {
    @Inject
    CrudService crudService;
    @Inject
    Event<AuditEvent> auditEvent;

    public User getUser(String username) {
        return this.crudService.findSingleWithNamedQuery("User.byUsername", QueryParameter.with("username", username).parameters(), User.class);
    }

    public User getTraderUser(String traderFiscalNumber) {
        return this.crudService.findSingleWithNamedQuery("User.byTrader", QueryParameter.with("traderFiscalNumber", traderFiscalNumber).parameters(), User.class);
    }

    public User save(User user) {
        user.validate();
        if (user.getId() == null) {
            Utils.validateCondition(this.getUser(user.getUsername()) != null, "UserManager.create.duplicateUsername", user.getUsername());
            Utils.validateCondition(!user.isAdministrator() && this.getTraderUser(user.getTraderFiscalNumber()) != null, "UserManager.create.duplicateTrader", user.getTraderFiscalNumber());
            user.setPassword(this.encryptPassword(user.getPassword()));
        } else {
            User existing = this.crudService.find(User.class, user.getId());
            Utils.validateCondition(!user.getUsername().equals(existing.getUsername()), "UserManager.update.usernameChangeNotAllowed", new Object[0]);
            if (!user.isAdministrator() && !user.getTraderFiscalNumber().equals(existing.getTraderFiscalNumber())) {
                User traderUser = this.getTraderUser(user.getTraderFiscalNumber());
                Utils.validateCondition(traderUser != null, "UserManager.create.duplicateTrader", user.getTraderFiscalNumber());
            }
            if (!user.getPassword().equals(existing.getPassword())) {
                user.setPassword(this.encryptPassword(user.getPassword()));
            }
        }
        this.auditEvent.fire(new AuditEvent(user, user.getId() == null ? AuditLog.ActionType.CREATION : AuditLog.ActionType.UPDATE, AuditLog.EntityType.USER));
        return this.crudService.save(user);
    }

    public void activate(String username) {
        this.changeUserActivity(username, true);
    }

    public void deactivate(String username) {
        this.changeUserActivity(username, false);
    }

    private void changeUserActivity(String username, boolean active) {
        User theUser = this.getUser(username);
        this.crudService.detach(theUser);
        theUser.setActive(active);
        this.auditEvent.fire(new AuditEvent(theUser, active ? AuditLog.ActionType.ACTIVATION : AuditLog.ActionType.DEACTIVATION, AuditLog.EntityType.USER));
        this.crudService.save(theUser);
    }

    private String encryptPassword(String password) {
        return new Sha256Hash((Object)password).toHex();
    }

    public void updateUser(User user) {
        User theUser = this.getUser(user.getUsername());
        if (Utils.isNullOrEmpty(user.getPassword())) {
            user.setPassword(theUser.getPassword());
        } else {
            user.setPassword(this.encryptPassword(user.getPassword()));
        }
        this.auditEvent.fire(new AuditEvent(user, AuditLog.ActionType.UPDATE, AuditLog.EntityType.USER));
        this.crudService.save(user);
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.inject.Inject
 *  javax.inject.Named
 *  org.omnifaces.util.Messages
 *  org.primefaces.context.RequestContext
 */
package mk.com.snt.kc.warehouse.view;

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.boundary.UserManager;
import mk.com.snt.kc.warehouse.domain.User;
import mk.com.snt.kc.warehouse.util.Utils;
import mk.com.snt.kc.warehouse.view.SessionData;
import mk.com.snt.kc.warehouse.view.util.BundleMessages;
import org.omnifaces.util.Messages;
import org.primefaces.context.RequestContext;

@SessionScoped
@Named(value="userProfileBean")
public class UserProfileBean
implements Serializable {
    @Inject
    SessionData sessionData;
    @Inject
    UserManager userManager;
    private String name;
    private String password;
    private String confirmPassword;
    private String lang;

    public void init() {
        this.name = this.sessionData.getUser().getName();
        this.lang = this.sessionData.getUser().getLang();
    }

    public void save() {
        RequestContext context = RequestContext.getCurrentInstance();
        User user = this.sessionData.getUser();
        if (Utils.isNullOrEmpty(this.name)) {
            Messages.addGlobalError((String)"required_message", (Object[])new Object[]{BundleMessages.getMessage("user_name", new Object[0])});
            context.addCallbackParam("invalid", (Object)true);
            return;
        }
        if (!Utils.isNullOrEmpty(this.password)) {
            if (!this.isPasswordValid()) {
                context.addCallbackParam("invalid", (Object)true);
                return;
            }
            user.setPassword(this.password);
        } else {
            user.setPassword(null);
        }
        user.setName(this.name);
        user.setLang(this.lang);
        this.userManager.updateUser(user);
        Messages.addGlobalInfo((String)"UsersBean.save.success", (Object[])new Object[]{this.sessionData.getCurrentUsername()});
    }

    private boolean isPasswordValid() {
        if (!this.password.equals(this.confirmPassword)) {
            Messages.addGlobalError((String)"UsersBean.save.passwordMismatch", (Object[])new Object[0]);
            return false;
        }
        return true;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return this.confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}

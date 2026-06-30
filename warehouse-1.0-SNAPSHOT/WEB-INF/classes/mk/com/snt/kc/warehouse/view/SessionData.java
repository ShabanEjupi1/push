/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.enterprise.event.Observes
 *  javax.enterprise.inject.Produces
 *  javax.inject.Inject
 *  javax.inject.Named
 *  org.apache.shiro.SecurityUtils
 *  org.omnifaces.util.Faces
 */
package mk.com.snt.kc.warehouse.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.boundary.UserManager;
import mk.com.snt.kc.warehouse.domain.User;
import mk.com.snt.kc.warehouse.view.security.LoginEvent;
import org.apache.shiro.SecurityUtils;
import org.omnifaces.util.Faces;

@SessionScoped
@Named
public class SessionData
implements Serializable {
    @Inject
    UserManager userManager;
    private User user;

    public void onLogin(@Observes LoginEvent loginEvent) {
        this.user = this.userManager.getUser(loginEvent.getUsername());
    }

    public boolean isAuthenticated() {
        return SecurityUtils.getSubject().isAuthenticated();
    }

    public String getCurrentUsername() {
        return (String)SecurityUtils.getSubject().getPrincipal();
    }

    public String getCurrentUserDisplay() {
        return this.user != null ? this.user.getDisplay() : "";
    }

    public User getUser() {
        return this.user;
    }

    public boolean isAdmin() {
        return this.user.isAdministrator();
    }

    public String getTrader() {
        return this.user.isAdministrator() ? "" : this.user.getTraderFiscalNumber();
    }

    @Produces
    @Named(value="supportedLocales")
    public List<String> getSupportedLocales() {
        ArrayList<String> supportedLocales = new ArrayList<String>();
        List<Locale> locales = Faces.getSupportedLocales();
        for (Locale locale : locales) {
            supportedLocales.add(locale.getLanguage());
        }
        return supportedLocales;
    }

    public String getLocale() {
        return this.user != null ? this.user.getLang() : "en";
    }
}

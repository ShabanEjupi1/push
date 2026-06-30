/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.Singleton
 *  javax.ejb.Startup
 *  javax.inject.Inject
 */
package mk.com.snt.kc.warehouse.startup;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import mk.com.snt.kc.warehouse.boundary.UserManager;
import mk.com.snt.kc.warehouse.domain.User;

@Singleton
@Startup
public class Initializator {
    private static final String DEFAULT_USER = "admin";
    @Inject
    UserManager userManager;

    @PostConstruct
    public void init() {
        try {
            this.initUser();
        } catch (Exception e) {
            System.err.println("[Initializator] Startup DB check failed: " + e.getMessage());
        }
    }

    private void initUser() {
        if (this.userManager.getUser(DEFAULT_USER) == null) {
            User user = new User(DEFAULT_USER, DEFAULT_USER, DEFAULT_USER);
            this.userManager.save(user);
        }
    }
}

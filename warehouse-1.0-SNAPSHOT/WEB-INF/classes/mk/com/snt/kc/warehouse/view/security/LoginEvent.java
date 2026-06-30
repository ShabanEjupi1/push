/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.view.security;

import java.io.Serializable;

public class LoginEvent
implements Serializable {
    private String username;

    public LoginEvent(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }
}

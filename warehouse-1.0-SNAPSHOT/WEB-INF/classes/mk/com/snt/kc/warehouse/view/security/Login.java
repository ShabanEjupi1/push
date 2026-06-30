/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.event.Event
 *  javax.enterprise.inject.Model
 *  javax.inject.Inject
 *  javax.servlet.ServletRequest
 *  org.apache.shiro.SecurityUtils
 *  org.apache.shiro.authc.AuthenticationException
 *  org.apache.shiro.authc.AuthenticationToken
 *  org.apache.shiro.authc.UsernamePasswordToken
 *  org.apache.shiro.web.util.SavedRequest
 *  org.apache.shiro.web.util.WebUtils
 *  org.omnifaces.util.Faces
 *  org.omnifaces.util.Messages
 */
package mk.com.snt.kc.warehouse.view.security;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.servlet.ServletRequest;
import mk.com.snt.kc.warehouse.view.security.LoginEvent;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;

@Model
public class Login {
    private static final String HOME_URL = "home";
    @Inject
    Logger LOG;
    @Inject
    Event<LoginEvent> event;
    private String username;
    private String password;

    public void submit() throws IOException {
        try {
            SecurityUtils.getSubject().login((AuthenticationToken)new UsernamePasswordToken(this.username, this.password, false));
            this.event.fire(new LoginEvent(this.username));
            SavedRequest savedRequest = WebUtils.getAndClearSavedRequest((ServletRequest)Faces.getRequest());
            Faces.redirect((String)(savedRequest != null ? savedRequest.getRequestUrl() : HOME_URL), (String[])new String[0]);
        }
        catch (AuthenticationException e) {
            Messages.addGlobalError((String)"login_auth_error", (Object[])new Object[0]);
            this.LOG.log(Level.SEVERE, e.getMessage());
        }
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

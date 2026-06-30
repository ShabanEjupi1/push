/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.inject.Model
 *  org.apache.shiro.SecurityUtils
 *  org.omnifaces.util.Faces
 */
package mk.com.snt.kc.warehouse.view.security;

import java.io.IOException;
import javax.enterprise.inject.Model;
import org.apache.shiro.SecurityUtils;
import org.omnifaces.util.Faces;

@Model
public class Logout {
    private static final String LOGIN_PAGE = "login";

    public void submit() throws IOException {
        SecurityUtils.getSubject().logout();
        Faces.invalidateSession();
        Faces.redirect((String)LOGIN_PAGE, (String[])new String[0]);
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.ServletRequest
 *  javax.servlet.ServletResponse
 *  org.apache.shiro.subject.Subject
 *  org.apache.shiro.web.filter.authz.AuthorizationFilter
 */
package mk.com.snt.kc.warehouse.view.security;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.AuthorizationFilter;

public class AnyRoleAuthorizationFilter
extends AuthorizationFilter {
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        Subject subject = this.getSubject(request, response);
        String[] rolesArray = (String[])mappedValue;
        if (rolesArray == null || rolesArray.length == 0) {
            return true;
        }
        for (String roleName : rolesArray) {
            if (!subject.hasRole(roleName)) continue;
            return true;
        }
        return false;
    }
}

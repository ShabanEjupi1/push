/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.inject.Inject
 *  org.apache.shiro.authc.AuthenticationException
 *  org.apache.shiro.authc.AuthenticationInfo
 *  org.apache.shiro.authc.AuthenticationToken
 *  org.apache.shiro.authc.SimpleAuthenticationInfo
 *  org.apache.shiro.authc.UsernamePasswordToken
 *  org.apache.shiro.authz.AuthorizationInfo
 *  org.apache.shiro.authz.SimpleAuthorizationInfo
 *  org.apache.shiro.realm.AuthorizingRealm
 *  org.apache.shiro.subject.PrincipalCollection
 */
package mk.com.snt.kc.warehouse.view.security;

import javax.inject.Inject;
import mk.com.snt.kc.warehouse.boundary.UserManager;
import mk.com.snt.kc.warehouse.domain.User;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

public class DatabaseRealm
extends AuthorizingRealm {
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    @Inject
    UserManager userManager;

    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = (String)principals.fromRealm(this.getName()).iterator().next();
        User user = this.userManager.getUser(username);
        if (user != null) {
            SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
            info.addRole(ROLE_USER);
            if (user.isAdministrator()) {
                info.addRole(ROLE_ADMIN);
            }
            return info;
        }
        return null;
    }

    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken)token;
        User user = this.userManager.getUser(upToken.getUsername());
        if (user != null) {
            return new SimpleAuthenticationInfo((Object)upToken.getUsername(), (Object)user.getPassword(), this.getName());
        }
        throw new AuthenticationException("Invalid username");
    }
}

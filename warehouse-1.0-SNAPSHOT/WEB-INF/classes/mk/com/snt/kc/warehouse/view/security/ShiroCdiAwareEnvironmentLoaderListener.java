/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.inject.Inject
 *  javax.servlet.ServletContext
 *  org.apache.shiro.authc.credential.CredentialsMatcher
 *  org.apache.shiro.authc.credential.HashedCredentialsMatcher
 *  org.apache.shiro.cache.CacheManager
 *  org.apache.shiro.cache.MemoryConstrainedCacheManager
 *  org.apache.shiro.mgt.RealmSecurityManager
 *  org.apache.shiro.mgt.SecurityManager
 *  org.apache.shiro.realm.Realm
 *  org.apache.shiro.web.env.DefaultWebEnvironment
 *  org.apache.shiro.web.env.EnvironmentLoaderListener
 *  org.apache.shiro.web.env.WebEnvironment
 */
package mk.com.snt.kc.warehouse.view.security;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import mk.com.snt.kc.warehouse.view.security.DatabaseRealm;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.env.WebEnvironment;

public class ShiroCdiAwareEnvironmentLoaderListener
extends EnvironmentLoaderListener {
    @Inject
    private DatabaseRealm theRealm;

    protected WebEnvironment createEnvironment(ServletContext pServletContext) {
        WebEnvironment environment = super.createEnvironment(pServletContext);
        RealmSecurityManager rsm = (RealmSecurityManager)environment.getSecurityManager();
        HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher();
        credentialsMatcher.setHashAlgorithmName("SHA-256");
        this.theRealm.setCredentialsMatcher((CredentialsMatcher)credentialsMatcher);
        MemoryConstrainedCacheManager cacheManager = new MemoryConstrainedCacheManager();
        this.theRealm.setCacheManager((CacheManager)cacheManager);
        rsm.setRealm((Realm)this.theRealm);
        ((DefaultWebEnvironment)environment).setSecurityManager((SecurityManager)rsm);
        return environment;
    }
}

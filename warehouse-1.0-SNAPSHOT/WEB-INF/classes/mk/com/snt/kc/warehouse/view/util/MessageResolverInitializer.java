/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.ServletContextEvent
 *  javax.servlet.ServletContextListener
 *  javax.servlet.annotation.WebListener
 *  org.omnifaces.util.Messages
 *  org.omnifaces.util.Messages$Resolver
 */
package mk.com.snt.kc.warehouse.view.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import mk.com.snt.kc.warehouse.view.util.BundleMessages;
import org.omnifaces.util.Messages;

@WebListener
public class MessageResolverInitializer
implements ServletContextListener {
    public void contextInitialized(ServletContextEvent sce) {
        Messages.setResolver((Messages.Resolver)new Messages.Resolver(){

            public String getMessage(String message, Object ... params) {
                return BundleMessages.getMessage(message, params);
            }
        });
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}

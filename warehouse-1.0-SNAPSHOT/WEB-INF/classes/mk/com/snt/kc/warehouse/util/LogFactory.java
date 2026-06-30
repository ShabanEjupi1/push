/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.inject.Produces
 *  javax.enterprise.inject.spi.InjectionPoint
 */
package mk.com.snt.kc.warehouse.util;

import java.util.logging.Logger;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

public class LogFactory {
    @Produces
    public Logger createLogger(InjectionPoint ip) {
        Class beanClass = ip.getBean().getBeanClass();
        return Logger.getLogger(beanClass.getName());
    }
}

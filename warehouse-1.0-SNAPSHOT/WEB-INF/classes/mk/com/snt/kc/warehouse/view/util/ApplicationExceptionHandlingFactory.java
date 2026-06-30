/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.faces.context.ExceptionHandler
 *  javax.faces.context.ExceptionHandlerFactory
 */
package mk.com.snt.kc.warehouse.view.util;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import mk.com.snt.kc.warehouse.view.util.ApplicationExceptionHandler;

public class ApplicationExceptionHandlingFactory
extends ExceptionHandlerFactory {
    private ExceptionHandlerFactory wrapped;

    public ApplicationExceptionHandlingFactory(ExceptionHandlerFactory wrapped) {
        this.wrapped = wrapped;
    }

    public ExceptionHandler getExceptionHandler() {
        return new ApplicationExceptionHandler(this.wrapped.getExceptionHandler());
    }
}

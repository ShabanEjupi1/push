/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.faces.FacesException
 *  javax.faces.context.ExceptionHandler
 *  javax.faces.event.ExceptionQueuedEvent
 *  org.omnifaces.exceptionhandler.FullAjaxExceptionHandler
 *  org.omnifaces.util.Exceptions
 *  org.omnifaces.util.Messages
 */
package mk.com.snt.kc.warehouse.view.util;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.event.ExceptionQueuedEvent;
import mk.com.snt.kc.warehouse.InvalidDataException;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Messages;

public class ApplicationExceptionHandler
extends FullAjaxExceptionHandler {
    private static final Logger LOG = Logger.getLogger(ApplicationExceptionHandler.class.getName());

    public ApplicationExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    public void handle() throws FacesException {
        this.handleApplicationException();
    }

    private void handleApplicationException() {
        Iterator unhandledExceptionQueuedEvents = this.getUnhandledExceptionQueuedEvents().iterator();
        if (unhandledExceptionQueuedEvents.hasNext()) {
            Throwable exception = ((ExceptionQueuedEvent)unhandledExceptionQueuedEvents.next()).getContext().getException();
            if ((exception = Exceptions.unwrap((Throwable)exception)) instanceof InvalidDataException) {
                InvalidDataException ide = (InvalidDataException)exception;
                Messages.addGlobalError((String)ide.getMessage(), (Object[])ide.getParams());
            } else {
                LOG.log(Level.SEVERE, "", exception);
                super.handle();
            }
        }
    }
}

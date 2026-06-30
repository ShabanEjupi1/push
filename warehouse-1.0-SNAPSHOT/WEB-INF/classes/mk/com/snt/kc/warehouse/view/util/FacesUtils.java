/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.faces.application.FacesMessage
 *  org.primefaces.context.RequestContext
 */
package mk.com.snt.kc.warehouse.view.util;

import javax.faces.application.FacesMessage;
import mk.com.snt.kc.warehouse.view.util.BundleMessages;
import org.primefaces.context.RequestContext;

public class FacesUtils {
    public static void showError(String message, Object ... parms) {
        String messageResolved = BundleMessages.getMessage(message, parms);
        if (messageResolved.equals("")) {
            messageResolved = message;
        }
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleMessages.getMessage("error", new Object[0]), messageResolved);
        FacesUtils.showDialog(msg);
    }

    public static void showDialog(FacesMessage msg) {
        RequestContext.getCurrentInstance().showMessageInDialog(msg);
    }
}

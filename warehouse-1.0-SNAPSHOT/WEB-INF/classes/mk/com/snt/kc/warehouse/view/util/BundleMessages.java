/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.omnifaces.util.Faces
 */
package mk.com.snt.kc.warehouse.view.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import org.omnifaces.util.Faces;

public class BundleMessages {
    private static final String BASE_NAME = "mk.com.snt.kc.warehouse.messages";

    public static String getMessage(String message, Object ... params) {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, Faces.getLocale());
        String value = null;
        if (bundle.containsKey(message)) {
            value = bundle.getString(message);
        }
        return value == null ? "" : MessageFormat.format(value, params);
    }
}

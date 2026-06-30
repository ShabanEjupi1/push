/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.view.util;

import java.util.ResourceBundle;

public class VersionBundleAccessor {
    private static final String VERSION_BUNDLE_NAME = "mk.com.snt.kc.warehouse.version";
    private static final ResourceBundle VERSION = ResourceBundle.getBundle("mk.com.snt.kc.warehouse.version");

    public static final String getTimestamp() {
        return VERSION.getString("date");
    }
}

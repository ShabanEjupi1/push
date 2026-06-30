/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.faces.application.Resource
 *  javax.faces.application.ResourceWrapper
 */
package mk.com.snt.kc.warehouse.view.util;

import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;
import mk.com.snt.kc.warehouse.view.util.VersionBundleAccessor;

public class CustomResource
extends ResourceWrapper {
    private static final String REQUEST_PATH_FORMAT = "%s%st=%s";
    private Resource wrapped;

    public CustomResource(Resource wrapped) {
        this.wrapped = wrapped;
    }

    public Resource getWrapped() {
        return this.wrapped;
    }

    public String getRequestPath() {
        String requestPath = this.wrapped.getRequestPath();
        return String.format(REQUEST_PATH_FORMAT, requestPath, requestPath.contains("?") ? "&" : "?", VersionBundleAccessor.getTimestamp());
    }

    public String getContentType() {
        return this.getWrapped().getContentType();
    }

    public String getLibraryName() {
        return this.getWrapped().getLibraryName();
    }

    public String getResourceName() {
        return this.getWrapped().getResourceName();
    }
}

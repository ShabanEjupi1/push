/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.faces.application.Resource
 *  javax.faces.application.ResourceHandler
 *  javax.faces.application.ResourceHandlerWrapper
 */
package mk.com.snt.kc.warehouse.view.util;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import mk.com.snt.kc.warehouse.view.util.CustomResource;

public class CustomResourceHandler
extends ResourceHandlerWrapper {
    private static final String FILE_NAME = "pf_overrides.css";
    private ResourceHandler wrapped;

    public CustomResourceHandler(ResourceHandler wrapped) {
        this.wrapped = wrapped;
    }

    public ResourceHandler getWrapped() {
        return this.wrapped;
    }

    public Resource createResource(String resourceName, String libraryName) {
        Resource resource = super.createResource(resourceName, libraryName);
        return CustomResourceHandler.isResourceCustomized(resourceName) ? new CustomResource(resource) : resource;
    }

    private static boolean isResourceCustomized(String resourceName) {
        return FILE_NAME.equals(resourceName);
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.faces.component.UIComponent
 *  javax.faces.context.FacesContext
 *  javax.faces.convert.Converter
 *  javax.faces.convert.FacesConverter
 *  javax.inject.Inject
 */
package mk.com.snt.kc.warehouse.view.util;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import mk.com.snt.kc.warehouse.boundary.TraderManager;
import mk.com.snt.kc.warehouse.domain.Trader;
import mk.com.snt.kc.warehouse.util.Utils;

@FacesConverter(value="traderConverter")
public class TraderConverter
implements Converter {
    @Inject
    TraderManager traderManager;

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (Utils.isNullOrEmpty(value)) {
            return null;
        }
        return this.traderManager.getTrader(Long.valueOf(value));
    }

    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value != null) {
            return ((Trader)value).getId().toString();
        }
        return "";
    }
}

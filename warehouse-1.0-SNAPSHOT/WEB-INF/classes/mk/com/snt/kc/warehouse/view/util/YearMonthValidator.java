/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.faces.application.FacesMessage
 *  javax.faces.component.UIComponent
 *  javax.faces.context.FacesContext
 *  javax.faces.validator.FacesValidator
 *  javax.faces.validator.Validator
 *  javax.faces.validator.ValidatorException
 *  org.omnifaces.util.Messages
 */
package mk.com.snt.kc.warehouse.view.util;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import mk.com.snt.kc.warehouse.util.Utils;
import org.omnifaces.util.Messages;

@FacesValidator(value="yearMonthValidator")
public class YearMonthValidator
implements Validator {
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String month;
        if (value != null && !Utils.isNullOrEmpty(month = (String)value)) {
            if (!month.matches("\\d\\d.\\d\\d\\d\\d")) {
                this.error();
            } else {
                try {
                    int iMonth = Integer.parseInt(month.substring(0, 2));
                    if (iMonth < 1 || iMonth > 12) {
                        this.error();
                    }
                }
                catch (NumberFormatException ex) {
                    this.error();
                }
            }
        }
    }

    private void error() {
        FacesMessage msg = Messages.createError((String)"invalid_year_month_value", (Object[])new Object[0]);
        msg.setSeverity(FacesMessage.SEVERITY_ERROR);
        throw new ValidatorException(msg);
    }
}

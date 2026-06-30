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

import java.math.BigDecimal;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import org.omnifaces.util.Messages;

@FacesValidator(value="positiveDecimalValidator")
public class PositiveDecimalValidator
implements Validator {
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value != null) {
            try {
                if (new BigDecimal(value.toString()).signum() < 1) {
                    this.throwError(value);
                }
            }
            catch (NumberFormatException ex) {
                this.throwError(value);
            }
        }
    }

    private void throwError(Object value) throws ValidatorException {
        FacesMessage msg = Messages.createError((String)"invalid_decimal_number", (Object[])new Object[]{value});
        msg.setSeverity(FacesMessage.SEVERITY_ERROR);
        throw new ValidatorException(msg);
    }
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.ApplicationException
 */
package mk.com.snt.kc.warehouse;

import javax.ejb.ApplicationException;

@ApplicationException(rollback=true)
public class InvalidDataException
extends RuntimeException {
    private Object[] params;

    public InvalidDataException() {
    }

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Object ... params) {
        super(message);
        this.params = params;
    }

    public InvalidDataException(Throwable cause) {
        super(cause);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public Object[] getParams() {
        return this.params;
    }
}

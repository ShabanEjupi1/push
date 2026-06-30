/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.persistence;

import java.util.HashMap;
import java.util.Map;

public class QueryParameter {
    private Map<String, Object> parameters = new HashMap<String, Object>();

    private QueryParameter(String name, Object value) {
        this.parameters.put(name, value);
    }

    public static QueryParameter with(String name, Object value) {
        return new QueryParameter(name, value);
    }

    public QueryParameter and(String name, Object value) {
        if (value != null) {
            this.parameters.put(name, value);
        }
        return this;
    }

    public Map<String, Object> parameters() {
        return this.parameters;
    }
}

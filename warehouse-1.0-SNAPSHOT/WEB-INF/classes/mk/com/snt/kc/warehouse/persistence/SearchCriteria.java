/*
 * Decompiled with CFR 0.152.
 */
package mk.com.snt.kc.warehouse.persistence;

import java.util.ArrayList;
import java.util.List;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;
import mk.com.snt.kc.warehouse.util.Utils;

public class SearchCriteria {
    private List<String> whereConditions = new ArrayList<String>();
    private List<Parameter> parameters = new ArrayList<Parameter>();
    private List<Ordering> orderBys = new ArrayList<Ordering>();

    public SearchCriteria addInCriteria(String whereCondition, List<?> values) {
        if (values != null && !values.isEmpty()) {
            String parmName = this.getParameterName();
            this.whereConditions.add(whereCondition + " IN :" + parmName);
            this.parameters.add(new Parameter(parmName, values));
        }
        return this;
    }

    public SearchCriteria addEqualsCriteria(String whereCondition, Object value) {
        if (value != null) {
            if (value instanceof String && Utils.isNullOrEmpty((String)value)) {
                return this;
            }
            String parmName = this.getParameterName();
            this.whereConditions.add(whereCondition + " = :" + parmName);
            this.parameters.add(new Parameter(parmName, value));
        }
        return this;
    }

    public SearchCriteria addLikeCriteria(String whereCondition, String value) {
        if (!Utils.isNullOrEmpty(value)) {
            String parmName = this.getParameterName();
            this.whereConditions.add("LOWER(" + whereCondition + ") LIKE :" + parmName);
            this.parameters.add(new Parameter(parmName, "%" + value.toLowerCase() + "%"));
        }
        return this;
    }

    public SearchCriteria addORLikeCriterias(List<String> ws, String value) {
        if (!Utils.isNullOrEmpty(value) && ws != null && !ws.isEmpty()) {
            String whereCondition = "";
            String parmName = this.getParameterName();
            for (String s : ws) {
                whereCondition = whereCondition + s + " LIKE :" + parmName + " OR ";
            }
            this.parameters.add(new Parameter(parmName, "%" + value + "%"));
            whereCondition = "(" + whereCondition.substring(0, whereCondition.lastIndexOf("OR")) + ")";
            this.whereConditions.add(whereCondition);
        }
        return this;
    }

    public SearchCriteria addLessThanAndEqualsCriteria(String whereCondition, Object value) {
        return this.addCondition(value, whereCondition, "<=");
    }

    public SearchCriteria addGreaterThanAndEqualsCriteria(String whereCondition, Object value) {
        return this.addCondition(value, whereCondition, ">=");
    }

    public SearchCriteria addOrEqualsCriteria(String field1, String field2, Object value) {
        if (value != null) {
            if (value instanceof String && Utils.isNullOrEmpty((String)value)) {
                return this;
            }
            String base = this.getParameterName();
            String parm1 = base + "a";
            String parm2 = base + "b";
            this.whereConditions.add("(" + field1 + " = :" + parm1 + " OR " + field2 + " = :" + parm2 + ")");
            this.parameters.add(new Parameter(parm1, value));
            this.parameters.add(new Parameter(parm2, value));
        }
        return this;
    }

    public SearchCriteria addGreaterThanCriteria(String whereCondition, Object value) {
        return this.addCondition(value, whereCondition, ">");
    }

    public SearchCriteria addLessThanCriteria(String whereCondition, Object value) {
        return this.addCondition(value, whereCondition, "<");
    }

    public SearchCriteria addBetweenCriteria(String whereCondition, Object valueFrom, Object valueTo) {
        if (valueFrom != null || valueTo != null) {
            String parmFrom = this.getParameterName() + "From";
            String parmTo = this.getParameterName() + "To";
            if (valueFrom != null && valueTo != null) {
                this.whereConditions.add(whereCondition + " >= :" + parmFrom + " AND " + whereCondition + " <= :" + parmTo);
                this.parameters.add(new Parameter(parmFrom, valueFrom));
                this.parameters.add(new Parameter(parmTo, valueTo));
            }
            if (valueFrom != null && valueTo == null) {
                this.whereConditions.add(whereCondition + " >= :" + parmFrom);
                this.parameters.add(new Parameter(parmFrom, valueFrom));
            }
            if (valueFrom == null && valueTo != null) {
                this.whereConditions.add(whereCondition + " <= :" + parmTo);
                this.parameters.add(new Parameter(parmTo, valueTo));
            }
        }
        return this;
    }

    public SearchCriteria addOrderByCriteria(String columnName, SearchFilter.SortOrder type) {
        this.orderBys.add(new Ordering(columnName, type));
        return this;
    }

    public SearchCriteria addOrderByCriteria(Ordering orderBy) {
        if (orderBy != null) {
            this.orderBys.add(orderBy);
        }
        return this;
    }

    private String getParameterName() {
        return "parm" + this.whereConditions.size();
    }

    public List<Parameter> getParameters() {
        return this.parameters;
    }

    public String getCriteriaString(boolean startsWithWhere) {
        String criteriaString = "";
        if (!this.whereConditions.isEmpty()) {
            for (String whereCondition : this.whereConditions) {
                criteriaString = " " + criteriaString + " AND " + whereCondition + " ";
            }
        }
        if (startsWithWhere && !this.whereConditions.isEmpty()) {
            criteriaString = " WHERE " + criteriaString.replaceFirst("AND", "");
        }
        return criteriaString;
    }

    public String getOrderByString() {
        String s = " ";
        if (this.orderBys != null && !this.orderBys.isEmpty()) {
            s = " ORDER BY ";
            for (Ordering orderBy : this.orderBys) {
                s = s + orderBy.getColumnName() + " " + orderBy.getType().name() + ", ";
            }
            s = s.substring(0, s.lastIndexOf(", "));
        }
        return s;
    }

    private SearchCriteria addCondition(Object value, String whereCondition, String condition) {
        if (value != null) {
            String parmName = this.getParameterName();
            this.whereConditions.add(whereCondition + " " + condition + " :" + parmName);
            this.parameters.add(new Parameter(parmName, value));
        }
        return this;
    }

    public static class Ordering {
        private String columnName;
        private SearchFilter.SortOrder type;

        public Ordering(String columnName, SearchFilter.SortOrder type) {
            this.columnName = columnName;
            this.type = type;
        }

        public String getColumnName() {
            return this.columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public SearchFilter.SortOrder getType() {
            return this.type;
        }

        public void setType(SearchFilter.SortOrder type) {
            this.type = type;
        }
    }

    public class Parameter {
        private String parameterName;
        private Object parameterValue;

        public Parameter(String parameterName, Object parameterValue) {
            this.parameterName = parameterName;
            this.parameterValue = parameterValue;
        }

        public String getParameterName() {
            return this.parameterName;
        }

        public Object getParameterValue() {
            return this.parameterValue;
        }
    }
}

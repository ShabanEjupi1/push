/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.faces.application.FacesMessage
 *  javax.persistence.Column
 *  javax.persistence.Embedded
 *  javax.persistence.Enumerated
 *  javax.persistence.ManyToOne
 *  javax.persistence.OneToOne
 *  javax.persistence.Temporal
 *  javax.validation.ConstraintViolation
 *  javax.validation.Validation
 *  javax.validation.Validator
 *  javax.validation.ValidatorFactory
 *  org.joda.time.DateTime
 *  org.joda.time.LocalDate
 *  org.joda.time.ReadableInstant
 *  org.omnifaces.util.Messages
 *  org.primefaces.model.SortOrder
 */
package mk.com.snt.kc.warehouse.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import mk.com.snt.kc.warehouse.InvalidDataException;
import mk.com.snt.kc.warehouse.persistence.Displayable;
import mk.com.snt.kc.warehouse.persistence.Persistable;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;
import mk.com.snt.kc.warehouse.view.util.BundleMessages;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;
import org.omnifaces.util.Messages;
import org.primefaces.model.SortOrder;

public class Utils {
    private static final Logger LOG = Logger.getLogger(Utils.class.getName());
    private static final Locale DEFAULT_LOCALE = new Locale("sq", "AL");
    private static final String DECIMAL_FORMAT_PATTERN_PREFIX = "###,##0.";
    private static final int DEFAULT_DECIMAL_SCALE = 2;
    private static final int ROUNDING_SCALE = 0;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static String convert(Date date) {
        return Utils.convert(date, "dd.MM.yyyy");
    }

    public static String convert(Date date, String pattern) {
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(date);
        }
        return "";
    }

    public static String convertToTime(Date date) {
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            return sdf.format(date);
        }
        return "";
    }

    public static String convert(BigDecimal amount) {
        return Utils.convert(amount, 2);
    }

    public static String convertForPrint(BigDecimal amount) {
        if (amount != null && BigDecimal.ZERO.compareTo(amount) == 0) {
            return "";
        }
        return Utils.convert(amount, 2);
    }

    public static String convert(BigDecimal amount, int scale) {
        if (amount == null) {
            return "";
        }
        String DECIMAL_FORMAT_PATTERN = Utils.appendZeros(scale);
        DecimalFormat df = null;
        try {
            df = (DecimalFormat)NumberFormat.getInstance(DEFAULT_LOCALE);
            df.applyPattern(DECIMAL_FORMAT_PATTERN);
        }
        catch (ClassCastException cce) {
            df = new DecimalFormat(DECIMAL_FORMAT_PATTERN);
        }
        StringBuffer sb = df.format((Object)amount, new StringBuffer(), new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR));
        return sb.toString();
    }

    public static BigDecimal convert(String value) {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(DEFAULT_LOCALE);
        value = value.replace(String.valueOf(dfs.getGroupingSeparator()), "");
        value = value.replace(String.valueOf(dfs.getDecimalSeparator()), ".");
        return new BigDecimal(value);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return Utils.ifNull(a).add(Utils.ifNull(b));
    }

    public static BigDecimal ifNull(BigDecimal a) {
        return a == null ? BigDecimal.ZERO : a;
    }

    public static BigDecimal round(BigDecimal a) {
        return a.setScale(0, ROUNDING_MODE);
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isAllDigits(String s) {
        return s.matches("\\d+");
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static Date parse(String date) {
        return Utils.parse(date, "dd.MM.yyyy");
    }

    public static Date parse(String date, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(date);
        }
        catch (ParseException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private static String appendZeros(int scale) {
        StringBuilder sb = new StringBuilder();
        sb.append(DECIMAL_FORMAT_PATTERN_PREFIX);
        for (int i = 0; i < scale; ++i) {
            sb.append("0");
        }
        return sb.toString();
    }

    public static <T> void createMessages(String prefix, Set<ConstraintViolation<T>> constraintViolations) {
        String pattern = "%s %s: %s";
        String fieldPattern = "%s.%s";
        for (ConstraintViolation<T> violation : constraintViolations) {
            String field = String.format(fieldPattern, violation.getRootBeanClass().getName(), violation.getPropertyPath());
            String errorMessage = String.format(pattern, prefix, BundleMessages.getMessage(field, new Object[0]), violation.getMessage());
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMessage, errorMessage);
            Messages.addGlobal((FacesMessage)msg);
        }
    }

    public static <T> void createMessages(Set<ConstraintViolation<T>> constraintViolations) {
        Utils.createMessages("", constraintViolations);
    }

    public static <T> boolean isValid(String prefix, T object) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set errors = validator.validate(object, new Class[0]);
        Utils.createMessages(prefix, errors);
        return errors == null || errors.isEmpty();
    }

    public static <T> boolean isValid(T object) {
        return Utils.isValid("", object);
    }

    public static SearchFilter.SortOrder convert(SortOrder sortOrder) {
        return sortOrder == null ? null : (SortOrder.ASCENDING.equals((Object)sortOrder) ? SearchFilter.SortOrder.ASC : SearchFilter.SortOrder.DESC);
    }

    public static Object getFieldValue(Field field, Object object) {
        try {
            String fieldName = field.getName();
            String firstLetter = fieldName.substring(0, 1);
            String remainingPart = fieldName.substring(1, fieldName.length());
            String methodPrefix = "get";
            if (field.getType().isPrimitive() && field.getType().equals(Boolean.TYPE)) {
                methodPrefix = "is";
            }
            String methodName = methodPrefix + firstLetter.toUpperCase() + remainingPart;
            Method method = object.getClass().getMethod(methodName, new Class[0]);
            return method.invoke(object, new Object[0]);
        }
        catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static Object getFieldValue(String fieldName, Object object) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            return Utils.getFieldValue(field, object);
        }
        catch (NoSuchFieldException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String convertFieldValue(Field field, Object object) {
        Object output = Utils.getFieldValue(field, object);
        return Utils.convertFieldValue(output);
    }

    public static String convertFieldValue(Object fieldValue) {
        if (fieldValue != null) {
            if (fieldValue instanceof Date) {
                return Utils.convert((Date)fieldValue);
            }
            if (fieldValue instanceof Displayable) {
                return ((Displayable)fieldValue).getDisplay();
            }
            if (fieldValue instanceof BigDecimal) {
                return Utils.convert((BigDecimal)fieldValue);
            }
            return fieldValue.toString();
        }
        return "";
    }

    public static boolean isJPAField(Field field) {
        return field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(Embedded.class) || field.isAnnotationPresent(Temporal.class) || field.isAnnotationPresent(Enumerated.class);
    }

    public static boolean isNullOrEmpty(Persistable entity) {
        return entity == null || entity.getId() == null;
    }

    public static void validateCondition(boolean condition, String message, Object ... params) {
        if (condition) {
            Utils.error(message, params);
        }
    }

    public static void validateRequiredField(Field field, Object object, String prefix) {
        boolean error = false;
        Object value = Utils.getFieldValue(field, object);
        Class<?> clazz = field.getType();
        if (value == null) {
            error = true;
        } else if (clazz.equals(String.class)) {
            error = Utils.isNullOrEmpty((String)value);
        } else if (Persistable.class.isAssignableFrom(clazz)) {
            error = Utils.isNullOrEmpty((Persistable)value);
        } else if (clazz.equals(List.class)) {
            error = ((List)value).isEmpty();
        }
        if (error) {
            String message = BundleMessages.getMessage(field.getName(), new Object[0]);
            if (Utils.isNullOrEmpty(message)) {
                message = BundleMessages.getMessage(prefix + field.getName(), new Object[0]);
            }
            Utils.error("required_message", message);
        }
    }

    public static void error(String message, Object ... params) throws InvalidDataException {
        throw new InvalidDataException(message, params);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return Utils.prepare(a).subtract(Utils.prepare(b));
    }

    public static BigDecimal prepare(BigDecimal a) {
        return a == null ? BigDecimal.ZERO : a;
    }

    public static boolean isPositive(BigDecimal a) {
        return Utils.prepare(a).compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isToday(Date date) {
        LocalDate today = new LocalDate();
        return today.equals((Object)new LocalDate(date.getTime()));
    }

    public static Date minusDays(Date date, int days) {
        return new DateTime(date.getTime()).minusDays(days).toDate();
    }

    public static Date firstDay(int month, int year) {
        return new LocalDate().withMonthOfYear(month).withYear(year).dayOfMonth().withMinimumValue().toDate();
    }

    public static Date lastDay(int month, int year) {
        return new LocalDate().withMonthOfYear(month).withYear(year).dayOfMonth().withMaximumValue().toDate();
    }

    public static boolean isBefore(Date first, Date second) {
        return new DateTime((Object)first).isBefore((ReadableInstant)new DateTime((Object)second));
    }
}

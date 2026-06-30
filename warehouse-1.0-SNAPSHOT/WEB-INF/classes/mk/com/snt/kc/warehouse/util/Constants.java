/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.inject.Produces
 *  javax.inject.Named
 */
package mk.com.snt.kc.warehouse.util;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class Constants {
    public static final int DECIMAL_PRECISION = 12;
    public static final int DECIMAL_SCALE = 2;
    public static final int PAGE_SIZE = 10;
    public static final String DATE_PATTERN = "dd.MM.yyyy";
    public static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm";
    public static final String TIME_PATTERN = "HH:mm";
    public static final String TIME_ZONE = "Europe/Skopje";
    public static final String DEFAULT_LOCALE = "en";
    public static final int AUTOCOMPLETE_LIMIT = 20;

    @Produces
    @Named(value="datePattern")
    public String getDatePattern() {
        return DATE_PATTERN;
    }

    @Produces
    @Named(value="dateTimePattern")
    public String getDateTimePattern() {
        return DATE_TIME_PATTERN;
    }

    @Produces
    @Named(value="timezone")
    public String getTimezone() {
        return TIME_ZONE;
    }
}

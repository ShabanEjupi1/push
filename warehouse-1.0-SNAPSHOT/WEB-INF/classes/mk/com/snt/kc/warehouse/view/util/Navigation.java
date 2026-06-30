/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.inject.Model
 *  javax.inject.Inject
 */
package mk.com.snt.kc.warehouse.view.util;

import javax.enterprise.inject.Model;
import javax.inject.Inject;
import mk.com.snt.kc.warehouse.view.DailyExitsBean;

@Model
public class Navigation {
    @Inject
    private DailyExitsBean dailyExitsBean;

    public String home() {
        return this.page("home");
    }

    public String login() {
        return this.page("login");
    }

    public String users() {
        return this.page("admin_users");
    }

    public String daily_exits() {
        this.dailyExitsBean.setStorn(false);
        return this.page("daily_exits");
    }

    public String storn_daily_exits() {
        this.dailyExitsBean.setStorn(true);
        return this.page("daily_exits");
    }

    public String pdfReport() {
        return this.page("pdf_report");
    }

    public String pdfReport1() {
        return this.page("pdf_report1");
    }

    public String excelReport() {
        return this.page("excel_report");
    }

    public String auditLog() {
        return this.page("admin_audit_log");
    }

    public String im4_list() {
        return this.page("im4_list");
    }

    public String relink() {
        return this.page("admin_relink");
    }

    public String error404() {
        return this.page("404");
    }

    private String page(String mapping) {
        return "pretty:" + mapping;
    }
}

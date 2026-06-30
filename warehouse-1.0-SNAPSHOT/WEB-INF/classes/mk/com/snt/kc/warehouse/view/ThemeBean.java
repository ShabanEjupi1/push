/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.inject.Named
 */
package mk.com.snt.kc.warehouse.view;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@SessionScoped
@Named
public class ThemeBean
implements Serializable {
    private Map<String, String> themes;
    private String theme = "sam";

    @PostConstruct
    public void init() {
        this.themes = new TreeMap<String, String>();
        this.themes.put("Afterwork", "afterwork");
        this.themes.put("Aristo", "aristo");
        this.themes.put("Bootstrap", "bootstrap");
        this.themes.put("Delta", "delta");
        this.themes.put("Overcast", "overcast");
        this.themes.put("Sam", "sam");
        this.themes.put("Smoothness", "smoothness");
    }

    public Map<String, String> getThemes() {
        return this.themes;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getTheme() {
        return this.theme;
    }
}

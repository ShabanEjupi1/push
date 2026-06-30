/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.enterprise.context.SessionScoped
 *  javax.inject.Inject
 *  javax.inject.Named
 *  org.apache.commons.lang3.RandomStringUtils
 *  org.omnifaces.util.Faces
 *  org.omnifaces.util.Messages
 *  org.primefaces.model.LazyDataModel
 *  org.primefaces.model.SortOrder
 */
package mk.com.snt.kc.warehouse.view;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import mk.com.snt.kc.warehouse.InvalidDataException;
import mk.com.snt.kc.warehouse.boundary.ReportManager;
import mk.com.snt.kc.warehouse.boundary.TraderManager;
import mk.com.snt.kc.warehouse.boundary.UserFilter;
import mk.com.snt.kc.warehouse.boundary.UserManager;
import mk.com.snt.kc.warehouse.domain.Trader;
import mk.com.snt.kc.warehouse.domain.User;
import mk.com.snt.kc.warehouse.util.Utils;
import mk.com.snt.kc.warehouse.view.util.FacesUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

@SessionScoped
@Named
public class UsersBean
implements Serializable {
    @Inject
    UserManager userManager;
    @Inject
    ReportManager reportManager;
    @Inject
    TraderManager traderManager;
    private LazyDataModel<User> data;
    private User user;
    private String confirmPassword;
    private UserFilter userFilter;
    private Trader trader;

    public void init() {
        if (Faces.isAjaxRequest()) {
            return;
        }
        this.initUser();
        this.setupData();
    }

    private void initUser() {
        this.user = new User();
        this.user.setActive(true);
        this.confirmPassword = null;
        this.trader = new Trader();
    }

    private void setupData() {
        this.userFilter = new UserFilter();
        this.data = new LazyDataModel<User>(){

            public List<User> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                UsersBean.this.userFilter.setFirst(first);
                UsersBean.this.userFilter.setSortField(sortField);
                UsersBean.this.userFilter.setSortOrder(Utils.convert(sortOrder));
                UsersBean.this.setupFilters(filters);
                UsersBean.this.data.setRowCount(UsersBean.this.reportManager.count(UsersBean.this.userFilter));
                return UsersBean.this.reportManager.getData(UsersBean.this.userFilter);
            }
        };
        this.data.setPageSize(10);
    }

    private void setupFilters(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            this.userFilter.setUsername(null);
            this.userFilter.setName(null);
            this.userFilter.setTrader(null);
            this.userFilter.setLang(null);
        } else {
            Iterator<String> i$ = filters.keySet().iterator();
            while (i$.hasNext()) {
                String filterKey;
                switch (filterKey = i$.next()) {
                    case "username": {
                        this.userFilter.setUsername(filters.get(filterKey));
                        break;
                    }
                    case "traderFiscalNumber": {
                        this.userFilter.setTrader(filters.get(filterKey));
                        break;
                    }
                    case "name": {
                        this.userFilter.setName(filters.get(filterKey));
                        break;
                    }
                    case "lang": {
                        this.userFilter.setLang(filters.get(filterKey));
                    }
                }
            }
        }
    }

    public void onChangeTrader() {
        if (Utils.isNullOrEmpty(this.user.getTraderFiscalNumber())) {
            FacesUtils.showError("User.required.trader", new Object[0]);
        } else {
            this.isTraderValid();
        }
    }

    protected boolean isTraderValid() {
        this.trader = this.traderManager.getValidTrader(this.user.getTraderFiscalNumber(), new Date());
        if (this.trader == null) {
            FacesUtils.showError("User.invalid.traderFiscalNumber", this.user.getTraderFiscalNumber());
            return false;
        }
        this.user.setTraderId(this.trader.getId());
        return true;
    }

    public void save() {
        boolean valid;
        boolean bl = valid = this.user.isAdministrator() && this.isPasswordValid() || !this.user.isAdministrator() && this.isTraderValid();
        if (valid) {
            if (this.user.getId() != null && Utils.isNullOrEmpty(this.user.getPassword())) {
                this.user.setPassword(this.userManager.getUser(this.user.getUsername()).getPassword());
            }
            try {
                this.userManager.save(this.user);
                Messages.addGlobalInfo((String)"UsersBean.save.success", (Object[])new Object[]{this.user.getUsername()});
                this.initUser();
            }
            catch (InvalidDataException e) {
                this.user.setPassword(null);
                FacesUtils.showError(e.getMessage(), e.getParams());
            }
        }
    }

    public void cancel() {
        this.initUser();
    }

    public void onChangeAdmin() {
        if (this.user.isAdministrator()) {
            this.user.setTraderFiscalNumber(null);
            this.user.setTraderId(null);
            this.trader = new Trader();
        }
    }

    private boolean isPasswordValid() {
        if (!this.user.getPassword().equals(this.confirmPassword)) {
            FacesUtils.showError("UsersBean.save.passwordMismatch", new Object[0]);
            return false;
        }
        return true;
    }

    public void edit(User selected) {
        this.user = selected;
        this.user.setPassword(null);
        this.confirmPassword = null;
        if (!this.user.isAdministrator()) {
            this.trader = this.traderManager.getTrader(this.user.getTraderId());
        }
    }

    public void deactivate(User user) {
        this.userManager.deactivate(user.getUsername());
        Messages.addGlobalInfo((String)"UsersBean.deactivate.success", (Object[])new Object[]{user.getUsername()});
        this.cancel();
    }

    public void activate(User user) {
        this.userManager.activate(user.getUsername());
        Messages.addGlobalInfo((String)"UsersBean.activate.success", (Object[])new Object[]{user.getUsername()});
        this.cancel();
    }

    public void generatePassword() {
        this.user.setPassword(RandomStringUtils.randomAlphanumeric((int)10));
    }

    public boolean isEdit() {
        return this.user.getId() != null;
    }

    public LazyDataModel<User> getData() {
        return this.data;
    }

    public User getUser() {
        return this.user;
    }

    public String getConfirmPassword() {
        return this.confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public UserFilter getUserFilter() {
        return this.userFilter;
    }

    public Trader getTrader() {
        return this.trader;
    }
}

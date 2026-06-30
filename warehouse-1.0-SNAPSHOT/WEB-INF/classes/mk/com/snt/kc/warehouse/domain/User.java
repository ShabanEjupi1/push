/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.NamedQueries
 *  javax.persistence.NamedQuery
 *  javax.persistence.SequenceGenerator
 *  javax.persistence.Table
 */
package mk.com.snt.kc.warehouse.domain;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import mk.com.snt.kc.warehouse.persistence.Persistable;
import mk.com.snt.kc.warehouse.util.Utils;

@Entity
@Table(name="WHS_ADM_USERS", schema="KCUSTOMS")
@NamedQueries(value={@NamedQuery(name="User.byUsername", query="SELECT u FROM User u WHERE u.username = :username"), @NamedQuery(name="User.byTrader", query="SELECT u FROM User u WHERE u.traderFiscalNumber = :traderFiscalNumber")})
public class User
implements Serializable,
Persistable {
    private static final long serialVersionUID = 1L;
    public static final String BY_USERNAME = "User.byUsername";
    public static final String BY_TRADER = "User.byTrader";
    private static final String SEQ_NAME = "WHS_ADM_USERS_SEQ";
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="WHS_ADM_USERS_SEQ")
    @SequenceGenerator(name="WHS_ADM_USERS_SEQ", sequenceName="WHS_ADM_USERS_SEQ", allocationSize=1, initialValue=1, schema="KCUSTOMS")
    private Long id;
    @Column(name="USERNAME", nullable=false, unique=true)
    private String username;
    @Column(name="PASSWORD", nullable=false)
    private String password;
    @Column(name="NAME", nullable=false)
    private String name;
    @Column(name="LANG", nullable=false)
    private String lang;
    @Column(name="TRADER")
    private String traderFiscalNumber;
    @Column(name="TRADER_ID")
    private Long traderId;
    @Column(name="ADMINISTRATOR", nullable=false)
    private boolean administrator;
    @Column(name="ACTIVE", nullable=false)
    private boolean active;

    public User() {
    }

    public User(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.administrator = true;
        this.lang = "en";
        this.active = true;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTraderFiscalNumber() {
        return this.traderFiscalNumber;
    }

    public void setTraderFiscalNumber(String traderFiscalNumber) {
        this.traderFiscalNumber = traderFiscalNumber;
    }

    public Long getTraderId() {
        return this.traderId;
    }

    public void setTraderId(Long traderId) {
        this.traderId = traderId;
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean isAdministrator() {
        return this.administrator;
    }

    public void setAdministrator(boolean administrator) {
        this.administrator = administrator;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void validate() {
        Utils.validateCondition(Utils.isNullOrEmpty(this.getUsername()), "User.required.username", new Object[0]);
        Utils.validateCondition(Utils.isNullOrEmpty(this.getName()), "User.required.name", new Object[0]);
        Utils.validateCondition(Utils.isNullOrEmpty(this.getLang()), "User.required.lang", new Object[0]);
        Utils.validateCondition(!this.isAdministrator() && Utils.isNullOrEmpty(this.getTraderFiscalNumber()), "User.required.trader", new Object[0]);
        Utils.validateCondition(this.isAdministrator() && !Utils.isNullOrEmpty(this.getTraderFiscalNumber()), "User.invalid.trader", new Object[0]);
        Utils.validateCondition(Utils.isNullOrEmpty(this.getPassword()), "User.required.password", new Object[0]);
    }

    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.id);
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        User other = (User)obj;
        return Objects.equals(this.id, other.id);
    }

    public String getDisplay() {
        return this.getUsername();
    }

    public String toString() {
        return "User{id=" + this.id + ", username=" + this.username + ", name=" + this.name + ", traderFiscalNumber=" + this.traderFiscalNumber + ", administrator=" + this.administrator + '}';
    }
}

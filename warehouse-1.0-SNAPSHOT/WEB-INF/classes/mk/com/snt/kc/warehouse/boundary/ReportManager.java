/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.Stateless
 *  javax.inject.Inject
 */
package mk.com.snt.kc.warehouse.boundary;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import mk.com.snt.kc.warehouse.persistence.CrudService;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;

@Stateless
public class ReportManager {
    @Inject
    CrudService crudService;

    public List getData(SearchFilter filter) {
        return this.crudService.find(filter);
    }

    public int count(SearchFilter filter) {
        return this.crudService.count(filter);
    }
}

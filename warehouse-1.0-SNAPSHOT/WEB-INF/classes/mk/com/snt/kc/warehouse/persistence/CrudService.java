/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.ejb.Stateless
 *  javax.ejb.TransactionAttribute
 *  javax.ejb.TransactionAttributeType
 *  javax.persistence.EntityManager
 *  javax.persistence.PersistenceContext
 *  javax.persistence.Query
 */
package mk.com.snt.kc.warehouse.persistence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import mk.com.snt.kc.warehouse.persistence.Persistable;
import mk.com.snt.kc.warehouse.persistence.SearchCriteria;
import mk.com.snt.kc.warehouse.persistence.SearchFilter;

@Stateless
@TransactionAttribute(value=TransactionAttributeType.MANDATORY)
public class CrudService {
    @PersistenceContext(unitName="warehouse_PU")
    private EntityManager em;

    public <T extends Persistable> T save(T entity) {
        if (entity.getId() == null) {
            this.em.persist(entity);
        } else {
            entity = (T)this.em.merge(entity);
        }
        this.em.flush();
        this.em.refresh(entity);
        return entity;
    }

    public Object create(Object entity) {
        this.em.persist(entity);
        this.em.flush();
        this.em.refresh(entity);
        return entity;
    }

    public Object update(Object entity) {
        entity = this.em.merge(entity);
        this.em.flush();
        this.em.refresh(entity);
        return entity;
    }

    public void detach(Object entity) {
        this.em.detach(entity);
    }

    public List findWithNamedQuery(String namedQueryName) {
        return this.findWithNamedQuery(namedQueryName, null, 0, 0);
    }

    public <T> T findSingleWithNamedQuery(String namedQueryName, Class<T> clazz) {
        List results = this.findWithNamedQuery(namedQueryName);
        return (T)this.getSingle(results);
    }

    public List findWithNamedQuery(String namedQueryName, Map<String, Object> parameters) {
        return this.findWithNamedQuery(namedQueryName, parameters, 0, 0);
    }

    public <T> T findSingleWithNamedQuery(String namedQueryName, Map<String, Object> parameters, Class<T> clazz) {
        return this.findSingleWithNamedQuery(namedQueryName, parameters, clazz, false);
    }

    public <T> T findSingleWithNamedQuery(String namedQueryName, Map<String, Object> parameters, Class<T> clazz, boolean refresh) {
        List results = this.findWithNamedQuery(namedQueryName, parameters, 0, 0, refresh);
        return (T)this.getSingle(results);
    }

    public List findWithNamedQuery(String namedQueryName, Map<String, Object> parameters, int first, int resultLimit) {
        return this.findWithNamedQuery(namedQueryName, parameters, first, resultLimit, false);
    }

    public List findWithNamedQuery(String namedQueryName, Map<String, Object> parameters, int first, int resultLimit, boolean refresh) {
        Query query = this.em.createNamedQuery(namedQueryName);
        query.setFirstResult(first);
        if (resultLimit > 0) {
            query.setMaxResults(resultLimit);
        }
        this.setParameters(parameters, query);
        query.setHint("eclipselink.refresh", (Object)"True");
        return query.getResultList();
    }

    private <T> T getSingle(List<T> results) {
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public <T> T find(Class<T> aClass, Object id) {
        Object result = this.em.find(aClass, id);
        if (result != null) {
            this.em.refresh(result);
        }
        return (T)result;
    }

    public List findWithQuery(String sql) {
        Query query = this.em.createQuery(sql);
        return query.getResultList();
    }

    public List findWithQuery(String sql, Map<String, Object> parameters) {
        Query query = this.em.createQuery(sql);
        this.setParameters(parameters, query);
        return query.getResultList();
    }

    public List findWithNativeQuery(String sql, Map<String, Object> parameters) {
        Query query = this.em.createNativeQuery(sql);
        this.setParameters(parameters, query);
        return query.getResultList();
    }

    public List findWithNativeQuery(String sql, Map<String, Object> parameters, int first, int resultLimit) {
        Query query = this.em.createNativeQuery(sql);
        query.setFirstResult(first);
        query.setMaxResults(resultLimit);
        this.setParameters(parameters, query);
        return query.getResultList();
    }

    public List findWithQuery(String sql, Map<String, Object> parameters, int first, int resultLimit) {
        Query query = this.em.createQuery(sql);
        query.setFirstResult(first);
        if (resultLimit > 0) {
            query.setMaxResults(resultLimit);
        }
        this.setParameters(parameters, query);
        query.setHint("eclipselink.refresh", (Object)"True");
        return query.getResultList();
    }

    public <T> T findSingleWithQuery(String sql, Map<String, Object> parameters, Class<T> clazz) {
        List results = this.findWithQuery(sql, parameters);
        return (T)this.getSingle(results);
    }

    public List findWithQuery(String sql, SearchCriteria searchCriteria) {
        Query query = this.em.createQuery(sql);
        if (searchCriteria.getParameters() != null && !searchCriteria.getParameters().isEmpty()) {
            for (SearchCriteria.Parameter parm : searchCriteria.getParameters()) {
                query.setParameter(parm.getParameterName(), parm.getParameterValue());
            }
        }
        return query.getResultList();
    }

    public void delete(Object t) {
        Object t1 = this.em.merge(t);
        this.em.remove(t1);
        this.em.flush();
    }

    public List find(SearchFilter searchFilter) {
        Query query = this.em.createQuery(searchFilter.getSql());
        SearchCriteria searchCriteria = searchFilter.createSearchCriteria();
        if (searchFilter.getPageSize() > 0) {
            query.setMaxResults(searchFilter.getPageSize());
        }
        query.setFirstResult(searchFilter.getFirst());
        if (searchCriteria.getParameters() != null && !searchCriteria.getParameters().isEmpty()) {
            for (SearchCriteria.Parameter parm : searchCriteria.getParameters()) {
                query.setParameter(parm.getParameterName(), parm.getParameterValue());
            }
        }
        query.setHint("eclipselink.refresh", (Object)"True");
        return query.getResultList();
    }

    public int count(SearchFilter searchFilter) {
        Query query = this.em.createQuery(searchFilter.getCountSql());
        SearchCriteria searchCriteria = searchFilter.createSearchCriteria();
        if (searchCriteria.getParameters() != null && !searchCriteria.getParameters().isEmpty()) {
            for (SearchCriteria.Parameter parm : searchCriteria.getParameters()) {
                query.setParameter(parm.getParameterName(), parm.getParameterValue());
            }
        }
        query.setHint("eclipselink.refresh", (Object)"True");
        List results = query.getResultList();
        return results == null || results.isEmpty() ? 0 : ((Long)results.get(0)).intValue();
    }

    private void setParameters(Map<String, Object> parameters, Query query) {
        if (parameters != null) {
            Set<Map.Entry<String, Object>> rawParameters = parameters.entrySet();
            for (Map.Entry<String, Object> entry : rawParameters) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
        }
    }
}

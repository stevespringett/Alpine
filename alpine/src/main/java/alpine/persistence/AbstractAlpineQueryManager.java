/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.persistence;

import alpine.resources.AlpineRequest;
import alpine.resources.OrderDirection;
import alpine.resources.Pagination;
import alpine.validation.RegexSequence;
import org.datanucleus.api.jdo.JDOQuery;
import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Base persistence manager that implements AutoCloseable so that the PersistenceManager will
 * be automatically closed when used in a try-with-resource block.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public abstract class AbstractAlpineQueryManager implements AutoCloseable {

    protected final Principal principal;
    protected Pagination pagination;
    protected final String filter;
    protected final String orderBy;
    protected final OrderDirection orderDirection;

    protected final PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager();

    /**
     * Default constructor
     */
    public AbstractAlpineQueryManager() {
        principal = null;
        pagination = new Pagination(Pagination.Strategy.NONE, 0, 0);
        filter = null;
        orderBy = null;
        orderDirection = OrderDirection.UNSPECIFIED;
    }

    /**
     * Constructs a new QueryManager with the following:
     * @param principal a Principal, or null
     * @param pagination a Pagination request, or null
     * @param filter a String filter, or null
     * @param orderBy the field to order by
     * @param orderDirection the sorting direction
     * @since 1.0.0
     */
    public AbstractAlpineQueryManager(final Principal principal, final Pagination pagination, final String filter,
                                      final String orderBy, final OrderDirection orderDirection) {
        this.principal = principal;
        this.pagination = pagination;
        this.filter = filter;
        this.orderBy = orderBy;
        this.orderDirection = orderDirection;
    }

    /**
     * Constructs a new QueryManager. Deconstructs the specified AlpineRequest
     * into its individual components including pagination and ordering.
     * @param request an AlpineRequest object
     * @since 1.0.0
     */
    public AbstractAlpineQueryManager(final AlpineRequest request) {
        this.principal = request.getPrincipal();
        this.pagination = request.getPagination();
        this.filter = request.getFilter();
        this.orderBy = request.getOrderBy();
        this.orderDirection = request.getOrderDirection();
    }

    /**
     * Wrapper around {@link Query#execute()} that adds transparent support for
     * pagination and ordering of results via {@link #decorate(Query)}.
     * @param query the JDO Query object to execute
     * @return a PaginatedResult object
     * @since 1.0.0
     */
    public PaginatedResult execute(final Query query) {
        final long count = getCount(query);
        decorate(query);
        return new PaginatedResult()
                .objects(query.execute())
                .total(count);
    }

    /**
     * Wrapper around {@link Query#execute(Object)} that adds transparent support for
     * pagination and ordering of results via {@link #decorate(Query)}.
     * @param query the JDO Query object to execute
     * @param p1 the value of the first parameter declared.
     * @return a PaginatedResult object
     * @since 1.0.0
     */
    public PaginatedResult execute(final Query query, final Object p1) {
        final long count = getCount(query, p1);
        decorate(query);
        return new PaginatedResult()
                .objects(query.execute(p1))
                .total(count);
    }

    /**
     * Wrapper around {@link Query#execute(Object, Object)} that adds transparent support for
     * pagination and ordering of results via {@link #decorate(Query)}.
     * @param query the JDO Query object to execute
     * @param p1 the value of the first parameter declared.
     * @param p2 the value of the second parameter declared.
     * @return a PaginatedResult object
     * @since 1.0.0
     */
    public PaginatedResult execute(final Query query, final Object p1, final Object p2) {
        final long count = getCount(query, p1, p2);
        decorate(query);
        return new PaginatedResult()
                .objects(query.execute(p1, p2))
                .total(count);
    }

    /**
     * Wrapper around {@link Query#execute(Object, Object, Object)} that adds transparent support for
     * pagination and ordering of results via {@link #decorate(Query)}.
     * @param query the JDO Query object to execute
     * @param p1 the value of the first parameter declared.
     * @param p2 the value of the second parameter declared.
     * @param p3 the value of the third parameter declared.
     * @return a PaginatedResult object
     * @since 1.0.0
     */
    public PaginatedResult execute(final Query query, final Object p1, final Object p2, final Object p3) {
        final long count = getCount(query, p1, p2, p3);
        decorate(query);
        return new PaginatedResult()
                .objects(query.execute(p1, p2, p3))
                .total(count);
    }

    /**
     * Wrapper around {@link Query#executeWithArray(Object...)} that adds transparent support for
     * pagination and ordering of results via {@link #decorate(Query)}.
     * @param query the JDO Query object to execute
     * @param parameters the <code>Object</code> array with all of the parameters
     * @return a PaginatedResult object
     * @since 1.0.0
     */
    public PaginatedResult execute(final Query query, final Object... parameters) {
        final long count = getCount(query, parameters);
        decorate(query);
        return new PaginatedResult()
                .objects(query.executeWithArray(parameters))
                .total(count);
    }

    /**
     * Wrapper around {@link Query#executeWithMap(Map)} that adds transparent support for
     * pagination and ordering of results via {@link #decorate(Query)}.
     * @param query the JDO Query object to execute
     * @param parameters the <code>Map</code> containing all of the parameters.
     * @return a PaginatedResult object
     * @since 1.0.0
     */
    public PaginatedResult execute(final Query query, final Map parameters) {
        final long count = getCount(query, parameters);
        decorate(query);
        return new PaginatedResult()
                .objects(query.executeWithMap(parameters))
                .total(count);
    }

    /**
     * Advances the pagination based on the previous pagination settings. This is purely a
     * convenience method as the method by itself is not aware of the query being executed,
     * the result count, etc.
     * @since 1.0.0
     */
    public void advancePagination() {
        if (pagination.isPaginated()) {
            pagination = new Pagination(pagination.getStrategy(), pagination.getOffset() + pagination.getLimit(), pagination.getLimit());
        }
    }

    /**
     * Given a query, this method will decorate that query with pagination, ordering,
     * and sorting direction. Specific checks are performed to ensure the execution
     * of the query is capable of being paged and that ordering can be securely performed.
     * @param query the JDO Query object to execute
     * @return a Collection of objects
     * @since 1.0.0
     */
    public Query decorate(final Query query) {
        // Clear the result to fetch if previously specified (i.e. by getting count)
        query.setResult(null);
        if (pagination != null && pagination.isPaginated()) {
            final long begin = pagination.getOffset();
            final long end = begin + pagination.getLimit();
            query.setRange(begin, end);
        }
        if (orderBy != null && RegexSequence.Pattern.ALPHA_NUMERIC.matcher(orderBy).matches() && orderDirection != OrderDirection.UNSPECIFIED) {
            // Check to see if the specified orderBy field is defined in the class being queried.
            boolean found = false;
            final org.datanucleus.store.query.Query iq = ((JDOQuery) query).getInternalQuery();
            for (Field field: iq.getCandidateClass().getDeclaredFields()) {
                if (orderBy.equals(field.getName())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                query.setOrdering(orderBy + " " + orderDirection.name().toLowerCase());
            }
        }
        return query;
    }

    /**
     * Returns the number of items that would have resulted from returning all object.
     * This method is performant in that the objects are not actually retrieved, only
     * the count.
     * @param query the query to return a count from
     * @return the number of items
     * @since 1.0.0
     */
    public long getCount(final Query query) {
        //query.addExtension("datanucleus.query.resultSizeMethod", "count");
        final String ordering = ((org.datanucleus.api.jdo.JDOQuery) query).getInternalQuery().getOrdering();
        query.setResult("count(id)");
        query.setOrdering(null);
        final long count = (Long) query.execute();
        query.setOrdering(ordering);
        return count;
    }

    /**
     * Returns the number of items that would have resulted from returning all object.
     * This method is performant in that the objects are not actually retrieved, only
     * the count.
     * @param query the query to return a count from
     * @param p1 the value of the first parameter declared.
     * @return the number of items
     * @since 1.0.0
     */
    public long getCount(final Query query, final Object p1) {
        final String ordering = ((org.datanucleus.api.jdo.JDOQuery) query).getInternalQuery().getOrdering();
        query.setResult("count(id)");
        query.setOrdering(null);
        final long count = (Long) query.execute(p1);
        query.setOrdering(ordering);
        return count;
    }

    /**
     * Returns the number of items that would have resulted from returning all object.
     * This method is performant in that the objects are not actually retrieved, only
     * the count.
     * @param query the query to return a count from
     * @param p1 the value of the first parameter declared.
     * @param p2 the value of the second parameter declared.
     * @return the number of items
     * @since 1.0.0
     */
    public long getCount(final Query query, final Object p1, final Object p2) {
        final String ordering = ((org.datanucleus.api.jdo.JDOQuery) query).getInternalQuery().getOrdering();
        query.setResult("count(id)");
        query.setOrdering(null);
        final long count = (Long) query.execute(p1, p2);
        query.setOrdering(ordering);
        return count;
    }

    /**
     * Returns the number of items that would have resulted from returning all object.
     * This method is performant in that the objects are not actually retrieved, only
     * the count.
     * @param query the query to return a count from
     * @param p1 the value of the first parameter declared.
     * @param p2 the value of the second parameter declared.
     * @param p3 the value of the third parameter declared.
     * @return the number of items
     * @since 1.0.0
     */
    public long getCount(final Query query, final Object p1, final Object p2, final Object p3) {
        final String ordering = ((org.datanucleus.api.jdo.JDOQuery) query).getInternalQuery().getOrdering();
        query.setResult("count(id)");
        query.setOrdering(null);
        final long count = (Long) query.execute(p1, p2, p3);
        query.setOrdering(ordering);
        return count;
    }

    /**
     * Returns the number of items that would have resulted from returning all object.
     * This method is performant in that the objects are not actually retrieved, only
     * the count.
     * @param query the query to return a count from
     * @param parameters the <code>Object</code> array with all of the parameters
     * @return the number of items
     * @since 1.0.0
     */
    public long getCount(final Query query, final Object... parameters) {
        final String ordering = ((org.datanucleus.api.jdo.JDOQuery) query).getInternalQuery().getOrdering();
        query.setResult("count(id)");
        query.setOrdering(null);
        final long count = (Long) query.executeWithArray(parameters);
        query.setOrdering(ordering);
        return count;
    }

    /**
     * Returns the number of items that would have resulted from returning all object.
     * This method is performant in that the objects are not actually retrieved, only
     * the count.
     * @param query the query to return a count from
     * @param parameters the <code>Map</code> containing all of the parameters.
     * @return the number of items
     * @since 1.0.0
     */
    public long getCount(final Query query, final Map parameters) {
        final String ordering = ((org.datanucleus.api.jdo.JDOQuery) query).getInternalQuery().getOrdering();
        query.setResult("count(id)");
        query.setOrdering(null);
        final long count = (Long) query.executeWithMap(parameters);
        query.setOrdering(ordering);
        return count;
    }

    /**
     * Returns the number of items that would have resulted from returning all object.
     * This method is performant in that the objects are not actually retrieved, only
     * the count.
     * @param cls the persistence-capable class to query
     * @return the number of items
     * @param <T> candidate type for the query
     * @since 1.0.0
     */
    public <T> long getCount(final Class<T> cls) {
        final Query query = pm.newQuery(cls);
        //query.addExtension("datanucleus.query.resultSizeMethod", "count");
        query.setResult("count(id)");
        return (Long) query.execute();
    }

    /**
     * Persists the specified PersistenceCapable object.
     * @param object a PersistenceCapable object
     * @param <T> the type to return
     * @return the persisted object
     */
    @SuppressWarnings("unchecked")
    public <T> T persist(T object) {
        pm.currentTransaction().begin();
        pm.makePersistent(object);
        pm.currentTransaction().commit();
        pm.getFetchPlan().setDetachmentOptions(FetchPlan.DETACH_LOAD_FIELDS);
        pm.refresh(object);
        return object;
    }

    /**
     * Persists the specified PersistenceCapable objects.
     * @param pcs an array of PersistenceCapable objects
     * @param <T> the type to return
     * @return the persisted objects
     */
    @SuppressWarnings("unchecked")
    public <T> T[] persist(T... pcs) {
        pm.currentTransaction().begin();
        pm.makePersistentAll(pcs);
        pm.currentTransaction().commit();
        pm.getFetchPlan().setDetachmentOptions(FetchPlan.DETACH_LOAD_FIELDS);
        pm.refreshAll(pcs);
        return pcs;
    }

    /**
     * Persists the specified PersistenceCapable objects.
     * @param pcs a collection of PersistenceCapable objects
     * @param <T> the type to return
     * @return the persisted objects
     */
    @SuppressWarnings("unchecked")
    public <T> Collection<T> persist(Collection pcs) {
        pm.currentTransaction().begin();
        pm.makePersistentAll(pcs);
        pm.currentTransaction().commit();
        pm.getFetchPlan().setDetachmentOptions(FetchPlan.DETACH_LOAD_FIELDS);
        pm.refreshAll(pcs);
        return pcs;
    }

    /**
     * Deletes one or more PersistenceCapable objects.
     * @param objects an array of one or more objects to delete
     * @since 1.0.0
     */
    public void delete(Object... objects) {
        pm.currentTransaction().begin();
        pm.deletePersistentAll(objects);
        pm.currentTransaction().commit();
    }

    /**
     * Deletes one or more PersistenceCapable objects.
     * @param collection a collection of one or more objects to delete
     * @since 1.0.0
     */
    public void delete(Collection collection) {
        pm.currentTransaction().begin();
        pm.deletePersistentAll(collection);
        pm.currentTransaction().commit();
    }

    /**
     * Refreshes and detaches an object by its ID.
     * @param <T> A type parameter. This type will be returned
     * @param clazz the persistence class to retrive the ID for
     * @param id the object id to retrieve
     * @return an object of the specified type
     * @since 1.3.0
     */
    public <T> T detach(Class<T> clazz, Object id) {
        pm.getFetchPlan().setDetachmentOptions(FetchPlan.DETACH_LOAD_FIELDS);
        return pm.detachCopy(pm.getObjectById(clazz, id));
    }

    /**
     * Refreshes and detaches an objects.
     * @param pcs the instances to detach
     * @return the detached instances
     * @since 1.3.0
     */
    public <T> List<T> detach(List<T> pcs) {
        pm.getFetchPlan().setDetachmentOptions(FetchPlan.DETACH_LOAD_FIELDS);
        return new ArrayList<>(pm.detachCopyAll(pcs));
    }

    /**
     * Refreshes and detaches an objects.
     * @param pcs the instances to detach
     * @return the detached instances
     * @since 1.3.0
     */
    public <T> Set<T> detach(Set<T> pcs) {
        pm.getFetchPlan().setDetachmentOptions(FetchPlan.DETACH_LOAD_FIELDS);
        return new LinkedHashSet<>(pm.detachCopyAll(pcs));
    }

    /**
     * Retrieves an object by its ID.
     * @param <T> A type parameter. This type will be returned
     * @param clazz the persistence class to retrive the ID for
     * @param id the object id to retrieve
     * @return an object of the specified type
     * @since 1.0.0
     */
    public <T> T getObjectById(Class<T> clazz, Object id) {
        return pm.getObjectById(clazz, id);
    }

    /**
     * Retrieves an object by its UUID.
     * @param <T> A type parameter. This type will be returned
     * @param clazz the persistence class to retrive the ID for
     * @param uuid the uuid of the object to retrieve
     * @return an object of the specified type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public <T> T getObjectByUuid(Class<T> clazz, UUID uuid) {
        final Query query = pm.newQuery(clazz, "uuid == :uuid");
        final List<T> result = (List<T>) query.execute(uuid);
        return result.size() == 0 ? null : result.get(0);
    }

    /**
     * Retrieves an object by its UUID.
     * @param <T> A type parameter. This type will be returned
     * @param clazz the persistence class to retrive the ID for
     * @param uuid the uuid of the object to retrieve
     * @return an object of the specified type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public <T> T getObjectByUuid(Class<T> clazz, String uuid) {
        return getObjectByUuid(clazz, UUID.fromString(uuid));
    }

    /**
     * Retrieves an object by its UUID.
     * @param <T> A type parameter. This type will be returned
     * @param clazz the persistence class to retrive the ID for
     * @param uuid the uuid of the object to retrieve
     * @param fetchGroup the JDO fetchgroup to use when making the query
     * @return an object of the specified type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public <T> T getObjectByUuid(Class<T> clazz, UUID uuid, String fetchGroup) {
        pm.getFetchPlan().addGroup(fetchGroup);
        return getObjectByUuid(clazz, uuid);
    }

    /**
     * Retrieves an object by its UUID.
     * @param <T> A type parameter. This type will be returned
     * @param clazz the persistence class to retrive the ID for
     * @param uuid the uuid of the object to retrieve
     * @param fetchGroup the JDO fetchgroup to use when making the query
     * @return an object of the specified type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public <T> T getObjectByUuid(Class<T> clazz, String uuid, String fetchGroup) {
        return getObjectByUuid(clazz, UUID.fromString(uuid), fetchGroup);
    }

    /**
     * Closes the PersistenceManager instance.
     * @since 1.0.0
     */
    public void close() {
        pm.close();
    }

    public PersistenceManager getPersistenceManager() {
        return pm;
    }

}

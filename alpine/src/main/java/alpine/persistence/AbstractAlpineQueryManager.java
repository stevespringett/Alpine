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
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.security.Principal;
import java.util.Collection;
import java.util.List;

/**
 * Base persistence manager that implements AutoCloseable so that the PersistenceManager will
 * be automatically closed when used in a try-with-resource block.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public abstract class AbstractAlpineQueryManager implements AutoCloseable {

    final protected Principal principal;
    final protected Pagination pagination;
    final protected String filter;
    final protected String orderBy;
    final protected OrderDirection orderDirection;

    final protected PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager();

    /**
     * Default constructor
     */
    public AbstractAlpineQueryManager() {
        principal = null;
        pagination = new Pagination(0, 0);
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
     * pagination and ordering of results. Specific checks are performed to ensure
     * the execution of the query is capable of being paged and that ordering
     * can be securely performed.
     * @param query the JDO Query object to execute
     * @return a Collection of objects
     */
    public Object execute(Query query) {
        if (pagination != null && pagination.isPaginated()) {
            long begin = (pagination.getPage() * pagination.getSize()) -  pagination.getSize();
            long end = begin + pagination.getSize();
            query.setRange(begin, end);
        }
        if (orderBy != null && RegexSequence.Pattern.ALPHA_NUMERIC.matcher(orderBy).matches() && orderDirection != OrderDirection.UNSPECIFIED) {
            query.setOrdering(orderBy + " " + orderDirection.name().toLowerCase());
        }
        return query.execute();
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
    public <T> T getObjectByUuid(Class<T> clazz, String uuid) {
        final Query query = pm.newQuery(clazz, "uuid == :uuid");
        final List<T> result = (List<T>) query.execute(uuid);
        return result.size() == 0 ? null : result.get(0);
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
        pm.getFetchPlan().addGroup(fetchGroup);
        return getObjectByUuid(clazz, uuid);
    }

    /**
     * Closes the PersistenceManager instance.
     * @since 1.0.0
     */
    public void close() {
        pm.close();
    }

    /**
     * Upon finalization, closes the PersistenceManager, if not already closed.
     * @throws Throwable the {@code Exception} raised by this method
     * @since 1.0.0
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}


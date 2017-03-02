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

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
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

    protected PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager();

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


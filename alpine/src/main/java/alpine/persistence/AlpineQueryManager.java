/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.persistence;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.util.Collection;
import java.util.List;

public class AlpineQueryManager implements AutoCloseable {

    protected PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager();

    public void delete(Object... objects) {
        pm.currentTransaction().begin();
        pm.deletePersistentAll(objects);
        pm.currentTransaction().commit();
    }

    public void delete(Collection collection) {
        pm.currentTransaction().begin();
        pm.deletePersistentAll(collection);
        pm.currentTransaction().commit();
    }

    public <T>T getObjectById (Class<T> clazz, Object key) {
        return pm.getObjectById(clazz, key);
    }

    @SuppressWarnings("unchecked")
    public <T>T getObjectByUuid(Class<T> clazz, String uuid) {
        Query query = pm.newQuery(clazz, "uuid == :uuid");
        List<T> result = (List<T>)query.execute(uuid);
        return result.size() == 0 ? null : result.get(0);
    }

    @SuppressWarnings("unchecked")
    public <T>T getObjectByUuid(Class<T> clazz, String uuid, String fetchGroup) {
        pm.getFetchPlan().addGroup(fetchGroup);
        return getObjectByUuid(clazz, uuid);
    }

    public void close() {
        pm.close();
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}

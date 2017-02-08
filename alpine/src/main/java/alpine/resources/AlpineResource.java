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
package alpine.resources;

import alpine.model.ApiKey;
import alpine.model.LdapUser;
import alpine.resources.OrderBy;
import alpine.resources.Pagination;
import javax.annotation.PostConstruct;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;

public abstract class AlpineResource {

    @Context
    private ContainerRequestContext requestContext;

    @Context
    private UriInfo uriInfo;

    private OrderBy orderBy;
    private Pagination pagination;
    private String filter;
    private boolean serverPaging;
    private boolean serverSorting;


    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    protected OrderBy getOrderBy() {
        return orderBy;
    }

    protected Pagination getPagination() {
        return pagination;
    }

    protected String getFilter() {
        return filter;
    }

    @PostConstruct
    private void initialize() {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String top = queryParams.getFirst("top");
        String skip = queryParams.getFirst("skip");
        String filter = queryParams.getFirst("filter");
        String orderBy = queryParams.getFirst("orderBy");

        if ("asc".equalsIgnoreCase(orderBy))
            this.orderBy = OrderBy.ASCENDING;
        else if ("desc".equalsIgnoreCase(orderBy))
            this.orderBy = OrderBy.DESCENDING;
        else
            this.orderBy = OrderBy.UNSPECIFIED;

        this.filter = filter;
        this.pagination = new Pagination(top, skip);
    }

    /**
     * Returns the principal for who initiated the request.
     * @see {@link alpine.model.ApiKey}
     * @see {@link alpine.model.LdapUser}
     */
    protected Principal getPrincipal() {
        Object principal = requestContext.getProperty("Principal");
        if (principal != null) {
            return (Principal)principal;
        } else {
            return null;
        }
    }

    protected boolean isLdapUser() {
        return (getPrincipal() instanceof LdapUser);
    }

    protected boolean isApiKey() {
        return (getPrincipal() instanceof ApiKey);
    }

}

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
import alpine.model.ManagedUser;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.validation.ValidationError;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A value-added resource that all Alpine resources should extend from. This resource provides
 * access to pagination, ordering, filtering, convenience methods for obtaining specific HTTP
 * request information, along with the ability to perform input validation and automatically
 * fail requests (with HTTP status 400) if validation failure occurs.
 *
 * @since 1.0.0
 */
public abstract class AlpineResource {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    @Context
    private ContainerRequestContext requestContext;

    @Context
    private HttpServletRequest request;

    @Context
    private UriInfo uriInfo;

    private OrderBy orderBy;
    private Pagination pagination;
    private String filter;
    private boolean serverPaging;
    private boolean serverSorting;


    /**
     * Returns the ContainerRequestContext. This is automatically injected
     * into every instance of an AlpineResource.
     *
     * @since 1.0.0
     */
    protected ContainerRequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * Returns the UriInfo. This is automatically injected into every
     * instance of an AlpineResource.
     *
     * @since 1.0.0
     */
    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    /**
     * Returns the ordering (ASCENDING, DESCENDING, or UNSPECIFIED) that
     * is optionally part of any request. Ordering is determined by the
     * 'orderBy' parameter in the query string. Acceptable values for
     * 'orderBy' are 'asc' and 'desc'.
     *
     * @since 1.0.0
     */
    protected OrderBy getOrderBy() {
        return orderBy;
    }

    /**
     * Returns the pagination containing the page number and page size.
     * Pagination is determined by the 'page' and 'size' parameters in
     * the query string.
     *
     * @since 1.0.0
     */
    protected Pagination getPagination() {
        return pagination;
    }

    /**
     * Return the filter string (if specified). Filtering is determined
     * by the 'filter' parameter in the query string.
     *
     * @since 1.0.0
     */
    protected String getFilter() {
        return filter;
    }

    /**
     * Convenience method that returns the remote IP address that made
     * the request.
     *
     * @since 1.0.0
     */
    protected String getRemoteAddress() {
        return request.getRemoteAddr();
    }

    /**
     * Convenience method that returns the remote hostname that made
     * the request.
     *
     * @since 1.0.0
     */
    protected String getRemoteHost() {
        return request.getRemoteHost();
    }

    /**
     * Convenience method that returns the User-Agent string for the
     * application that made the request.
     *
     * @since 1.0.0
     */
    protected String getUserAgent() {
        return requestContext.getHeaderString("User-Agent");
    }

    /**
     * Returns a Validator instance. Internally, this uses
     * Validation.buildDefaultValidatorFactory().getValidator() so only call
     * this method sparingly and keep a reference to the Validator if possible.
     *
     * @since 1.0.0
     */
    protected Validator getValidator() {
        return VALIDATOR_FACTORY.getValidator();
    }

    /**
     * Accepts the result from one of the many validation methods available and
     * returns a List of ValidationErrors. If the size of the List is 0, no errors
     * were encounter during validation.
     *
     * Usage:
     * <pre>
     *     Validator validator = getValidator();
     *     List&lt;ValidationError&gt; errors = contOnValidationError(
     *         validator.validateProperty(myObject, "uuid"),
     *         validator.validateProperty(myObject, "name")
     *      );
     *      // If validation fails, this line will be reached.
     * </pre>
     *
     * @since 1.0.0
     */
    @SafeVarargs
    protected final List<ValidationError> contOnValidationError(Set<ConstraintViolation<Object>>... violationsArray) {
        List<ValidationError> errors = new ArrayList<>();
        for (Set<ConstraintViolation<Object>> violations : violationsArray) {
            for (ConstraintViolation violation : violations) {
                if (violation.getPropertyPath().iterator().next().getName() != null) {
                    String path = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : null;
                    String message = violation.getMessage() != null ? StringUtils.removeStart(violation.getMessage(), path + ".") : null;
                    String messageTemplate = violation.getMessageTemplate();
                    String invalidValue = violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : null;
                    ValidationError error = new ValidationError(message, messageTemplate, path, invalidValue);
                    errors.add(error);
                }
            }
        }
        return errors;
    }

    /**
     * Wrapper around {@link #contOnValidationError(Set[])} but instead of returning
     * a list of errors, this method will halt processing of the request by throwing
     * a BadRequestException, setting the HTTP status to 400 (BAD REQUEST) and providing
     * a full list of validation errors in the body of the response.
     *
     * Usage:
     * <pre>
     *     Validator validator = getValidator();
     *     failOnValidationError(
     *         validator.validateProperty(myObject, "uuid"),
     *         validator.validateProperty(myObject, "name")
     *      );
     *      // If validation fails, this line will not be reached.
     * </pre>
     *
     * @since 1.0.0
     */
    @SafeVarargs
    protected final void failOnValidationError(Set<ConstraintViolation<Object>>... violationsArray) {
        List<ValidationError> errors = contOnValidationError(violationsArray);
        if (errors.size() > 0) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(errors).build());
        }
    }

    @PostConstruct
    private void initialize() {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String page = queryParams.getFirst("page");
        String size = queryParams.getFirst("size");
        String filter = queryParams.getFirst("filter");
        String orderBy = queryParams.getFirst("orderBy");

        if ("asc".equalsIgnoreCase(orderBy))
            this.orderBy = OrderBy.ASCENDING;
        else if ("desc".equalsIgnoreCase(orderBy))
            this.orderBy = OrderBy.DESCENDING;
        else
            this.orderBy = OrderBy.UNSPECIFIED;

        this.filter = filter;
        this.pagination = new Pagination(page, size);
    }

    /**
     * Returns the principal for who initiated the request.
     * @see {@link alpine.model.ApiKey}
     * @see {@link alpine.model.LdapUser}
     *
     * @since 1.0.0
     */
    protected Principal getPrincipal() {
        Object principal = requestContext.getProperty("Principal");
        if (principal != null) {
            return (Principal)principal;
        } else {
            return null;
        }
    }

    /**
     * @since 1.0.0
     */
    protected boolean isLdapUser() {
        return (getPrincipal() instanceof LdapUser);
    }

    /**
     * @since 1.0.0
     */
    protected boolean isManagedUser() {
        return (getPrincipal() instanceof ManagedUser);
    }

    /**
     * @since 1.0.0
     */
    protected boolean isApiKey() {
        return (getPrincipal() instanceof ApiKey);
    }

}

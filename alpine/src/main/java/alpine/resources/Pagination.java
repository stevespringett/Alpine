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
package alpine.resources;

/**
 * Defines pagination used during a request.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class Pagination {

    private int page;
    private int size;

    /**
     * Creates a new Pagination object with the specified page number and size.
     * @param page the page number
     * @param size the size of the page
     */
    public Pagination(int page, int size) {
        this.page = page;
        this.size = size;
    }

    /**
     * Creates a new Pagination object with the specified page number and size. If either of the
     * specified parameters are null, a value of 0 will be used.
     * @param page the page number
     * @param size the size of the page
     */
    public Pagination(String page, String size) {
        this.page = parseIntegerFromParam(page);
        this.size = parseIntegerFromParam(size);
    }

    /**
     * Returns the page number.
     * @return the page number
     */
    public int getPage() {
        return page;
    }

    /**
     * Returns the page size.
     * @return the page size
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns if pagination is being used for this request. A page number and page size
     * greater than 0 will return true. If either of those are 0, method will return false.
     * @return if paginiation is used for this request
     */
    public boolean isPaginated() {
        return page > 0 && size > 0;
    }

    /**
     * Parses a parameter to an Integer, defaulting to 0 upon any errors encountered.
     * @param value the value to parse
     * @return an Integer
     */
    private Integer parseIntegerFromParam(String value) {
        try {
            return new Integer(value);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

}

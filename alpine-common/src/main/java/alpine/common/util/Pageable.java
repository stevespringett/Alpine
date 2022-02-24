/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.common.util;

import java.util.List;

/**
 * This class provides a simple way to paginate through a List without
 * having to implement a new type.
 *
 * @author Steve Springett
 * @since 1.3.0
 */
public class Pageable<T> {

    private final int pageSize;
    private final List<T> list;
    private final int totalPages;
    private int currentPage;
    private int startingIndex;
    private int endingIndex;


    /**
     * Creates a new Pageable instance.
     * @param pageSize the page size to use
     * @param list the list to paginate through
     */
    public Pageable(int pageSize, List<T> list) {
        this.pageSize = pageSize;
        this.list = list;
        this.currentPage = 1;

        if (pageSize > 0) {
            if (list.size() % pageSize == 0) {
                totalPages = list.size() / pageSize;
            } else {
                totalPages = (list.size() / pageSize) + 1;
            }
        } else {
            totalPages = 1;
        }

        setCurrentPage(1);
    }

    /**
     * Returns the page size as specified in the constructor.
     * @return the page size
     */
    public int getPageSize() {
        return this.pageSize;
    }

    /**
     * Returns the list as specified in the constructor.
     * @return the original list to paginate through
     */
    public List<T> getList() {
        return this.list;
    }

    /**
     * Returns a sublist (paginated) of the original list
     * based on the current page number.
     * @return a paginated list
     */
    public List<T> getPaginatedList() {
        return list.subList(startingIndex, endingIndex);
    }

    /**
     * Returns the current page number.
     * @return the current page number
     */
    public int getCurrentPage() {
        return this.currentPage;
    }

    /**
     * Returns the total number of pages.
     * @return the total number of pages
     */
    public int getTotalPages() {
        return this.totalPages;
    }

    /**
     * Determines if there are more pages.
     * @return true if more pages exist, false if not
     */
    public boolean hasMorePages() {
        return currentPage < totalPages;
    }

    /**
     * Determines if a loop should break out of pagination.
     * @return true if more pages exist, false if not
     */
    public boolean isPaginationComplete() {
        return currentPage == -1;
    }

    /**
     * Advances the pagination to the next page.
     */
    public void nextPage() {
        if (hasMorePages()) {
            setCurrentPage(currentPage + 1);
        } else {
            currentPage = -1;
        }
    }

    /**
     * Specifies a specific page to jump to.
     * @param page the page to jump to
     */
    private void setCurrentPage(int page) {
        if (page >= totalPages) {
            this.currentPage = totalPages;
        } else if (page <= 1) {
            this.currentPage = 1;
        } else {
            this.currentPage = page;
        }

        // now work out where the sub-list should start and end
        startingIndex = pageSize * (currentPage -1);
        if (startingIndex < 0) {
            startingIndex = 0;
        }
        endingIndex = startingIndex + pageSize;
        if (endingIndex > list.size()) {
            endingIndex = list.size();
        }
    }
}
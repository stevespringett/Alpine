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

public class Pagination {

    private int page;
    private int size;

    public Pagination(int page, int size) {
        this.page = this.page;
        this.size = this.size;
    }

    public Pagination(String page, String size) {
        this.page = parseIntegerFromParam(page);
        this.size = parseIntegerFromParam(size);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public boolean isPaginated() {
        return page > 0 && size > 0;
    }

    private Integer parseIntegerFromParam(String value) {
        try {
            return new Integer(value);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

}
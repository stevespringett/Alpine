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
package alpine.server.servlets;

import java.io.IOException;
import java.io.InputStream;

/**
 * The StaticResource defines a static resource that will be served.
 *
 * Adapted from http://stackoverflow.com/questions/132052/servlet-for-serving-static-content
 *
 * @author Steve Springett
 * @since 1.2.0
 */
interface StaticResource {

    /**
     * Returns the file name of the resource. This must be unique across all static resources. If any, the file
     * extension will be used to determine the content type being set. If the container doesn't recognize the
     * extension, then you can always register it as <code>&lt;mime-type&gt;</code> in <code>web.xml</code>.
     * @return The file name of the resource.
     */
    String getFileName();

    /**
     * Returns the last modified timestamp of the resource in milliseconds.
     * @return The last modified timestamp of the resource in milliseconds.
     */
    long getLastModified();

    /**
     * Returns the content length of the resource. This returns <code>-1</code> if the content length is unknown.
     * In that case, the container will automatically switch to chunked encoding if the response is already
     * committed after streaming. The file download progress may be unknown.
     * @return The content length of the resource.
     */
    long getContentLength();

    /**
     * Returns the input stream with the content of the resource. This method will be called only once by the
     * servlet, and only when the resource actually needs to be streamed, so lazy loading is not necessary.
     * @return The input stream with the content of the resource.
     * @throws IOException When something fails at I/O level.
     */
    InputStream getInputStream() throws IOException;

}

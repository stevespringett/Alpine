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

import alpine.common.logging.Logger;
import alpine.common.util.BooleanUtil;
import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The FileSystemResourceServlet serves {@link StaticResource}s from the file system
 * similar to a conventional web server.
 *
 * Adapted from http://stackoverflow.com/questions/132052/servlet-for-serving-static-content
 *
 * The Servlet contains two parameters, directory and absolute. The directory specifies the
 * the absolute or relative directory in which to serve files from. If the absolute parameter
 * is false (or not specified), then the directory will be relative from the context of the
 * webapp.
 *
 * Sample usage:
 * <pre>
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;My Images&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;alpine.servlets.FileSystemResourceServlet&lt;/servlet-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;directory&lt;/param-name&gt;
 *         &lt;param-value&gt;/path/to/images&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;absolute&lt;/param-name&gt;
 *         &lt;param-value&gt;true&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *     &lt;servlet-name&gt;My Images&lt;/servlet-name&gt;
 *     &lt;url-pattern&gt;/images/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public class FileSystemResourceServlet extends StaticResourceServlet {

    private static final Logger LOGGER = Logger.getLogger(FileSystemResourceServlet.class);

    private String directory;
    private boolean absolute;

    /**
     * Overrides the servlet init method and loads sets the InputStream necessary
     * to load application.properties.
     * @throws ServletException a general error that occurs during initialization
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        LOGGER.info("Initializing filesystem resource servlet");
        super.init(config);

        final String directory = config.getInitParameter("directory");
        if (StringUtils.isNotBlank(directory)) {
            this.directory = directory;
        }

        final String absolute = config.getInitParameter("absolute");
        if (StringUtils.isNotBlank(absolute)) {
            this.absolute = BooleanUtil.valueOf(absolute);
        }
    }

    @Override
    protected StaticResource getStaticResource(HttpServletRequest request) throws IllegalArgumentException {
        final String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            throw new IllegalArgumentException();
        }

        String name = "";
        try {
            name = URLDecoder.decode(pathInfo.substring(1), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage());
        }

        final ServletContext context = request.getServletContext();
        final File file = absolute ? new File(directory, name) : new File(context.getRealPath("/"), name).getAbsoluteFile();

        return !file.exists() ? null : new StaticResource() {
            @Override
            public long getLastModified() {
                return file.lastModified();
            }
            @Override
            public InputStream getInputStream() throws IOException {
                return Files.newInputStream(file.toPath());
            }
            @Override
            public String getFileName() {
                return file.getName();
            }
            @Override
            public long getContentLength() {
                return file.length();
            }
        };
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }
}
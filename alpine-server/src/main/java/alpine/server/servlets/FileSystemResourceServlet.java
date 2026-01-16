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
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * The FileSystemResourceServlet serves {@link StaticResource}s from the file system
 * similar to a conventional web server.
 * <p>
 * Adapted from http://stackoverflow.com/questions/132052/servlet-for-serving-static-content
 * <p>
 * Sample usage:
 * <pre>
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;My Images&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;alpine.servlets.FileSystemResourceServlet&lt;/servlet-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;directory&lt;/param-name&gt;
 *         &lt;param-value&gt;/path/to/images&lt;/param-value&gt;
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

    private Path directoryPath;

    /**
     * Overrides the servlet init method and loads sets the InputStream necessary
     * to load application.properties.
     *
     * @throws ServletException a general error that occurs during initialization
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        LOGGER.info("Initializing filesystem resource servlet");
        super.init(config);

        final String directory = config.getInitParameter("directory");
        if (StringUtils.isNotBlank(directory)) {
            setDirectoryPath(Path.of(directory).normalize());
        }
    }

    @Override
    protected StaticResource getStaticResource(HttpServletRequest request) throws IllegalArgumentException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            throw new IllegalArgumentException();
        }
        if (pathInfo.startsWith(request.getServletPath())) {
            // Workaround for ill behaviour in the Grizzly server used for testing,
            // where it includes the servlet path itself in the path info,
            // whereas the servlet spec defines it to be the path *after*
            // the servlet path.
            pathInfo = pathInfo.substring(request.getServletPath().length());
        }
        if (pathInfo.isEmpty() || "/".equals(pathInfo)) {
            throw new IllegalArgumentException();
        }

        final String fileName = URLDecoder.decode(pathInfo.substring(1), StandardCharsets.UTF_8);
        final Path filePath = directoryPath.resolve(fileName).normalize();

        if (!filePath.startsWith(directoryPath)) {
            throw new IllegalArgumentException("""
                    The provided file path %s does not resolve to a path within the \
                    configured base directory %s""".formatted(filePath, directoryPath));
        }
        if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(filePath)) {
            return null;
        }

        return new StaticResource() {
            @Override
            public long getLastModified() {
                return filePath.toFile().lastModified();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return Files.newInputStream(filePath, LinkOption.NOFOLLOW_LINKS);
            }

            @Override
            public String getFileName() {
                return filePath.toFile().getName();
            }

            @Override
            public long getContentLength() {
                return filePath.toFile().length();
            }
        };
    }

    public void setDirectoryPath(Path directoryPath) {
        if (!directoryPath.isAbsolute()) {
            throw new IllegalArgumentException("directoryPath must be absolute");
        }
        this.directoryPath = directoryPath;
    }

}
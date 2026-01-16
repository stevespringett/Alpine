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

import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemResourceServletTest extends JerseyTest {

    @TempDir
    static Path tempDir;

    static Path filesDir;

    @BeforeAll
    static void beforeAll() throws Exception {
        Files.writeString(tempDir.resolve("inaccessible.txt"), "nope");

        filesDir = Files.createDirectory(tempDir.resolve("files"));
        Files.writeString(filesDir.resolve("test.txt"), "test");
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext
                .forServlet(FileSystemResourceServlet.class)
                .servletPath("/files")
                .initParam("directory", filesDir.toString())
                .initParam("absolute", "true")
                .build();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Test
    void shouldReturnFile() {
        final Response response = target("/files/test.txt")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(String.class)).isEqualTo("test");
    }

    @Test
    void shouldReturnNotFoundWhenFileDoesNotExist() {
        final Response response = target("/files/doesNotExist")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void shouldNotAllowDirectoryTraversal() {
        final Response response = target("/files/../inaccessible.txt")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void shouldNotFollowSymlinks() throws Exception {
        Files.createSymbolicLink(filesDir.resolve("link"), tempDir.resolve("inaccessible.txt"));

        final Response response = target("/files/link")
                .request()
                .get();
        assertThat(response.getStatus()).isEqualTo(404);
    }

}
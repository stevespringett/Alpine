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
package alpine.server.util;

import alpine.model.UserPrincipal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GravatarUtilTest {

    @Test
    public void generateHashTest() {
        Assertions.assertEquals("e64c7d89f26bd1972efa854d13d7dd61", GravatarUtil.generateHash("admin@example.com"));
    }

    @Test
    public void getGravatarUrlFromPrincipalTest() {
        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getEmail()).thenReturn("admin@example.com");
        Assertions.assertEquals("https://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61.jpg?d=mm", GravatarUtil.getGravatarUrl(user));
    }

    @Test
    public void getGravatarUrlFromPrincipalSizeTest() {
        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getEmail()).thenReturn("admin@example.com");
        Assertions.assertEquals("https://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61.jpg?d=mm&s=200", GravatarUtil.getGravatarUrl(user, 200));
    }

    @Test
    public void getGravatarUrlTest() {
        Assertions.assertEquals("https://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61.jpg?d=mm", GravatarUtil.getGravatarUrl("admin@example.com"));
        String s = null;
        Assertions.assertEquals("https://www.gravatar.com/avatar/00000000000000000000000000000000.jpg?d=mm", GravatarUtil.getGravatarUrl(s));
    }

    @Test
    public void getGravatarUrlSizeTest() {
        Assertions.assertEquals("https://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61.jpg?d=mm&s=200", GravatarUtil.getGravatarUrl("admin@example.com", 200));
        String s = null;
        Assertions.assertEquals("https://www.gravatar.com/avatar/00000000000000000000000000000000.jpg?d=mm&s=200", GravatarUtil.getGravatarUrl(s, 200));
    }
}

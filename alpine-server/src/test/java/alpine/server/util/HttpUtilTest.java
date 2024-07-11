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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpUtilTest {

    public static class TestObject { }
    
    @Test
    public void getSessionAttributeTest() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("myobject")).thenReturn(new TestObject());
        Assertions.assertTrue(HttpUtil.getSessionAttribute(session, "myobject") instanceof TestObject);
        Assertions.assertTrue(HttpUtil.getSessionAttribute(session, "foo") == null);
    }

    @Test
    public void getRequestAttributeTest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("myobject")).thenReturn(new TestObject());
        Assertions.assertTrue(HttpUtil.getRequestAttribute(request, "myobject") instanceof TestObject);
        Assertions.assertTrue(HttpUtil.getRequestAttribute(request, "foo") == null);
    }
}

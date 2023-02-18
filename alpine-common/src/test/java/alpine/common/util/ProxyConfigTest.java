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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

public class ProxyConfigTest {

    @Test
    void shouldProxyWithoutHostTest() throws MalformedURLException {
        final var proxyCfg = new ProxyConfig();
        Assertions.assertFalse(proxyCfg.shouldProxy(null));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("ftp://example.com:21")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://example.com:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://example.com:8080")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://www.example.com:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://foo.example.com:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://fooexample.com:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://foo.bar.example.com:8000")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://www.example.net:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://foo.example.net:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://example.org:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://127.0.0.1:8080")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://127.0.0.1:8000")));
    }

    @Test
    void shouldProxyWithoutNoProxyTest() throws MalformedURLException {
        final var proxyCfg = new ProxyConfig();
        proxyCfg.setHost("proxy.example.com");
        Assertions.assertFalse(proxyCfg.shouldProxy(null));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("ftp://example.com:21")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://example.com:443")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://example.com:8080")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://www.example.com:443")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://foo.example.com:80")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://fooexample.com:80")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://foo.bar.example.com:8000")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://www.example.net:80")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://foo.example.net:80")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://example.org:443")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://127.0.0.1:8080")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://127.0.0.1:8000")));
    }

    @Test
    void shouldProxyWithNoProxyTest() throws MalformedURLException {
        final var proxyCfg = new ProxyConfig();
        proxyCfg.setHost("proxy.example.com");
        proxyCfg.setNoProxy(Set.of("localhost:443", "127.0.0.1:8080", "example.com", "www.example.net"));
        Assertions.assertFalse(proxyCfg.shouldProxy(null));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("ftp://example.com:21")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://example.com:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://example.com:8080")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://www.example.com:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://foo.example.com:80")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://fooexample.com:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://foo.bar.example.com:8000")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://www.example.net:80")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://foo.example.net:80")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://example.org:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://127.0.0.1:8080")));
        Assertions.assertTrue(proxyCfg.shouldProxy(new URL("http://127.0.0.1:8000")));
    }

    @Test
    void shouldProxyWithNoProxyStarTest() throws MalformedURLException {
        final var proxyCfg = new ProxyConfig();
        proxyCfg.setHost("proxy.example.com");
        proxyCfg.setNoProxy(Set.of("*"));
        Assertions.assertFalse(proxyCfg.shouldProxy(null));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("ftp://example.com:21")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://example.com:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://example.com:8080")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://www.example.com:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://foo.example.com:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://fooexample.com:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://foo.bar.example.com:8000")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://www.example.net:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://foo.example.net:80")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://example.org:443")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://127.0.0.1:8080")));
        Assertions.assertFalse(proxyCfg.shouldProxy(new URL("http://127.0.0.1:8000")));
    }

}
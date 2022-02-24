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

package alpine.server.auth;

import alpine.server.cache.CacheManager;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.HttpStatus;
import wiremock.org.apache.http.entity.ContentType;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcConfigurationResolverTest {

    private static final String OPENID_CONFIGURATION_PATH = "/.well-known/openid-configuration";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    @After
    public void tearDown() {
        // Remove configs from cache to keep testing environment clean
        CacheManager.getInstance().remove(OidcConfiguration.class, OidcConfigurationResolver.CONFIGURATION_CACHE_KEY);
    }

    @Test
    public void resolveShouldReturnNullWhenOidcIsNotEnabled() {
        assertThat(new OidcConfigurationResolver(false, wireMockRule.baseUrl()).resolve()).isNull();
    }

    @Test
    public void resolveShouldReturnNullWhenAuthorityIsNull() {
        assertThat(new OidcConfigurationResolver(true, null).resolve()).isNull();
    }

    @Test
    public void resolveShouldReturnCachedValueWhenAvailable() {
        final OidcConfiguration cachedConfiguration = new OidcConfiguration();
        CacheManager.getInstance().put(OidcConfigurationResolver.CONFIGURATION_CACHE_KEY, cachedConfiguration);

        assertThat(new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve()).isEqualTo(cachedConfiguration);
    }

    @Test
    public void resolveShouldReturnNullWhenServerRespondsWithNon200StatusCode() {
        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(OPENID_CONFIGURATION_PATH))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_NOT_FOUND)));

        assertThat(new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve()).isNull();
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlPathEqualTo(OPENID_CONFIGURATION_PATH)));
    }

    @Test
    public void resolveShouldReturnNullWhenServerRespondsWithInvalidJson() {
        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(OPENID_CONFIGURATION_PATH))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("<?xml version=\"1.0\" ?>")));

        assertThat(new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve()).isNull();
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlPathEqualTo(OPENID_CONFIGURATION_PATH)));
    }

    @Test
    public void resolveShouldReturnConfigurationAndStoreItInCache() {
        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(OPENID_CONFIGURATION_PATH))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{\n" +
                                "  \"issuer\": \"" + wireMockRule.baseUrl() + "\",\n" +
                                "  \"userinfo_endpoint\": \"" + wireMockRule.baseUrl() + "/protocol/openid-connect/userinfo\",\n" +
                                "  \"jwks_uri\": \"" + wireMockRule.baseUrl() + "/protocol/openid-connect/certs\",\n" +
                                "  \"subject_types_supported\": [\"public\",\"pairwise\"]" +
                                "}")));

        final OidcConfiguration oidcConfiguration = new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve();
        assertThat(oidcConfiguration).isNotNull();
        assertThat(oidcConfiguration.getIssuer()).isEqualTo(wireMockRule.baseUrl());
        assertThat(oidcConfiguration.getUserInfoEndpointUri().toString()).isEqualTo(wireMockRule.baseUrl() + "/protocol/openid-connect/userinfo");
        assertThat(oidcConfiguration.getJwksUri().toString()).isEqualTo(wireMockRule.baseUrl() + "/protocol/openid-connect/certs");

        // On the next invocation, the configuration should be loaded from cache
        assertThat(new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve()).isEqualTo(oidcConfiguration);

        // Only one request should've been made
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo(OPENID_CONFIGURATION_PATH)));
    }

}
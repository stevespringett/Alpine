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

import alpine.Config;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

/**
 * @since 1.8.0
 */
class OidcProxyHelper {

    static Proxy getProxyForHost(URL configURL) throws MalformedURLException {
        String proxyHost = Config.getInstance().getProperty(Config.AlpineKey.HTTP_PROXY_ADDRESS);
        String proxyPort = Config.getInstance().getProperty(Config.AlpineKey.HTTP_PROXY_PORT);
        String noProxy = Config.getInstance().getProperty(Config.AlpineKey.NO_PROXY);

        if (proxyHost == null){
            return null;
        }

        if (proxyPort == null){
            return null;
        }

        if (isTargetHostInNoProxy(configURL, noProxy)){
            return null;
        }

        int proxyPortAsInt = Config.getInstance().getPropertyAsInt(Config.AlpineKey.HTTP_PROXY_PORT);
        return new Proxy(Proxy.Type.HTTP,new InetSocketAddress(proxyHost,proxyPortAsInt));
    }

    static boolean isTargetHostInNoProxy(URL url, String noProxy) throws MalformedURLException {
        if (noProxy == null){
            return false;
        }
        for (String noProxyHost : noProxy.split(",") ) {
            if ("*".equals(noProxyHost)) {
                return true;
            }
            URL noProxyURL = new URL(noProxyHost);
            if (url.getHost().equals(noProxyURL.getHost()) ) {
                return true;
            }
        }
        return false;
    }

}

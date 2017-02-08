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
package alpine.auth;

import alpine.Config;
import org.apache.commons.lang3.StringUtils;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;

/**
 * Class that performs authentication against LDAP servers
 *
 * @since 1.0.0
 */
public class LdapAuthenticator {

    private static final String ldapUrl = Config.getInstance().getProperty(Config.Key.LDAP_SERVER_URL);
    private static final String domainName = Config.getInstance().getProperty(Config.Key.LDAP_DOMAIN);

    /**
     * Asserts a users credentials. Returns an LdapContext if assertion is successful
     * or an excpetion for any other reason.
     *
     * @since 1.0.0
     */
    public LdapContext getConnection(String username, String password) throws NamingException {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new NamingException("Username or password cannot be empty or null");
        }
        Hashtable<String, String> props = new Hashtable<>();
        String principalName = username + "@" + domainName;
        props.put(Context.SECURITY_PRINCIPAL, principalName);
        props.put(Context.SECURITY_CREDENTIALS, password);
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, ldapUrl);

        try{
            return new InitialLdapContext(props, null);
        } catch(javax.naming.CommunicationException e){
            throw new NamingException("Failed to connect to directory server");
        } catch(NamingException e){
            throw new NamingException("Failed to authenticate user");
        }
    }

    /**
     * Asserts a users credentials. Returns a boolean value indicating if
     * assertion was successful or not.
     *
     * @since 1.0.0
     */
    public boolean validateCredentials(String username, String password) {
        LdapContext ldapContext = null;
        try {
            ldapContext = getConnection(username, password);
            return true;
        } catch (NamingException e) {
            return false;
        } finally {
            if (ldapContext != null) {
                try {
                    ldapContext.close();
                } catch (NamingException e) { }
            }
        }
    }

}
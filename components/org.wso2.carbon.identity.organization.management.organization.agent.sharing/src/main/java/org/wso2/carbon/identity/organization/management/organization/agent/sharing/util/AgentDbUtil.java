/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.util;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Utility class for obtaining a {@link NamedJdbcTemplate} backed by the agent identity datasource.
 */
public class AgentDbUtil {

    private static final String AGENT_DB_JNDI_NAME = "jdbc/AgentIdentity";

    /**
     * Cached agent identity datasource. Resolved once via JNDI so that async threads,
     * where Tomcat's {@code SelectorContext} rejects non-{@code java:} JNDI lookups,
     * never need to perform the lookup themselves.
     */
    private static volatile DataSource agentDataSource;

    private AgentDbUtil() {

    }

    /**
     * Returns a new {@link NamedJdbcTemplate} backed by the agent identity datasource
     * ({@code jdbc/AgentIdentity}). The datasource is resolved via JNDI once and then
     * cached for all subsequent calls, including those from async thread-pool threads.
     *
     * @return A {@link NamedJdbcTemplate} for the agent identity database.
     * @throws RuntimeException if the datasource cannot be resolved via JNDI.
     */
    public static NamedJdbcTemplate getAgentNewTemplate() {

        return new NamedJdbcTemplate(getAgentDataSource());
    }

    private static DataSource getAgentDataSource() {

        if (agentDataSource == null) {
            synchronized (AgentDbUtil.class) {
                if (agentDataSource == null) {
                    // Use InitialContext.doLookup() (static method) instead of new InitialContext().lookup().
                    // The instance lookup() goes through Carbon's CarbonInitialJNDIContext interceptor, which
                    // calls createSubcontext on Tomcat's SelectorContext. That rejects non-java: names and
                    // fails in async threads. doLookup() bypasses the Carbon interceptor and goes directly
                    // to the underlying JNDI implementation, matching the pattern used by the kernel's
                    // DatabaseUtil.lookupDataSource().
                    try {
                        agentDataSource = InitialContext.doLookup(AGENT_DB_JNDI_NAME);
                    } catch (NamingException e) {
                        throw new RuntimeException("Error looking up agent datasource: " + AGENT_DB_JNDI_NAME, e);
                    }
                }
            }
        }
        return agentDataSource;
    }
}

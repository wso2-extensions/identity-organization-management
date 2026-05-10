/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import java.sql.Connection;
import java.util.Locale;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Utility class for obtaining a {@link NamedJdbcTemplate} backed by the agent identity datasource.
 */
public class AgentDbUtil {

    private static final String AGENT_DB_JNDI_NAME = "jdbc/AgentIdentity";

    // DB product-type keys matching SQLConstants.DBTypes in the user-sharing module.
    private static final String DB_TYPE_DB2 = "db2";
    private static final String DB_TYPE_MSSQL = "mssql";
    private static final String DB_TYPE_MYSQL = "mysql";
    private static final String DB_TYPE_ORACLE = "oracle";
    private static final String DB_TYPE_POSTGRESQL = "postgresql";
    private static final String DB_TYPE_DEFAULT = "default";
    private static final String PRODUCT_NAME_MSSQL = "microsoft sql server";
    private static final String PRODUCT_NAME_MARIADB = "mariadb";
    private static final String PRODUCT_NAME_H2 = "h2";

    /**
     * Cached agent identity datasource. Resolved once via JNDI so that async threads,
     * where Tomcat's {@code SelectorContext} rejects non-{@code java:} JNDI lookups,
     * never need to perform the lookup themselves.
     */
    private static volatile DataSource agentDataSource;

    /**
     * Cached DB product-type key for the agent identity datasource.
     * Populated on first use by {@link #getAgentDbProductType()}.
     */
    private static volatile String agentDbProductType;

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

    /**
     * Returns the DB product-type key for the agent identity datasource so that dialect-specific SQL
     * (Oracle, MSSQL, DB2, etc.) is selected based on the actual agent database rather than the global
     * UM database helpers. The result is detected once from the JDBC metadata and cached.
     *
     * @return One of "oracle", "mssql", "db2", "mysql", "postgresql", or "default".
     * @throws Exception if a connection to the agent datasource cannot be obtained.
     */
    public static String getAgentDbProductType() throws Exception {

        if (agentDbProductType == null) {
            synchronized (AgentDbUtil.class) {
                if (agentDbProductType == null) {
                    try (Connection conn = getAgentDataSource().getConnection()) {
                        agentDbProductType = mapProductNameToDbType(
                                conn.getMetaData().getDatabaseProductName());
                    }
                }
            }
        }
        return agentDbProductType;
    }

    private static String mapProductNameToDbType(String productName) {

        if (productName == null) {
            return DB_TYPE_DEFAULT;
        }
        String lowerName = productName.toLowerCase(Locale.ENGLISH);
        if (lowerName.contains(DB_TYPE_DB2)) {
            return DB_TYPE_DB2;
        }
        if (lowerName.contains(PRODUCT_NAME_MSSQL)) {
            return DB_TYPE_MSSQL;
        }
        if (lowerName.contains(DB_TYPE_MYSQL) || lowerName.contains(PRODUCT_NAME_MARIADB)
                || lowerName.contains(PRODUCT_NAME_H2)) {
            return DB_TYPE_MYSQL;
        }
        if (lowerName.contains(DB_TYPE_ORACLE)) {
            return DB_TYPE_ORACLE;
        }
        if (lowerName.contains(DB_TYPE_POSTGRESQL)) {
            return DB_TYPE_POSTGRESQL;
        }
        return DB_TYPE_DEFAULT;
    }

    private static DataSource getAgentDataSource() {

        if (agentDataSource == null) {
            synchronized (AgentDbUtil.class) {
                if (agentDataSource == null) {
                    try {
                        try {
                            agentDataSource = InitialContext.doLookup(AGENT_DB_JNDI_NAME);
                        } catch (NamingException ignored) {
                            // Fallback for environments that require java: namespace resolution.
                            agentDataSource = InitialContext.doLookup("java:comp/env/" + AGENT_DB_JNDI_NAME);
                        }
                    } catch (NamingException e) {
                        throw new RuntimeException("Error looking up agent datasource: " + AGENT_DB_JNDI_NAME, e);
                    }
                }
            }
        }
        return agentDataSource;
    }
}

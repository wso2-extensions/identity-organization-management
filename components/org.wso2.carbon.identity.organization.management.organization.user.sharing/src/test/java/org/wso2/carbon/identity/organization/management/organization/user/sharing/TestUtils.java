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

package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.internal.CarbonContextDataHolder;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

/**
 * Utility class for setting up and managing an in-memory H2 database for testing.
 */
public class TestUtils {

    public static final String DB_NAME = "testOrgMgt_db";
    public static final String H2_SCRIPT_NAME = "h2.sql";
    public static Map<String, BasicDataSource> dataSourceMap = new HashMap<>();
    private static String originalCarbonHome;
    private static String originalCarbonConfigDir;

    /**
     * Initializes an in-memory H2 database and executes the SQL script defined by {@link #H2_SCRIPT_NAME}.
     * The created {@link BasicDataSource} is stored in {@link #dataSourceMap}.
     *
     * @throws SQLException if a database access error occurs or the script execution fails
     */
    public static void initiateH2Base() throws SQLException {

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("username");
        dataSource.setPassword("password");
        dataSource.setUrl("jdbc:h2:mem:test" + DB_NAME);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("select 1");
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().executeUpdate("RUNSCRIPT FROM '" + getFilePath(H2_SCRIPT_NAME) + "'");
        }
        dataSourceMap.put(DB_NAME, dataSource);
    }

    /**
     * Resolves the absolute file path for a given database script located under src/test/resources/dbscripts.
     *
     * @param fileName name of the script file
     * @return absolute path to the script file
     * @throws IllegalArgumentException if the fileName is null or blank
     */
    public static String getFilePath(String fileName) {

        if (StringUtils.isNotBlank(fileName)) {
            return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "dbscripts",
                    fileName).toString();
        }
        throw new IllegalArgumentException("DB Script file name cannot be empty.");
    }

    /**
     * Closes and removes the H2 {@link BasicDataSource} associated with {@link #DB_NAME}.
     *
     * @throws Exception if an error occurs while closing the datasource
     */
    public static void closeH2Base() throws Exception {

        BasicDataSource dataSource = dataSourceMap.remove(DB_NAME);
        if (dataSource != null) {
            dataSource.close();
        }
        restoreSystemProperty(CarbonBaseConstants.CARBON_HOME, originalCarbonHome);
        restoreSystemProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, originalCarbonConfigDir);
    }

    /**
     * Mocks and injects the H2 {@link DataSource} into static contexts required for testing.
     * Also sets up Carbon configuration paths and a mocked {@link UserRealm}.
     *
     * @throws Exception if reflection fails or required fields cannot be accessed/set
     */
    public static void mockDataSource() throws Exception {

        originalCarbonHome = System.getProperty(CarbonBaseConstants.CARBON_HOME);
        originalCarbonConfigDir = System.getProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH);
        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome,
                "repository/conf").toString());

        DataSource dataSource = dataSourceMap.get(DB_NAME);

        setStatic(DatabaseUtil.class.getDeclaredField("dataSource"), dataSource);

        Field carbonContextHolderField =
                CarbonContext.getThreadLocalCarbonContext().getClass().getDeclaredField("carbonContextHolder");
        carbonContextHolderField.setAccessible(true);
        CarbonContextDataHolder carbonContextHolder
                = (CarbonContextDataHolder) carbonContextHolderField.get(CarbonContext.getThreadLocalCarbonContext());
        carbonContextHolder.setUserRealm(mock(UserRealm.class));
        setStatic(Utils.class.getDeclaredField("dataSource"), dataSource);
    }

    private static void setStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }

    private static void restoreSystemProperty(String key, String value) {

        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}

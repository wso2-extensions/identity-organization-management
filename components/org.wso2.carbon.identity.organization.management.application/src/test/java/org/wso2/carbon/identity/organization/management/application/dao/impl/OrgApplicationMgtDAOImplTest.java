/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.dao.impl;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrgApplicationMgtDAOImpl.
 */
public class OrgApplicationMgtDAOImplTest {

    private static final String DB_NAME = "testOrgAppMgtDB";
    private static final String H2_SCRIPT_NAME = "h2.sql";
    private static Map<String, BasicDataSource> dataSourceMap = new HashMap<>();

    private OrgApplicationMgtDAOImpl orgApplicationMgtDAO;

    private static final String ROOT_APP_ID = "fa9b9ac5-a429-49e2-9c51-4259c7ebe45e";
    private static final String ROOT_APP_NAME = "test-app";
    private static final String ROOT_ORG_ID = "72b81cba-51c7-4dc1-91be-b267e177c17a";
    private static final int ROOT_TENANT_ID = 1;
    private static final String SHARED_APP_ID_1 = "42ef1d92-add6-449b-8a3c-fc308d2a4eac";
    private static final String SHARED_ORG_ID_1 = "30b701c6-e309-4241-b047-0c299c45d1a0";
    private static final int SHARED_TENANT_ID_1 = 2;
    private static final String SHARED_APP_ID_2 = "1e2ef3df-e670-4339-9833-9df41dda7c96";
    private static final String SHARED_ORG_ID_2 = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final int SHARED_TENANT_ID_2 = 3;
    private static final String UNSHARED_ORG_ID = "89d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final String PRIMARY_USERSTORE = "PRIMARY";
    private static final String ADMIN_USERNAME = "admin";
    private static final String AUTH_TYPE = "default";

    private static final String CREATE_APPLICATION_SQL_STATEMENT = "INSERT INTO SP_APP " +
            "(TENANT_ID, APP_NAME, USER_STORE, USERNAME, AUTH_TYPE, UUID) VALUES (?, ?, ?, ?, ?, ?);";
    private static final String SHARE_APPLICATION_SQL_STATEMENT = "INSERT INTO SP_SHARED_APP " +
            "(MAIN_APP_ID, OWNER_ORG_ID, SHARED_APP_ID, SHARED_ORG_ID, SHARE_WITH_ALL_CHILDREN) " +
            "VALUES (?, ?, ?, ?, ?);";
    private static final String DELETE_APPLICATION_SQL_STATEMENT = "DELETE FROM SP_APP " +
            "WHERE UUID = ?;";
    private static final String DELETE_SHARED_APPLICATION_SQL_STATEMENT = "DELETE FROM SP_SHARED_APP " +
            "WHERE SHARED_APP_ID = ?;";

    @BeforeClass
    public void setUp() throws Exception {

        initiateH2Base();
        mockDataSource();

        // Create root application.
        createApplication(ROOT_TENANT_ID, ROOT_APP_NAME, ROOT_APP_ID, PRIMARY_USERSTORE, ADMIN_USERNAME, AUTH_TYPE);

        orgApplicationMgtDAO = new OrgApplicationMgtDAOImpl();
    }

    @AfterClass
    public void tearDown() throws Exception {

        // Delete root application.
        deleteApplication(ROOT_APP_ID);
        closeH2Base();
    }

    @DataProvider(name = "filteredSharedApplicationsTestData")
    public Object[][] getFilteredSharedApplicationsTestData() {

        return new Object[][]{
                // Passing org ids of both shared apps only.
                {Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2), 2},
                // Passing org ids of both shared apps and an unshared org id.
                {Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, UNSHARED_ORG_ID), 2},
                // Passing org id of one shared app only.
                {Collections.singletonList(SHARED_ORG_ID_1), 1},
                // Passing org id of shared app and an unshared org id.
                {Arrays.asList(SHARED_ORG_ID_1, UNSHARED_ORG_ID), 1},
                // Passing an unshared org id only.
                {Collections.singletonList(UNSHARED_ORG_ID), 0},
                // Passing an empty list
                {Collections.emptyList(), 0},
        };
    }

    @Test(dataProvider = "filteredSharedApplicationsTestData")
    public void testGetFilteredSharedApplications(List<String> sharedOrgIds, int expectedNumOfApps) throws Exception {

        // Create shared application 1.
        createApplication(SHARED_TENANT_ID_1, ROOT_APP_NAME, SHARED_APP_ID_1, PRIMARY_USERSTORE, ADMIN_USERNAME,
                AUTH_TYPE);
        shareApplication(ROOT_APP_ID, ROOT_ORG_ID, SHARED_APP_ID_1, SHARED_ORG_ID_1);

        // Create shared  application 2.
        createApplication(SHARED_TENANT_ID_2, ROOT_APP_NAME, SHARED_APP_ID_2, PRIMARY_USERSTORE, ADMIN_USERNAME,
                AUTH_TYPE);
        shareApplication(ROOT_APP_ID, ROOT_ORG_ID, SHARED_APP_ID_2, SHARED_ORG_ID_2);

        List<SharedApplicationDO> sharedApplications =
                orgApplicationMgtDAO.getSharedApplications(ROOT_APP_ID, ROOT_ORG_ID, sharedOrgIds);

        Assert.assertNotNull(sharedApplications);
        Assert.assertEquals(sharedApplications.size(), expectedNumOfApps);

        // Delete shared applications.
        deleteSharedApplication(SHARED_APP_ID_1);
        deleteSharedApplication(SHARED_APP_ID_2);
    }

    private void initiateH2Base() throws Exception {

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

    private String getFilePath(String fileName) {

        if (StringUtils.isNotBlank(fileName)) {
            return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "dbscripts",
                    fileName).toString();
        }
        throw new IllegalArgumentException("DB Script file name cannot be empty.");
    }

    private void mockDataSource() {

        DataSource dataSource = dataSourceMap.get(DB_NAME);
        mockStatic(IdentityDatabaseUtil.class);
        when(IdentityDatabaseUtil.getDataSource()).thenReturn(dataSource);
    }

    private Connection getConnection() throws SQLException {

        if (dataSourceMap.get(DB_NAME) != null) {
            return dataSourceMap.get(DB_NAME).getConnection();
        }
        throw new RuntimeException("No datasource initiated for database: " + DB_NAME);
    }

    public void closeH2Base() throws Exception {

        BasicDataSource dataSource = dataSourceMap.remove(DB_NAME);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void createApplication(int tenantId, String appName, String appId, String userstore, String username,
                                   String authType) throws Exception {

        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(CREATE_APPLICATION_SQL_STATEMENT);
            statement.setInt(1, tenantId);
            statement.setString(2, appName);
            statement.setString(3, userstore);
            statement.setString(4, username);
            statement.setString(5, authType);
            statement.setString(6, appId);
            statement.execute();
        }
    }

    private void shareApplication(String rootAppId, String rootOrgId, String sharedAppId, String sharedOrgId) {

        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(SHARE_APPLICATION_SQL_STATEMENT);
            statement.setString(1, rootAppId);
            statement.setString(2, rootOrgId);
            statement.setString(3, sharedAppId);
            statement.setString(4, sharedOrgId);
            statement.setBoolean(5, true);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Error while sharing application: " + rootAppId + " with organization: " +
                    sharedOrgId, e);
        }
    }

    private void deleteApplication(String appId) {

        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(DELETE_APPLICATION_SQL_STATEMENT);
            statement.setString(1, appId);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting application: " + appId, e);
        }
    }

    private void deleteSharedApplication(String appId) {

        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(DELETE_SHARED_APPLICATION_SQL_STATEMENT);
            statement.setString(1, appId);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting shared application: " + appId, e);
        }

        deleteApplication(appId);
    }
}

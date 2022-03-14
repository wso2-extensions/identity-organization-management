/*
 *
 *  * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.role.mgt.core.dao;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.JdbcUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.sql.DataSource;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.generateUniqueID;

@PrepareForTest({Utils.class, JdbcUtils.class})
public class OrganizationUserRoleMgtDAOImplTest extends PowerMockTestCase {

    private static final Calendar CALENDAR = Calendar.getInstance(TimeZone.getTimeZone(UTC));
    private static final String DB_NAME = "testUserRoleOrg_db";
    private static final String DESCRIPTION = "This is a child organization of (orgId : %s and orgName: %s).";
    private static final String INVALID_DATA = "invalid data";
    private static final int TENANT_ID = -1234;
    private static final String USER_ID = generateUniqueID();
    private static final String ROLE_ID = generateUniqueID();

    private OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
    private static Map<String, BasicDataSource> dataSourceMap = new HashMap<>();

    /*
     * The organization tree structure is as follows.
     *       ROOT
     *        |
     *       ORG-A
     *        |
     *       ORG-B
     *       /  \
     *    ORG-C ORG-D
     * */
    private String rootOrgId;
    private String[] orgIds = new String[4];
    private static final String[] ORG_NAMES = {"ORG-A", "ORG-B", "ORG-C", "ORG-D"};

    @BeforeClass
    public void setUp() throws Exception {

        initiateH2Base(getFilePath());
        storeRootOrganization();
        storeChildOrganizations();
    }

    @AfterClass
    public void tearDown() throws Exception {

        closeH2Base();
    }

    @DataProvider(name = "dataForTestingForcedOrganizationUserRoleMappingExists")
    public Object[][] dataForTestingForcedOrganizationUserRoleMappingExists() {

        return new Object[][]{
                {orgIds[0], orgIds[0]},
                {orgIds[0], orgIds[1]},
                {orgIds[1], orgIds[0]},
                {orgIds[1], orgIds[1]},
                {INVALID_DATA, INVALID_DATA}
        };
    }

    @Test(dataProvider = "dataForTestingForcedOrganizationUserRoleMappingExists")
    public void testIfForcedOrganizationUserRoleMappingExists(String orgId, String assignedAt) throws Exception {

        // add only forced organization user role mappings.
        removeAllOrganizationUserRoleMappings();
        storeOrganizationUserRoleMappingsForced();
        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean isOrgUserRoleMappingExists = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId,
                    USER_ID, ROLE_ID, assignedAt, true, TENANT_ID);
            if (StringUtils.equals(orgId, orgIds[0]) && StringUtils.equals(assignedAt, orgIds[0])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[0]) && StringUtils.equals(assignedAt, orgIds[1])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[1]) && StringUtils.equals(assignedAt, orgIds[0])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgIds[1], orgId) && StringUtils.equals(assignedAt, orgIds[1])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, INVALID_DATA) && StringUtils.equals(orgId, INVALID_DATA)) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            }
        }
    }

    @DataProvider(name = "dataForTestingForcedOrganizationUserRoleMappingIsDirectlyAssigned")
    public Object[][] dataForTestingForcedOrganizationUserRoleMappingIsDirectlyAssigned() {

        return new Object[][]{
                {orgIds[0]},
                {orgIds[1]},
                {INVALID_DATA}
        };
    }

    @Test(dataProvider = "dataForTestingForcedOrganizationUserRoleMappingIsDirectlyAssigned")
    public void testIfForcedOrganizationUserRoleMappingIsDirectlyAssigned(String orgId) throws Exception {

        // add only forced organization-user-role mappings.
        removeAllOrganizationUserRoleMappings();
        storeOrganizationUserRoleMappingsForced();
        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean isOrgUserRoleMappingExists = organizationUserRoleMgtDAO
                    .getDirectlyAssignedOrganizationUserRoleMappingInheritance(orgId, USER_ID, ROLE_ID, TENANT_ID) == 1;
            if (StringUtils.equals(orgId, orgIds[0])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[0])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, INVALID_DATA)) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            }
        }
    }

    @DataProvider(name = "dataForTestingNotForcedPropagatingOrganizationUserRoleMappingExists")
    public Object[][] dataForTestingNotForcedPropagatingOrganizationUserRoleMappingExists() {

        return new Object[][]{
                {orgIds[0], orgIds[0]},
                {orgIds[0], orgIds[1]},
                {orgIds[1], orgIds[0]},
                {orgIds[1], orgIds[1]},
                {INVALID_DATA, INVALID_DATA}
        };
    }

    @Test(dataProvider = "dataForTestingNotForcedPropagatingOrganizationUserRoleMappingExists")
    public void testIfNotForcedPropagatingOrganizationUserRoleMappingExists(String orgId, String assignedAt)
            throws Exception {

        // store Not-forced, propagating organization-user-role mappings.
        removeAllOrganizationUserRoleMappings();
        storeOrganizationUserRoleMappingsNotForcedPropagating();
        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean isOrgUserRoleMappingExists = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId,
                    USER_ID, ROLE_ID, assignedAt, false, TENANT_ID);
            if (StringUtils.equals(orgId, orgIds[0]) && StringUtils.equals(assignedAt, orgIds[0])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[0]) && StringUtils.equals(assignedAt, orgIds[1])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[1]) && StringUtils.equals(assignedAt, orgIds[0])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgIds[1], orgId) && StringUtils.equals(assignedAt, orgIds[1])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, INVALID_DATA) && StringUtils.equals(orgId, INVALID_DATA)) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            }
        }
    }

    @DataProvider(name = "dataForTestingNotForcedPropagatingOrganizationUserRoleMappingIsDirectlyAssigned")
    public Object[][] dataForTestingNotForcedPropagatingOrganizationUserRoleMappingIsDirectlyAssigned() {

        return new Object[][]{
                {orgIds[0]},
                {orgIds[1]},
                {INVALID_DATA}
        };
    }

    @Test(dataProvider = "dataForTestingNotForcedPropagatingOrganizationUserRoleMappingIsDirectlyAssigned")
    public void testIfNotForcedPropagatingOrganizationUserRoleMappingIsDirectlyAssigned(String orgId) throws Exception {

        // add only Not-forced, propagating organization-user-role mappings.
        removeAllOrganizationUserRoleMappings();
        storeOrganizationUserRoleMappingsNotForcedPropagating();
        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean isOrgUserRoleMappingExists = organizationUserRoleMgtDAO
                    .getDirectlyAssignedOrganizationUserRoleMappingInheritance(orgId, USER_ID, ROLE_ID, TENANT_ID) == 0;
            if (StringUtils.equals(orgId, orgIds[0])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[0])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, INVALID_DATA)) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            }
        }
    }

    @DataProvider(name = "dataForTestingNotForcedNotPropagatingOrganizationUserRoleMappingExists")
    public Object[][] dataForTestingNotForcedNotPropagatingOrganizationUserRoleMappingExists() {

        return new Object[][]{
                {orgIds[0], orgIds[0]},
                {orgIds[0], orgIds[1]},
                {orgIds[1], orgIds[0]},
                {orgIds[1], orgIds[1]},
                {INVALID_DATA, INVALID_DATA}
        };
    }

    @Test(dataProvider = "dataForTestingNotForcedNotPropagatingOrganizationUserRoleMappingExists")
    public void testIfNotForcedNotPropagatingOrganizationUserRoleMappingExists(String orgId, String assignedAt)
            throws Exception {

        // store Not-forced, propagating organization-user-role mappings.
        removeAllOrganizationUserRoleMappings();
        storeOrganizationUserRoleMappingsNotForcedNotPropagating();
        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean isOrgUserRoleMappingExists = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId,
                    USER_ID, ROLE_ID, assignedAt, false, TENANT_ID);
            if (StringUtils.equals(orgId, orgIds[0]) && StringUtils.equals(assignedAt, orgIds[0])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[0]) && StringUtils.equals(assignedAt, orgIds[1])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[1]) && StringUtils.equals(assignedAt, orgIds[0])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgIds[1], orgId) && StringUtils.equals(assignedAt, orgIds[1])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, INVALID_DATA) && StringUtils.equals(orgId, INVALID_DATA)) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            }
        }
    }

    @DataProvider(name = "dataForTestingNotForcedNotPropagatingOrganizationUserRoleMappingIsDirectlyAssigned")
    public Object[][] dataForTestingNotForcedNotPropagatingOrganizationUserRoleMappingIsDirectlyAssigned() {

        return new Object[][]{
                {orgIds[0]},
                {orgIds[1]},
                {INVALID_DATA}
        };
    }

    @Test(dataProvider = "dataForTestingNotForcedNotPropagatingOrganizationUserRoleMappingIsDirectlyAssigned")
    public void testIfNotForcedNotPropagatingOrganizationUserRoleMappingIsDirectlyAssigned(String orgId)
            throws Exception {

        // add only Not-forced, propagating organization-user-role mappings.
        removeAllOrganizationUserRoleMappings();
        removeAllOrganizationUserRoleMappings();
        storeOrganizationUserRoleMappingsNotForcedNotPropagating();
        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean isOrgUserRoleMappingExists = organizationUserRoleMgtDAO
                    .getDirectlyAssignedOrganizationUserRoleMappingInheritance(orgId, USER_ID, ROLE_ID, TENANT_ID) == 0;
            if (StringUtils.equals(orgId, orgIds[0])) {
                Assert.assertTrue(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, orgIds[0])) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            } else if (StringUtils.equals(orgId, INVALID_DATA)) {
                Assert.assertFalse(isOrgUserRoleMappingExists);
            }
        }
    }

    @DataProvider(name = "dataForTestingGetAllSubOrganizations")
    public Object[][] dataForTestingGetAllSubOrganizations() {

        return new Object[][]{
                {orgIds[0]},
                {orgIds[1]},
                {orgIds[2]},
                {INVALID_DATA}
        };
    }

    @Test(dataProvider = "dataForTestingGetAllSubOrganizations")
    public void testGetAllSubOrganizations(String orgId) throws Exception {

        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            List<String> subOrganizations = organizationUserRoleMgtDAO.getAllSubOrganizations(orgId);
            if (StringUtils.equals(orgId, orgIds[0])) {
                List<String> testSubOrgs = new ArrayList<>();
                testSubOrgs.add(orgIds[1]);
                testSubOrgs.add(orgIds[2]);
                testSubOrgs.add(orgIds[3]);
                Assert.assertTrue(CollectionUtils.isEqualCollection(
                        CollectionUtils.intersection(subOrganizations, testSubOrgs), testSubOrgs));
            } else if (StringUtils.equals(orgId, orgIds[1])) {
                List<String> testSubOrgs = new ArrayList<>();
                testSubOrgs.add(orgIds[2]);
                testSubOrgs.add(orgIds[3]);
                Assert.assertTrue(CollectionUtils.isEqualCollection(
                        CollectionUtils.intersection(subOrganizations, testSubOrgs), testSubOrgs));
            } else if (StringUtils.equals(orgId, orgIds[2])) {
                Assert.assertTrue(CollectionUtils.isEmpty(subOrganizations));
            } else if (StringUtils.equals(orgId, INVALID_DATA)) {
                Assert.assertTrue(CollectionUtils.isEmpty(subOrganizations));
            }
        }
    }

    private void storeOrganizationUserRoleMappingsForced() throws Exception {

        try (Connection connection = getConnection()) {
            int numberOfOrganizationUserRoleMappings = 4;
            String sql = buildQueryForOrganizationUserRoleMappingInsert(numberOfOrganizationUserRoleMappings);
            PreparedStatement stm = connection.prepareStatement(sql);
            int parameterIndex = 0;
            for (int i = 0; i < numberOfOrganizationUserRoleMappings; i++) {
                stm.setString(++parameterIndex, Utils.generateUniqueID());
                stm.setString(++parameterIndex, USER_ID);
                stm.setString(++parameterIndex, ROLE_ID);
                stm.setInt(++parameterIndex, TENANT_ID);
                stm.setString(++parameterIndex, orgIds[i]);
                stm.setString(++parameterIndex, orgIds[0]);
                stm.setInt(++parameterIndex, 1);
            }
            stm.execute();
        }
    }

    private void storeOrganizationUserRoleMappingsNotForcedPropagating() throws Exception {

        try (Connection connection = getConnection()) {
            int numberOfOrganizationUserRoleMappings = 4;
            String sql = buildQueryForOrganizationUserRoleMappingInsert(numberOfOrganizationUserRoleMappings);
            PreparedStatement stm = connection.prepareStatement(sql);
            int parameterIndex = 0;
            for (int i = 0; i < numberOfOrganizationUserRoleMappings; i++) {
                stm.setString(++parameterIndex, Utils.generateUniqueID());
                stm.setString(++parameterIndex, USER_ID);
                stm.setString(++parameterIndex, ROLE_ID);
                stm.setInt(++parameterIndex, TENANT_ID);
                stm.setString(++parameterIndex, orgIds[i]);
                stm.setString(++parameterIndex, orgIds[i]);
                stm.setInt(++parameterIndex, 0);
            }
            stm.execute();
        }
    }

    private void storeOrganizationUserRoleMappingsNotForcedNotPropagating() throws Exception {

        try (Connection connection = getConnection()) {
            int numberOfOrganizationUserRoleMappings = 1;
            String sql = buildQueryForOrganizationUserRoleMappingInsert(numberOfOrganizationUserRoleMappings); //ORG-A
            PreparedStatement stm = connection.prepareStatement(sql);
            int parameterIndex = 0;
            stm.setString(++parameterIndex, Utils.generateUniqueID());
            stm.setString(++parameterIndex, USER_ID);
            stm.setString(++parameterIndex, ROLE_ID);
            stm.setInt(++parameterIndex, TENANT_ID);
            stm.setString(++parameterIndex, orgIds[0]);
            stm.setString(++parameterIndex, orgIds[0]);
            stm.setInt(++parameterIndex, 0);
            stm.execute();
        }
    }

    private void removeAllOrganizationUserRoleMappings() throws Exception {

        try (Connection connection = getConnection()) {
            String sql = buildQueryForOrganizationUserRoleMappingDelete();
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.execute();
        }
    }

    private String buildQueryForOrganizationUserRoleMappingDelete() {

        StringBuilder sb = new StringBuilder("DELETE FROM UM_USER_ROLE_ORG WHERE ");
        for (int i = 0; i < orgIds.length; i++) {
            sb.append("ORG_ID='").append(orgIds[i]).append("'");
            if (i != orgIds.length - 1) {
                sb.append(" OR ");
            }
        }
        return sb.toString();
    }

    private String buildQueryForOrganizationUserRoleMappingInsert(int numberOfOrganizationUserRoleMappings) {

        String query = "INSERT INTO UM_USER_ROLE_ORG (UM_ID, UM_USER_ID, UM_ROLE_ID, UM_TENANT_ID, ORG_ID, " +
                "ASSIGNED_AT, FORCED) VALUES %s";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numberOfOrganizationUserRoleMappings; i++) {
            sb.append("(?,?,?,?,?,?,?)");
            if (i != numberOfOrganizationUserRoleMappings - 1) {
                sb.append(",");
            }
        }
        return String.format(query, sb);
    }

    private void initiateH2Base(String scriptPath) throws Exception {

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("username");
        dataSource.setPassword("password");
        dataSource.setUrl("jdbc:h2:mem:test" + DB_NAME);
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().executeUpdate("RUNSCRIPT FROM '" + scriptPath + "'");
        }
        dataSourceMap.put(DB_NAME, dataSource);
    }

    private void closeH2Base() throws Exception {

        BasicDataSource dataSource = dataSourceMap.get(DB_NAME);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private DataSource mockDataSource() {

        DataSource dataSource = mock(DataSource.class);
        mockStatic(Utils.class);
        when(Utils.getNewNamedJdbcTemplate()).thenReturn(new NamedJdbcTemplate(dataSource));
        return dataSource;
    }

    private static String getFilePath() {

        return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "dbscripts", "h2.sql").toString();
    }

    private static Connection spyConnection(Connection connection) throws SQLException {

        Connection spy = spy(connection);
        doNothing().when(spy).close();
        return spy;
    }

    private static Connection getConnection() throws SQLException {

        if (dataSourceMap.get(DB_NAME) != null) {
            return dataSourceMap.get(DB_NAME).getConnection();
        }
        throw new RuntimeException("No datasource initiated for database: " + DB_NAME);
    }

    private void storeRootOrganization() throws Exception {

        rootOrgId = generateUniqueID();
        storeOrganization(rootOrgId, "ROOT", "This is the ROOT organization", null);
    }

    private void storeChildOrganizations() throws Exception {

        for (int i = 0; i < ORG_NAMES.length; i++) {
            orgIds[i] = generateUniqueID();
        }
        // store child organizations according to the tree structure.
        storeOrganization(orgIds[0], ORG_NAMES[0], String.format(DESCRIPTION, rootOrgId, "ROOT"), rootOrgId);
        storeOrganization(orgIds[1], ORG_NAMES[1], String.format(DESCRIPTION, orgIds[0], ORG_NAMES[0]), orgIds[0]);
        storeOrganization(orgIds[2], ORG_NAMES[2], String.format(DESCRIPTION, orgIds[1], ORG_NAMES[1]), orgIds[1]);
        storeOrganization(orgIds[3], ORG_NAMES[3], String.format(DESCRIPTION, orgIds[1], ORG_NAMES[1]), orgIds[1]);
    }

    private void storeOrganization(String id, String name, String description, String parentId) throws Exception {

        try (Connection connection = getConnection()) {
            String sql = "INSERT INTO UM_ORG (UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, " +
                    "UM_LAST_MODIFIED, UM_TENANT_ID, UM_PARENT_ID) VALUES ( ?, ?, " +
                    "?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, id);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setTimestamp(4, Timestamp.from(Instant.now()), CALENDAR);
            statement.setTimestamp(5, Timestamp.from(Instant.now()), CALENDAR);
            statement.setInt(6, TENANT_ID);
            statement.setString(7, parentId);
            statement.execute();
        }
    }
}

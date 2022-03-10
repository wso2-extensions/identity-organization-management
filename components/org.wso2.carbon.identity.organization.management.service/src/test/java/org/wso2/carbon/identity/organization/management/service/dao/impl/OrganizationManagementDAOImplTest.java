/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.service.dao.impl;

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
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

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
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.OrganizationTypes.STRUCTURAL;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.generateUniqueID;

@PrepareForTest({Utils.class, JdbcUtils.class})
public class OrganizationManagementDAOImplTest extends PowerMockTestCase {

    private OrganizationManagementDAO organizationManagementDAO = new OrganizationManagementDAOImpl();
    private static Map<String, BasicDataSource> dataSourceMap = new HashMap<>();
    private static final String DB_NAME = "testOrgMgt_db";
    private static final Calendar CALENDAR = Calendar.getInstance(TimeZone.getTimeZone(UTC));
    private static final int TENANT_ID = -1234;
    private static final String TENANT_DOMAIN = "carbon.super";
    private static final String ATTRIBUTE_KEY = "country";
    private static final String ATTRIBUTE_VALUE = "Sri Lanka";
    private static final String ORG_NAME = "XYZ builders";
    private static final String INVALID_DATA = "invalid data";
    private String rootOrgId;
    private String orgId;

    @BeforeClass
    public void setUp() throws Exception {

        initiateH2Base(getFilePath());
        storeRootOrganization();
        storeChildOrganization(rootOrgId);
    }

    @AfterClass
    public void tearDown() throws Exception {

        closeH2Base();
    }

    @Test
    public void testAddOrganization() throws Exception {

        String orgId = generateUniqueID();
        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);

            Organization organization = new Organization();
            organization.setId(orgId);
            organization.setName("org1");
            organization.setDescription("org1 description.");
            organization.setCreated(Instant.now());
            organization.setLastModified(Instant.now());
            organization.setType(STRUCTURAL.toString());

            ParentOrganizationDO parentOrganizationDO = new ParentOrganizationDO();
            parentOrganizationDO.setId(rootOrgId);
            organization.setParent(parentOrganizationDO);

            List<OrganizationAttribute> attributes = new ArrayList<>();
            attributes.add(new OrganizationAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE));
            organization.setAttributes(attributes);

            organizationManagementDAO.addOrganization(TENANT_ID, TENANT_DOMAIN, organization);
        }
    }

    @DataProvider(name = "dataForIsOrganizationExistById")
    public Object[][] dataForIsOrganizationExistById() {

        return new Object[][]{

                {orgId},
                {INVALID_DATA},
        };
    }

    @Test(dataProvider = "dataForIsOrganizationExistById")
    public void testIsOrganizationExistById(String id) throws Exception {

        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean organizationExistByName = organizationManagementDAO.isOrganizationExistById(TENANT_ID,
                    id, TENANT_DOMAIN);
            if (StringUtils.equals(id, orgId)) {
                Assert.assertTrue(organizationExistByName);
            } else if (StringUtils.equals(id, INVALID_DATA)) {
                Assert.assertFalse(organizationExistByName);
            }
        }
    }

    @Test
    public void testGetOrganizationIdByName() throws Exception {

        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            String organizationId = organizationManagementDAO.getOrganizationIdByName(TENANT_ID,
                    ORG_NAME, TENANT_DOMAIN);
            Assert.assertEquals(organizationId, orgId);
        }
    }

    @Test
    public void testGetOrganization() throws Exception {

        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            Organization organization = organizationManagementDAO.getOrganization(TENANT_ID, orgId, TENANT_DOMAIN);
            Assert.assertEquals(organization.getName(), ORG_NAME);
            Assert.assertEquals(organization.getParent().getId(), rootOrgId);
        }
    }

    @DataProvider(name = "dataForHasChildOrganizations")
    public Object[][] dataForHasChildOrganizations() {

        return new Object[][]{

                {rootOrgId},
                {orgId},
        };
    }

    @Test(dataProvider = "dataForHasChildOrganizations")
    public void testHasChildOrganizations(String id) throws Exception {

        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean hasChildOrganizations = organizationManagementDAO.hasChildOrganizations(id, TENANT_DOMAIN);
            if (StringUtils.equals(id, orgId)) {
                Assert.assertFalse(hasChildOrganizations);
            } else if (StringUtils.equals(id, rootOrgId)) {
                Assert.assertTrue(hasChildOrganizations);
            }
        }
    }

    @DataProvider(name = "dataForIsAttributeExistByKey")
    public Object[][] dataForIsAttributeExistByKey() {

        return new Object[][]{

                {ATTRIBUTE_KEY},
                {INVALID_DATA},
        };
    }

    @Test(dataProvider = "dataForIsAttributeExistByKey")
    public void testIsAttributeExistByKey(String key) throws Exception {

        DataSource dataSource = mockDataSource();
        try (Connection connection = getConnection()) {
            Connection spy = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            boolean attributeExistByKey = organizationManagementDAO.isAttributeExistByKey(TENANT_DOMAIN, orgId,
                    key);
            if (StringUtils.equals(key, ATTRIBUTE_KEY)) {
                Assert.assertTrue(attributeExistByKey);
            } else if (StringUtils.equals(key, INVALID_DATA)) {
                Assert.assertFalse(attributeExistByKey);
            }
        }
    }

    private DataSource mockDataSource() {

        DataSource dataSource = mock(DataSource.class);
        mockStatic(Utils.class);
        when(Utils.getNewTemplate()).thenReturn(new NamedJdbcTemplate(dataSource));
        return dataSource;
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

    private void closeH2Base() throws Exception {

        BasicDataSource dataSource = dataSourceMap.get(DB_NAME);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void storeRootOrganization() throws Exception {

        rootOrgId = generateUniqueID();
        storeOrganization(rootOrgId, "ROOT", "This is the ROOT organization.", null);
    }

    private void storeChildOrganization(String parentId) throws Exception {

        orgId = generateUniqueID();
        storeOrganization(orgId, "XYZ builders", "This is a construction company.", parentId);
        storeOrganizationAttributes(orgId);
    }

    private void storeOrganization(String id, String name, String description, String parentId)
            throws Exception {

        try (Connection connection = getConnection()) {
            String sql = "INSERT INTO UM_ORG (UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, " +
                    "UM_LAST_MODIFIED, UM_TENANT_ID, UM_PARENT_ID, UM_ORG_TYPE) VALUES ( ?, ?, " +
                    "?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, id);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setTimestamp(4, Timestamp.from(Instant.now()), CALENDAR);
            statement.setTimestamp(5, Timestamp.from(Instant.now()), CALENDAR);
            statement.setInt(6, TENANT_ID);
            statement.setString(7, parentId);
            statement.setString(8, STRUCTURAL.toString());
            statement.execute();
        }
    }

    private void storeOrganizationAttributes(String id) throws Exception {

        try (Connection connection = getConnection()) {
            String sql = "INSERT INTO UM_ORG_ATTRIBUTE (UM_ORG_ID, UM_ATTRIBUTE_KEY, UM_ATTRIBUTE_VALUE) VALUES " +
                    "( ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, id);
            statement.setString(2, ATTRIBUTE_KEY);
            statement.setString(3, ATTRIBUTE_VALUE);
            statement.execute();
        }
    }
}

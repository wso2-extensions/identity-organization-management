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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.dao.impl;

import org.h2.jdbcx.JdbcDataSource;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.dao.ConnectionAssociationDAO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mockStatic;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_AFTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_BEFORE;

/**
 * H2-backed unit tests for {@link ConnectionAssociationDAOImpl}. Each DAO method is exercised against a real
 * in-memory database (the {@code IDN_ORG_CONNECTION_ASSOCIATION} table) so the SQL statements, parameter binding and
 * result mapping are all covered. The static database accessors ({@code IdentityDatabaseUtil}) are redirected to the
 * H2 datasource, and the table is truncated after each test for isolation.
 */
public class ConnectionAssociationDAOImplTest {

    // The UUID columns are CHAR(36); values must be exactly 36 characters as they are in production (real UUIDs),
    // otherwise the fixed-width column would pad shorter values with trailing spaces.
    private static final String RESOURCE_TYPE = ResourceType.CONNECTION_IDENTITY_PROVIDER.name();
    private static final String PARENT_CONNECTION_ID = "11111111-1111-1111-1111-111111111111";
    private static final String RESIDENT_ORG_ID = "22222222-2222-2222-2222-222222222222";
    private static final String SHARED_ORG_ID_1 = "33333333-3333-3333-3333-333333333331";
    private static final String SHARED_ORG_ID_2 = "33333333-3333-3333-3333-333333333332";
    private static final String SHARED_ORG_ID_3 = "33333333-3333-3333-3333-333333333333";
    private static final String SHARED_CONNECTION_ID_1 = "44444444-4444-4444-4444-444444444441";
    private static final String SHARED_CONNECTION_ID_2 = "44444444-4444-4444-4444-444444444442";
    private static final String SHARED_CONNECTION_ID_3 = "44444444-4444-4444-4444-444444444443";
    private static final String OTHER_PARENT_CONNECTION_ID = "55555555-5555-5555-5555-555555555555";
    private static final String OTHER_RESIDENT_ORG_ID = "66666666-6666-6666-6666-666666666666";
    private static final String OWNED_PARENT_CONNECTION_ID = "77777777-7777-7777-7777-777777777777";
    private static final String ORG_TO_DELETE = "88888888-8888-8888-8888-888888888888";
    private static final String MISSING_SHADOW_UUID = "99999999-9999-9999-9999-999999999999";

    private JdbcDataSource dataSource;
    private ConnectionAssociationDAO connectionAssociationDAO;
    private MockedStatic<IdentityDatabaseUtil> mockedIdentityDatabaseUtil;

    @BeforeClass
    public void setUpClass() throws Exception {

        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:connection_association_test_db;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("RUNSCRIPT FROM '" + getSchemaScriptPath() + "'");
        }
        connectionAssociationDAO = new ConnectionAssociationDAOImpl();
    }

    @AfterClass
    public void tearDownClass() throws Exception {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP ALL OBJECTS");
        }
    }

    @BeforeMethod
    public void setUp() {

        mockedIdentityDatabaseUtil = mockStatic(IdentityDatabaseUtil.class);
        mockedIdentityDatabaseUtil.when(IdentityDatabaseUtil::getDataSource).thenReturn(dataSource);
        mockedIdentityDatabaseUtil.when(() -> IdentityDatabaseUtil.getDBConnection(false))
                .thenAnswer(invocation -> dataSource.getConnection());
    }

    @AfterMethod
    public void tearDown() throws Exception {

        mockedIdentityDatabaseUtil.close();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM IDN_ORG_CONNECTION_ASSOCIATION");
        }
    }

    @Test
    public void testAddConnectionAssociationAndGetSharedConnectionId() throws Exception {

        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, SHARED_ORG_ID_1, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));

        Optional<String> sharedConnectionId = connectionAssociationDAO.getSharedConnectionId(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, SHARED_ORG_ID_1);
        Assert.assertTrue(sharedConnectionId.isPresent());
        Assert.assertEquals(sharedConnectionId.get(), SHARED_CONNECTION_ID_1);

        // The connection is not shared with a different organization.
        Optional<String> notShared = connectionAssociationDAO.getSharedConnectionId(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, SHARED_ORG_ID_2);
        Assert.assertFalse(notShared.isPresent());
    }

    @Test
    public void testGetConnectionAssociationBySharedConnectionId() throws Exception {

        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, SHARED_ORG_ID_1, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));

        Optional<ConnectionAssociation> result = connectionAssociationDAO
                .getConnectionAssociationBySharedConnectionId(RESOURCE_TYPE, SHARED_CONNECTION_ID_1);
        Assert.assertTrue(result.isPresent());
        ConnectionAssociation association = result.get();
        Assert.assertEquals(association.getResourceType(), ResourceType.CONNECTION_IDENTITY_PROVIDER);
        Assert.assertEquals(association.getSharedConnectionId(), SHARED_CONNECTION_ID_1);
        Assert.assertEquals(association.getOrganizationId(), SHARED_ORG_ID_1);
        Assert.assertEquals(association.getParentConnectionId(), PARENT_CONNECTION_ID);
        Assert.assertEquals(association.getConnectionResidentOrganizationId(), RESIDENT_ORG_ID);

        Optional<ConnectionAssociation> notFound = connectionAssociationDAO
                .getConnectionAssociationBySharedConnectionId(RESOURCE_TYPE, MISSING_SHADOW_UUID);
        Assert.assertFalse(notFound.isPresent());
    }

    @Test
    public void testGetConnectionAssociationsByParent() throws Exception {

        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, SHARED_ORG_ID_1, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_2, SHARED_ORG_ID_2, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        // A different parent connection that should not be returned.
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_3, SHARED_ORG_ID_3, OTHER_PARENT_CONNECTION_ID, RESIDENT_ORG_ID));

        List<ConnectionAssociation> associations = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID);
        Assert.assertEquals(associations.size(), 2);
    }

    @Test
    public void testGetConnectionAssociationsByResidentOrg() throws Exception {

        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, SHARED_ORG_ID_1, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_2, SHARED_ORG_ID_2, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        // Owned by a different resident organization.
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_3, SHARED_ORG_ID_3, OTHER_PARENT_CONNECTION_ID,
                        OTHER_RESIDENT_ORG_ID));

        List<ConnectionAssociation> associations =
                connectionAssociationDAO.getConnectionAssociationsByResidentOrg(RESIDENT_ORG_ID);
        Assert.assertEquals(associations.size(), 2);
        associations.forEach(a -> Assert.assertEquals(a.getConnectionResidentOrganizationId(), RESIDENT_ORG_ID));
    }

    @Test
    public void testGetConnectionAssociationsBySharedOrg() throws Exception {

        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, SHARED_ORG_ID_1, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        // Held by a different shared organization.
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_2, SHARED_ORG_ID_2, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));

        List<ConnectionAssociation> associations =
                connectionAssociationDAO.getConnectionAssociationsBySharedOrg(SHARED_ORG_ID_1);
        Assert.assertEquals(associations.size(), 1);
        Assert.assertEquals(associations.get(0).getOrganizationId(), SHARED_ORG_ID_1);
        Assert.assertEquals(associations.get(0).getSharedConnectionId(), SHARED_CONNECTION_ID_1);
    }

    @Test
    public void testGetConnectionAssociationsWithFilteringAndPagination() throws Exception {

        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, SHARED_ORG_ID_1, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_2, SHARED_ORG_ID_2, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_3, SHARED_ORG_ID_3, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));

        List<String> allSharedOrgIds = Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, SHARED_ORG_ID_3);

        // No limit returns all matches within the shared organization scope.
        List<ConnectionAssociation> all = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, allSharedOrgIds, Collections.emptyList(), "ASC", 0);
        Assert.assertEquals(all.size(), 3);

        // The limit caps the number of returned records.
        List<ConnectionAssociation> limited = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, allSharedOrgIds, Collections.emptyList(), "ASC", 2);
        Assert.assertEquals(limited.size(), 2);

        // Only the requested shared organization is included in the scope.
        List<ConnectionAssociation> scoped = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, Collections.singletonList(SHARED_ORG_ID_1),
                Collections.emptyList(), "ASC", 0);
        Assert.assertEquals(scoped.size(), 1);
        Assert.assertEquals(scoped.get(0).getOrganizationId(), SHARED_ORG_ID_1);

        // An empty shared organization scope short-circuits to an empty list.
        List<ConnectionAssociation> empty = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, Collections.emptyList(), Collections.emptyList(), "ASC", 0);
        Assert.assertTrue(empty.isEmpty());
    }

    @Test
    public void testDeleteConnectionAssociation() throws Exception {

        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, SHARED_ORG_ID_1, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_2, SHARED_ORG_ID_2, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));

        connectionAssociationDAO.deleteConnectionAssociation(RESOURCE_TYPE, PARENT_CONNECTION_ID, RESIDENT_ORG_ID,
                SHARED_ORG_ID_1);

        Assert.assertFalse(connectionAssociationDAO.getSharedConnectionId(RESOURCE_TYPE, PARENT_CONNECTION_ID,
                RESIDENT_ORG_ID, SHARED_ORG_ID_1).isPresent());
        // The other association is untouched.
        Assert.assertTrue(connectionAssociationDAO.getSharedConnectionId(RESOURCE_TYPE, PARENT_CONNECTION_ID,
                RESIDENT_ORG_ID, SHARED_ORG_ID_2).isPresent());
    }

    @Test
    public void testDeleteConnectionAssociationsByOrganizationId() throws Exception {

        String organizationId = ORG_TO_DELETE;
        // The organization holds a shadow (SHARED_ORG_ID).
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, organizationId, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        // The organization owns a connection shared elsewhere (ASSOCIATED_ORG_ID).
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_2, SHARED_ORG_ID_2, OWNED_PARENT_CONNECTION_ID, organizationId));
        // An unrelated association that must survive.
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_3, SHARED_ORG_ID_3, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));

        connectionAssociationDAO.deleteConnectionAssociationsByOrganizationId(organizationId);

        Assert.assertTrue(connectionAssociationDAO.getConnectionAssociationsBySharedOrg(organizationId).isEmpty());
        Assert.assertTrue(connectionAssociationDAO.getConnectionAssociationsByResidentOrg(organizationId).isEmpty());
        // The unrelated association remains.
        Assert.assertEquals(connectionAssociationDAO.getConnectionAssociationsBySharedOrg(SHARED_ORG_ID_3).size(), 1);
    }

    @Test
    public void testFilterBySharedOrganizationId() throws Exception {

        seedThreeAssociations();
        List<String> scope = Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, SHARED_ORG_ID_3);

        // ORGANIZATION_ID_FIELD narrows the shared-organization scope to a single organization.
        ExpressionNode orgFilter = expressionNode(ORGANIZATION_ID_FIELD, SHARED_ORG_ID_2);
        List<ConnectionAssociation> result = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, Collections.singletonList(orgFilter), "ASC", 0);

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getOrganizationId(), SHARED_ORG_ID_2);
    }

    @Test
    public void testFilterByPaginationAfterCursor() throws Exception {

        seedThreeAssociations();
        List<String> scope = Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, SHARED_ORG_ID_3);
        List<ConnectionAssociation> ordered = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, Collections.emptyList(), "ASC", 0);
        int middleId = ordered.get(1).getId();

        // PAGINATION_AFTER keeps only the records with an ID strictly less than the cursor.
        ExpressionNode after = expressionNode(PAGINATION_AFTER, String.valueOf(middleId));
        List<ConnectionAssociation> result = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, Collections.singletonList(after), "ASC", 0);

        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.get(0).getId() < middleId);
    }

    @Test
    public void testFilterByPaginationBeforeCursor() throws Exception {

        seedThreeAssociations();
        List<String> scope = Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, SHARED_ORG_ID_3);
        List<ConnectionAssociation> ordered = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, Collections.emptyList(), "ASC", 0);
        int middleId = ordered.get(1).getId();

        // PAGINATION_BEFORE keeps only the records with an ID strictly greater than the cursor.
        ExpressionNode before = expressionNode(PAGINATION_BEFORE, String.valueOf(middleId));
        List<ConnectionAssociation> result = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, Collections.singletonList(before), "ASC", 0);

        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.get(0).getId() > middleId);
    }

    @Test
    public void testMultipleExpressionNodesAreCombinedWithAnd() throws Exception {

        seedThreeAssociations();
        List<String> scope = Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, SHARED_ORG_ID_3);
        List<ConnectionAssociation> ordered = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, Collections.emptyList(), "ASC", 0);
        int firstId = ordered.get(0).getId();

        // (ID > firstId) AND (SHARED_ORG_ID = ORG_3) => only the third association qualifies.
        List<ExpressionNode> nodes = Arrays.asList(
                expressionNode(PAGINATION_BEFORE, String.valueOf(firstId)),
                expressionNode(ORGANIZATION_ID_FIELD, SHARED_ORG_ID_3));
        List<ConnectionAssociation> result = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, nodes, "ASC", 0);

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getOrganizationId(), SHARED_ORG_ID_3);
    }

    @Test
    public void testUnrecognizedAndBlankExpressionNodesAreIgnored() throws Exception {

        seedThreeAssociations();
        List<String> scope = Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, SHARED_ORG_ID_3);

        // An unknown attribute, a blank value and a blank attribute all contribute no filter condition.
        List<ExpressionNode> nodes = Arrays.asList(
                expressionNode("unknownAttribute", "someValue"),
                expressionNode(ORGANIZATION_ID_FIELD, ""),
                expressionNode("", "someValue"));
        List<ConnectionAssociation> result = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, nodes, "ASC", 0);

        Assert.assertEquals(result.size(), 3);
    }

    @Test
    public void testSortOrderAscendingAndDescending() throws Exception {

        seedThreeAssociations();
        List<String> scope = Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, SHARED_ORG_ID_3);

        List<ConnectionAssociation> ascending = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, Collections.emptyList(), "ASC", 0);
        List<ConnectionAssociation> descending = connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE,
                PARENT_CONNECTION_ID, RESIDENT_ORG_ID, scope, Collections.emptyList(), "DESC", 0);

        Assert.assertEquals(ascending.size(), 3);
        Assert.assertEquals(descending.size(), 3);
        Assert.assertTrue(ascending.get(0).getId() < ascending.get(1).getId());
        Assert.assertTrue(ascending.get(1).getId() < ascending.get(2).getId());
        Assert.assertTrue(descending.get(0).getId() > descending.get(1).getId());
        Assert.assertTrue(descending.get(1).getId() > descending.get(2).getId());
    }

    private void seedThreeAssociations() throws Exception {

        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_1, SHARED_ORG_ID_1, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_2, SHARED_ORG_ID_2, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
        connectionAssociationDAO.addConnectionAssociation(
                association(SHARED_CONNECTION_ID_3, SHARED_ORG_ID_3, PARENT_CONNECTION_ID, RESIDENT_ORG_ID));
    }

    private ExpressionNode expressionNode(String attribute, String value) {

        ExpressionNode expressionNode = new ExpressionNode();
        expressionNode.setAttributeValue(attribute);
        expressionNode.setValue(value);
        return expressionNode;
    }

    private ConnectionAssociation association(String sharedConnectionId, String sharedOrgId, String parentConnectionId,
                                             String residentOrgId) {

        return new ConnectionAssociation.Builder()
                .resourceType(ResourceType.CONNECTION_IDENTITY_PROVIDER)
                .sharedConnectionId(sharedConnectionId)
                .organizationId(sharedOrgId)
                .parentConnectionId(parentConnectionId)
                .connectionResidentOrganizationId(residentOrgId)
                .build();
    }

    private String getSchemaScriptPath() {

        return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "dbscripts", "h2.sql").toString();
    }
}

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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.dao;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.TestUtils;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_DB2;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_DEFAULT;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_MYSQL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_POSTGRESQL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Unit tests for OrganizationUserSharingDAOImpl.
 */
public class OrganizationUserSharingDAOImplTest {

    private OrganizationUserSharingDAOImpl organizationUserSharingDAO;

    private static final String TEST_ORG_ID = "c524c30a-cbd4-4169-ac9d-1ee3edf1bf16";
    private static final String TEST_ORG_ID_2 = "d634d40b-dce5-5270-be0e-2ff4fef2cf27";
    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_USER_ID_2 = "660e8400-e29b-41d4-a716-446655440001";
    private static final String TEST_ASSOCIATED_ORG_ID = "770e8400-e29b-41d4-a716-446655440002";

    @BeforeClass
    public void setup() throws Exception {

        TestUtils.initiateH2Base();
        TestUtils.mockDataSource();
        organizationUserSharingDAO = new OrganizationUserSharingDAOImpl();
    }

    @AfterClass
    public void tearDown() throws Exception {

        TestUtils.closeH2Base();
    }

    /**
     * Resets the static defaultDbProductType cache via reflection so tests can exercise both the
     * uncached and cached resolution paths of resolveDefaultDbProductType().
     */
    private void resetDefaultDbProductTypeCache() throws Exception {

        Field field = OrganizationUserSharingDAOImpl.class.getDeclaredField("defaultDbProductType");
        field.setAccessible(true);
        field.set(null, null);
    }

    /**
     * Creates a DAO backed by the shared H2 datasource but with the given DB-type string hard-wired,
     * allowing dialect-specific SQL branches to be exercised without a matching real database.
     */
    private OrganizationUserSharingDAOImpl createCustomDao(String dbType) {

        NamedJdbcTemplate h2Template = new NamedJdbcTemplate(TestUtils.dataSourceMap.get(TestUtils.DB_NAME));
        return new OrganizationUserSharingDAOImpl(() -> h2Template, () -> dbType);
    }

    /**
     * Test deleteUserAssociationsByOrganizationId method for successful deletion with real database.
     */
    @Test
    public void testDeleteUserAssociationsByOrganizationId() throws Exception {

        // Create associations for TEST_ORG_ID (two users) and TEST_ORG_ID_2 (one user).
        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID_2, TEST_ORG_ID, TEST_USER_ID_2, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID_2, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);

        // Verify the associations exist before deletion.
        assertNotNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID, TEST_ORG_ID));
        assertNotNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID_2, TEST_ORG_ID));
        assertNotNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID, TEST_ORG_ID_2));

        // Delete all associations for TEST_ORG_ID.
        boolean result = organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);

        // Verify the deletion returned true.
        assertTrue(result);

        // Verify associations for TEST_ORG_ID are deleted.
        assertNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID, TEST_ORG_ID));
        assertNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID_2, TEST_ORG_ID));

        // Verify associations for TEST_ORG_ID_2 are not affected.
        assertNotNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID, TEST_ORG_ID_2));

        // Cleanup remaining test data.
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID_2);
    }

    /**
     * Test that the two-arg constructor stores the provided suppliers and routes all association
     * CRUD through them, covering the custom-constructor body and getAssociationTemplate().
     */
    @Test
    public void testCustomConstructorUsesProvidedSuppliers() throws Exception {

        OrganizationUserSharingDAOImpl customDao = createCustomDao(DB_TYPE_DEFAULT);

        // Create and retrieve via the custom DAO to exercise the supplied template.
        customDao.createOrganizationUserAssociation(TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID,
                TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        UserAssociation association = customDao.getUserAssociation(TEST_USER_ID, TEST_ORG_ID);

        assertNotNull(association);
        assertEquals(association.getUserId(), TEST_USER_ID);
        assertEquals(association.getOrganizationId(), TEST_ORG_ID);
        customDao.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test that getAssociationDbProductType() wraps an unexpected exception in
     * OrganizationManagementServerException.
     */
    @Test(expectedExceptions = OrganizationManagementServerException.class)
    public void testGetAssociationDbProductTypeWrapsGenericException() throws Exception {

        OrganizationUserSharingDAOImpl dao = new OrganizationUserSharingDAOImpl(
                () -> new NamedJdbcTemplate(TestUtils.dataSourceMap.get(TestUtils.DB_NAME)),
                () -> {
                    throw new RuntimeException("simulated failure");
                }
        );
        // hasUserAssociations calls getDBSpecificQuery which calls getAssociationDbProductType.
        dao.hasUserAssociations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID);
    }

    /**
     * Test that getAssociationDbProductType() rethrows OrganizationManagementServerException as-is.
     */
    @Test(expectedExceptions = OrganizationManagementServerException.class)
    public void testGetAssociationDbProductTypeRethrowsServerException() throws Exception {

        OrganizationManagementServerException serverException =
                handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS,
                        new RuntimeException("original cause"));
        OrganizationUserSharingDAOImpl dao = new OrganizationUserSharingDAOImpl(
                () -> new NamedJdbcTemplate(TestUtils.dataSourceMap.get(TestUtils.DB_NAME)),
                () -> {
                    throw serverException;
                }
        );
        dao.hasUserAssociations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID);
    }

    /**
     * Test that hasUserAssociations returns true when an association exists and exercises both the
     * uncached (first call) and cached (second call) paths of resolveDefaultDbProductType().
     */
    @Test
    public void testHasUserAssociations_ReturnsTrueWhenAssociationExists() throws Exception {

        // Reset the static cache so the full resolution path is executed on the first call.
        resetDefaultDbProductTypeCache();
        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);

        // First call: resolveDefaultDbProductType computes and caches the value (un-cached path).
        assertTrue(organizationUserSharingDAO.hasUserAssociations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID));

        // Second call: resolveDefaultDbProductType returns the cached value (cache-hit path).
        assertTrue(organizationUserSharingDAO.hasUserAssociations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID));
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test that hasUserAssociations returns false when no association exists.
     */
    @Test
    public void testHasUserAssociations_ReturnsFalseWhenNoAssociationExists() throws Exception {

        assertFalse(organizationUserSharingDAO.hasUserAssociations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID));
    }

    /**
     * Test that hasUserAssociations selects the MySQL-specific query via getDBSpecificQuery.
     */
    @Test
    public void testHasUserAssociations_WithMysqlDialect() throws Exception {

        OrganizationUserSharingDAOImpl mysqlDao = createCustomDao(DB_TYPE_MYSQL);
        mysqlDao.createOrganizationUserAssociation(TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID,
                TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        assertTrue(mysqlDao.hasUserAssociations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID));
        mysqlDao.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test that hasUserAssociations selects the PostgreSQL-specific query via getDBSpecificQuery.
     */
    @Test
    public void testHasUserAssociations_WithPostgresqlDialect() throws Exception {

        OrganizationUserSharingDAOImpl postgresDao = createCustomDao(DB_TYPE_POSTGRESQL);
        postgresDao.createOrganizationUserAssociation(TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID,
                TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        assertTrue(postgresDao.hasUserAssociations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID));
        postgresDao.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test that hasUserAssociations selects the MSSQL-specific query via getDBSpecificQuery.
     * H2 can execute the CASE WHEN EXISTS ... END syntax without a FROM clause.
     */
    @Test
    public void testHasUserAssociations_WithMssqlDialect() throws Exception {

        OrganizationUserSharingDAOImpl mssqlDao = createCustomDao(DB_TYPE_MSSQL);
        mssqlDao.createOrganizationUserAssociation(TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID,
                TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        assertTrue(mssqlDao.hasUserAssociations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID));
        mssqlDao.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test that hasUserAssociationsInOrganizations returns true when the association's org is in scope.
     */
    @Test
    public void testHasUserAssociationsInOrganizations_ReturnsTrueWhenInScope() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<String> orgIds = Arrays.asList(TEST_ORG_ID);
        assertTrue(organizationUserSharingDAO.hasUserAssociationsInOrganizations(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, orgIds));
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test that hasUserAssociationsInOrganizations returns false immediately for an empty org list.
     */
    @Test
    public void testHasUserAssociationsInOrganizations_ReturnsFalseForEmptyOrgList() throws Exception {

        assertFalse(organizationUserSharingDAO.hasUserAssociationsInOrganizations(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, Collections.emptyList()));
    }

    /**
     * Test that hasUserAssociationsInOrganizations uses the MSSQL-specific query via getDBSpecificQuery.
     */
    @Test
    public void testHasUserAssociationsInOrganizations_WithMssqlDialect() throws Exception {

        OrganizationUserSharingDAOImpl mssqlDao = createCustomDao(DB_TYPE_MSSQL);
        mssqlDao.createOrganizationUserAssociation(TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID,
                TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        assertTrue(mssqlDao.hasUserAssociationsInOrganizations(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID,
                Arrays.asList(TEST_ORG_ID)));
        mssqlDao.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test getUserAssociationsOfAssociatedUser (simple overload) returns all associations for a user.
     */
    @Test
    public void testGetUserAssociationsOfAssociatedUser_Simple() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> associations =
                organizationUserSharingDAO.getUserAssociationsOfAssociatedUser(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID);
        assertNotNull(associations);
        assertFalse(associations.isEmpty());
        assertEquals(associations.get(0).getUserId(), TEST_USER_ID);
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test getUserAssociationsOfAssociatedUser filtered by SharedType.
     */
    @Test
    public void testGetUserAssociationsOfAssociatedUser_BySharedType() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> associations = organizationUserSharingDAO.getUserAssociationsOfAssociatedUser(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        assertNotNull(associations);
        assertFalse(associations.isEmpty());
        assertEquals(associations.get(0).getSharedType(), SharedType.SHARED);
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test that the filtered getUserAssociationsOfAssociatedUser returns empty list immediately when
     * the org scope is empty.
     */
    @Test
    public void testGetUserAssociationsOfAssociatedUserFiltered_EmptyScope() throws Exception {

        List<UserAssociation> result = organizationUserSharingDAO.getUserAssociationsOfAssociatedUser(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, Collections.emptyList(), Collections.emptyList(), "ASC", 0);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test filtered getUserAssociationsOfAssociatedUser with limit=0 (no-limit SQL tail).
     */
    @Test
    public void testGetUserAssociationsOfAssociatedUserFiltered_NoLimit() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> result = organizationUserSharingDAO.getUserAssociationsOfAssociatedUser(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, Arrays.asList(TEST_ORG_ID), Collections.emptyList(), "ASC", 0);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(result.get(0).getUserId(), TEST_USER_ID);
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test filtered getUserAssociationsOfAssociatedUser with limit > 0 and the default SQL dialect
     * (exercises getUserAssociationsByFilteringTailWithLimit default/LIMIT branch).
     */
    @Test
    public void testGetUserAssociationsOfAssociatedUserFiltered_WithLimitDefaultDialect() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> result = organizationUserSharingDAO.getUserAssociationsOfAssociatedUser(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, Arrays.asList(TEST_ORG_ID), Collections.emptyList(), "ASC", 5);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test filtered getUserAssociationsOfAssociatedUser with limit > 0 and Oracle dialect.
     * H2 supports FETCH FIRST n ROWS ONLY, covering the Oracle branch of
     * getUserAssociationsByFilteringTailWithLimit.
     */
    @Test
    public void testGetUserAssociationsOfAssociatedUserFiltered_WithLimitOracleDialect() throws Exception {

        OrganizationUserSharingDAOImpl oracleDao = createCustomDao(DB_TYPE_ORACLE);
        oracleDao.createOrganizationUserAssociation(TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID,
                TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> result = oracleDao.getUserAssociationsOfAssociatedUser(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, Arrays.asList(TEST_ORG_ID), Collections.emptyList(), "ASC", 5);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        oracleDao.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test filtered getUserAssociationsOfAssociatedUser with limit > 0 and MSSQL dialect.
     * H2 supports OFFSET ... ROWS FETCH NEXT ... ROWS ONLY, covering the MSSQL branch of
     * getUserAssociationsByFilteringTailWithLimit.
     */
    @Test
    public void testGetUserAssociationsOfAssociatedUserFiltered_WithLimitMssqlDialect() throws Exception {

        OrganizationUserSharingDAOImpl mssqlDao = createCustomDao(DB_TYPE_MSSQL);
        mssqlDao.createOrganizationUserAssociation(TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID,
                TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> result = mssqlDao.getUserAssociationsOfAssociatedUser(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, Arrays.asList(TEST_ORG_ID), Collections.emptyList(), "ASC", 5);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        mssqlDao.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test filtered getUserAssociationsOfAssociatedUser with limit > 0 and DB2 dialect.
     * H2 supports FETCH FIRST n ROWS ONLY, covering the DB2 branch of
     * getUserAssociationsByFilteringTailWithLimit.
     */
    @Test
    public void testGetUserAssociationsOfAssociatedUserFiltered_WithLimitDb2Dialect() throws Exception {

        OrganizationUserSharingDAOImpl db2Dao = createCustomDao(DB_TYPE_DB2);
        db2Dao.createOrganizationUserAssociation(TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID,
                TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> result = db2Dao.getUserAssociationsOfAssociatedUser(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, Arrays.asList(TEST_ORG_ID), Collections.emptyList(), "ASC", 5);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        db2Dao.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test deleteUserAssociationOfUserByAssociatedOrg removes the correct association.
     */
    @Test
    public void testDeleteUserAssociationOfUserByAssociatedOrg() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        assertNotNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID, TEST_ORG_ID));
        boolean result = organizationUserSharingDAO.deleteUserAssociationOfUserByAssociatedOrg(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID);
        assertTrue(result);
        assertNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID, TEST_ORG_ID));
    }

    /**
     * Test deleteUserAssociationsOfAssociatedUser removes all associations for a root user.
     */
    @Test
    public void testDeleteUserAssociationsOfAssociatedUser() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        assertNotNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID, TEST_ORG_ID));
        boolean result = organizationUserSharingDAO.deleteUserAssociationsOfAssociatedUser(
                TEST_USER_ID, TEST_ASSOCIATED_ORG_ID);
        assertTrue(result);
        assertNull(organizationUserSharingDAO.getUserAssociation(TEST_USER_ID, TEST_ORG_ID));
    }

    /**
     * Test getUserAssociationOfAssociatedUserByOrgId returns the association for the root user in a given org.
     */
    @Test
    public void testGetUserAssociationOfAssociatedUserByOrgId() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        UserAssociation association =
                organizationUserSharingDAO.getUserAssociationOfAssociatedUserByOrgId(TEST_USER_ID, TEST_ORG_ID);
        assertNotNull(association);
        assertEquals(association.getAssociatedUserId(), TEST_USER_ID);
        assertEquals(association.getOrganizationId(), TEST_ORG_ID);
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test getUserAssociationsOfGivenUserOnGivenOrgs returns an empty list when org list is empty.
     */
    @Test
    public void testGetUserAssociationsOfGivenUserOnGivenOrgs_EmptyOrgList() throws Exception {

        List<UserAssociation> result = organizationUserSharingDAO
                .getUserAssociationsOfGivenUserOnGivenOrgs(TEST_USER_ID, Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test getUserAssociationsOfGivenUserOnGivenOrgs returns associations scoped to the given org list.
     */
    @Test
    public void testGetUserAssociationsOfGivenUserOnGivenOrgs() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> result = organizationUserSharingDAO.getUserAssociationsOfGivenUserOnGivenOrgs(
                TEST_USER_ID, Arrays.asList(TEST_ORG_ID));
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(result.get(0).getOrganizationId(), TEST_ORG_ID);
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }

    /**
     * Test updateSharedTypeOfUserAssociation updates the shared type of an existing association.
     */
    @Test
    public void testUpdateSharedTypeOfUserAssociation() throws Exception {

        organizationUserSharingDAO.createOrganizationUserAssociation(
                TEST_USER_ID, TEST_ORG_ID, TEST_USER_ID, TEST_ASSOCIATED_ORG_ID, SharedType.SHARED);
        List<UserAssociation> associations =
                organizationUserSharingDAO.getUserAssociationsOfAssociatedUser(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID);
        assertFalse(associations.isEmpty());
        int id = associations.get(0).getId();
        assertEquals(associations.get(0).getSharedType(), SharedType.SHARED);
        organizationUserSharingDAO.updateSharedTypeOfUserAssociation(id, SharedType.INVITED);
        List<UserAssociation> updated =
                organizationUserSharingDAO.getUserAssociationsOfAssociatedUser(TEST_USER_ID, TEST_ASSOCIATED_ORG_ID);
        assertFalse(updated.isEmpty());
        assertEquals(updated.get(0).getSharedType(), SharedType.INVITED);
        organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }
}

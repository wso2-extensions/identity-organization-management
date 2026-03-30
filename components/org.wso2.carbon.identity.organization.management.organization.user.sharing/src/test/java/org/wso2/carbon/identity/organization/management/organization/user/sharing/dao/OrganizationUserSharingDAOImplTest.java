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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.TestUtils;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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
    }
}

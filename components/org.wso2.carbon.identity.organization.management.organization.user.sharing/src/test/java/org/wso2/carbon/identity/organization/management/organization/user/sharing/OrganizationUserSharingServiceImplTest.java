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

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for OrganizationUserSharingServiceImpl.
 */
public class OrganizationUserSharingServiceImplTest {

    @InjectMocks
    private OrganizationUserSharingServiceImpl organizationUserSharingService;

    @Mock
    private OrganizationUserSharingDAO organizationUserSharingDAO;

    private static final String TEST_ORG_ID = "c524c30a-cbd4-4169-ac9d-1ee3edf1bf16";

    @BeforeMethod
    public void setUp() throws Exception {

        openMocks(this);
        organizationUserSharingService = new OrganizationUserSharingServiceImpl();
        Field daoField = OrganizationUserSharingServiceImpl.class.getDeclaredField("organizationUserSharingDAO");
        daoField.setAccessible(true);
        daoField.set(organizationUserSharingService, organizationUserSharingDAO);
    }

    /**
     * Test deleteUserAssociationsByOrganizationId method for successful deletion.
     */
    @Test
    public void testDeleteUserAssociationsByOrganizationId() throws OrganizationManagementException {

        when(organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(anyString())).thenReturn(true);
        boolean result = organizationUserSharingService.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
        verify(organizationUserSharingDAO).deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
        assertTrue(result, "User associations should be deleted successfully.");
    }

    /**
     * Test deleteUserAssociationsByOrganizationId method when DAO throws exception.
     */
    @Test(expectedExceptions = OrganizationManagementException.class)
    public void testDeleteUserAssociationsByOrganizationIdWithException() throws OrganizationManagementException {

        // Mock DAO to throw exception.
        when(organizationUserSharingDAO.deleteUserAssociationsByOrganizationId(anyString()))
                .thenThrow(new OrganizationManagementServerException("Test exception."));
        organizationUserSharingService.deleteUserAssociationsByOrganizationId(TEST_ORG_ID);
    }
}

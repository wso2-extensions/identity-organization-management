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

package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_1_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_1_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_2_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_2_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_3_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_3_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_SUPER_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_1_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_2_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_3_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MESSAGE_RESPONSE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MESSAGE_RESPONSE_SHARED_ORGS;

public class UserSharingPolicyHandlerServiceImplTest {

    @InjectMocks
    private UserSharingPolicyHandlerServiceImpl userSharingPolicyHandlerService;

    private MockedStatic<UserSharingPolicyHandlerServiceImpl> userSharingPolicyHandlerServiceMock;

    @BeforeMethod
    public void setUp() {

        openMocks(this);
        userSharingPolicyHandlerServiceMock = mockStatic(UserSharingPolicyHandlerServiceImpl.class);
    }

    @AfterMethod
    public void tearDown() {

        userSharingPolicyHandlerServiceMock.close();
    }

    @DataProvider(name = "getSharedOrgsDataProvider")
    public Object[][] getSharedOrgsDataProvider() {

        return new Object[][]{
                {USER_1_ID, setExpectedResultsForGetSharedOrgsTestCase1()},
                {USER_2_ID, setExpectedResultsForGetSharedOrgsTestCase2()},
                {USER_3_ID, setExpectedResultsForGetSharedOrgsTestCase3()}
        };
    }

    @Test(dataProvider = "getSharedOrgsDataProvider")
    public void testGetSharedOrganizationsOfUser(String associatedUserId, Map<String, UserAssociation> expectedResults)
            throws Exception {

        try (MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class);
             MockedStatic<OrganizationUserSharingDataHolder> dataHolderMock = mockStatic(
                     OrganizationUserSharingDataHolder.class)) {

            // Mock getOrganizationId() from the Utils class
            utilsMockedStatic.when(Utils::getOrganizationId).thenReturn(ORG_SUPER_ID);

            // Mock the data holder and return the mock service
            OrganizationUserSharingDataHolder dataHolder = mock(OrganizationUserSharingDataHolder.class);
            when(OrganizationUserSharingDataHolder.getInstance()).thenReturn(dataHolder);
            OrganizationUserSharingService mockOrgUserSharingService = mock(OrganizationUserSharingService.class);
            when(dataHolder.getOrganizationUserSharingService()).thenReturn(mockOrgUserSharingService);
            OrganizationManager mockOrgManager = mock(OrganizationManager.class);
            when(dataHolder.getOrganizationManager()).thenReturn(mockOrgManager);

            // Mock organization names dynamically
            List<UserAssociation> mockUserAssociations = new ArrayList<>();
            for (Map.Entry<String, UserAssociation> entry : expectedResults.entrySet()) {
                String orgName = entry.getKey();
                UserAssociation userAssociation = entry.getValue();
                when(mockOrgManager.getOrganizationNameById(userAssociation.getOrganizationId())).thenReturn(orgName);
                mockUserAssociations.add(userAssociation);
            }

            when(mockOrgUserSharingService.getUserAssociationsOfGivenUser(associatedUserId, ORG_SUPER_ID))
                    .thenReturn(mockUserAssociations);

            // Call the method
            ResponseSharedOrgsDO response =
                    userSharingPolicyHandlerService.getSharedOrganizationsOfUser(associatedUserId, null, null, null,
                            null, false);

            // Validate response
            assertNotNull(response, VALIDATE_MESSAGE_RESPONSE);
            assertEquals(response.getSharedOrgs().size(), mockUserAssociations.size(),
                    VALIDATE_MESSAGE_RESPONSE_SHARED_ORGS);

            for (int i = 0; i < mockUserAssociations.size(); i++) {
                UserAssociation expectedAssociation = mockUserAssociations.get(i);
                assertEquals(response.getSharedOrgs().get(i).getOrganizationId(),
                        expectedAssociation.getOrganizationId());
                assertEquals(response.getSharedOrgs().get(i).getSharedUserId(), expectedAssociation.getUserId());
                assertEquals(response.getSharedOrgs().get(i).getSharedType(), SharedType.SHARED);
                assertTrue(expectedResults.containsKey(response.getSharedOrgs().get(i).getOrganizationName()));
            }
        }
    }

    @Test(expectedExceptions = UserSharingMgtClientException.class)
    public void testGetSharedOrganizationsOfUser_ExceptionHandling() throws Exception {

        try (MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class);
             MockedStatic<OrganizationUserSharingDataHolder> dataHolderMock = mockStatic(
                     OrganizationUserSharingDataHolder.class)) {

            // Mock getOrganizationId() from the Utils class
            utilsMockedStatic.when(Utils::getOrganizationId).thenReturn(ORG_SUPER_ID);

            // Mock the data holder and return the mock service
            OrganizationUserSharingDataHolder dataHolder = mock(OrganizationUserSharingDataHolder.class);
            when(OrganizationUserSharingDataHolder.getInstance()).thenReturn(dataHolder);
            OrganizationUserSharingService mockOrgUserSharingService = mock(OrganizationUserSharingService.class);
            when(dataHolder.getOrganizationUserSharingService()).thenReturn(mockOrgUserSharingService);

            // Force an exception when calling getUserAssociationsOfGivenUser
            when(mockOrgUserSharingService.getUserAssociationsOfGivenUser(anyString(), anyString()))
                    .thenThrow(new OrganizationManagementException("Simulated Exception"));

            // Invoke the method (expected to throw an exception)
            userSharingPolicyHandlerService.getSharedOrganizationsOfUser(USER_1_ID, null, null, null, null, false);
        }
    }

    // Test case Builders.

    private Map<String, UserAssociation> setExpectedResultsForGetSharedOrgsTestCase1() {

        Map<String, UserAssociation> expectedResults = new HashMap<>();
        expectedResults.put(ORG_1_NAME, createUserAssociation(USER_1_ID, ORG_1_ID));
        expectedResults.put(ORG_2_NAME, createUserAssociation(USER_1_ID, ORG_2_ID));
        expectedResults.put(ORG_3_NAME, createUserAssociation(USER_1_ID, ORG_3_ID));
        return expectedResults;
    }

    private Map<String, UserAssociation> setExpectedResultsForGetSharedOrgsTestCase2() {

        return new HashMap<>();
    }

    private Map<String, UserAssociation> setExpectedResultsForGetSharedOrgsTestCase3() {

        Map<String, UserAssociation> expectedResults = new HashMap<>();
        expectedResults.put(ORG_1_NAME, createUserAssociation(USER_3_ID, ORG_1_ID));
        return expectedResults;
    }

    // Helper Methods.

    private UserAssociation createUserAssociation(String userId, String organizationId) {

        UserAssociation userAssociation = new UserAssociation();
        userAssociation.setUserId(userId);
        userAssociation.setOrganizationId(organizationId);
        userAssociation.setSharedType(SharedType.SHARED);
        return userAssociation;
    }
}

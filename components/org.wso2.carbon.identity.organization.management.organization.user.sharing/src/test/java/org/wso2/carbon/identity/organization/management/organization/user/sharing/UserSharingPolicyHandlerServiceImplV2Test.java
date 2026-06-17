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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.RoleAssignmentMode;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleAssignmentDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareOrgDetailsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserCriteriaType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserIdList;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for UserSharingPolicyHandlerServiceImplV2.
 */
public class UserSharingPolicyHandlerServiceImplV2Test {

    private static final String USER_1_ID = "user-1-id";
    private static final String ORG_1_ID = "org-1-id";

    private UserSharingPolicyHandlerServiceImplV2 userSharingPolicyHandlerService;

    @BeforeMethod
    public void setUp() {

        userSharingPolicyHandlerService = new UserSharingPolicyHandlerServiceImplV2();
    }

    @DataProvider(name = "sharedUserResolutionUsernameDataProvider")
    public Object[][] sharedUserResolutionUsernameDataProvider() {

        return new Object[][]{
                {"bob", "bob"},
                {"DEFAULT/bob", "bob"},
                {"SOME_SECONDARY_USER_STORE/bob", "bob"}
        };
    }

    @Test(dataProvider = "sharedUserResolutionUsernameDataProvider")
    public void testGetDomainFreeUsernameForSharedUserResolution(String originalUsername, String expectedUsername)
            throws Exception {

        Method method = UserSharingPolicyHandlerServiceImplV2.class.getDeclaredMethod(
                "getDomainFreeUsernameForSharedUserResolution", String.class);
        method.setAccessible(true);

        String actualUsername = (String) method.invoke(userSharingPolicyHandlerService, originalUsername);

        assertEquals(actualUsername, expectedUsername);
    }

    // Input Validation Tests.

    @DataProvider(name = "selectiveShareV2ValidationDataProvider")
    public Object[][] selectiveShareV2ValidationDataProvider() {

        return new Object[][]{
                {Collections.emptyList(), ORG_1_ID, "OUS-10070"},
                {Arrays.asList(USER_1_ID, null), ORG_1_ID, "OUS-10071"},
                {Collections.singletonList(""), ORG_1_ID, "OUS-10071"},
                {Collections.singletonList("   "), ORG_1_ID, "OUS-10071"},
                {Collections.singletonList(USER_1_ID), null, "OUS-10073"},
                {Collections.singletonList(USER_1_ID), "", "OUS-10072"},
                {Collections.singletonList(USER_1_ID), "   ", "OUS-10072"},
        };
    }

    @Test(dataProvider = "selectiveShareV2ValidationDataProvider")
    public void testValidateSelectiveUserShareV2DO(List<String> userIds, String orgId, String expectedErrorCode)
            throws Exception {

        SelectiveUserShareV2DO selectiveUserShareV2DO = new SelectiveUserShareV2DO();
        Map<String, UserCriteriaType> userCriteria = new HashMap<>();
        userCriteria.put("userIds", new UserIdList(userIds));
        selectiveUserShareV2DO.setUserCriteria(userCriteria);

        if (orgId == null) {
            selectiveUserShareV2DO.setOrganizations(new ArrayList<>());
        } else {
            SelectiveUserShareOrgDetailsV2DO orgDetails = new SelectiveUserShareOrgDetailsV2DO();
            orgDetails.setOrganizationId(orgId);
            orgDetails.setPolicy(PolicyEnum.SELECTED_ORG_ONLY);
            RoleAssignmentDO roleAssignment = new RoleAssignmentDO();
            roleAssignment.setMode(RoleAssignmentMode.NONE);
            orgDetails.setRoleAssignments(roleAssignment);
            selectiveUserShareV2DO.setOrganizations(Collections.singletonList(orgDetails));
        }

        Method method = UserSharingPolicyHandlerServiceImplV2.class.getDeclaredMethod(
                "validateSelectiveUserShareV2DO", SelectiveUserShareV2DO.class);
        method.setAccessible(true);

        try {
            method.invoke(userSharingPolicyHandlerService, selectiveUserShareV2DO);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof UserSharingMgtClientException,
                    "Expected UserSharingMgtClientException");
            UserSharingMgtClientException clientException = (UserSharingMgtClientException) e.getCause();
            assertEquals(clientException.getErrorCode(), expectedErrorCode,
                    "Expected error code " + expectedErrorCode);
            return;
        }
        throw new AssertionError("Expected UserSharingMgtClientException was not thrown.");
    }

    @DataProvider(name = "generalShareV2ValidationDataProvider")
    public Object[][] generalShareV2ValidationDataProvider() {

        return new Object[][]{
                {Collections.emptyList(), "OUS-10070"},
                {Arrays.asList(USER_1_ID, null), "OUS-10071"},
                {Collections.singletonList(""), "OUS-10071"},
                {Collections.singletonList("   "), "OUS-10071"},
        };
    }

    @Test(dataProvider = "generalShareV2ValidationDataProvider")
    public void testValidateGeneralUserShareV2DO(List<String> userIds, String expectedErrorCode) throws Exception {

        GeneralUserShareV2DO generalUserShareV2DO = new GeneralUserShareV2DO();
        Map<String, UserCriteriaType> userCriteria = new HashMap<>();
        userCriteria.put("userIds", new UserIdList(userIds));
        generalUserShareV2DO.setUserCriteria(userCriteria);
        generalUserShareV2DO.setPolicy(PolicyEnum.SELECTED_ORG_ONLY);
        RoleAssignmentDO roleAssignment = new RoleAssignmentDO();
        roleAssignment.setMode(RoleAssignmentMode.NONE);
        generalUserShareV2DO.setRoleAssignments(roleAssignment);

        Method method = UserSharingPolicyHandlerServiceImplV2.class.getDeclaredMethod(
                "validateGeneralUserShareV2DO", GeneralUserShareV2DO.class);
        method.setAccessible(true);

        try {
            method.invoke(userSharingPolicyHandlerService, generalUserShareV2DO);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof UserSharingMgtClientException,
                    "Expected UserSharingMgtClientException");
            UserSharingMgtClientException clientException = (UserSharingMgtClientException) e.getCause();
            assertEquals(clientException.getErrorCode(), expectedErrorCode,
                    "Expected error code " + expectedErrorCode);
            return;
        }
        throw new AssertionError("Expected UserSharingMgtClientException was not thrown.");
    }

    @DataProvider(name = "selectiveUnshareValidationDataProvider")
    public Object[][] selectiveUnshareValidationDataProvider() {

        return new Object[][]{
                {Collections.emptyList(), ORG_1_ID, "OUS-10070"},
                {Collections.singletonList(null), ORG_1_ID, "OUS-10071"},
                {Collections.singletonList(""), ORG_1_ID, "OUS-10071"},
                {Collections.singletonList(USER_1_ID), null, "OUS-10073"},
                {Collections.singletonList(USER_1_ID), "", "OUS-10072"},
                {Collections.singletonList(USER_1_ID), "   ", "OUS-10072"},
        };
    }

    @Test(dataProvider = "selectiveUnshareValidationDataProvider")
    public void testValidateSelectiveUserUnshareDO(List<String> userIds, String orgId, String expectedErrorCode)
            throws Exception {

        SelectiveUserUnshareDO selectiveUserUnshareDO = new SelectiveUserUnshareDO();
        Map<String, UserCriteriaType> userCriteria = new HashMap<>();
        userCriteria.put("userIds", new UserIdList(userIds));
        selectiveUserUnshareDO.setUserCriteria(userCriteria);

        if (orgId == null) {
            selectiveUserUnshareDO.setOrganizations(new ArrayList<>());
        } else {
            selectiveUserUnshareDO.setOrganizations(Collections.singletonList(orgId));
        }

        Method method = UserSharingPolicyHandlerServiceImplV2.class.getDeclaredMethod(
                "validateSelectiveUserUnshareDO", SelectiveUserUnshareDO.class);
        method.setAccessible(true);

        try {
            method.invoke(userSharingPolicyHandlerService, selectiveUserUnshareDO);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof UserSharingMgtClientException,
                    "Expected UserSharingMgtClientException");
            UserSharingMgtClientException clientException = (UserSharingMgtClientException) e.getCause();
            assertEquals(clientException.getErrorCode(), expectedErrorCode,
                    "Expected error code " + expectedErrorCode);
            return;
        }
        throw new AssertionError("Expected UserSharingMgtClientException was not thrown.");
    }

    @DataProvider(name = "generalUnshareValidationDataProvider")
    public Object[][] generalUnshareValidationDataProvider() {

        return new Object[][]{
                {Collections.emptyList(), "OUS-10070"},
                {Collections.singletonList(null), "OUS-10071"},
                {Collections.singletonList(""), "OUS-10071"},
                {Collections.singletonList("   "), "OUS-10071"},
        };
    }

    @Test(dataProvider = "generalUnshareValidationDataProvider")
    public void testValidateGeneralUserUnshareDO(List<String> userIds, String expectedErrorCode) throws Exception {

        GeneralUserUnshareDO generalUserUnshareDO = new GeneralUserUnshareDO();
        Map<String, UserCriteriaType> userCriteria = new HashMap<>();
        userCriteria.put("userIds", new UserIdList(userIds));
        generalUserUnshareDO.setUserCriteria(userCriteria);

        Method method = UserSharingPolicyHandlerServiceImplV2.class.getDeclaredMethod(
                "validateGeneralUserUnshareDO", GeneralUserUnshareDO.class);
        method.setAccessible(true);

        try {
            method.invoke(userSharingPolicyHandlerService, generalUserUnshareDO);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof UserSharingMgtClientException,
                    "Expected UserSharingMgtClientException");
            UserSharingMgtClientException clientException = (UserSharingMgtClientException) e.getCause();
            assertEquals(clientException.getErrorCode(), expectedErrorCode,
                    "Expected error code " + expectedErrorCode);
            return;
        }
        throw new AssertionError("Expected UserSharingMgtClientException was not thrown.");
    }
}

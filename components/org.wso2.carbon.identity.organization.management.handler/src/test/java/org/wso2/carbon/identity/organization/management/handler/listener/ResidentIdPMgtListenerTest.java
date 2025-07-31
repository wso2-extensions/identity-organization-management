/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler.listener;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test class for ResidentIdPMgtListener.
 */
public class ResidentIdPMgtListenerTest {

    private static final String TENANT_DOMAIN = "test.domain";
    private static final String MAIN_ROLE_ID_1 = "main-role-id-1";
    private static final String MAIN_ROLE_ID_2 = "main-role-id-2";
    private static final String SHARED_ROLE_ID_1 = "shared-role-id-1";
    private static final String SHARED_ROLE_ID_2 = "shared-role-id-2";
    private static final String PASSWORD_EXPIRY_RULE_PREFIX = "passwordExpiry.rule";
    public static final String ROLES_RULE_PREFIX = "1,30,roles,eq,";
    public static final String GROUP_RULE = "1,30,groups,eq,groupId123";
    public static final String OTHER_PROPERTY = "other.property";
    public static final String VALUE_1 = "value1";
    public static final String VALUE_2 = "value2";
    public static final String ANOTHER_PROPERTY = "another.property";
    public static final String RULE_1 = ".1";
    public static final String RULE_2 = ".2";

    @Mock
    private OrganizationManagementHandlerDataHolder dataHolder;

    @Mock
    private RoleManagementService roleManagementService;

    private ResidentIdPMgtListener listener;
    private MockedStatic<OrganizationManagementHandlerDataHolder> dataHolderMockedStatic;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        listener = new ResidentIdPMgtListener();

        dataHolderMockedStatic = mockStatic(OrganizationManagementHandlerDataHolder.class);
        dataHolderMockedStatic.when(OrganizationManagementHandlerDataHolder::getInstance)
                .thenReturn(dataHolder);
        when(dataHolder.getRoleManagementServiceV2()).thenReturn(roleManagementService);
    }

    @AfterMethod
    public void tearDown() {

        dataHolderMockedStatic.close();
    }

    @Test
    public void testGetDefaultOrderId() {

        assertEquals(listener.getDefaultOrderId(), 300);
    }

    @Test(description = "Test for doPostGetResidentIdP with no password expiry rules")
    public void testDoPostGetResidentIdPWithNoPasswordExpiryRules() throws Exception {

        IdentityProvider identityProvider = createIdentityProvider(new IdentityProviderProperty[]{
                createProperty(OTHER_PROPERTY, VALUE_1),
                createProperty(ANOTHER_PROPERTY, VALUE_2)
        });

        boolean result = listener.doPostGetResidentIdP(identityProvider, TENANT_DOMAIN);

        assertTrue(result);
        verify(roleManagementService, never()).getMainRoleToSharedRoleMappingsBySubOrg(anyList(), anyString());
    }

    @Test(description = "Test for doPostGetResidentIdP with group related password expiry rules")
    public void testDoPostGetResidentIdPWithNonRolePasswordExpiryRules() throws Exception {

        IdentityProviderProperty[] properties = {
                createProperty(PASSWORD_EXPIRY_RULE_PREFIX + RULE_1, GROUP_RULE),
                createProperty(OTHER_PROPERTY, VALUE_1)
        };

        IdentityProvider identityProvider = createIdentityProvider(properties);
        boolean result = listener.doPostGetResidentIdP(identityProvider, TENANT_DOMAIN);

        assertTrue(result);
        verify(roleManagementService, never()).getMainRoleToSharedRoleMappingsBySubOrg(anyList(), anyString());
    }

    @Test(description = "Test for doPostGetResidentIdP with roles that are shared with the sub-organization")
    public void testDoPostGetResidentIdPWithSharedRoleIds() throws Exception {

        IdentityProviderProperty[] properties = {
                createProperty(PASSWORD_EXPIRY_RULE_PREFIX + RULE_1, ROLES_RULE_PREFIX + MAIN_ROLE_ID_1),
                createProperty(PASSWORD_EXPIRY_RULE_PREFIX + RULE_2, ROLES_RULE_PREFIX + MAIN_ROLE_ID_2),
                createProperty(OTHER_PROPERTY, VALUE_1)
        };

        IdentityProvider identityProvider = createIdentityProvider(properties);
        Map<String, String> roleMapping = new HashMap<>();
        roleMapping.put(MAIN_ROLE_ID_1, SHARED_ROLE_ID_1);
        roleMapping.put(MAIN_ROLE_ID_2, SHARED_ROLE_ID_2);

        when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Arrays.asList(MAIN_ROLE_ID_1, MAIN_ROLE_ID_2), TENANT_DOMAIN))
                .thenReturn(roleMapping);
        boolean result = listener.doPostGetResidentIdP(identityProvider, TENANT_DOMAIN);

        assertTrue(result);
        IdentityProviderProperty[] updatedProperties = identityProvider.getIdpProperties();

        boolean foundUpdatedRule1 = false, foundUpdatedRule2 = false;
        for (IdentityProviderProperty property : updatedProperties) {
            if (property.getName().equals(PASSWORD_EXPIRY_RULE_PREFIX + RULE_1)) {
                assertEquals(property.getValue(), ROLES_RULE_PREFIX + SHARED_ROLE_ID_1);
                foundUpdatedRule1 = true;
            } else if (property.getName().equals(PASSWORD_EXPIRY_RULE_PREFIX + RULE_2)) {
                assertEquals(property.getValue(), ROLES_RULE_PREFIX + SHARED_ROLE_ID_2);
                foundUpdatedRule2 = true;
            }
        }

        assertTrue(foundUpdatedRule1);
        assertTrue(foundUpdatedRule2);
    }

    @Test(description = "Test for doPostGetResidentIdP with a shared role and a role that was created in the " +
            "sub-organization")
    public void testDoPostGetResidentIdPWithPartialSharedRoleMappings() throws Exception {

        IdentityProviderProperty[] properties = {
                createProperty(PASSWORD_EXPIRY_RULE_PREFIX + RULE_1, ROLES_RULE_PREFIX + MAIN_ROLE_ID_1),
                createProperty(PASSWORD_EXPIRY_RULE_PREFIX + RULE_2, ROLES_RULE_PREFIX + MAIN_ROLE_ID_2),
                createProperty(OTHER_PROPERTY, VALUE_1)
        };

        IdentityProvider identityProvider = createIdentityProvider(properties);
        Map<String, String> roleMapping = new HashMap<>();
        roleMapping.put(MAIN_ROLE_ID_1, SHARED_ROLE_ID_1);

        when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Arrays.asList(MAIN_ROLE_ID_1, MAIN_ROLE_ID_2), TENANT_DOMAIN))
                .thenReturn(roleMapping);
        when(roleManagementService.isExistingRole(MAIN_ROLE_ID_2, TENANT_DOMAIN)).thenReturn(true);
        boolean result = listener.doPostGetResidentIdP(identityProvider, TENANT_DOMAIN);

        assertTrue(result);
        IdentityProviderProperty[] updatedProperties = identityProvider.getIdpProperties();

        boolean foundUpdatedRule1 = false, foundOriginalRule2 = false;
        for (IdentityProviderProperty property : updatedProperties) {
            if (property.getName().equals(PASSWORD_EXPIRY_RULE_PREFIX + RULE_1)) {
                assertEquals(property.getValue(), ROLES_RULE_PREFIX + SHARED_ROLE_ID_1);
                foundUpdatedRule1 = true;
            } else if (property.getName().equals(PASSWORD_EXPIRY_RULE_PREFIX + RULE_2)) {
                assertEquals(property.getValue(), ROLES_RULE_PREFIX + MAIN_ROLE_ID_2);
                foundOriginalRule2 = true;
            }
        }

        assertTrue(foundUpdatedRule1);
        assertTrue(foundOriginalRule2);
    }

    @Test(description = "Test for doPostGetResidentIdP with a shared role and a role which was not shared")
    public void testDoPostGetResidentIdPWithNonSharedRole() throws Exception {

        IdentityProviderProperty[] properties = {
                createProperty(PASSWORD_EXPIRY_RULE_PREFIX + RULE_1, ROLES_RULE_PREFIX + MAIN_ROLE_ID_1),
                createProperty(PASSWORD_EXPIRY_RULE_PREFIX + RULE_2, ROLES_RULE_PREFIX + MAIN_ROLE_ID_2),
                createProperty(OTHER_PROPERTY, VALUE_1)
        };

        IdentityProvider identityProvider = createIdentityProvider(properties);
        Map<String, String> roleMapping = new HashMap<>();
        when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Arrays.asList(MAIN_ROLE_ID_1, MAIN_ROLE_ID_2), TENANT_DOMAIN))
                .thenReturn(roleMapping);
        when(roleManagementService.isExistingRole(MAIN_ROLE_ID_1, TENANT_DOMAIN)).thenReturn(false);
        when(roleManagementService.isExistingRole(MAIN_ROLE_ID_2, TENANT_DOMAIN)).thenReturn(true);
        boolean result = listener.doPostGetResidentIdP(identityProvider, TENANT_DOMAIN);

        assertTrue(result);
        IdentityProviderProperty[] updatedProperties = identityProvider.getIdpProperties();

        boolean foundRule1 = false, foundRule2 = false;
        for (IdentityProviderProperty property : updatedProperties) {
            if (property.getName().equals(PASSWORD_EXPIRY_RULE_PREFIX + RULE_1)) {
                foundRule1 = true;
            } else if (property.getName().equals(PASSWORD_EXPIRY_RULE_PREFIX + RULE_2)) {
                foundRule2 = true;
            }
        }

        assertFalse(foundRule1);
        assertTrue(foundRule2);
    }

    @Test(description = "Test for doPostGetResidentIdP with an exception",
            expectedExceptions = IdentityProviderManagementException.class)
    public void testDoPostGetResidentIdPWithRoleMappingException() throws Exception {

        IdentityProviderProperty[] properties = {
                createProperty(PASSWORD_EXPIRY_RULE_PREFIX + RULE_1, ROLES_RULE_PREFIX + MAIN_ROLE_ID_1)
        };

        IdentityProvider identityProvider = createIdentityProvider(properties);
        when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(anyList(), anyString()))
                .thenThrow(new IdentityRoleManagementException("Test exception"));
        listener.doPostGetResidentIdP(identityProvider, TENANT_DOMAIN);
    }

    private IdentityProvider createIdentityProvider(IdentityProviderProperty[] properties) {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdpProperties(properties);
        return identityProvider;
    }

    private IdentityProviderProperty createProperty(String name, String value) {

        IdentityProviderProperty property = new IdentityProviderProperty();
        property.setName(name);
        property.setValue(value);
        return property;
    }
}

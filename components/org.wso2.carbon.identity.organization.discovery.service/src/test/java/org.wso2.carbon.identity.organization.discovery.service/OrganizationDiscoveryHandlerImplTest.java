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

package org.wso2.carbon.identity.organization.discovery.service;

import org.mockito.Mock;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.model.OrganizationDiscoveryInput;
import org.wso2.carbon.identity.application.authentication.framework.model.OrganizationDiscoveryResult;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_DISCOVERY_TYPE_NOT_ENABLED_OR_SUPPORTED;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.OrgDiscoveryFailureDetails.VALID_DISCOVERY_PARAMETERS_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT;

/**
 * Test class for Organization Discovery Handler Implementation.
 */
public class OrganizationDiscoveryHandlerImplTest {

    private static final String ROOT_ORG_ID = "rootOrgId";
    private static final String ROOT_TENANT_DOMAIN = "rootTenantDomain";
    private static final String MAIN_APP_ID = "mainAppId";
    private static final String ORG_ID = "orgId";
    private static final String INVALID_ORG_ID = "invalidOrgId";
    private static final String ORG_HANDLE = "orgHandle";
    private static final String INVALID_ORG_HANDLE = "invalidOrgHandle";
    private static final String ORG_NAME = "orgName";
    private static final String INVALID_ORG_NAME = "invalidOrgName";
    private static final String LOGIN_HINT = "loginHint";
    private static final String INVALID_LOGIN_HINT = "invalidLoginHint";
    private static final String ORG_DISCOVERY_TYPE = "emailDomain";
    private static final String INVALID_ORG_DISCOVERY_TYPE = "invalidOrgDiscoveryType";
    private static final String SHARED_APP_ID = "sharedAppId";

    private final OrganizationDiscoveryHandlerImpl organizationDiscoveryHandler =
            new OrganizationDiscoveryHandlerImpl();

    private AutoCloseable closeable;
    @Mock
    private OrganizationManager organizationManager;
    @Mock
    private ApplicationManagementService applicationManagementService;
    @Mock
    private OrganizationDiscoveryManager organizationDiscoveryManager;
    @Mock
    private OrganizationConfigManager organizationConfigManager;
    @Mock
    private AuthenticationContext authenticationContext;

    @BeforeClass
    public void setUp() throws Exception {

        closeable = openMocks(this);

        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationManager(organizationManager);
        OrganizationDiscoveryServiceHolder.getInstance().setApplicationManagementService(applicationManagementService);
        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationDiscoveryManager(organizationDiscoveryManager);
        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationConfigManager(organizationConfigManager);

        when(authenticationContext.getServiceProviderResourceId()).thenReturn(MAIN_APP_ID);
        when(authenticationContext.getTenantDomain()).thenReturn(ROOT_TENANT_DOMAIN);

        when(organizationManager.resolveOrganizationId(ROOT_TENANT_DOMAIN)).thenReturn(ROOT_ORG_ID);

        Organization organization = new Organization();
        organization.setId(ORG_ID);
        organization.setOrganizationHandle(ORG_HANDLE);
        organization.setName(ORG_NAME);
        when(organizationManager.getOrganization(ORG_ID, false, false))
                .thenReturn(organization);
        when(organizationManager.getOrganization(INVALID_ORG_ID, false, false))
                .thenThrow(new OrganizationManagementClientException(
                        ERROR_CODE_INVALID_ORGANIZATION.getMessage(),
                        ERROR_CODE_INVALID_ORGANIZATION.getDescription(),
                        ERROR_CODE_INVALID_ORGANIZATION.getCode()));

        when(organizationManager.resolveOrganizationId(ORG_HANDLE)).thenReturn(ORG_ID);
        when(organizationManager.resolveOrganizationId(INVALID_ORG_HANDLE))
                .thenThrow(new OrganizationManagementClientException(
                        ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getMessage(),
                        ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getDescription(),
                        ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode()));

        when(organizationManager.getOrganizationIdByName(ORG_NAME)).thenReturn(ORG_ID);
        when(organizationManager.getOrganizationIdByName(INVALID_ORG_NAME)).thenReturn(null);

        when(organizationDiscoveryManager.getOrganizationIdByDiscoveryAttribute(
                ORG_DISCOVERY_TYPE, LOGIN_HINT, ROOT_ORG_ID, authenticationContext)).thenReturn(ORG_ID);
        when(organizationDiscoveryManager.getOrganizationIdByDiscoveryAttribute(
                ORG_DISCOVERY_TYPE, INVALID_LOGIN_HINT, ROOT_ORG_ID, authenticationContext)).thenReturn(null);

        ConfigProperty emailDomainDiscoveryEnableConfig = new ConfigProperty("emailDomain.enable", "true");
        List<ConfigProperty> discoveryConfigurationList = List.of(emailDomainDiscoveryEnableConfig);
        DiscoveryConfig discoveryConfig = new DiscoveryConfig(discoveryConfigurationList);
        when(organizationConfigManager.getDiscoveryConfiguration()).thenReturn(discoveryConfig);
        OrganizationDiscoveryServiceHolder.getInstance().setAttributeBasedOrganizationDiscoveryHandler(
                new EmailDomainBasedDiscoveryHandler());

        when(applicationManagementService.getSharedAppId(MAIN_APP_ID, ROOT_ORG_ID, ORG_ID)).thenReturn(SHARED_APP_ID);
    }

    @AfterClass
    public void tearDown() throws Exception {

        closeable.close();
    }

    @DataProvider
    public Object[][] discoverOrganizationSuccessDataProvider() {

        return new Object[][]{
                {ORG_ID, null, null, null, null},
                {null, ORG_HANDLE, null, null, null},
                {null, null, ORG_NAME, null, null},
                {null, null, null, LOGIN_HINT, null},
                {null, null, null, LOGIN_HINT, ORG_DISCOVERY_TYPE},
        };
    }

    @Test(dataProvider = "discoverOrganizationSuccessDataProvider")
    public void testDiscoverOrganizationSuccess(String orgId, String orgHandle, String orgName, String loginHint,
                                                String orgDiscoveryType) throws Exception {

        OrganizationDiscoveryInput orgDiscoveryInput = new OrganizationDiscoveryInput.Builder()
                .orgId(orgId)
                .orgHandle(orgHandle)
                .orgName(orgName)
                .loginHint(loginHint)
                .orgDiscoveryType(orgDiscoveryType)
                .build();

        OrganizationDiscoveryResult orgDiscoveryResult = organizationDiscoveryHandler
                .discoverOrganization(orgDiscoveryInput, authenticationContext);

        assertTrue(orgDiscoveryResult.isSuccessful(), "Organization discovery should be successful.");
        assertNotNull(orgDiscoveryResult.getDiscoveredOrganization(),
                "Discovered organization should not be null.");
        assertEquals(orgDiscoveryResult.getDiscoveredOrganization().getId(), ORG_ID,
                "Discovered organization ID should match the expected ID.");
        assertEquals(orgDiscoveryResult.getDiscoveredOrganization().getOrganizationHandle(), ORG_HANDLE,
                "Discovered organization handle should match the expected handle.");
        assertEquals(orgDiscoveryResult.getDiscoveredOrganization().getName(), ORG_NAME,
                "Discovered organization name should match the expected name.");
        assertEquals(orgDiscoveryResult.getSharedApplicationId(), SHARED_APP_ID,
                "Shared application ID should match the expected shared application ID.");
    }

    @DataProvider
    public Object[][] discoverOrganizationFailureDataProvider() {

        return new Object[][]{
                {null, null, null, null, null, VALID_DISCOVERY_PARAMETERS_NOT_FOUND.getCode()},
                {INVALID_ORG_ID, null, null, null, null, ORGANIZATION_NOT_FOUND.getCode()},
                {null, null, INVALID_ORG_NAME, null, null, ORGANIZATION_NOT_FOUND.getCode()},
                {null, null, null, INVALID_LOGIN_HINT, ORG_DISCOVERY_TYPE, ORGANIZATION_NOT_FOUND.getCode()},
                {null, null, null, LOGIN_HINT, INVALID_ORG_DISCOVERY_TYPE,
                        ORGANIZATION_DISCOVERY_TYPE_NOT_ENABLED_OR_SUPPORTED.getCode()},
        };
    }

    @Test(dataProvider = "discoverOrganizationFailureDataProvider")
    public void testDiscoverOrganizationFailure(String orgId, String orgHandle, String orgName, String loginHint,
                                                String orgDiscoveryType, String failureCode)
            throws Exception {

        OrganizationDiscoveryInput orgDiscoveryInput = new OrganizationDiscoveryInput.Builder()
                .orgId(orgId)
                .orgHandle(orgHandle)
                .orgName(orgName)
                .loginHint(loginHint)
                .orgDiscoveryType(orgDiscoveryType)
                .build();

        OrganizationDiscoveryResult orgDiscoveryResult = organizationDiscoveryHandler
                .discoverOrganization(orgDiscoveryInput, authenticationContext);

        assertFalse(orgDiscoveryResult.isSuccessful(), "Organization discovery should be not successful.");
        assertNotNull(orgDiscoveryResult.getFailureDetails(), "Failure details should not be null.");
        assertEquals(orgDiscoveryResult.getFailureDetails().getCode(), failureCode,
                "Failure code should match the expected code.");
    }
}

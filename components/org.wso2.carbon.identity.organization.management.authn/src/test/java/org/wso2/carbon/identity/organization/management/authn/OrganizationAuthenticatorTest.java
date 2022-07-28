/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.authn;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.common.testng.WithAxisConfiguration;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.authn.internal.AuthenticatorDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.authn.constant.AuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
import static org.wso2.carbon.identity.organization.management.authn.constant.AuthenticatorConstants.AUTHENTICATOR_NAME;
import static org.wso2.carbon.identity.organization.management.authn.constant.AuthenticatorConstants.INBOUND_AUTH_TYPE_OAUTH;
import static org.wso2.carbon.identity.organization.management.authn.constant.AuthenticatorConstants.ORG_ID_PARAMETER;
import static org.wso2.carbon.identity.organization.management.authn.constant.AuthenticatorConstants.ORG_PARAMETER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_APPLICATION_NOT_SHARED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_RETRIEVING_ORGANIZATIONS_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;

/**
 * Unit test class for {@link OrganizationAuthenticator} class.
 */

@WithAxisConfiguration
public class OrganizationAuthenticatorTest {

    private static final String contextIdentifier = "4952b467-86b2-31df-b63c-0bf25cec4f86s";
    private static final String orgId = "ef35863f-58f0-4a18-aef1-a8d9dd20cfbe";
    private static final String org = "greater";
    private static final String saasApp = "medlife";
    private static final String saasAppOwnedTenant = "carbon.super";
    private static final String saasAppOwnedOrgId = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String clientId = "3_TCRZ93rTQtPL8k02_trEYTfVca";
    private static final String secretKey = "uW4q6dYgSaHJIv11Llqi1nvOQBUa";

    private static Map<String, String> authenticatorParamProperties;
    private static Map<String, String> authenticatorProperties;
    private static Map<String, Object> mockContextParam;

    private HttpServletRequest mockServletRequest;
    private HttpServletResponse mockServletResponse;
    private AuthenticationContext mockAuthenticationContext;
    private RealmService mockRealmService;
    private OrganizationManager mockOrganizationManager;
    private OrgApplicationManager mockOrgApplicationManager;
    private ServiceProvider mockServiceProvider;
    private InboundAuthenticationConfig mockInboundAuthenticationConfig;
    private OAuthAdminServiceImpl mockOAuthAdminServiceImpl;
    private OAuthConsumerAppDTO mockOAuthConsumerAppDTO;
    private ExternalIdPConfig mockExternalIdPConfig;
    private Organization mockOrganization;
    private OrganizationAuthenticator organizationAuthenticator;
    private AuthenticatorDataHolder authenticatorDataHolder;

    @BeforeMethod
    public void init() throws UserStoreException {

        mockServletRequest = mock(HttpServletRequest.class);
        mockServletResponse = mock(HttpServletResponse.class);
        mockAuthenticationContext = mock(AuthenticationContext.class);
        mockRealmService = mock(RealmService.class);
        mockOrganizationManager = mock(OrganizationManager.class);
        mockOrgApplicationManager = mock(OrgApplicationManager.class);
        mockServiceProvider = mock(ServiceProvider.class);
        mockInboundAuthenticationConfig = mock(InboundAuthenticationConfig.class);
        mockOAuthAdminServiceImpl = mock(OAuthAdminServiceImpl.class);
        mockOAuthConsumerAppDTO = mock(OAuthConsumerAppDTO.class);
        mockExternalIdPConfig = mock(ExternalIdPConfig.class);
        mockOrganization = mock(Organization.class);

        organizationAuthenticator = new OrganizationAuthenticator();
        authenticatorParamProperties = new HashMap<>();
        authenticatorProperties = new HashMap<>();
        mockContextParam = new HashMap<>();

        authenticatorDataHolder = AuthenticatorDataHolder.getInstance();
        authenticatorDataHolder.setRealmService(mockRealmService);
        authenticatorDataHolder.setOrganizationManager(mockOrganizationManager);
        authenticatorDataHolder.setOrgApplicationManager(mockOrgApplicationManager);
        authenticatorDataHolder.setOAuthAdminService(mockOAuthAdminServiceImpl);
        Tenant tenant = mock(Tenant.class);
        TenantManager mockTenantManager = mock(TenantManager.class);
        when(mockRealmService.getTenantManager()).thenReturn(mockTenantManager);
        when(mockRealmService.getTenantManager().getTenant(anyInt())).thenReturn(tenant);
        when(tenant.getAssociatedOrganizationUUID()).thenReturn(orgId);
        when(mockAuthenticationContext.getProperties()).thenReturn(mockContextParam);
    }

    private void mockCarbonContext() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome,
                "repository/conf").toString());

        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SUPER_TENANT_DOMAIN_NAME);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(SUPER_TENANT_ID);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername("admin");
    }

    @Test
    public void testGetFriendlyName() {

        Assert.assertEquals(organizationAuthenticator.getFriendlyName(), AUTHENTICATOR_FRIENDLY_NAME,
                "Invalid friendly name.");
    }

    @Test
    public void testGetName() {

        Assert.assertEquals(organizationAuthenticator.getName(), AUTHENTICATOR_NAME,
                "Invalid authenticator name.");
    }

    @Test
    public void testProcessLogoutRequest() throws Exception {

        when(mockAuthenticationContext.isLogoutRequest()).thenReturn(true);
        AuthenticatorFlowStatus status = organizationAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
        Assert.assertEquals(status, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
    }

    @Test
    public void testProcessWithoutOrgParameter() throws Exception {

        when(mockAuthenticationContext.getContextIdentifier()).thenReturn(contextIdentifier);
        when(mockAuthenticationContext.getExternalIdP()).thenReturn(mockExternalIdPConfig);
        when(mockExternalIdPConfig.getName()).thenReturn(AUTHENTICATOR_FRIENDLY_NAME);
        mockCarbonContext();

        AuthenticatorFlowStatus status = organizationAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test
    public void testProcessInvalidOrgParam() throws Exception {

        Map<String, String[]> mockParamMap = new HashMap<>();
        mockParamMap.put(ORG_PARAMETER, new String[]{org});
        when(mockServletRequest.getParameterMap()).thenReturn(mockParamMap);
        when(mockServletRequest.getParameter(ORG_PARAMETER)).thenReturn(org);
        when(authenticatorDataHolder.getOrganizationManager().getOrganizationsByName(anyString()))
                .thenThrow(handleClientException(ERROR_CODE_RETRIEVING_ORGANIZATIONS_BY_NAME));

        when(mockAuthenticationContext.getContextIdentifier()).thenReturn(contextIdentifier);
        when(mockAuthenticationContext.getExternalIdP()).thenReturn(mockExternalIdPConfig);
        when(mockExternalIdPConfig.getName()).thenReturn(AUTHENTICATOR_FRIENDLY_NAME);
        mockCarbonContext();

        AuthenticatorFlowStatus status = organizationAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testProcessInvalidOrgIdParam() throws Exception {

        Map<String, String[]> mockParamMap = new HashMap<>();
        mockParamMap.put(ORG_ID_PARAMETER, new String[]{orgId});
        when(mockServletRequest.getParameterMap()).thenReturn(mockParamMap);
        when(mockServletRequest.getParameter(ORG_ID_PARAMETER)).thenReturn(orgId);
        when(authenticatorDataHolder.getOrganizationManager().getOrganizationNameById(anyString()))
                .thenThrow(handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ID));
        organizationAuthenticator.process(mockServletRequest, mockServletResponse, mockAuthenticationContext);

    }

    @Test
    public void testProcessOrgParamForOrgsWithSameName() throws Exception {

        Map<String, String[]> mockParamMap = new HashMap<>();
        mockParamMap.put(ORG_PARAMETER, new String[]{org});
        when(mockServletRequest.getParameterMap()).thenReturn(mockParamMap);
        when(mockServletRequest.getParameter(ORG_PARAMETER)).thenReturn(org);

        when(mockOrganization.getId()).thenReturn(orgId);
        when(mockOrganization.getName()).thenReturn(org);
        when(mockOrganization.getDescription()).thenReturn("description");
        when(mockOrganizationManager.getOrganizationsByName(anyString()))
                .thenReturn(Arrays.asList(mockOrganization, mockOrganization));

        when(mockAuthenticationContext.getContextIdentifier()).thenReturn(contextIdentifier);
        when(mockAuthenticationContext.getExternalIdP()).thenReturn(mockExternalIdPConfig);
        when(mockExternalIdPConfig.getName()).thenReturn(AUTHENTICATOR_FRIENDLY_NAME);
        mockCarbonContext();

        AuthenticatorFlowStatus status = organizationAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test
    public void testProcessWithValidOrgIdParamSet() throws Exception {

        Map<String, String[]> mockParamMap = new HashMap<>();
        mockParamMap.put(ORG_ID_PARAMETER, new String[]{orgId});
        when(mockServletRequest.getParameterMap()).thenReturn(mockParamMap);
        when(mockServletRequest.getParameter(ORG_ID_PARAMETER)).thenReturn(orgId);
        when(authenticatorDataHolder.getOrganizationManager().getOrganizationNameById(anyString()))
                .thenReturn(org);

        authenticatorParamProperties.put(ORG_PARAMETER, "");
        when(organizationAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);
        when(mockAuthenticationContext.getServiceProviderName()).thenReturn(saasApp);
        when(mockAuthenticationContext.getTenantDomain()).thenReturn(saasAppOwnedTenant);
        when(authenticatorDataHolder.getOrganizationManager().resolveOrganizationId(anyString()))
                .thenReturn(saasAppOwnedOrgId);
        setMockContextParamForValidOrganization();
        when(authenticatorDataHolder.getOrgApplicationManager()
                .resolveSharedApplication(anyString(), anyString(), anyString())).thenReturn(mockServiceProvider);
        when(mockServiceProvider.getInboundAuthenticationConfig()).thenReturn(mockInboundAuthenticationConfig);
        when(authenticatorDataHolder.getOrganizationManager().resolveTenantDomain(anyString()))
                .thenReturn(orgId);
        InboundAuthenticationRequestConfig inbound = new InboundAuthenticationRequestConfig();
        inbound.setInboundAuthType(INBOUND_AUTH_TYPE_OAUTH);
        inbound.setInboundAuthKey(clientId);
        InboundAuthenticationRequestConfig[] inbounds = {inbound};

        when(mockInboundAuthenticationConfig.getInboundAuthenticationRequestConfigs()).thenReturn(inbounds);

        when(authenticatorDataHolder.getOAuthAdminService().getOAuthApplicationData(anyString()))
                .thenReturn(mockOAuthConsumerAppDTO);
        when(mockOAuthConsumerAppDTO.getOauthConsumerSecret()).thenReturn(secretKey);

        when(mockAuthenticationContext.getAuthenticatorProperties()).thenReturn(authenticatorProperties);
        when(mockAuthenticationContext.getContextIdentifier()).thenReturn(contextIdentifier);
        when(mockAuthenticationContext.getExternalIdP()).thenReturn(mockExternalIdPConfig);

        mockCarbonContext();
        AuthenticatorFlowStatus status = organizationAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);

        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestWithoutOrgParameter() throws AuthenticationFailedException {

        organizationAuthenticator.initiateAuthenticationRequest(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestNoSharedApp() throws Exception {

        setMockContextParamForValidOrganization();
        authenticatorParamProperties.put(ORG_PARAMETER, orgId);
        authenticatorParamProperties.put(ORG_ID_PARAMETER, orgId);
        when(organizationAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);

        when(mockOrganizationManager
                .getOrganizationIdByName(anyString())).thenReturn(orgId);
        when(mockOrganizationManager
                .getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockOrganization);

        when(mockOrganizationManager.resolveOrganizationId(anyString()))
                .thenReturn(saasAppOwnedOrgId);
        when(mockAuthenticationContext.getTenantDomain()).thenReturn(saasAppOwnedTenant);
        when(mockAuthenticationContext.getServiceProviderName()).thenReturn(saasApp);
        when(mockOrgApplicationManager.resolveSharedApplication(anyString(),
                anyString(), anyString())).thenThrow(
                new OrganizationManagementServerException(ERROR_CODE_APPLICATION_NOT_SHARED.getCode(),
                        ERROR_CODE_APPLICATION_NOT_SHARED.getMessage()));
        organizationAuthenticator.initiateAuthenticationRequest(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestInvalidSharedAppInbound() throws Exception {

        setMockContextParamForValidOrganization();
        authenticatorParamProperties.put(ORG_PARAMETER, orgId);
        authenticatorParamProperties.put(ORG_ID_PARAMETER, orgId);
        when(organizationAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);

        when(authenticatorDataHolder.getOrganizationManager()
                .getOrganizationIdByName(anyString())).thenReturn(orgId);
        when(authenticatorDataHolder.getOrganizationManager()
                .getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockOrganization);
        when(authenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenReturn(mockServiceProvider);

        when(authenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenThrow(
                new OrganizationManagementServerException(ERROR_CODE_INVALID_APPLICATION.getCode(),
                        ERROR_CODE_INVALID_APPLICATION.getMessage()));
        when(authenticatorDataHolder.getOrganizationManager().resolveOrganizationId(anyString()))
                .thenReturn(saasAppOwnedOrgId);
        when(mockAuthenticationContext.getTenantDomain()).thenReturn(saasAppOwnedTenant);
        when(mockAuthenticationContext.getServiceProviderName()).thenReturn(saasApp);

        organizationAuthenticator.initiateAuthenticationRequest(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestInvalidSharedApp() throws Exception {

        setMockContextParamForValidOrganization();
        authenticatorParamProperties.put(ORG_PARAMETER, orgId);
        authenticatorParamProperties.put(ORG_ID_PARAMETER, orgId);
        when(organizationAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);

        when(authenticatorDataHolder.getOrganizationManager()
                .getOrganizationIdByName(anyString())).thenReturn(orgId);
        when(authenticatorDataHolder.getOrganizationManager()
                .getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockOrganization);
        when(authenticatorDataHolder.getOrganizationManager().resolveTenantDomain(anyString())).thenReturn(
                orgId);

        when(authenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenReturn(mockServiceProvider);
        when(authenticatorDataHolder.getOrganizationManager().resolveOrganizationId(anyString()))
                .thenReturn(saasAppOwnedOrgId);
        when(mockServiceProvider.getInboundAuthenticationConfig()).thenReturn(mockInboundAuthenticationConfig);
        when(mockAuthenticationContext.getTenantDomain()).thenReturn(saasAppOwnedTenant);
        when(mockAuthenticationContext.getServiceProviderName()).thenReturn(saasApp);

        organizationAuthenticator.initiateAuthenticationRequest(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
    }

    private void setMockContextParamForValidOrganization() {

        mockContextParam.put(ORG_PARAMETER, org);
        when(mockAuthenticationContext.getProperty(ORG_PARAMETER)).thenReturn(org);
        mockContextParam.put(ORG_ID_PARAMETER, orgId);
        when(mockAuthenticationContext.getProperty(ORG_ID_PARAMETER)).thenReturn(orgId);
    }
}

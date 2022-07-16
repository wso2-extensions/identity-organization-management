package org.wso2.carbon.identity.organization.management.application.authn;

import org.mockito.Answers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.authn.internal.EnterpriseIDPAuthenticatorDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.AUTHENTICATOR_NAME;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.INBOUND_AUTH_TYPE_OAUTH;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORG_ID_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORG_PARAMETER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_APPLICATION_NOT_SHARED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_APPLICATION;

/**
 * Unit test class for EnterpriseIDPAuthenticatorTest class.
 */
@PrepareForTest({EnterpriseIDPAuthenticator.class, EnterpriseIDPAuthenticatorDataHolder.class,
        ServiceURLBuilder.class, IdentityTenantUtil.class})
public class EnterpriseIDPAuthenticatorTest extends PowerMockTestCase {

    private static final String contextIdentifier = "4952b467-86b2-31df-b63c-0bf25cec4f86s";
    private static final String orgId = "ef35863f-58f0-4a18-aef1-a8d9dd20cfbe";
    private static final String clientId = "3_TCRZ93rTQtPL8k02_trEYTfVca";
    private static final String secretKey = "uW4q6dYgSaHJIv11Llqi1nvOQBUa";

    private static Map<String, String> authenticatorParamProperties;
    private static Map<String, String> authenticatorProperties;

    @Mock
    private HttpServletRequest mockServletRequest;
    @Mock
    private HttpServletResponse mockServletResponse;
    @Mock
    private AuthenticationContext mockAuthenticationContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RealmService mockRealmService;
    @Mock
    private OrganizationManager mockOrganizationManager;
    @Mock
    private OrgApplicationManager mockOrgApplicationManager;
    @Mock
    private ServiceProvider mockServiceProvider;
    @Mock
    private InboundAuthenticationConfig mockInboundAuthenticationConfig;
    @Mock
    private OAuthAdminServiceImpl mockOAuthAdminServiceImpl;
    @Mock
    private OAuthConsumerAppDTO mockOAuthConsumerAppDTO;
    @Mock
    private ExternalIdPConfig mockExternalIdPConfig;
    @Mock
    private Organization mockOrganization;
    private EnterpriseIDPAuthenticator enterpriseIDPAuthenticator;
    private EnterpriseIDPAuthenticatorDataHolder enterpriseIDPAuthenticatorDataHolder;

    @BeforeMethod
    public void init() throws UserStoreException {

        enterpriseIDPAuthenticator = new EnterpriseIDPAuthenticator();
        authenticatorParamProperties = new HashMap<>();
        authenticatorProperties = new HashMap<>();

        enterpriseIDPAuthenticatorDataHolder = spy(new EnterpriseIDPAuthenticatorDataHolder());
        mockStatic(EnterpriseIDPAuthenticatorDataHolder.class);
        enterpriseIDPAuthenticatorDataHolder.setRealmService(mockRealmService);
        enterpriseIDPAuthenticatorDataHolder.setOrganizationManager(mockOrganizationManager);
        enterpriseIDPAuthenticatorDataHolder.setOrgApplicationManager(mockOrgApplicationManager);
        enterpriseIDPAuthenticatorDataHolder.setOAuthAdminService(mockOAuthAdminServiceImpl);
        when(EnterpriseIDPAuthenticatorDataHolder.getInstance()).thenReturn(enterpriseIDPAuthenticatorDataHolder);
        mockStatic(IdentityTenantUtil.class);
        when(IdentityTenantUtil.getTenantId(anyString())).thenReturn(12);
        Tenant tenant = mock(Tenant.class);
        when(mockRealmService.getTenantManager().getTenant(anyInt())).thenReturn(tenant);
        when(tenant.getAssociatedOrganizationUUID()).thenReturn(orgId);
    }

    private void mockServiceURLBuilder() {

        ServiceURLBuilder builder = new ServiceURLBuilder() {

            String path = "";

            @Override
            public ServiceURLBuilder addPath(String... strings) {

                Arrays.stream(strings).forEach(x -> path += "/" + x);
                return this;
            }

            @Override
            public ServiceURLBuilder addParameter(String s, String s1) {

                return this;
            }

            @Override
            public ServiceURLBuilder setFragment(String s) {

                return this;
            }

            @Override
            public ServiceURLBuilder addFragmentParameter(String s, String s1) {

                return this;
            }

            @Override
            public ServiceURL build() {

                ServiceURL serviceURL = mock(ServiceURL.class);
                PowerMockito.when(serviceURL.getAbsolutePublicURL()).thenReturn("https://localhost:9443" + path);
                PowerMockito.when(serviceURL.getRelativePublicURL()).thenReturn(path);
                path = "";
                return serviceURL;
            }
        };

        mockStatic(ServiceURLBuilder.class);
        when(ServiceURLBuilder.create()).thenReturn(builder);
    }

    @Test
    public void testGetFriendlyName() {

        Assert.assertEquals(enterpriseIDPAuthenticator.getFriendlyName(), AUTHENTICATOR_FRIENDLY_NAME,
                "Invalid friendly name.");
    }

    @Test
    public void testGetName() {

        Assert.assertEquals(enterpriseIDPAuthenticator.getName(), AUTHENTICATOR_NAME,
                "Invalid authenticator name.");
    }

    @Test
    public void testProcessLogoutRequest() throws Exception {

        when(mockAuthenticationContext.isLogoutRequest()).thenReturn(true);
        AuthenticatorFlowStatus status = enterpriseIDPAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
        Assert.assertEquals(status, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
    }

    @Test
    public void testProcessWithoutOrgParameter() throws Exception {

        when(mockAuthenticationContext.getContextIdentifier()).thenReturn(contextIdentifier);
        when(mockAuthenticationContext.getExternalIdP()).thenReturn(mockExternalIdPConfig);
        when(mockExternalIdPConfig.getName()).thenReturn(AUTHENTICATOR_FRIENDLY_NAME);
        mockServiceURLBuilder();

        AuthenticatorFlowStatus status = enterpriseIDPAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test
    public void testProcessInvalidOrg() throws Exception {

        authenticatorParamProperties.put(ORG_PARAMETER, orgId);
        when(enterpriseIDPAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);

        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager().isOrganizationExistById(anyString()))
                .thenReturn(false);

        when(mockAuthenticationContext.getContextIdentifier()).thenReturn(contextIdentifier);
        when(mockAuthenticationContext.getExternalIdP()).thenReturn(mockExternalIdPConfig);
        when(mockExternalIdPConfig.getName()).thenReturn(AUTHENTICATOR_FRIENDLY_NAME);
        mockServiceURLBuilder();

        AuthenticatorFlowStatus status = enterpriseIDPAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test
    public void testProcess() throws Exception {

        authenticatorParamProperties.put(ORG_PARAMETER, orgId);
        when(enterpriseIDPAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);

        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager().isOrganizationExistById(anyString()))
                .thenReturn(true);
        when(enterpriseIDPAuthenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenReturn(mockServiceProvider);
        when(mockServiceProvider.getInboundAuthenticationConfig()).thenReturn(mockInboundAuthenticationConfig);

        InboundAuthenticationRequestConfig inbound = new InboundAuthenticationRequestConfig();
        inbound.setInboundAuthType(INBOUND_AUTH_TYPE_OAUTH);
        inbound.setInboundAuthKey(clientId);
        InboundAuthenticationRequestConfig[] inbounds = {inbound};

        when(mockInboundAuthenticationConfig.getInboundAuthenticationRequestConfigs()).thenReturn(inbounds);

        when(enterpriseIDPAuthenticatorDataHolder.getOAuthAdminService().getOAuthApplicationData(anyString()))
                .thenReturn(mockOAuthConsumerAppDTO);
        when(mockOAuthConsumerAppDTO.getOauthConsumerSecret()).thenReturn(secretKey);

        when(mockAuthenticationContext.getAuthenticatorProperties()).thenReturn(authenticatorProperties);
        when(mockAuthenticationContext.getContextIdentifier()).thenReturn(contextIdentifier);
        when(mockAuthenticationContext.getExternalIdP()).thenReturn(mockExternalIdPConfig);

        mockServiceURLBuilder();
        AuthenticatorFlowStatus status = enterpriseIDPAuthenticator.process(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);

        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestWithoutOrgParameter() throws AuthenticationFailedException {

        enterpriseIDPAuthenticator.initiateAuthenticationRequest(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestNoSharedApp() throws Exception {

        authenticatorParamProperties.put(ORG_PARAMETER, orgId);
        when(enterpriseIDPAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);

        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager()
                .getOrganizationIdByName(anyString())).thenReturn(orgId);
        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager()
                .getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockOrganization);
        when(enterpriseIDPAuthenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenReturn(mockServiceProvider);

        when(enterpriseIDPAuthenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenThrow(
                new OrganizationManagementServerException(ERROR_CODE_APPLICATION_NOT_SHARED.getCode(),
                        ERROR_CODE_APPLICATION_NOT_SHARED.getMessage()));
        enterpriseIDPAuthenticator.initiateAuthenticationRequest(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestInvalidSharedAppInbound() throws Exception {

        authenticatorParamProperties.put(ORG_PARAMETER, orgId);
        when(enterpriseIDPAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);

        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager()
                .getOrganizationIdByName(anyString())).thenReturn(orgId);
        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager()
                .getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockOrganization);
        when(enterpriseIDPAuthenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenReturn(mockServiceProvider);

        when(enterpriseIDPAuthenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenThrow(
                new OrganizationManagementServerException(ERROR_CODE_INVALID_APPLICATION.getCode(),
                        ERROR_CODE_INVALID_APPLICATION.getMessage()));

        enterpriseIDPAuthenticator.initiateAuthenticationRequest(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestInvalidSharedApp() throws Exception {

        authenticatorParamProperties.put(ORG_PARAMETER, orgId);
        when(enterpriseIDPAuthenticator.getRuntimeParams(mockAuthenticationContext))
                .thenReturn(authenticatorParamProperties);

        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager()
                .getOrganizationIdByName(anyString())).thenReturn(orgId);
        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager()
                .getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(mockOrganization);
        when(enterpriseIDPAuthenticatorDataHolder.getOrganizationManager().resolveTenantDomain(anyString())).thenReturn(
                orgId);

        when(enterpriseIDPAuthenticatorDataHolder.getOrgApplicationManager().resolveSharedApplication(anyString(),
                anyString(), anyString())).thenReturn(mockServiceProvider);
        when(mockServiceProvider.getInboundAuthenticationConfig()).thenReturn(mockInboundAuthenticationConfig);

        enterpriseIDPAuthenticator.initiateAuthenticationRequest(mockServletRequest, mockServletResponse,
                mockAuthenticationContext);
    }
}

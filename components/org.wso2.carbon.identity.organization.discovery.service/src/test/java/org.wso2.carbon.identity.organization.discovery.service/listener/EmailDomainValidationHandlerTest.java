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

package org.wso2.carbon.identity.organization.discovery.service.listener;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.PostAuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.PostAuthnHandlerFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigClientException;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigServerException;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;
import org.wso2.carbon.identity.organization.discovery.service.OrganizationDiscoveryManager;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.discovery.service.model.OrgDiscoveryAttribute;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_DOMAIN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_NO_EMAIL_ATTRIBUTE_FOUND;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_WHILE_RETRIEVING_ORG_DISCOVERY_ATTRIBUTES;

/**
 * This class contains unit tests to verify the behavior of the EmailDomainValidationHandler class.
 */
public class EmailDomainValidationHandlerTest {

    private static final String DISCOVERY_MANAGER_FIELD = "organizationDiscoveryManager";
    private static final String EMAIL_DOMAIN_ENABLE = "emailDomain.enable";
    private static final String EMAIL_DOMAIN = "emailDomain";
    private static final String SUB_ORG_TENANT_DOMAIN = "0f1c2b3a-4d5e-6f70-8192-a3b4c5d6e7f8";
    private static final String SUB_ORG_ID = "0f1c2b3a-4d5e-6f70-8192-a3b4c5d6e7f8";
    private static final String PRIMARY_ORG_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String PRIMARY_TENANT_DOMAIN = "carbon.super";
    private static final int PRIMARY_TENANT_ID = -1234;
    private static final String MAPPED_EMAIL_DOMAIN = "abc.com";
    private static final String IDP_CLAIM_DIALECT = "http://wso2.org/oidc/claim";
    private static final String REMOTE_EMAIL_CLAIM = "email";

    private EmailDomainValidationHandler emailDomainValidationHandler;
    private OrganizationDiscoveryManager originalDiscoveryManager;

    private AutoCloseable closeable;
    private MockedStatic<FrameworkUtils> frameworkUtils;
    private MockedStatic<IdentityUtil> identityUtil;
    private MockedStatic<IdentityTenantUtil> identityTenantUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationContext context;

    @Mock
    private OrganizationDiscoveryManager organizationDiscoveryManager;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private OrganizationConfigManager organizationConfigManager;

    @BeforeClass
    public void initTestClass() throws Exception {

        emailDomainValidationHandler = EmailDomainValidationHandler.getInstance();
        originalDiscoveryManager = (OrganizationDiscoveryManager) getDiscoveryManagerField()
                .get(emailDomainValidationHandler);
    }

    @AfterClass
    public void cleanUpTestClass() throws Exception {

        // Restore the discovery manager of the shared singleton instance.
        getDiscoveryManagerField().set(emailDomainValidationHandler, originalDiscoveryManager);
    }

    @BeforeMethod
    public void setUp() throws Exception {

        closeable = openMocks(this);
        frameworkUtils = mockStatic(FrameworkUtils.class);
        identityUtil = mockStatic(IdentityUtil.class);
        identityTenantUtil = mockStatic(IdentityTenantUtil.class);

        // No event listener configuration is available for the handler, hence it is enabled by default.
        identityUtil.when(() -> IdentityUtil.readEventListenerProperty(anyString(), anyString())).thenReturn(null);
        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(PRIMARY_TENANT_DOMAIN))
                .thenReturn(PRIMARY_TENANT_ID);

        getDiscoveryManagerField().set(emailDomainValidationHandler, organizationDiscoveryManager);

        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationManager(organizationManager);
        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationConfigManager(organizationConfigManager);

        /*
         * The handler resolves the organization from the carbon context. A tenant flow is started so that the tenant
         * domain can be set without mutating the carbon context shared with the other test classes of the suite.
         */
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SUB_ORG_TENANT_DOMAIN);

        when(context.getTenantDomain()).thenReturn(SUB_ORG_TENANT_DOMAIN);
        frameworkUtils.when(() -> FrameworkUtils.isStepBasedSequenceHandlerExecuted(context)).thenReturn(true);
    }

    @AfterMethod
    public void tearDown() throws Exception {

        PrivilegedCarbonContext.endTenantFlow();
        frameworkUtils.close();
        identityUtil.close();
        identityTenantUtil.close();
        closeable.close();
    }

    @Test
    public void testHandleSkipValidationWhenStepBasedSequenceHandlerNotExecuted()
            throws PostAuthenticationFailedException {

        frameworkUtils.when(() -> FrameworkUtils.isStepBasedSequenceHandlerExecuted(context)).thenReturn(false);

        PostAuthnHandlerFlowStatus status = emailDomainValidationHandler.handle(request, response, context);

        assertEquals(status, PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED,
                "Email domain validation should be skipped when the step based sequence handler is not executed.");
        verify(context, never()).getSequenceConfig();
    }

    @Test
    public void testHandleWhenLocalClaimValuesNotSetInSubjectAttributeStep() {

        mockSequenceConfigWithFederatedAuthenticator(true);
        when(context.getProperty(FrameworkConstants.UNFILTERED_LOCAL_CLAIM_VALUES)).thenReturn(null);

        assertPostAuthnFailure(ERROR_CODE_NO_EMAIL_ATTRIBUTE_FOUND.getCode());
    }

    @DataProvider(name = "emailDomainValidationDataProvider")
    public Object[][] emailDomainValidationDataProvider() {

        return new Object[][]{
                // Email claim value, mapped email domains, expected error code (null if validation should pass).
                {"user@" + MAPPED_EMAIL_DOMAIN, Collections.singletonList(MAPPED_EMAIL_DOMAIN), null},
                {"user@xyz.com", Arrays.asList(MAPPED_EMAIL_DOMAIN, "def.com"),
                        ERROR_CODE_INVALID_EMAIL_DOMAIN.getCode()},
                {"user@def.com", Arrays.asList(MAPPED_EMAIL_DOMAIN, "def.com"), null},
                {null, Collections.singletonList(MAPPED_EMAIL_DOMAIN), ERROR_CODE_NO_EMAIL_ATTRIBUTE_FOUND.getCode()},
                {"  ", Collections.singletonList(MAPPED_EMAIL_DOMAIN), ERROR_CODE_NO_EMAIL_ATTRIBUTE_FOUND.getCode()},
                {"invalidEmail", Collections.singletonList(MAPPED_EMAIL_DOMAIN),
                        ERROR_CODE_NO_EMAIL_ATTRIBUTE_FOUND.getCode()},
                {"user@abc@com", Collections.singletonList(MAPPED_EMAIL_DOMAIN),
                        ERROR_CODE_NO_EMAIL_ATTRIBUTE_FOUND.getCode()},
                // No email domains are mapped to the organization, hence validation is skipped.
                {"user@xyz.com", null, null},
        };
    }

    @Test(dataProvider = "emailDomainValidationDataProvider")
    public void testHandleEmailDomainValidation(String emailClaimValue, List<String> mappedEmailDomains,
                                                String expectedErrorCode) throws Exception {

        mockSequenceConfigWithFederatedAuthenticator(true);
        mockUnfilteredLocalClaimValues(emailClaimValue);
        mockOrganizationDiscoveryAttributes(EMAIL_DOMAIN, mappedEmailDomains);

        if (expectedErrorCode == null) {
            assertEquals(emailDomainValidationHandler.handle(request, response, context),
                    PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED, "Email domain validation should have succeeded.");
        } else {
            assertPostAuthnFailure(expectedErrorCode);
        }
    }

    @Test
    public void testHandleWhenNoEmailDomainDiscoveryAttributeIsMapped() throws Exception {

        mockSequenceConfigWithFederatedAuthenticator(true);
        mockUnfilteredLocalClaimValues("user@xyz.com");
        // Only a discovery attribute of a different type is mapped to the organization.
        mockOrganizationDiscoveryAttributes("customAttribute", Collections.singletonList("customValue"));

        assertEquals(emailDomainValidationHandler.handle(request, response, context),
                PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED,
                "Discovery attributes of other types should not be validated against the email domain.");
    }

    @Test
    public void testHandleWhenRetrievingDiscoveryAttributesFails() throws Exception {

        mockSequenceConfigWithFederatedAuthenticator(true);
        mockUnfilteredLocalClaimValues("user@" + MAPPED_EMAIL_DOMAIN);
        when(organizationDiscoveryManager.getOrganizationDiscoveryAttributes(SUB_ORG_TENANT_DOMAIN, false))
                .thenThrow(new OrganizationManagementException("Error while retrieving discovery attributes."));

        assertPostAuthnFailure(ERROR_WHILE_RETRIEVING_ORG_DISCOVERY_ATTRIBUTES.getCode());
    }

    @Test
    public void testHandleSkipValidationForLocalAuthenticator() throws Exception {

        StepConfig stepConfig = mockStepConfig(mock(ApplicationAuthenticator.class), true);
        mockSequenceConfig(stepConfig);

        assertEquals(emailDomainValidationHandler.handle(request, response, context),
                PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED,
                "Email domain validation should be skipped for local authenticators.");
        verify(organizationDiscoveryManager, never()).getOrganizationDiscoveryAttributes(anyString(), anyBoolean());
    }

    @Test
    public void testHandleSkipStepsWithoutAuthenticatedAuthenticator() throws Exception {

        StepConfig stepConfig = mock(StepConfig.class);
        when(stepConfig.getAuthenticatedAutenticator()).thenReturn(null);
        mockSequenceConfig(stepConfig);

        assertEquals(emailDomainValidationHandler.handle(request, response, context),
                PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED,
                "Steps without an authenticated authenticator should be skipped.");
        verify(organizationDiscoveryManager, never()).getOrganizationDiscoveryAttributes(anyString(), anyBoolean());
    }

    @DataProvider(name = "nonAttributeSelectionStepDataProvider")
    public Object[][] nonAttributeSelectionStepDataProvider() {

        return new Object[][]{
                {"user@" + MAPPED_EMAIL_DOMAIN, null},
                {"user@xyz.com", ERROR_CODE_INVALID_EMAIL_DOMAIN.getCode()},
        };
    }

    @Test(dataProvider = "nonAttributeSelectionStepDataProvider")
    public void testHandleEmailDomainValidationInNonAttributeSelectionStep(String emailClaimValue,
                                                                          String expectedErrorCode) throws Exception {

        StepConfig stepConfig = mockStepConfig(mock(FederatedApplicationAuthenticator.class), false);
        AuthenticatedUser authenticatedUser = mock(AuthenticatedUser.class);
        when(stepConfig.getAuthenticatedUser()).thenReturn(authenticatedUser);
        when(authenticatedUser.getUserAttributes()).thenReturn(new HashMap<>());
        mockSequenceConfig(stepConfig);

        ExternalIdPConfig externalIdPConfig = mock(ExternalIdPConfig.class);
        when(context.getExternalIdP()).thenReturn(externalIdPConfig);
        when(externalIdPConfig.useDefaultLocalIdpDialect()).thenReturn(false);
        when(externalIdPConfig.getClaimMappings()).thenReturn(new ClaimMapping[]{
                ClaimMapping.build(FrameworkConstants.EMAIL_ADDRESS_CLAIM, REMOTE_EMAIL_CLAIM, null, true)});

        Map<String, String> externalClaimValues = new HashMap<>();
        externalClaimValues.put(REMOTE_EMAIL_CLAIM, emailClaimValue);
        frameworkUtils.when(() -> FrameworkUtils.getClaimMappings(any(Map.class), anyBoolean()))
                .thenReturn(externalClaimValues);

        mockOrganizationDiscoveryAttributes(EMAIL_DOMAIN, Collections.singletonList(MAPPED_EMAIL_DOMAIN));

        if (expectedErrorCode == null) {
            assertEquals(emailDomainValidationHandler.handle(request, response, context),
                    PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED, "Email domain validation should have succeeded.");
        } else {
            assertPostAuthnFailure(expectedErrorCode);
        }
        // The unfiltered local claim values should not be used when the step is not the subject attribute step.
        verify(context, never()).getProperty(FrameworkConstants.UNFILTERED_LOCAL_CLAIM_VALUES);
    }

    @Test
    public void testIsEnabledForPrimaryOrganization() throws Exception {

        when(organizationManager.resolveOrganizationId(SUB_ORG_TENANT_DOMAIN)).thenReturn(PRIMARY_ORG_ID);
        when(organizationManager.isPrimaryOrganization(PRIMARY_ORG_ID)).thenReturn(true);

        assertFalse(emailDomainValidationHandler.isEnabled(),
                "Email domain validation should be disabled for primary organizations.");
        verify(organizationConfigManager, never()).getDiscoveryConfigurationByTenantId(PRIMARY_TENANT_ID);
    }

    @DataProvider(name = "discoveryConfigDataProvider")
    public Object[][] discoveryConfigDataProvider() {

        return new Object[][]{
                {Collections.singletonList(new ConfigProperty(EMAIL_DOMAIN_ENABLE, "true")), true},
                {Collections.singletonList(new ConfigProperty(EMAIL_DOMAIN_ENABLE, "false")), false},
                {Collections.singletonList(new ConfigProperty("selfService.enable", "true")), false},
                {Collections.emptyList(), false},
        };
    }

    @Test(dataProvider = "discoveryConfigDataProvider")
    public void testIsEnabledForSubOrganization(List<ConfigProperty> configProperties, boolean expectedResult)
            throws Exception {

        mockSubOrganizationResolution();
        when(organizationConfigManager.getDiscoveryConfigurationByTenantId(PRIMARY_TENANT_ID))
                .thenReturn(new DiscoveryConfig(configProperties));

        assertEquals(emailDomainValidationHandler.isEnabled(), expectedResult,
                "Unexpected handler enabled state for the given discovery configuration.");
    }

    @Test
    public void testIsEnabledWhenDiscoveryConfigNotFound() throws Exception {

        mockSubOrganizationResolution();
        when(organizationConfigManager.getDiscoveryConfigurationByTenantId(PRIMARY_TENANT_ID))
                .thenThrow(new OrganizationConfigClientException("No discovery configuration found."));

        assertFalse(emailDomainValidationHandler.isEnabled(),
                "Handler should be disabled when no discovery configuration is found.");
    }

    @Test
    public void testIsEnabledWhenRetrievingDiscoveryConfigFails() throws Exception {

        mockSubOrganizationResolution();
        when(organizationConfigManager.getDiscoveryConfigurationByTenantId(PRIMARY_TENANT_ID))
                .thenThrow(new OrganizationConfigServerException("Error while retrieving the discovery config."));

        assertFalse(emailDomainValidationHandler.isEnabled(),
                "Handler should be disabled when retrieving the discovery configuration fails.");
    }

    @Test
    public void testIsEnabledWhenOrganizationResolutionFails() throws Exception {

        when(organizationManager.resolveOrganizationId(SUB_ORG_TENANT_DOMAIN))
                .thenThrow(new OrganizationManagementException("Error while resolving the organization."));

        assertFalse(emailDomainValidationHandler.isEnabled(),
                "Handler should be disabled when the organization cannot be resolved.");
    }

    @Test
    public void testGetPriority() {

        assertEquals(emailDomainValidationHandler.getPriority(), 15,
                "Default priority of the email domain validation handler should be 15.");
    }

    private void mockSubOrganizationResolution() throws OrganizationManagementException {

        when(organizationManager.resolveOrganizationId(SUB_ORG_TENANT_DOMAIN)).thenReturn(SUB_ORG_ID);
        when(organizationManager.isPrimaryOrganization(SUB_ORG_ID)).thenReturn(false);
        when(organizationManager.getPrimaryOrganizationId(SUB_ORG_ID)).thenReturn(PRIMARY_ORG_ID);
        when(organizationManager.resolveTenantDomain(PRIMARY_ORG_ID)).thenReturn(PRIMARY_TENANT_DOMAIN);
    }

    private void mockSequenceConfigWithFederatedAuthenticator(boolean subjectAttributeStep) {

        mockSequenceConfig(mockStepConfig(mock(FederatedApplicationAuthenticator.class), subjectAttributeStep));
    }

    private StepConfig mockStepConfig(ApplicationAuthenticator authenticator, boolean subjectAttributeStep) {

        AuthenticatorConfig authenticatorConfig = mock(AuthenticatorConfig.class);
        when(authenticatorConfig.getApplicationAuthenticator()).thenReturn(authenticator);

        StepConfig stepConfig = mock(StepConfig.class);
        when(stepConfig.getAuthenticatedAutenticator()).thenReturn(authenticatorConfig);
        when(stepConfig.isSubjectAttributeStep()).thenReturn(subjectAttributeStep);
        return stepConfig;
    }

    private void mockSequenceConfig(StepConfig stepConfig) {

        SequenceConfig sequenceConfig = new SequenceConfig();
        sequenceConfig.setStepMap(Collections.singletonMap(1, stepConfig));
        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
    }

    private void mockUnfilteredLocalClaimValues(String emailClaimValue) {

        Map<String, String> localClaimValues = new HashMap<>();
        localClaimValues.put(FrameworkConstants.EMAIL_ADDRESS_CLAIM, emailClaimValue);
        when(context.getProperty(FrameworkConstants.UNFILTERED_LOCAL_CLAIM_VALUES)).thenReturn(localClaimValues);
    }

    private void mockOrganizationDiscoveryAttributes(String type, List<String> values)
            throws OrganizationManagementException {

        List<OrgDiscoveryAttribute> discoveryAttributes = Collections.emptyList();
        if (values != null) {
            OrgDiscoveryAttribute discoveryAttribute = new OrgDiscoveryAttribute();
            discoveryAttribute.setType(type);
            discoveryAttribute.setValues(values);
            discoveryAttributes = Collections.singletonList(discoveryAttribute);
        }
        when(organizationDiscoveryManager.getOrganizationDiscoveryAttributes(SUB_ORG_TENANT_DOMAIN, false))
                .thenReturn(discoveryAttributes);
    }

    private void assertPostAuthnFailure(String expectedErrorCode) {

        try {
            emailDomainValidationHandler.handle(request, response, context);
            fail("Expected a PostAuthenticationFailedException with the error code: " + expectedErrorCode);
        } catch (PostAuthenticationFailedException e) {
            assertEquals(e.getErrorCode(), expectedErrorCode, "Unexpected error code in the thrown exception.");
        }
    }

    private Field getDiscoveryManagerField() throws NoSuchFieldException {

        Field field = EmailDomainValidationHandler.class.getDeclaredField(DISCOVERY_MANAGER_FIELD);
        field.setAccessible(true);
        return field;
    }
}

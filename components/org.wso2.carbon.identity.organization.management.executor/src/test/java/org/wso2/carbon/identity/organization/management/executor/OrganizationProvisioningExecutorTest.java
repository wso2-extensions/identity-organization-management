/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.executor;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.flow.execution.engine.Constants;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowOrganization;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowUser;
import org.wso2.carbon.identity.organization.management.executor.internal.OrganizationManagementExecutorDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.TenantTypeOrganization;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.organization.management.executor.ExecutorConstants.ExecutorErrorMessages
        .ERROR_CODE_INVALID_ORGANIZATION_NAME;
import static org.wso2.carbon.identity.organization.management.executor.ExecutorConstants.ExecutorErrorMessages
        .ERROR_CODE_ORGANIZATION_HANDLE_ALREADY_EXISTS;
import static org.wso2.carbon.identity.organization.management.executor.ExecutorConstants.ExecutorErrorMessages
        .ERROR_CODE_ORGANIZATION_ONBOARD_FAILURE;
import static org.wso2.carbon.identity.organization.management.executor.ExecutorConstants.ExecutorErrorMessages
        .ERROR_CODE_ORGANIZATION_PROVISIONING_FAILURE;
import static org.wso2.carbon.identity.organization.management.executor.ExecutorConstants.ExecutorErrorMessages
        .ERROR_CODE_RESOLVE_PARENT_ORGANIZATION_FAILURE;

/**
 * Unit tests for {@link OrganizationProvisioningExecutor}.
 */
public class OrganizationProvisioningExecutorTest {

    private static final String PARENT_ORG_ID = "9e7f1c33-1a2b-4d5e-8f90-abcdef123456";
    private static final String GENERATED_ORG_ID = "11112222-3333-4444-5555-666677778888";
    private static final String USER_ID = "aaaa1111-bbbb-2222-cccc-333344445555";
    private static final String USERNAME = "jane";
    private static final String ORG_NAME = "Acme Corporation";
    private static final String TENANT_DOMAIN = "parentorg";
    private static final String ADMIN_USER_ID = "dddd4444-eeee-5555-ffff-666677778888";
    private static final String ADMIN_USERNAME = "parent-admin";
    private static final int PARENT_TENANT_ID = 34;

    private OrganizationProvisioningExecutor executor;
    private OrganizationManager organizationManager;
    private UserRealm userRealm;
    private RealmConfiguration realmConfiguration;
    private MockedStatic<Utils> utils;

    @BeforeMethod
    public void setUp() throws Exception {

        // PrivilegedCarbonContext cannot be instrumented until carbon home is set.
        System.setProperty(CarbonBaseConstants.CARBON_HOME,
                Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString());

        executor = new OrganizationProvisioningExecutor();
        organizationManager = mock(OrganizationManager.class);
        OrganizationManagementExecutorDataHolder.getInstance().setOrganizationManager(organizationManager);

        RealmService realmService = mock(RealmService.class);
        userRealm = mock(UserRealm.class);
        realmConfiguration = mock(RealmConfiguration.class);
        when(realmService.getTenantUserRealm(anyInt())).thenReturn(userRealm);
        when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
        when(realmConfiguration.getAdminUserId()).thenReturn(ADMIN_USER_ID);
        when(realmConfiguration.getAdminUserName()).thenReturn(ADMIN_USERNAME);
        OrganizationManagementExecutorDataHolder.getInstance().setRealmService(realmService);

        utils = mockStatic(Utils.class);
        utils.when(Utils::generateUniqueID).thenReturn(GENERATED_ORG_ID);
        when(organizationManager.resolveOrganizationId(TENANT_DOMAIN)).thenReturn(PARENT_ORG_ID);

        // No handle is taken unless a test says otherwise.
        when(organizationManager.isOrganizationExistByHandle(anyString())).thenReturn(false);
    }

    @AfterMethod
    public void tearDown() {

        utils.close();
        OrganizationManagementExecutorDataHolder.getInstance().setOrganizationManager(null);
    }

    @Test
    public void testExecutorName() {

        Assert.assertEquals(executor.getName(), "OrganizationProvisioningExecutor");
    }

    @Test
    public void testBlankOrganizationNameReturnsUserError() throws Exception {

        FlowExecutionContext context = buildContext(null, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_USER_ERROR);
        Assert.assertEquals(response.getErrorCode(), ERROR_CODE_INVALID_ORGANIZATION_NAME.getCode());
        verify(organizationManager, never()).addOrganization(any());
    }

    @Test
    public void testCreatorFallsBackToCurrentOrganizationAdmin() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.getFlowUser().setUserId(null);
        context.getFlowUser().setUsername(null);

        try (MockedStatic<PrivilegedCarbonContext> carbonContext = mockStatic(PrivilegedCarbonContext.class)) {
            PrivilegedCarbonContext threadLocalContext = mock(PrivilegedCarbonContext.class);
            carbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                    .thenReturn(threadLocalContext);
            when(threadLocalContext.getTenantId()).thenReturn(PARENT_TENANT_ID);

            ExecutorResponse response = executor.execute(context);

            Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_COMPLETE);
            Organization created = captureCreatedOrganization();
            Assert.assertEquals(created.getCreatorId(), ADMIN_USER_ID);
            Assert.assertEquals(created.getCreatorUsername(), ADMIN_USERNAME);
        }
    }

    @Test
    public void testCreatorIdIsResolvedWhenRealmHasNoAdminUserId() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.getFlowUser().setUserId(null);
        context.getFlowUser().setUsername(null);
        AbstractUserStoreManager userStoreManager = mock(AbstractUserStoreManager.class);
        when(realmConfiguration.getAdminUserId()).thenReturn(null);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(userStoreManager.getUserIDFromUserName(ADMIN_USERNAME)).thenReturn(ADMIN_USER_ID);

        try (MockedStatic<PrivilegedCarbonContext> carbonContext = mockStatic(PrivilegedCarbonContext.class)) {
            PrivilegedCarbonContext threadLocalContext = mock(PrivilegedCarbonContext.class);
            carbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                    .thenReturn(threadLocalContext);
            when(threadLocalContext.getTenantId()).thenReturn(PARENT_TENANT_ID);

            executor.execute(context);

            Organization created = captureCreatedOrganization();
            Assert.assertEquals(created.getCreatorId(), ADMIN_USER_ID);
            Assert.assertEquals(created.getCreatorUsername(), ADMIN_USERNAME);
        }
    }

    @Test
    public void testProvisionedUserRemainsCreator() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);

        executor.execute(context);

        Organization created = captureCreatedOrganization();
        Assert.assertEquals(created.getCreatorId(), USER_ID);
        Assert.assertEquals(created.getCreatorUsername(), USERNAME);
    }

    @Test
    public void testUnresolvableParentReturnsError() throws Exception {

        when(organizationManager.resolveOrganizationId(TENANT_DOMAIN)).thenThrow(
                new OrganizationManagementClientException("Not found.", "Not found.", "60024"));
        FlowExecutionContext context = buildContext(ORG_NAME, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_ERROR);
        Assert.assertEquals(response.getErrorCode(), ERROR_CODE_RESOLVE_PARENT_ORGANIZATION_FAILURE.getCode());
        verify(organizationManager, never()).addOrganization(any());
    }

    @Test
    public void testSuccessfulCreation() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_COMPLETE);

        Organization created = captureCreatedOrganization();
        Assert.assertTrue(created instanceof TenantTypeOrganization);
        Assert.assertEquals(created.getName(), ORG_NAME);
        Assert.assertEquals(created.getParent().getId(), PARENT_ORG_ID);
        Assert.assertEquals(created.getCreatorId(), USER_ID);
        Assert.assertEquals(created.getCreatorUsername(), USERNAME);
        Assert.assertEquals(created.getType(),
                OrganizationManagementConstants.OrganizationTypes.TENANT.toString());
        Assert.assertEquals(created.getStatus(),
                OrganizationManagementConstants.OrganizationStatus.ACTIVE.toString());
    }

    @Test
    public void testParentIsTheRequestInitiatedOrganization() throws Exception {

        String subOrgTenantDomain = "suborg";
        String subOrgId = "5555aaaa-6666-bbbb-7777-cccc8888dddd";
        when(organizationManager.resolveOrganizationId(subOrgTenantDomain)).thenReturn(subOrgId);
        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.setTenantDomain(subOrgTenantDomain);

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getParent().getId(), subOrgId);
    }

    @Test
    public void testSubmittedHandleIsUsed() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, "  customHandle  ");

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getOrganizationHandle(), "customHandle");
        verify(organizationManager).isOrganizationExistByHandle("customHandle");
    }

    @Test
    public void testTakenSubmittedHandleReturnsUserError() throws Exception {

        when(organizationManager.isOrganizationExistByHandle("takenhandle")).thenReturn(true);
        utils.when(() -> Utils.handleClientException(
                        OrganizationManagementConstants.ErrorMessages.ERROR_CODE_EXISTING_ORGANIZATION_HANDLE,
                        "takenhandle"))
                .thenCallRealMethod();
        FlowExecutionContext context = buildContext(ORG_NAME, "takenhandle");

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_USER_ERROR);
        Assert.assertEquals(response.getErrorCode(), ERROR_CODE_ORGANIZATION_HANDLE_ALREADY_EXISTS.getCode());
        verify(organizationManager, never()).addOrganization(any());
    }

    @Test
    public void testOrganizationIdIsTheHandleWhenNoneIsSubmitted() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);

        executor.execute(context);

        Organization created = captureCreatedOrganization();
        Assert.assertEquals(created.getId(), GENERATED_ORG_ID);
        Assert.assertEquals(created.getOrganizationHandle(), GENERATED_ORG_ID);
        Assert.assertEquals(context.getFlowOrganization().getOrganizationHandle(), GENERATED_ORG_ID);
        verify(organizationManager, never()).isOrganizationExistByHandle(anyString());
    }

    @Test
    public void testDescriptionFromFlowOrganization() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.getFlowOrganization().setOrganizationDescription("A real business");

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getDescription(), "A real business");
    }

    @Test
    public void testNoDescriptionCollected() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);

        executor.execute(context);

        Assert.assertNull(captureCreatedOrganization().getDescription());
    }

    @Test
    public void testCustomAttributesArePersisted() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.getFlowOrganization().setAttribute("industry", "software");

        executor.execute(context);

        Organization created = captureCreatedOrganization();
        Assert.assertEquals(created.getAttributes().size(), 1);
        OrganizationAttribute attribute = created.getAttributes().getFirst();
        Assert.assertEquals(attribute.getKey(), "industry");
        Assert.assertEquals(attribute.getValue(), "software");
    }

    @Test
    public void testFlowCollectedCreatorAttributesDoNotOverrideCreator() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.getFlowOrganization().setAttribute(OrganizationManagementConstants.CREATOR_ID, "spoofed-id");
        context.getFlowOrganization().setAttribute(OrganizationManagementConstants.CREATOR_USERNAME, "spoofed-user");
        context.getFlowOrganization().setAttribute(OrganizationManagementConstants.CREATOR_EMAIL, "spoofed@mail.com");

        executor.execute(context);

        Organization created = captureCreatedOrganization();
        Assert.assertEquals(created.getCreatorId(), USER_ID);
        Assert.assertEquals(created.getCreatorUsername(), USERNAME);
        Assert.assertTrue(created.getAttributes().isEmpty(),
                "Creator details set by the executor must not be overridden by flow collected attributes.");
    }

    @Test
    public void testClientFailureReturnsUserError() throws Exception {

        doThrow(new OrganizationManagementClientException(
                        "Organization name already in use.", "Organization name already in use.", "60116"))
                .when(organizationManager).addOrganization(any());
        FlowExecutionContext context = buildContext(ORG_NAME, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_USER_ERROR);
        Assert.assertEquals(response.getErrorCode(), ERROR_CODE_ORGANIZATION_PROVISIONING_FAILURE.getCode());
    }

    @Test
    public void testServerFailureReturnsError() throws Exception {

        doThrowOnAdd();
        FlowExecutionContext context = buildContext(ORG_NAME, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_ERROR);
        Assert.assertEquals(response.getErrorCode(), ERROR_CODE_ORGANIZATION_ONBOARD_FAILURE.getCode());
        Assert.assertFalse(response.getErrorMessage().contains("Creation failed"),
                "The internal failure message must not reach the end user.");
    }

    private void doThrowOnAdd() throws OrganizationManagementException {

        doThrow(new OrganizationManagementServerException("Creation failed", "ERR_01"))
                .when(organizationManager).addOrganization(any());
    }

    private Organization captureCreatedOrganization() throws OrganizationManagementException {

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationManager).addOrganization(captor.capture());
        return captor.getValue();
    }

    @Test
    public void testRollbackDeletesTheCreatedOrganization() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        executor.execute(context);
        when(organizationManager.resolveOrganizationId(GENERATED_ORG_ID)).thenReturn(GENERATED_ORG_ID);
        when(organizationManager.isOrganizationExistById(GENERATED_ORG_ID)).thenReturn(true);

        executor.rollback(context);

        verify(organizationManager).deleteOrganization(GENERATED_ORG_ID);
        Assert.assertNull(context.getFlowOrganization().getOrganizationHandle(),
                "Clearing the handle keeps a repeated rollback from deleting something else.");
    }

    @Test
    public void testRollbackDeletesNothingWhenTheOrganizationWasNeverCreated() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);

        executor.rollback(context);

        verify(organizationManager, never()).deleteOrganization(anyString());
    }

    @Test
    public void testRollbackDeletesNothingWhenTheOrganizationIsAlreadyGone() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        executor.execute(context);
        when(organizationManager.resolveOrganizationId(GENERATED_ORG_ID)).thenReturn(GENERATED_ORG_ID);
        when(organizationManager.isOrganizationExistById(GENERATED_ORG_ID)).thenReturn(false);

        executor.rollback(context);

        verify(organizationManager, never()).deleteOrganization(anyString());
    }

    @Test
    public void testRollbackFailureIsContainedAndDoesNotThrow() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        executor.execute(context);
        when(organizationManager.resolveOrganizationId(GENERATED_ORG_ID)).thenReturn(GENERATED_ORG_ID);
        when(organizationManager.isOrganizationExistById(GENERATED_ORG_ID)).thenReturn(true);
        doThrow(new OrganizationManagementServerException("Delete failed", "ERR_02"))
                .when(organizationManager).deleteOrganization(GENERATED_ORG_ID);

        executor.rollback(context);

        verify(organizationManager).deleteOrganization(GENERATED_ORG_ID);
    }

    /**
     * Builds a context carrying a provisioned user, mirroring a flow where the user provisioning step
     * has already run.
     *
     * @param organizationName   Organization name collected by the flow.
     * @param organizationHandle Organization handle collected by the flow, or {@code null}.
     */
    private FlowExecutionContext buildContext(String organizationName, String organizationHandle) {

        FlowUser user = new FlowUser();
        user.setUserId(USER_ID);
        user.setUsername(USERNAME);

        FlowOrganization organization = new FlowOrganization();
        organization.setOrganizationName(organizationName);
        organization.setOrganizationHandle(organizationHandle);

        FlowExecutionContext context = new FlowExecutionContext();
        context.setTenantDomain(TENANT_DOMAIN);
        context.setFlowUser(user);
        context.setFlowOrganization(organization);
        return context;
    }
}

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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.flow.execution.engine.Constants;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowOrganization;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowUser;
import org.wso2.carbon.identity.organization.management.executor.internal.OrganizationManagementExecutorDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.TenantTypeOrganization;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrganizationProvisioningExecutor}.
 */
public class OrganizationProvisioningExecutorTest {

    private static final String PARENT_ORG_ID = "9e7f1c33-1a2b-4d5e-8f90-abcdef123456";
    private static final String GENERATED_ORG_ID = "11112222-3333-4444-5555-666677778888";
    private static final String USER_ID = "aaaa1111-bbbb-2222-cccc-333344445555";
    private static final String USERNAME = "jane";
    private static final String ORG_NAME = "Acme Corporation";
    private static final String DERIVED_HANDLE = "acmecorporation";

    private OrganizationProvisioningExecutor executor;
    private OrganizationManager organizationManager;
    private MockedStatic<Utils> utils;

    @BeforeMethod
    public void setUp() throws Exception {

        executor = new OrganizationProvisioningExecutor();
        organizationManager = mock(OrganizationManager.class);
        OrganizationManagementExecutorDataHolder.getInstance().setOrganizationManager(organizationManager);

        utils = mockStatic(Utils.class);
        utils.when(Utils::getOrganizationId).thenReturn(PARENT_ORG_ID);
        utils.when(Utils::generateUniqueID).thenReturn(GENERATED_ORG_ID);

        // No handle is taken unless a test says otherwise.
        when(organizationManager.isOrganizationExistByHandle(anyString())).thenReturn(false);
    }

    @AfterMethod
    public void tearDown() {

        utils.close();
        OrganizationManagementExecutorDataHolder.getInstance().setOrganizationManager(null);
    }

    @Test(description = "Executor name is referenced by string from persisted flows and must not change.")
    public void testExecutorName() {

        Assert.assertEquals(executor.getName(), "OrganizationProvisioningExecutor");
    }

    @Test(description = "A blank organization name cannot be derived, so the user is asked again.")
    public void testBlankOrganizationNameReturnsRetry() throws Exception {

        FlowExecutionContext context = buildContext(null, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_RETRY);
        verify(organizationManager, never()).addOrganization(any());
    }

    @Test(description = "Without an upstream provisioning step the flow is misconfigured, so it errors.")
    public void testMissingUserIdReturnsError() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.getFlowUser().setUserId(null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_ERROR);
        verify(organizationManager, never()).addOrganization(any());
    }

    @Test(description = "An unresolvable parent must abort rather than create the org in the wrong place.")
    public void testUnresolvableParentReturnsError() throws Exception {

        utils.when(Utils::getOrganizationId).thenThrow(new RuntimeException("no carbon context"));
        FlowExecutionContext context = buildContext(ORG_NAME, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_ERROR);
        verify(organizationManager, never()).addOrganization(any());
    }

    @Test(description = "The happy path persists the organization and reports the new ID forward.")
    public void testSuccessfulCreation() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_COMPLETE);
        Assert.assertEquals(response.getContextProperties().get("org.created.id"), GENERATED_ORG_ID);
        Assert.assertEquals(response.getContextProperties().get("org.created.name"), ORG_NAME);

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

    @Test(description = "The parent is the organization the flow runs in, so sub-organizations can onboard children.")
    public void testParentIsTheExecutingOrganization() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);

        String subOrgId = "5555aaaa-6666-bbbb-7777-cccc8888dddd";
        utils.when(Utils::getOrganizationId).thenReturn(subOrgId);

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getParent().getId(), subOrgId);
    }

    @Test(description = "A handle submitted through the flow wins over a derived one.")
    public void testSubmittedHandleWins() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, "  customHandle  ");

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getOrganizationHandle(), "customHandle");
        verify(organizationManager, never()).isOrganizationExistByHandle(anyString());
    }

    @Test(dataProvider = "handleDerivationProvider",
            description = "Derived handles must match what the console handle field would produce.")
    public void testHandleDerivation(String organizationName, String expectedHandle) throws Exception {

        FlowExecutionContext context = buildContext(organizationName, null);

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getOrganizationHandle(), expectedHandle);
    }

    @DataProvider(name = "handleDerivationProvider")
    public Object[][] handleDerivationProvider() {

        return new Object[][]{
                {"Acme Corporation", DERIVED_HANDLE},
                {"Acme Corp Ltd.", "acmecorpltd"},
                {"  Hello World  ", "helloworld"},
                {"123 Acme Corp", "acmecorp"},
                {"acme-corp_1.0", "acmecorp10"},
                {"The Very Long Organization Name That Exceeds Thirty", "theverylongorganizationnametha"}
        };
    }

    @Test(description = "A name with nothing usable leaves the handle to the server, which uses the org ID.")
    public void testUnusableNameYieldsNullHandle() throws Exception {

        FlowExecutionContext context = buildContext("!!! ???", null);

        executor.execute(context);

        Assert.assertNull(captureCreatedOrganization().getOrganizationHandle());
    }

    @Test(description = "A short but usable name is padded, rather than left to a server generated handle.")
    public void testShortNameIsPaddedToMinimumLength() throws Exception {

        FlowExecutionContext context = buildContext("IBM", null);

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getOrganizationHandle(), "ibm0");
    }

    @Test(description = "A name yielding a single usable character is padded up to the minimum length.")
    public void testVeryShortNameIsPaddedToMinimumLength() throws Exception {

        FlowExecutionContext context = buildContext("A B", null);

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getOrganizationHandle(), "ab00");
    }

    @Test(description = "A taken handle gets a numeric suffix rather than failing.")
    public void testHandleCollisionAppendsSuffix() throws Exception {

        when(organizationManager.isOrganizationExistByHandle(DERIVED_HANDLE)).thenReturn(true);
        FlowExecutionContext context = buildContext(ORG_NAME, null);

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getOrganizationHandle(), DERIVED_HANDLE + "1");
    }

    @Test(description = "When the short suffixes are exhausted a random token is used, not endless probing.")
    public void testExhaustedSuffixesFallBackToRandomToken() throws Exception {

        when(organizationManager.isOrganizationExistByHandle(anyString())).thenAnswer(
                invocation -> {
                    String candidate = invocation.getArgument(0);
                    // Every numeric suffix is taken; only the random token is free.
                    return candidate.length() <= DERIVED_HANDLE.length() + 1;
                });
        FlowExecutionContext context = buildContext(ORG_NAME, null);

        executor.execute(context);

        String handle = captureCreatedOrganization().getOrganizationHandle();
        Assert.assertNotNull(handle);
        Assert.assertTrue(handle.startsWith(DERIVED_HANDLE));
        Assert.assertEquals(handle.length(), DERIVED_HANDLE.length() + 6);
    }

    @Test(description = "The description is a first class flow organization field, carried straight over.")
    public void testDescriptionFromFlowOrganization() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.getFlowOrganization().setOrganizationDescription("A real business");

        executor.execute(context);

        Assert.assertEquals(captureCreatedOrganization().getDescription(), "A real business");
    }

    @Test(description = "A flow that collects no description leaves the organization description unset.")
    public void testNoDescriptionCollected() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);

        executor.execute(context);

        Assert.assertNull(captureCreatedOrganization().getDescription());
    }

    @Test(description = "Custom attributes collected by the flow are persisted on the organization.")
    public void testCustomAttributesArePersisted() throws Exception {

        FlowExecutionContext context = buildContext(ORG_NAME, null);
        context.getFlowOrganization().setAttributes("industry", "software");

        executor.execute(context);

        Organization created = captureCreatedOrganization();
        Assert.assertEquals(created.getAttributes().size(), 1);
        OrganizationAttribute attribute = created.getAttributes().get(0);
        Assert.assertEquals(attribute.getKey(), "industry");
        Assert.assertEquals(attribute.getValue(), "software");
    }

    @Test(description = "A failure from the organization manager is surfaced as a retry, not a crash.")
    public void testCreationFailureReturnsRetry() throws Exception {

        doThrowOnAdd();
        FlowExecutionContext context = buildContext(ORG_NAME, null);

        ExecutorResponse response = executor.execute(context);

        Assert.assertEquals(response.getResult(), Constants.ExecutorStatus.STATUS_RETRY);
    }

    private void doThrowOnAdd() throws OrganizationManagementException {

        org.mockito.Mockito.doThrow(new OrganizationManagementServerException("Creation failed", "ERR_01"))
                .when(organizationManager).addOrganization(any());
    }

    private Organization captureCreatedOrganization() throws OrganizationManagementException {

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationManager).addOrganization(captor.capture());
        return captor.getValue();
    }

    /**
     * Builds a context carrying a provisioned user, mirroring a flow where the user provisioning step
     * has already run.
     *
     * @param organizationName   Organization name collected by the flow.
     * @param organizationHandle Organization handle collected by the flow, or {@code null}.
     * @return The flow execution context.
     */
    private FlowExecutionContext buildContext(String organizationName, String organizationHandle) {

        FlowUser user = new FlowUser();
        user.setUserId(USER_ID);
        user.setUsername(USERNAME);

        FlowOrganization organization = new FlowOrganization();
        organization.setOrganizationName(organizationName);
        organization.setOrganizationHandle(organizationHandle);

        FlowExecutionContext context = new FlowExecutionContext();
        context.setFlowUser(user);
        context.setFlowOrganization(organization);
        return context;
    }
}

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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.service;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.dto.GeneralConnectionShareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.dto.GeneralConnectionUnshareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.dto.GetConnectionSharedOrgsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.dto.ResponseSharedConnectionOrgsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.dto.SelectiveConnectionShareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.dto.SelectiveConnectionShareOrgConfigDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.dto.SelectiveConnectionUnshareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.component.ConnectionSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler.ConnectionTypeHandler;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.util.ConnectionSharingInitiatorContext;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConnectionSharingPolicyHandlerServiceImpl}: input validation, connection-type-handler
 * resolution, and the actual share / unshare dispatch to the resolved {@link ConnectionTypeHandler}. The
 * asynchronous processing is run synchronously via a direct executor injected through the package-private
 * constructor so the dispatch can be verified deterministically.
 */
public class ConnectionSharingPolicyHandlerServiceImplTest {

    private static final ResourceType RESOURCE_TYPE = ResourceType.CONNECTION_IDENTITY_PROVIDER;
    private static final String CONNECTION_ID = "connection-id";
    private static final String INITIATING_ORG_ID = "initiating-org-id";
    private static final String CHILD_ORG_ID = "child-org-id";

    private MockedStatic<ConnectionSharingDataHolder> mockedDataHolder;
    private MockedStatic<ConnectionSharingInitiatorContext> mockedInitiatorContext;
    private MockedStatic<PrivilegedCarbonContext> mockedCarbonContext;
    private ConnectionSharingDataHolder dataHolder;
    private ConnectionTypeHandler handler;
    private ResourceSharingPolicyHandlerService policyService;
    private OrganizationManager organizationManager;

    private ConnectionSharingPolicyHandlerServiceImpl service;

    @BeforeClass
    public void setUpClass() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH,
                Paths.get(carbonHome, "repository", "conf").toString());
    }

    @BeforeMethod
    public void setUp() {

        mockedDataHolder = mockStatic(ConnectionSharingDataHolder.class);
        mockedInitiatorContext = mockStatic(ConnectionSharingInitiatorContext.class);
        mockedCarbonContext = mockStatic(PrivilegedCarbonContext.class);

        handler = mock(ConnectionTypeHandler.class);
        policyService = mock(ResourceSharingPolicyHandlerService.class);
        organizationManager = mock(OrganizationManager.class);
        dataHolder = mock(ConnectionSharingDataHolder.class);

        mockedDataHolder.when(ConnectionSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getConnectionTypeHandler(RESOURCE_TYPE)).thenReturn(handler);
        when(dataHolder.getResourceSharingPolicyHandlerService()).thenReturn(policyService);
        when(dataHolder.getOrganizationManager()).thenReturn(organizationManager);
        when(handler.getResourceType()).thenReturn(RESOURCE_TYPE);

        ConnectionSharingInitiatorContext context = mock(ConnectionSharingInitiatorContext.class);
        when(context.getSharingInitiatedOrgId()).thenReturn(INITIATING_ORG_ID);
        when(context.getSharingInitiatedTenantDomain()).thenReturn("carbon.super");
        mockedInitiatorContext.when(ConnectionSharingInitiatorContext::capture).thenReturn(context);

        mockedCarbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                .thenReturn(mock(PrivilegedCarbonContext.class));

        // Direct executor runs the asynchronous processing synchronously on the calling thread.
        Executor directExecutor = Runnable::run;
        service = new ConnectionSharingPolicyHandlerServiceImpl(directExecutor);
    }

    @AfterMethod
    public void tearDown() {

        mockedDataHolder.close();
        mockedInitiatorContext.close();
        mockedCarbonContext.close();
    }

    // ----- Validation -----

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testPopulateSelectiveConnectionShareRejectsNullRequest() throws Exception {

        service.populateSelectiveConnectionShare(null);
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testPopulateGeneralConnectionShareRejectsNullRequest() throws Exception {

        service.populateGeneralConnectionShare(null);
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testPopulateSelectiveConnectionUnshareRejectsNullRequest() throws Exception {

        service.populateSelectiveConnectionUnshare(null);
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testPopulateGeneralConnectionUnshareRejectsNullRequest() throws Exception {

        service.populateGeneralConnectionUnshare(null);
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testGetConnectionSharedOrganizationsRejectsNullRequest() throws Exception {

        service.getConnectionSharedOrganizations(null);
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testPopulateGeneralConnectionShareRejectsMissingConnectionId() throws Exception {

        GeneralConnectionShareDTO dto = new GeneralConnectionShareDTO();
        dto.setResourceType(RESOURCE_TYPE);
        dto.setPolicy(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS);
        service.populateGeneralConnectionShare(dto);
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testPopulateGeneralConnectionShareRejectsUnsupportedPolicy() throws Exception {

        GeneralConnectionShareDTO dto = new GeneralConnectionShareDTO();
        dto.setConnectionId(CONNECTION_ID);
        dto.setResourceType(RESOURCE_TYPE);
        // General sharing only supports ALL_EXISTING_AND_FUTURE_ORGS.
        dto.setPolicy(PolicyEnum.SELECTED_ORG_ONLY);
        service.populateGeneralConnectionShare(dto);
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testPopulateGeneralConnectionShareRejectsWhenNoHandlerRegistered() throws Exception {

        when(dataHolder.getConnectionTypeHandler(RESOURCE_TYPE)).thenReturn(null);
        service.populateGeneralConnectionShare(generalShareDto());
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testGetConnectionSharedOrganizationsRejectsUnsupportedAttribute() throws Exception {

        GetConnectionSharedOrgsDTO dto = getSharedOrgsDto();
        dto.setAttributes(Collections.singletonList("unsupportedAttribute"));
        service.getConnectionSharedOrganizations(dto);
    }

    // ----- Dispatch -----

    @Test
    public void testGeneralConnectionShareDispatchesToHandler() throws Exception {

        service.populateGeneralConnectionShare(generalShareDto());

        verify(handler).shareConnectionToAllOrgs(CONNECTION_ID, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS,
                INITIATING_ORG_ID);
        verify(policyService).addResourceSharingPolicyWithAttributes(any(), any());
    }

    @Test
    public void testGeneralConnectionUnshareDispatchesToHandler() throws Exception {

        GeneralConnectionUnshareDTO dto = new GeneralConnectionUnshareDTO();
        dto.setConnectionId(CONNECTION_ID);
        dto.setResourceType(RESOURCE_TYPE);

        service.populateGeneralConnectionUnshare(dto);

        verify(handler).unshareConnectionFromAllOrgs(CONNECTION_ID, INITIATING_ORG_ID);
    }

    @Test
    public void testSelectiveConnectionShareDispatchesToImmediateChildOrganization() throws Exception {

        // Only immediate children are eligible for selective sharing.
        when(organizationManager.getChildOrganizationsIds(INITIATING_ORG_ID, false))
                .thenReturn(Collections.singletonList(CHILD_ORG_ID));

        SelectiveConnectionShareOrgConfigDTO orgConfig = new SelectiveConnectionShareOrgConfigDTO();
        orgConfig.setOrgId(CHILD_ORG_ID);
        orgConfig.setPolicy(PolicyEnum.SELECTED_ORG_ONLY);
        SelectiveConnectionShareDTO dto = new SelectiveConnectionShareDTO();
        dto.setConnectionId(CONNECTION_ID);
        dto.setResourceType(RESOURCE_TYPE);
        dto.setOrganizations(Collections.singletonList(orgConfig));

        service.populateSelectiveConnectionShare(dto);

        verify(handler).shareConnectionToOrg(CONNECTION_ID, CHILD_ORG_ID, PolicyEnum.SELECTED_ORG_ONLY,
                INITIATING_ORG_ID);
    }

    @Test
    public void testSelectiveConnectionShareSkipsNonChildOrganization() throws Exception {

        // The target organization is not an immediate child, so it must be filtered out (never shared).
        when(organizationManager.getChildOrganizationsIds(INITIATING_ORG_ID, false))
                .thenReturn(Collections.emptyList());

        SelectiveConnectionShareOrgConfigDTO orgConfig = new SelectiveConnectionShareOrgConfigDTO();
        orgConfig.setOrgId(CHILD_ORG_ID);
        orgConfig.setPolicy(PolicyEnum.SELECTED_ORG_ONLY);
        SelectiveConnectionShareDTO dto = new SelectiveConnectionShareDTO();
        dto.setConnectionId(CONNECTION_ID);
        dto.setResourceType(RESOURCE_TYPE);
        dto.setOrganizations(Collections.singletonList(orgConfig));

        service.populateSelectiveConnectionShare(dto);

        verify(handler, org.mockito.Mockito.never()).shareConnectionToOrg(anyString(), anyString(), any(),
                anyString());
    }

    @Test
    public void testSelectiveConnectionUnshareDispatchesToHandler() throws Exception {

        SelectiveConnectionUnshareDTO dto = new SelectiveConnectionUnshareDTO();
        dto.setConnectionId(CONNECTION_ID);
        dto.setResourceType(RESOURCE_TYPE);
        dto.setOrgIds(Collections.singletonList(CHILD_ORG_ID));

        service.populateSelectiveConnectionUnshare(dto);

        verify(handler).unshareConnectionFromOrg(CONNECTION_ID, CHILD_ORG_ID, INITIATING_ORG_ID);
    }

    // ----- Query -----

    @Test
    public void testGetConnectionSharedOrganizationsReturnsEmptyResponseWhenNoAssociations() throws Exception {

        when(organizationManager.getChildOrganizationsIds(anyString(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(handler.getConnectionAssociations(anyString(), anyString(), anyList(), anyList(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        ResponseSharedConnectionOrgsDTO response = service.getConnectionSharedOrganizations(getSharedOrgsDto());

        Assert.assertNotNull(response);
        verify(handler).getConnectionAssociations(eq(CONNECTION_ID), eq(INITIATING_ORG_ID), anyList(), anyList(),
                anyString(), anyInt());
    }

    @Test
    public void testGetConnectionSharedOrganizationsReturnsOrgDetails() throws Exception {

        ConnectionAssociation association = new ConnectionAssociation.Builder()
                .resourceType(RESOURCE_TYPE)
                .parentConnectionId(CONNECTION_ID)
                .connectionResidentOrganizationId(INITIATING_ORG_ID)
                .sharedConnectionId("shadow-id")
                .organizationId(CHILD_ORG_ID)
                .build();
        when(organizationManager.getChildOrganizationsIds(anyString(), anyBoolean()))
                .thenReturn(Collections.singletonList(CHILD_ORG_ID));
        when(handler.getConnectionAssociations(anyString(), anyString(), anyList(), anyList(), anyString(), anyInt()))
                .thenReturn(Collections.singletonList(association));

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(CHILD_ORG_ID);
        when(organization.getName()).thenReturn("Shared Organization");
        when(organization.getOrganizationHandle()).thenReturn("shared.org");
        when(organization.hasChildren()).thenReturn(false);
        when(organizationManager.getOrganization(CHILD_ORG_ID, true, false)).thenReturn(organization);
        when(organizationManager.resolveTenantDomain(CHILD_ORG_ID)).thenReturn("shared-tenant");
        when(organizationManager.getOrganizationDepthInHierarchy(CHILD_ORG_ID)).thenReturn(1);

        ResponseSharedConnectionOrgsDTO response = service.getConnectionSharedOrganizations(getSharedOrgsDto());

        Assert.assertEquals(response.getSharedOrgs().size(), 1);
        Assert.assertEquals(response.getSharedOrgs().get(0).getOrgId(), CHILD_ORG_ID);
        Assert.assertEquals(response.getSharedOrgs().get(0).getParentConnectionId(), CONNECTION_ID);
    }

    @Test
    public void testGetConnectionSharedOrganizationsResolvesSharingModeAttribute() throws Exception {

        ConnectionAssociation association = new ConnectionAssociation.Builder()
                .resourceType(RESOURCE_TYPE)
                .parentConnectionId(CONNECTION_ID)
                .connectionResidentOrganizationId(INITIATING_ORG_ID)
                .sharedConnectionId("shadow-id")
                .organizationId(CHILD_ORG_ID)
                .build();
        when(organizationManager.getChildOrganizationsIds(anyString(), anyBoolean()))
                .thenReturn(Collections.singletonList(CHILD_ORG_ID));
        when(handler.getConnectionAssociations(anyString(), anyString(), anyList(), anyList(), anyString(), anyInt()))
                .thenReturn(Collections.singletonList(association));
        when(policyService.getResourceSharingPolicyAndAttributesByInitiatingOrgId(anyString(), anyString(),
                anyString())).thenReturn(Collections.emptyMap());

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(CHILD_ORG_ID);
        when(organizationManager.getOrganization(CHILD_ORG_ID, true, false)).thenReturn(organization);
        when(organizationManager.resolveTenantDomain(CHILD_ORG_ID)).thenReturn("shared-tenant");
        when(organizationManager.getOrganizationDepthInHierarchy(CHILD_ORG_ID)).thenReturn(1);
        when(organizationManager.getOrganizationDepthInHierarchy(INITIATING_ORG_ID)).thenReturn(0);

        GetConnectionSharedOrgsDTO dto = getSharedOrgsDto();
        dto.setAttributes(Collections.singletonList("sharingMode"));

        ResponseSharedConnectionOrgsDTO response = service.getConnectionSharedOrganizations(dto);

        Assert.assertEquals(response.getSharedOrgs().size(), 1);
    }

    private GeneralConnectionShareDTO generalShareDto() {

        GeneralConnectionShareDTO dto = new GeneralConnectionShareDTO();
        dto.setConnectionId(CONNECTION_ID);
        dto.setResourceType(RESOURCE_TYPE);
        dto.setPolicy(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS);
        return dto;
    }

    private GetConnectionSharedOrgsDTO getSharedOrgsDto() {

        GetConnectionSharedOrgsDTO dto = new GetConnectionSharedOrgsDTO();
        dto.setConnectionId(CONNECTION_ID);
        dto.setResourceType(RESOURCE_TYPE);
        dto.setInitiatingOrgId(INITIATING_ORG_ID);
        dto.setAttributes(Collections.emptyList());
        return dto;
    }
}

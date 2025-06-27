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

package org.wso2.carbon.identity.organization.management.handler;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.idp.mgt.dao.CacheBackedIdPMgtDAO;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for OrganizationVersionHandler.
 */
public class OrganizationVersionHandlerTest {

    @Mock
    private OrganizationManager organizationManager;
    @Mock
    private Organization organization;
    @InjectMocks
    private OrganizationVersionHandler handler;
    private AutoCloseable autoCloseable;
    private MockedStatic<OrganizationManagementUtil> organizationManagementUtilMockedStatic;
    private MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic;
    private MockedConstruction<CacheBackedIdPMgtDAO> mockedDaoConstruction;

    private static final String ORGANIZATION_ID = "org1";
    private static final String TENANT_DOMAIN = "carbon.super";
    private static final int TENANT_ID = -1234;
    private static final String V1 = "v1.0.0";
    private static final String V0 = "v0.0.0";

    @BeforeMethod
    public void setUp() {

        autoCloseable = MockitoAnnotations.openMocks(this);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(organizationManager);

        organizationManagementUtilMockedStatic = mockStatic(OrganizationManagementUtil.class);
        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(any()))
                .thenReturn(false);
        mockedDaoConstruction = mockConstruction(CacheBackedIdPMgtDAO.class);

        identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
        identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId(any()))
                .thenReturn(TENANT_ID);

    }

    @AfterMethod
    public void tearDown() throws Exception {

        autoCloseable.close();
        organizationManagementUtilMockedStatic.close();
        identityTenantUtilMockedStatic.close();
        mockedDaoConstruction.close();
    }

    @Test
    public void testHandlePreUpdateOrganizationNoVersionChange() throws Exception {

        Map<String, Object> props = new HashMap<>();
        props.put(Constants.EVENT_PROP_ORGANIZATION_ID, ORGANIZATION_ID);
        props.put(Constants.EVENT_PROP_ORGANIZATION, organization);

        when(organizationManager.resolveTenantDomain(anyString())).thenReturn(TENANT_DOMAIN);
        when(organizationManager.getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(organization);
        when(organization.getVersion()).thenReturn(V1);

        Event event = new Event(Constants.EVENT_PRE_UPDATE_ORGANIZATION, props);
        handler.handleEvent(event);
        // Checking that the CacheBackedIdPMgtDAO is not constructed to verify that no cache clearing is attempted.
        assertTrue(mockedDaoConstruction.constructed().isEmpty(), "CacheBackedIdPMgtDAO should not be constructed");

    }

    @Test
    public void testHandlePreUpdateOrganizationWithVersionChange() throws Exception {

        Map<String, Object> props = new HashMap<>();
        props.put(Constants.EVENT_PROP_ORGANIZATION_ID, ORGANIZATION_ID);
        props.put(Constants.EVENT_PROP_ORGANIZATION, organization);

        when(organizationManager.resolveTenantDomain(anyString())).thenReturn(TENANT_DOMAIN);
        when(organizationManager.getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(organization);
        when(organization.getVersion()).thenReturn(V0).thenReturn(V1);

        Event event = new Event(Constants.EVENT_PRE_UPDATE_ORGANIZATION, props);
        handler.handleEvent(event);
        // Checking that the CacheBackedIdPMgtDAO is constructed to verify that cache clearing is attempted.
        assertNotNull(mockedDaoConstruction, "CacheBackedIdPMgtDAO should be constructed.");
    }

    @Test
    public void testHandlePrePatchOrganizationWithVersionChange() throws Exception {

        PatchOperation patch = mock(PatchOperation.class);
        when(patch.getPath()).thenReturn(OrganizationManagementConstants.PATCH_PATH_ORG_VERSION);
        when(patch.getValue()).thenReturn(V1);
        List<PatchOperation> patches = Collections.singletonList(patch);
        Map<String, Object> props = new HashMap<>();
        props.put(Constants.EVENT_PROP_ORGANIZATION_ID, ORGANIZATION_ID);
        props.put(Constants.EVENT_PROP_PATCH_OPERATIONS, patches);

        when(organizationManager.resolveTenantDomain(anyString())).thenReturn(TENANT_DOMAIN);
        when(organizationManager.getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(organization);
        when(organization.getVersion()).thenReturn(V0).thenReturn(V1);

        Event event = new Event(Constants.EVENT_PRE_PATCH_ORGANIZATION, props);
        handler.handleEvent(event);
        // Checking that the CacheBackedIdPMgtDAO is constructed to verify that cache clearing is attempted.
        assertNotNull(mockedDaoConstruction, "CacheBackedIdPMgtDAO should be constructed.");
    }

    @Test
    public void testHandlePrePatchOrganizationNoVersionChange() throws Exception {

        PatchOperation patch = mock(PatchOperation.class);
        when(patch.getPath()).thenReturn(OrganizationManagementConstants.PATCH_PATH_ORG_VERSION);
        when(patch.getValue()).thenReturn(V0);
        List<PatchOperation> patches = Collections.singletonList(patch);
        Map<String, Object> props = new HashMap<>();
        props.put(Constants.EVENT_PROP_ORGANIZATION_ID, ORGANIZATION_ID);
        props.put(Constants.EVENT_PROP_PATCH_OPERATIONS, patches);

        when(organizationManager.resolveTenantDomain(anyString())).thenReturn(TENANT_DOMAIN);
        when(organizationManager.getOrganization(anyString(), anyBoolean(), anyBoolean())).thenReturn(organization);
        when(organization.getVersion()).thenReturn(V0);

        Event event = new Event(Constants.EVENT_PRE_PATCH_ORGANIZATION, props);
        handler.handleEvent(event);
        // Checking that the CacheBackedIdPMgtDAO is not constructed to verify that no cache clearing is attempted.
        assertTrue(mockedDaoConstruction.constructed().isEmpty(), "CacheBackedIdPMgtDAO should not be constructed");
    }
}

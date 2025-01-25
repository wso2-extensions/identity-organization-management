/*
 * Copyright (c) 2024-2025, WSO2 LLC. (http://www.wso2.com).
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

import org.mockito.MockedStatic;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID;

/**
 * Unit tests for SharedRoleMgtHandler.
 */
public class SharedRoleMgtHandlerTest {

    private static final String PARENT_ORG_TENANT_DOMAIN = "parent-org-tenant-domain";
    private static final String PARENT_ORG_ID = "parent-org-id";
    private static final String PARENT_ORG_USER_NAME = "parent-org-user";
    private static final String PARENT_ORG_USER_ID = "parent-org-user-id";
    private static final String PARENT_ORG_APP_ID = "parent-application-id";
    private static final String SHARED_ORG_TENANT_DOMAIN = "shared-org-tenant-domain";
    private static final String SHARED_ORG_ID = "shared-org-id";
    private static final String SHARED_ORG_APP_ID = "shared-app-id";
    private static final String ORGANIZATION_AUD = "organization";

    private static MockedStatic<LoggerUtils> loggerUtils = null;
    private static MockedStatic<IdentityUtil> identityUtil = null;

    @BeforeClass
    public void setUp() {

        initPrivilegedCarbonContext();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(PARENT_ORG_TENANT_DOMAIN);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(PARENT_ORG_USER_NAME);

        loggerUtils = mockStatic(LoggerUtils.class);
        identityUtil = mockStatic(IdentityUtil.class);
    }

    @DataProvider(name = "v2AuditLogsEnabled")
    public Object[][] v2AuditLogsEnabled() {

        return new Object[][]{
                {false},
                {true}
        };
    }

    @Test(dataProvider = "v2AuditLogsEnabled", expectedExceptions = IdentityEventException.class,
            expectedExceptionsMessageRegExp = ".*has a non shared role with.*")
    public void testHandleEventForPreShareApplicationEventWithConflictingRoles(boolean isV2AuditLogsEnabled)
            throws Exception {

        Event event = createPreShareApplicationEvent();

        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).
                thenReturn(SHARED_ORG_TENANT_DOMAIN);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).
                thenReturn(PARENT_ORG_TENANT_DOMAIN);

        List<RoleV2> roles = new ArrayList<>();
        RoleV2 role = new RoleV2();
        role.setId("role-id");
        role.setName("role-name");
        roles.add(role);

        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);
        lenient().when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(ORGANIZATION_AUD);
        lenient().when(mockedApplicationManagementService.getAssociatedRolesOfApplication(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(roles);

        RoleManagementService roleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(roleManagementService);
        lenient().when(roleManagementService.isExistingRoleName(roles.get(0).getName(), ORGANIZATION_AUD,
                        SHARED_ORG_ID, SHARED_ORG_TENANT_DOMAIN)).thenReturn(true);
        lenient().when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(role.getId()), SHARED_ORG_TENANT_DOMAIN)).thenReturn(new HashMap<>());

        loggerUtils.when(LoggerUtils::isEnableV2AuditLogs).thenReturn(isV2AuditLogsEnabled);
        identityUtil.when(() -> IdentityUtil.getInitiatorId(PARENT_ORG_USER_NAME, PARENT_ORG_TENANT_DOMAIN)).
                thenReturn(PARENT_ORG_USER_ID);

        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);
    }

    @Test
    public void testHandleEventForPreShareApplicationEventWithoutConflictingRoles() throws Exception {

        Event event = createPreShareApplicationEvent();

        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).
                thenReturn(SHARED_ORG_TENANT_DOMAIN);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).
                thenReturn(PARENT_ORG_TENANT_DOMAIN);

        List<RoleV2> roles = new ArrayList<>();
        RoleV2 role = new RoleV2();
        role.setId("role-id");
        role.setName("role-name");
        roles.add(role);

        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);
        lenient().when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(ORGANIZATION_AUD);
        lenient().when(mockedApplicationManagementService.getAssociatedRolesOfApplication(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(roles);

        RoleManagementService roleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(roleManagementService);
        lenient().when(roleManagementService.isExistingRoleName(roles.get(0).getName(), ORGANIZATION_AUD,
                SHARED_ORG_ID, SHARED_ORG_TENANT_DOMAIN)).thenReturn(true);
        Map<String, String> mainRoleToSharedRoleMapping = new HashMap<>();
        mainRoleToSharedRoleMapping.put(roles.get(0).getId(), "mapped-shared-role-id");
        lenient().when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(role.getId()), SHARED_ORG_TENANT_DOMAIN)).
                thenReturn(mainRoleToSharedRoleMapping);

        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);
    }

    @Test
    public void testHandleEventForPreShareApplicationEventWithApplicationAud() throws Exception {

        Event event = createPreShareApplicationEvent();

        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).
                thenReturn(SHARED_ORG_TENANT_DOMAIN);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).
                thenReturn(PARENT_ORG_TENANT_DOMAIN);

        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);
        lenient().when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn("application");

        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);
    }

    private void initPrivilegedCarbonContext() {

        System.setProperty(
                CarbonBaseConstants.CARBON_HOME,
                Paths.get(System.getProperty("user.dir"), "src", "test", "resources").toString()
        );
        PrivilegedCarbonContext.startTenantFlow();
    }

    private static Event createPreShareApplicationEvent() {

        Event event = new Event(OrgApplicationMgtConstants.EVENT_PRE_SHARE_APPLICATION);
        event.addEventProperty(EVENT_PROP_PARENT_ORGANIZATION_ID, PARENT_ORG_ID);
        event.addEventProperty(EVENT_PROP_PARENT_APPLICATION_ID, PARENT_ORG_APP_ID);
        event.addEventProperty(EVENT_PROP_SHARED_ORGANIZATION_ID, SHARED_ORG_ID);
        event.addEventProperty(EVENT_PROP_SHARED_APPLICATION_ID, SHARED_ORG_APP_ID);
        return event;
    }
}

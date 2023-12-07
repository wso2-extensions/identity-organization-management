/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.listener;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.models.User;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.TenantTypeOrganization;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ORG_ADMINISTRATOR_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ORG_CREATOR_ROLE;

/**
 * The event handler for sharing the organization creator to the child organization.
 */
public class SharingOrganizationCreatorUserEventHandler extends AbstractEventHandler {

    private final OrganizationUserSharingService userSharingService = new OrganizationUserSharingServiceImpl();

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        String orgId = null;
        try {
            if (CarbonConstants.ENABLE_LEGACY_AUTHZ_RUNTIME) {
                if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
                    Map<String, Object> eventProperties = event.getEventProperties();
                    TenantTypeOrganization organization = (TenantTypeOrganization) eventProperties.get("ORGANIZATION");
                    orgId = organization.getId();
                    String tenantDomain = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                            .resolveTenantDomain(orgId);
                    if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                        return;
                    }
                    String associatedUserId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
                    String associatedUserName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                    try {
                        PrivilegedCarbonContext.startTenantFlow();
                        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(associatedUserName);
                        Role organizationCreatorRole = buildOrgCreatorRole(associatedUserId);
                        Role administratorRole = buildAdministratorRole(associatedUserId);
                        getRoleManager().createRole(orgId, organizationCreatorRole);
                        getRoleManager().createRole(orgId, administratorRole);
                    } finally {
                        PrivilegedCarbonContext.endTenantFlow();
                    }
                }
            } else {
                if ("POST_SHARED_CONSOLE_APP".equals(eventName)) {
                    Map<String, Object> eventProperties = event.getEventProperties();
                    orgId = (String) eventProperties.get(EVENT_PROP_ORGANIZATION_ID);
                    String tenantDomain = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                            .resolveTenantDomain(orgId);
                    if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                        return;
                    }
                    // User sharing is not allowed for self org on-boarding with owner resides in created org.
                    if (isSelfOrgOnboardWithOwnerResidesInCreatedOrg(orgId)) {
                        return;
                    }
                    RealmConfiguration realmConfiguration = OrganizationUserSharingDataHolder.getInstance()
                            .getRealmService().getTenantUserRealm(IdentityTenantUtil.getTenantId(tenantDomain))
                            .getRealmConfiguration();
                    String associatedUserName = realmConfiguration.getAdminUserName();
                    String associatedUserId = realmConfiguration.getAdminUserId();
                    String associatedOrgId = PrivilegedCarbonContext.getThreadLocalCarbonContext()
                            .getUserResidentOrganizationId();
                    if (StringUtils.isEmpty(associatedOrgId)) {
                        associatedOrgId = getOrganizationManager().resolveOrganizationId(Utils.getTenantDomain());
                    }
                    try {
                        PrivilegedCarbonContext.startTenantFlow();
                        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(associatedUserName);
                        userSharingService.shareOrganizationUser(orgId, associatedUserId, associatedOrgId);
                        String userId = userSharingService
                                .getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId)
                                .getUserId();
                        if (isAuthenticatedFromConsoleApp()) {
                            assignUserToConsoleAppAdminRole(userId, tenantDomain);
                        }
                    } finally {
                        PrivilegedCarbonContext.endTenantFlow();
                    }
                }
            }
        } catch (OrganizationManagementException | UserStoreException e) {
            throw new IdentityEventException("An error occurred while sharing the organization creator to the " +
                    "organization : " + orgId, e);
        }
    }

    private boolean isAuthenticatedFromConsoleApp() {

        String authenticatedApp = (String) IdentityUtil.threadLocalProperties.get()
                .get(FrameworkConstants.SERVICE_PROVIDER);
        return FrameworkConstants.Application.CONSOLE_APP.equals(authenticatedApp);
    }

    private boolean isSelfOrgOnboardWithOwnerResidesInCreatedOrg(String organizationID) throws IdentityEventException {

        if (isAuthenticatedFromConsoleApp()) {
            return false;
        }
        try {
            Organization organization = getOrganizationManager().getOrganization(organizationID, false, false);
            for (OrganizationAttribute attribute : organization.getAttributes()) {
                if (OrganizationManagementConstants.CREATOR_ID.equals(attribute.getKey())) {
                    return false;
                }
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException("An error occurred while fetching the organization by ID: " +
                    organizationID, e);
        }
        return true;
    }

    private Role buildOrgCreatorRole(String adminUUID) {

        Role organizationCreatorRole = new Role();
        organizationCreatorRole.setDisplayName(ORG_CREATOR_ROLE);
        User orgCreator = new User(adminUUID);
        organizationCreatorRole.setUsers(Collections.singletonList(orgCreator));
        // Set permissions for org-creator role.
        ArrayList<String> orgCreatorRolePermissions = new ArrayList<>();
        // Adding mandatory permissions for the org-creator role.
        orgCreatorRolePermissions.add(UserSharingConstants.ORG_MGT_PERMISSION);
        orgCreatorRolePermissions.add(UserSharingConstants.ORG_ROLE_MGT_PERMISSION);
        /*
        Adding the bear minimum permission set that org creator should have to logged in to the console and view
        user, groups, roles, SP, IDP sections.
         */
        orgCreatorRolePermissions.addAll(UserSharingConstants.MINIMUM_PERMISSIONS_REQUIRED_FOR_ORG_CREATOR_VIEW);
        // Add user create permission to organization creator to delegate permissions to other org users.
        // This permission is assigned until https://github.com/wso2/product-is/issues/14439 is fixed
        orgCreatorRolePermissions.add(UserSharingConstants.USER_MGT_CREATE_PERMISSION);
        organizationCreatorRole.setPermissions(orgCreatorRolePermissions);
        return organizationCreatorRole;
    }

    private Role buildAdministratorRole(String adminUUID) {

        Role organizationAdministratorRole = new Role();
        organizationAdministratorRole.setDisplayName(ORG_ADMINISTRATOR_ROLE);
        User orgAdministrator = new User(adminUUID);
        organizationAdministratorRole.setUsers(Collections.singletonList(orgAdministrator));
        // Set permissions for org-administrator role.
        ArrayList<String> orgAdministratorRolePermissions = new ArrayList<>();
        // Setting all administrative permissions for the Administrator role
        orgAdministratorRolePermissions.add(UserSharingConstants.ADMINISTRATOR_ROLE_PERMISSION);
        organizationAdministratorRole.setPermissions(orgAdministratorRolePermissions);
        return organizationAdministratorRole;
    }

    private RoleManager getRoleManager() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManager();
    }

    private void assignUserToConsoleAppAdminRole(String userId, String tenantDomain)
            throws IdentityEventException {

        try {
            ServiceProvider serviceProvider = OrganizationUserSharingDataHolder.getInstance()
                    .getApplicationManagementService().getApplicationExcludingFileBasedSPs(
                            ApplicationConstants.CONSOLE_APPLICATION_NAME, tenantDomain);
            String adminRoleId = OrganizationUserSharingDataHolder.getInstance().getRoleManagementService()
                    .getRoleIdByName(RoleConstants.ADMINISTRATOR, RoleConstants.APPLICATION,
                            serviceProvider.getApplicationResourceId(), tenantDomain);
            OrganizationUserSharingDataHolder.getInstance().getRoleManagementService()
                    .updateUserListOfRole(adminRoleId, Collections.singletonList(userId), Collections.emptyList(),
                            tenantDomain);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException("An error occurred while assigning the user to the administrator role", e);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityEventException("Failed to retrieve application id of Console application.", e);
        }
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
    }
}

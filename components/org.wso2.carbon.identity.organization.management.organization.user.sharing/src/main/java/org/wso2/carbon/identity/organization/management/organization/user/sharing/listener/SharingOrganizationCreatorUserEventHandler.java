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

import org.wso2.carbon.context.PrivilegedCarbonContext;
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
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ORG_ADMINISTRATOR_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ORG_CREATOR_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;

/**
 * The event handler for sharing the organization creator to the child organization.
 */
public class SharingOrganizationCreatorUserEventHandler extends AbstractEventHandler {

    private final OrganizationUserSharingService userSharingService = new OrganizationUserSharingServiceImpl();

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();

        if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
            Map<String, Object> eventProperties = event.getEventProperties();
            Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            String orgId = organization.getId();

            try {
                String tenantDomain = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                        .resolveTenantDomain(orgId);
                if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                    return;
                }

                String associatedUserId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
                // #TODO associatedUserOrgId should be retrieved from the carbon context. As of now, set SUPER_ORG_ID.
                // Feature won't work for b2b enabled tenant.
                userSharingService.shareOrganizationUser(orgId, associatedUserId, SUPER_ORG_ID);
                String userId = userSharingService.getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId)
                                .getUserId();
                Role organizationCreatorRole = buildOrgCreatorRole(userId);
                Role administratorRole = buildAdministratorRole(userId);
                getRoleManager().createRole(orgId, organizationCreatorRole);
                getRoleManager().createRole(orgId, administratorRole);
            } catch (OrganizationManagementException e) {
                throw new IdentityEventException("An error occurred while sharing the organization creator to the " +
                        "organization : " + orgId, e);
            }
        }
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
}

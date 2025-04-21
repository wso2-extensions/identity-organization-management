/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.NotImplementedException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.Group;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.InternalRoleDomains.APPLICATION_DOMAIN;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.user.mgt.UserMgtConstants.INTERNAL_ROLE;

/**
 * The event handler for sharing the organization creator to the child organization.
 */
public class SharingOrganizationCreatorUserEventHandler extends AbstractEventHandler {

    private final OrganizationUserSharingService userSharingService = new OrganizationUserSharingServiceImpl();
    private final UserIDResolver userIDResolver = new UserIDResolver();

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        String orgId = null;

        if (!"POST_SHARED_CONSOLE_APP".equals(eventName)) {
            return;
        }

        try {
            Map<String, Object> eventProperties = event.getEventProperties();
            orgId = (String) eventProperties.get(EVENT_PROP_ORGANIZATION_ID);
            Organization organization = OrganizationUserSharingDataHolder.getInstance()
                    .getOrganizationManager().getOrganization(orgId, false, false);
            boolean isOrgOwnerSetInAttributes = checkOrgCreatorSetInOrgAttributes(organization);
            String authenticationType = (String) IdentityUtil.threadLocalProperties.get()
                    .get(UserSharingConstants.AUTHENTICATION_TYPE);
            if (!isOrgOwnerSetInAttributes &&
                    UserSharingConstants.APPLICATION_AUTHENTICATION_TYPE.equals(authenticationType)) {
                return;
            }

            String tenantDomain = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                    .resolveTenantDomain(orgId);
            if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
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
            String associatedTenantDomain =
                    OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                            .resolveTenantDomain(associatedOrgId);
            try {
                String parentOrgId = getOrganizationId();
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(associatedUserName);
                userSharingService.shareOrganizationUser(orgId, associatedUserId, associatedOrgId, SharedType.OWNER);
                String userId = userSharingService
                        .getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId)
                        .getUserId();
                if (isConsoleAdministratorRoleAssignmentAllowed(associatedUserId, associatedTenantDomain)) {
                    assignUserToConsoleAppAdminRole(userId, tenantDomain, parentOrgId);
                }
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        } catch (OrganizationManagementException | UserStoreException e) {
            throw new IdentityEventException("An error occurred while sharing the organization creator to the " +
                    "organization : " + orgId, e);
        }
    }

    /**
     * Determines whether the shared Console Administrator role can be assigned to a user.
     * <p>
     * Role assignment is permitted if any of the following conditions are met:
     * <ul>
     *     <li>The user already has any role with the Console application's audience.</li>
     *     <li>The user is authenticated using Basic Auth (i.e., no service provider is set).</li>
     * </ul>
     *
     * @param userId       ID of the user being evaluated.
     * @param tenantDomain Tenant domain of the user.
     * @return {@code true} if assigning the Console Administrator role is allowed; {@code false} otherwise.
     * @throws IdentityEventException If an error occurs during role or application retrieval.
     */
    private boolean isConsoleAdministratorRoleAssignmentAllowed(String userId, String tenantDomain)
            throws IdentityEventException {

        String[] userRolesAssociatedWithConsole = getConsoleRolesForLocalUser(userId, tenantDomain);
        if (userRolesAssociatedWithConsole.length > 0) {
            return true; // Allow if the user already has Console roles.
        }

        // Retrieve the authenticated service provider from thread-local context.
        Object authenticatedAppFromThreadLocal = IdentityUtil.threadLocalProperties.get()
                .get(FrameworkConstants.SERVICE_PROVIDER);
        // Allow if no service provider is set (indicates Basic Auth usage).
        if (!(authenticatedAppFromThreadLocal instanceof String)) {
            // When organization creation is initiated via basic auth, SP is not set in the thread local.
            return true;
        }

        return false;
    }

    private void assignUserToConsoleAppAdminRole(String userId, String tenantDomain, String parentOrgId)
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
            addEditRestrictionToOwner(adminRoleId, userId, tenantDomain, parentOrgId);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException("An error occurred while assigning the user to the administrator role", e);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityEventException("Failed to retrieve application id of Console application.", e);
        } catch (UserSharingMgtException e) {
            throw new IdentityEventException("An error occurred while adding edit restrictions to the owner", e);
        }
    }

    /**
     * Add edit restrictions to the organization owner, ensuring that the owner cannot be deleted within the sub-org.
     * The owner can only be unshared from the parent organization.
     *
     * @param adminRoleId  Admin role id.
     * @param userId       User id.
     * @param tenantDomain Tenant domain.
     * @throws UserSharingMgtException         User sharing management exception.
     * @throws IdentityRoleManagementException Identity role management exception.
     */
    private void addEditRestrictionToOwner(String adminRoleId, String userId, String tenantDomain, String parentOrgId)
            throws UserSharingMgtException, IdentityRoleManagementException {

        String usernameWithDomain = userIDResolver.getNameByID(userId, tenantDomain);
        String username = UserCoreUtil.removeDomainFromName(usernameWithDomain);
        String domainName = UserCoreUtil.extractDomainFromName(usernameWithDomain);

        getOrganizationUserSharingService().addEditRestrictionsForSharedUserRole(adminRoleId, username,
                tenantDomain, domainName, EditOperation.DELETE, parentOrgId);
    }

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
    }

    private boolean checkOrgCreatorSetInOrgAttributes(Organization organization) {

        if (CollectionUtils.isEmpty(organization.getAttributes())) {
            return false;
        }
        return organization.getAttributes().stream()
                .anyMatch(x -> x.getKey().equals(OrganizationManagementConstants.CREATOR_ID));
    }

    /**
     * Get console roles for local user for given app.
     *
     * @param userId        userID.
     * @param tenantDomain  Tenant domain.
     * @return Console roles for local user.
     * @throws IdentityEventException If an error occurred while getting console roles for local user.
     */
    private String[] getConsoleRolesForLocalUser(String userId, String tenantDomain)
            throws IdentityEventException {

        Set<String> userRoleIds = getAllRolesOfLocalUser(userId, tenantDomain);
        List<RoleV2> rolesAssociatedWithConsoleApp = getRolesAssociatedWithConsoleApplication(tenantDomain);

        return rolesAssociatedWithConsoleApp.stream()
                .filter(role -> userRoleIds.contains(role.getId()))
                .map(RoleV2::getName)
                .toArray(String[]::new);
    }

    /**
     * Get roles associated with the console application.
     *
     * @param tenantDomain  Tenant domain.
     * @return Roles associated with the console application.
     * @throws IdentityEventException If an error occurred while getting roles associated with the console application.
     */
    private List<RoleV2> getRolesAssociatedWithConsoleApplication(String tenantDomain)
            throws IdentityEventException {

        try {
            ServiceProvider consoleAppInfo =
                    OrganizationUserSharingDataHolder.getInstance().getApplicationManagementService()
                            .getApplicationExcludingFileBasedSPs(ApplicationConstants.CONSOLE_APPLICATION_NAME,
                                    tenantDomain);
            return getApplicationManagementService().getAssociatedRolesOfApplication(
                    consoleAppInfo.getApplicationResourceId(), tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityEventException("Error while retrieving console application roles", e);
        }
    }

    /**
     * Get all roles of the local user.
     *
     * @param userId       UserID.
     * @param tenantDomain Tenant domain.
     * @return All the roles assigned to the local user.
     * @throws IdentityEventException If an error occurred while getting all roles of a local user.
     */
    private Set<String> getAllRolesOfLocalUser(String userId, String tenantDomain)
            throws IdentityEventException {

        try {
            List<String> userGroups = getUserGroups(userId, tenantDomain);
            List<String> roleIdsFromUserGroups = getRoleIdsOfGroups(userGroups, tenantDomain);
            List<String> roleIdsFromUser = getRoleIdsOfUser(userId, tenantDomain);

            return new HashSet<>(CollectionUtils.union(roleIdsFromUserGroups, roleIdsFromUser));
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException("Error while retrieving application roles", e);
        }
    }

    /**
     * Get the groups of the local authenticated user.
     *
     * @param userId       UserID.
     * @param tenantDomain Tenant domain.
     * @return Groups of the local user.
     * @throws IdentityEventException If an error occurred while getting groups of the local user.
     */
    private List<String> getUserGroups(String userId, String tenantDomain) throws IdentityEventException {

        List<String> userGroups = new ArrayList<>();

        RealmService realmService = UserCoreUtil.getRealmService();
        try {
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            UserStoreManager userStoreManager = realmService.getTenantUserRealm(tenantId).getUserStoreManager();
            List<Group> groups = ((AbstractUserStoreManager) userStoreManager)
                    .getGroupListOfUser(userId, null, null);
            // Exclude internal and application groups from the list.
            for (Group group : groups) {
                String groupName = group.getGroupName();
                if (!StringUtils.containsIgnoreCase(groupName, INTERNAL_ROLE) &&
                        !StringUtils.containsIgnoreCase(groupName, APPLICATION_DOMAIN)) {
                    userGroups.add(group.getGroupID());
                }
            }
        } catch (UserStoreException e) {
            if (isDoGetGroupListOfUserNotImplemented(e)) {
                return userGroups;
            }
            throw new IdentityEventException("Error while retrieving local user groups", e);
        }
        return userGroups;
    }

    /**
     * Check if the UserStoreException occurred due to the doGetGroupListOfUser method not being implemented.
     *
     * @param e UserStoreException.
     * @return true if the UserStoreException was caused by the doGetGroupListOfUser method not being implemented,
     * false otherwise.
     */
    private boolean isDoGetGroupListOfUserNotImplemented(UserStoreException e) {

        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof NotImplementedException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Get Role IDs assigned to user through groups.
     *
     * @param userGroups   User groups.
     * @param tenantDomain Tenant domain.
     * @return Role IDs assigned to user through groups.
     * @throws IdentityRoleManagementException If an error occurred while getting role IDs assigned through groups.
     */
    private List<String> getRoleIdsOfGroups(List<String> userGroups, String tenantDomain)
            throws IdentityRoleManagementException {

        return getRoleManagementService().getRoleIdListOfGroups(userGroups, tenantDomain);
    }

    /**
     * Get Role IDs assigned to user directly.
     *
     * @param userId       User ID.
     * @param tenantDomain Tenant domain.
     * @return Role IDs assigned to user directly.
     * @throws IdentityRoleManagementException If an error occurred while getting role IDs assigned directly.
     */
    private List<String> getRoleIdsOfUser(String userId, String tenantDomain)
            throws IdentityRoleManagementException {

        return getRoleManagementService().getRoleIdListOfUser(userId, tenantDomain);
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getApplicationManagementService();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
    }
}

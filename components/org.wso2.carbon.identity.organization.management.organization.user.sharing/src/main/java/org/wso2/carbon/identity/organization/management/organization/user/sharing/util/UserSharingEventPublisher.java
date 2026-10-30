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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_POST_SHARE_USER;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ASSOCIATED_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ASSOCIATED_USER_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ROLES;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ROLE_IS_SHARED;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_ROLE_IS_SHARED;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_TENANT_DOMAIN;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_USER_ID;

/**
 * Publishes identity events for users shared into, and unshared from, organizations.
 * <p>
 * One event is published per user per organization, after the shared user has been created in or removed from the
 * organization. The event is published under the tenant of the organization the user was shared from, and also
 * carries the tenant of the organization the user was shared to, so that it can be delivered to the webhooks of both.
 * A share event is published once the roles of the shared user are assigned, and carries them.
 * <p>
 * Event publishing is best effort. A failure to publish never fails the operation that triggered it.
 */
public class UserSharingEventPublisher {

    private static final Log LOG = LogFactory.getLog(UserSharingEventPublisher.class);

    private UserSharingEventPublisher() {

    }

    /**
     * Publish the event of a user shared into an organization, with the roles assigned to the shared user by the
     * sharing. Call it once the roles are assigned.
     *
     * @param userAssociation The association of the shared user with the user it was shared from.
     */
    public static void publishUserSharedEvent(UserAssociation userAssociation) {

        if (userAssociation == null) {
            return;
        }
        try {
            Map<String, Object> eventProperties = buildEventProperties(userAssociation);
            eventProperties.put(EVENT_PROP_ROLES, resolveSharedRoles(userAssociation,
                    (String) eventProperties.get(EVENT_PROP_SHARED_TENANT_DOMAIN),
                    (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN)));
            handleEvent(EVENT_POST_SHARE_USER, eventProperties, userAssociation);
        } catch (OrganizationManagementException e) {
            LOG.error("Error while publishing the user sharing event: " + EVENT_POST_SHARE_USER +
                    " for the shared user: " + userAssociation.getUserId(), e);
        }
    }

    /**
     * Publish a user sharing event for a shared user.
     *
     * @param eventName       The name of the event.
     * @param userAssociation The association of the shared user with the user it was shared from.
     */
    public static void publish(String eventName, UserAssociation userAssociation) {

        try {
            handleEvent(eventName, buildEventProperties(userAssociation), userAssociation);
        } catch (OrganizationManagementException e) {
            LOG.error("Error while publishing the user sharing event: " + eventName + " for the shared user: " +
                    userAssociation.getUserId(), e);
        }
    }

    private static Map<String, Object> buildEventProperties(UserAssociation userAssociation)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN,
                resolveTenantDomain(userAssociation.getUserResidentOrganizationId()));
        eventProperties.put(EVENT_PROP_SHARED_TENANT_DOMAIN,
                resolveTenantDomain(userAssociation.getOrganizationId()));
        eventProperties.put(EVENT_PROP_SHARED_USER_ID, userAssociation.getUserId());
        eventProperties.put(EVENT_PROP_SHARED_ORGANIZATION_ID, userAssociation.getOrganizationId());
        eventProperties.put(EVENT_PROP_ASSOCIATED_USER_ID, userAssociation.getAssociatedUserId());
        eventProperties.put(EVENT_PROP_ASSOCIATED_ORGANIZATION_ID,
                userAssociation.getUserResidentOrganizationId());
        return eventProperties;
    }

    private static void handleEvent(String eventName, Map<String, Object> eventProperties,
                                    UserAssociation userAssociation) {

        IdentityEventService identityEventService =
                OrganizationUserSharingDataHolder.getInstance().getIdentityEventService();
        if (identityEventService == null) {
            LOG.debug("Identity event service is not available. Skipping the user sharing event: " + eventName);
            return;
        }
        try {
            identityEventService.handleEvent(new Event(eventName, eventProperties));
        } catch (IdentityEventException e) {
            LOG.error("Error while publishing the user sharing event: " + eventName + " for the shared user: " +
                    userAssociation.getUserId(), e);
        }
    }

    /**
     * Resolve the roles assigned to the shared user by the sharing, each as the role of the organization the user was
     * shared to and the corresponding role of the organization the user was shared from. Roles the shared user was
     * given in its organization directly are not included. Resolution is best effort: on failure no roles are
     * reported, and the event is still published.
     */
    private static List<Map<String, String>> resolveSharedRoles(UserAssociation userAssociation,
                                                                String sharedTenantDomain,
                                                                String residentTenantDomain) {

        RoleManagementService roleManagementService =
                OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
        try {
            List<String> userRoleIds =
                    roleManagementService.getRoleIdListOfUser(userAssociation.getUserId(), sharedTenantDomain);
            List<String> sharedRoleIds = OrganizationUserSharingDataHolder.getInstance()
                    .getOrganizationUserSharingService().getSharedUserRolesFromUserRoles(userRoleIds,
                            sharedTenantDomain);
            if (CollectionUtils.isEmpty(sharedRoleIds)) {
                return Collections.emptyList();
            }
            // Shared roles map to the roles of the root organization they were shared from.
            Map<String, String> sharedToMainRoleIds =
                    roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(sharedRoleIds, sharedTenantDomain);
            // When the user resides in a sub organization, its roles are the shared copies of the root roles.
            Map<String, String> mainToResidentRoleIds = null;
            if (OrganizationManagementUtil.isOrganization(residentTenantDomain)) {
                mainToResidentRoleIds = roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                        new ArrayList<>(sharedToMainRoleIds.values()), residentTenantDomain);
            }
            List<Map<String, String>> roles = new ArrayList<>();
            for (String sharedRoleId : sharedRoleIds) {
                String mainRoleId = sharedToMainRoleIds.get(sharedRoleId);
                Map<String, String> role = new HashMap<>();
                // A role of the organization the user was shared to is a shared role when it maps to a main role.
                role.put(EVENT_PROP_SHARED_ROLE_ID, sharedRoleId);
                role.put(EVENT_PROP_SHARED_ROLE_IS_SHARED, String.valueOf(mainRoleId != null));
                // A role of the organization the user was shared from is a shared role when that organization is a
                // sub organization, and the main role otherwise.
                role.put(EVENT_PROP_ROLE_ID, mainToResidentRoleIds != null && mainRoleId != null ?
                        mainToResidentRoleIds.get(mainRoleId) : mainRoleId);
                role.put(EVENT_PROP_ROLE_IS_SHARED, String.valueOf(mainToResidentRoleIds != null));
                roles.add(role);
            }
            return roles;
        } catch (IdentityRoleManagementException | OrganizationManagementException e) {
            LOG.debug("Error while resolving the roles of the shared user: " + userAssociation.getUserId(), e);
            return Collections.emptyList();
        }
    }

    private static String resolveTenantDomain(String organizationId) throws OrganizationManagementException {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                .resolveTenantDomain(organizationId);
    }
}

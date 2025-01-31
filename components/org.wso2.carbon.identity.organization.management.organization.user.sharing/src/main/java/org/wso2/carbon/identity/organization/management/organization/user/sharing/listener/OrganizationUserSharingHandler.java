/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.bean.context.MessageContext;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The event handler for sharing users to the newly created organization.
 */
public class OrganizationUserSharingHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationUserSharingHandler.class);
    private final OrganizationUserSharingService userSharingService = new OrganizationUserSharingServiceImpl();
    private final UserIDResolver userIDResolver = new UserIDResolver();

    /**
     * Handles the user sharing for the newly created organization.
     *
     * @param event The event to be handled.
     * @throws IdentityEventException If an error occurs while handling the event.
     */
    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        Organization createdOrganization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);

        try {
            if (OrganizationManagementUtil.isOrganization(createdOrganization.getId())) {
                shareUsers(createdOrganization.getId());
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException("Error while handling user sharing for event: " + eventName, e);
        } catch (ResourceSharingPolicyMgtException e) {
            throw new IdentityEventException("Error while retrieving resource sharing policies for event: " + eventName,
                    e);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException("Error while handling role sharing for event: " + eventName, e);
        }
    }

    @Override
    public int getPriority(MessageContext messageContext) {

        /*
         * This handler should run after OrganizationCreationHandler to make sure shared application roles are created
         * in the newly created organization before sharing users.
         */
        int priority = super.getPriority(messageContext);
        if (priority == -1) {
            priority = 15;
        }
        return priority;
    }

    private void shareUsers(String createdOrgId)
            throws OrganizationManagementException, ResourceSharingPolicyMgtException, IdentityRoleManagementException {

        List<String> allAncestorOrgs = getOrganizationManager().getAncestorOrganizationIds(createdOrgId);
        int topHierarchyLevel = Utils.getSubOrgStartLevel() >= 1 ? Utils.getSubOrgStartLevel() - 1 : 0;
        List<String> relevantAncestorOrgs =
                new ArrayList<>(allAncestorOrgs.subList(1, allAncestorOrgs.size() - topHierarchyLevel));

        // Get all resources of each ancestor organization.
        Map<String, List<ResourceSharingPolicy>> resourcesGroupedByOrganization =
                getResourceSharingPolicyHandlerService().getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
                        relevantAncestorOrgs);

        for (Map.Entry<String, List<ResourceSharingPolicy>> ancestorOrgEntry :
                resourcesGroupedByOrganization.entrySet()) {
            List<ResourceSharingPolicy> resourcesList = ancestorOrgEntry.getValue();
            for (ResourceSharingPolicy resource : resourcesList) {
                if (!ResourceType.USER.equals(resource.getResourceType())) {
                    continue;
                }

                if (shouldShareUser(resource.getSharingPolicy(), createdOrgId, ancestorOrgEntry.getKey())) {
                    shareUser(resource, resource.getResourceId(),
                            resource.getInitiatingOrgId(), createdOrgId);
                }
            }
        }
    }

    private boolean shouldShareUser(PolicyEnum policy, String createdOrgId, String ancestorOrgId)
            throws OrganizationManagementServerException {

        return isAllExistingAndFutureOrgs(policy) ||
                (isImmediateLevelOrganization(createdOrgId, ancestorOrgId) && isImmediateExistingAndFutureOrgs(policy));
    }

    private void shareUser(ResourceSharingPolicy resource, String userId, String residentOrgId, String createdOrgId)
            throws OrganizationManagementException, ResourceSharingPolicyMgtException, IdentityRoleManagementException {

        List<SharedResourceAttribute> userAttributes =
                OrganizationUserSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService()
                        .getSharedResourceAttributesBySharingPolicyId(resource.getResourceSharingPolicyId());

        // Extract the role IDs from the user attributes.
        List<String> roleIds = userAttributes.stream()
                .filter(attribute -> SharedAttributeType.ROLE.equals(attribute.getSharedAttributeType()))
                .map(SharedResourceAttribute::getSharedAttributeId).collect(Collectors.toList());

        UserAssociation existingUserAssociation =
                userSharingService.getUserAssociationOfAssociatedUserByOrgId(userId, createdOrgId);
        if (existingUserAssociation == null) {
            userSharingService.shareOrganizationUser(createdOrgId, userId, residentOrgId, SharedType.SHARED);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("User: " + userId + " is already shared with the organization: " + createdOrgId);
        }

        if (roleIds.isEmpty()) {
            return;
        }

        // Get the corresponding shared role IDs in the created organization.
        List<String> sharedRoleIds = getSharedRoleIds(roleIds, createdOrgId);
        String tenantDomain = getOrganizationManager().resolveTenantDomain(createdOrgId);
        String sharedUserId =
                userSharingService.getUserAssociationOfAssociatedUserByOrgId(userId, createdOrgId).getUserId();
        List<String> sharedUserExistingRoles =
                getRoleManagementService().getRoleIdListOfUser(sharedUserId, tenantDomain);

        // Remove the shared roles that are already assigned to the shared user.
        sharedRoleIds.removeIf(sharedUserExistingRoles::contains);

        for (String sharedRoleId : sharedRoleIds) {
            // Assign the shared roles to the shared user.
            getRoleManagementService().updateUserListOfRole(sharedRoleId, Collections.singletonList(sharedUserId),
                    Collections.emptyList(), tenantDomain);
            restrictUserRoleDeletion(sharedRoleId, userId, tenantDomain, createdOrgId, resource.getInitiatingOrgId());
        }
    }

    private void restrictUserRoleDeletion(String rolId, String userId, String tenantDomain, String createdOrgId,
                                          String permittedOrgId) {

        try {
            UserAssociation userAssociation =
                    getOrganizationUserSharingService().getUserAssociationOfAssociatedUserByOrgId(userId, createdOrgId);
            String usernameWithDomain = userIDResolver.getNameByID(userAssociation.getUserId(), tenantDomain);
            String username = UserCoreUtil.removeDomainFromName(usernameWithDomain);
            String domainName = UserCoreUtil.extractDomainFromName(usernameWithDomain);

            getOrganizationUserSharingService().addEditRestrictionsForSharedUserRole(rolId, username,
                    tenantDomain, domainName, EditOperation.DELETE, permittedOrgId);
        } catch (IdentityRoleManagementException | UserSharingMgtException | OrganizationManagementException e) {
            LOG.error("Error while adding edit restrictions for shared user role deletion.", e);
        }
    }

    private List<String> getSharedRoleIds(List<String> roleIds, String createdOrgId)
            throws OrganizationManagementException, IdentityRoleManagementException {

        Map<String, String> mainRoleToSharedRoleMappings =
                OrganizationUserSharingDataHolder.getInstance().getRoleManagementService()
                        .getMainRoleToSharedRoleMappingsBySubOrg(roleIds,
                                getOrganizationManager().resolveTenantDomain(createdOrgId));

        List<String> rolesWithoutSharedRoles = roleIds.stream().filter(roleId -> !mainRoleToSharedRoleMappings
                .containsKey(roleId)).collect(Collectors.toList());

        if (!rolesWithoutSharedRoles.isEmpty() && LOG.isDebugEnabled()) {
            LOG.debug("No shared roles found in organization: " + createdOrgId + " for the following roles: " +
                    rolesWithoutSharedRoles);
        }
        return new ArrayList<>(mainRoleToSharedRoleMappings.values());
    }

    private boolean isAllExistingAndFutureOrgs(PolicyEnum policy) {

        return PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS.equals(policy) ||
                PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN.equals(policy);
    }

    private boolean isImmediateExistingAndFutureOrgs(PolicyEnum policy) {

        return PolicyEnum.IMMEDIATE_EXISTING_AND_FUTURE_ORGS.equals(policy) ||
                PolicyEnum.SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN.equals(policy);
    }

    private boolean isImmediateLevelOrganization(String createdOrgId, String ancestorOrg)
            throws OrganizationManagementServerException {

        return getOrganizationManager().getRelativeDepthBetweenOrganizationsInSameBranch(ancestorOrg,
                createdOrgId) == 1;
    }

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationUserSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
    }
}

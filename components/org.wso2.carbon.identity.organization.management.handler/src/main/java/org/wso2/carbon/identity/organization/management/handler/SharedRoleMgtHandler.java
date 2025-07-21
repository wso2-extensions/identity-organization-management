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

package org.wso2.carbon.identity.organization.management.handler;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.central.log.mgt.utils.LogConstants;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.application.model.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareUpdateOperation;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.ChildOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementClientException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.utils.AuditLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ROLE_SHARING_MODE;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.getAppAssociatedRoleSharingMode;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_APPLICATION;

/**
 * Event handler to manage shared roles in sub-organizations.
 */
public class SharedRoleMgtHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(SharedRoleMgtHandler.class);
    private static final String ALLOWED_AUDIENCE_FOR_ASSOCIATED_ROLES = "allowedAudienceForAssociatedRoles";
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        switch (eventName) {
            case OrgApplicationMgtConstants.EVENT_POST_SHARE_APPLICATION:
                createSharedRolesOnApplicationSharing(eventProperties);
                break;
            case IdentityEventConstants.Event.POST_ADD_ROLE_V2_EVENT:
                createSharedRolesOnNewRoleCreation(eventProperties);
                break;
            case OrgApplicationMgtConstants.EVENT_PRE_SHARE_APPLICATION:
                checkSharingRoleConflicts(eventProperties);
                break;
            case OrgApplicationMgtConstants.EVENT_POST_UPDATE_ROLES_OF_SHARED_APPLICATION:
                updateSharedApplicationRoles(eventProperties);
                break;
            default:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unsupported event: " + eventName);
                }
                break;
        }
    }

    private void createSharedRolesOnApplicationSharing(Map<String, Object> eventProperties)
            throws IdentityEventException {

        String mainOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_MAIN_ORGANIZATION_ID);
        String mainApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_MAIN_APPLICATION_ID);
        String sharedOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID);
        String sharedApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID);
        ApplicationShareRolePolicy applicationShareRolePolicy = (ApplicationShareRolePolicy) eventProperties.get(
                OrgApplicationMgtConstants.EVENT_PROP_ROLE_SHARING_CONFIG);
        try {
            String mainAppTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
            String allowedAudienceForRoleAssociationInMainApp =
                    getApplicationMgtService().getAllowedAudienceForRoleAssociation(mainApplicationId,
                            mainAppTenantDomain);

            if (StringUtils.equalsIgnoreCase(allowedAudienceForRoleAssociationInMainApp.toLowerCase(),
                    RoleConstants.APPLICATION)) {
                createSharedAppRolesSelectively(mainApplicationId, mainOrganizationId, sharedApplicationId,
                        sharedOrganizationId, applicationShareRolePolicy);
            } else {
                createSharedOrgRoleSelectively(mainOrganizationId, mainApplicationId, sharedOrganizationId,
                        applicationShareRolePolicy);
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException |
                 IdentityApplicationManagementException e) {
            throw new IdentityEventException(
                    String.format("Error while sharing roles related to application %s.", sharedApplicationId), e);
        }
    }

    private void updateSharedApplicationRoles(Map<String, Object> eventProperties)
            throws IdentityEventException {

        String mainOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_MAIN_ORGANIZATION_ID);
        String mainApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_MAIN_APPLICATION_ID);
        String sharedOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID);
        String sharedApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID);
        ApplicationShareUpdateOperation.Operation operation = (ApplicationShareUpdateOperation.Operation)
                eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_UPDATE_OPERATION);
        List<RoleWithAudienceDO> roleChanges = (List<RoleWithAudienceDO>) eventProperties.get(
                OrgApplicationMgtConstants.EVENT_PROP_ROLE_AUDIENCES);

        try {
            if (operation == null) {
                throw new IdentityEventException("Invalid operation type null.");
            }
            String mainAppTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
            String allowedAudienceForRoleAssociationInMainApp =
                    getApplicationMgtService().getAllowedAudienceForRoleAssociation(mainApplicationId,
                            mainAppTenantDomain);
            if (RoleConstants.APPLICATION.equalsIgnoreCase(allowedAudienceForRoleAssociationInMainApp)) {
                if (ApplicationShareUpdateOperation.Operation.ADD.ordinal() == operation.ordinal()) {
                    addAppRolesToSharedApp(mainApplicationId, mainOrganizationId,
                            sharedApplicationId, sharedOrganizationId, roleChanges);
                } else if (ApplicationShareUpdateOperation.Operation.REMOVE.ordinal() == operation.ordinal()) {
                    removeAppRolesFromSharedApp(mainApplicationId, mainOrganizationId, sharedApplicationId,
                            sharedOrganizationId, roleChanges);
                } else {
                    throw new IdentityEventException(
                            String.format("Invalid operation type %s.", operation));
                }
            } else {
                if (ApplicationShareUpdateOperation.Operation.ADD.ordinal() == operation.ordinal()) {
                    addOrgRolesToSharedApp(mainApplicationId, mainOrganizationId, sharedApplicationId,
                            sharedOrganizationId, roleChanges);
                } else if (ApplicationShareUpdateOperation.Operation.REMOVE.ordinal() == operation.ordinal()) {
                    throw new IdentityEventException(
                            String.format("Removing Organization audience roles from shared applications " +
                                            "is not supported."));
                } else {
                    throw new IdentityEventException(
                            String.format("Invalid operation type %s.", operation));
                }
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException(
                    String.format("Error while resolving tenant domain for organization %s.", mainOrganizationId), e);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityEventException(
                    String.format("Error while getting allowed audience for role association for application %s.",
                            mainApplicationId), e);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException(
                    String.format("Error while adding roles to shared application %s.", sharedApplicationId), e);
        }
    }

    private List<String> createSharedOrgRoleSelectively(String mainOrganizationId, String mainApplicationId,
                                               String sharedAppOrgId, ApplicationShareRolePolicy sharingConfig) throws
            OrganizationManagementException, IdentityRoleManagementException, IdentityApplicationManagementException {


        Organization subOrg = getOrganization(sharedAppOrgId, false);
        String parentOrgId = subOrg.getParent().getId();
        String mainAppTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
        // Here the caller of this function validate that this is definitely an organization audience. So we
        // don't need to check for the audience type validations.
        List<RoleV2> associatedRolesOfMainApplication =
                            getApplicationMgtService().getAssociatedRolesOfApplication(
                                    mainApplicationId, mainAppTenantDomain);
        if (ApplicationShareRolePolicy.Mode.NONE.ordinal() == sharingConfig.getMode().ordinal()) {
            return Collections.emptyList();
        }
        List<String> mainRolesIds = new ArrayList<>();
        for (RoleV2 mainRole : associatedRolesOfMainApplication) {
            mainRolesIds.add(mainRole.getId());
        }
        String parentOrgTenantDomain = getOrganizationManager().resolveTenantDomain(parentOrgId);
        Map<String, String> mainOrgToParentOrgRoleAssociations = getRoleManagementServiceV2()
                .getMainRoleToSharedRoleMappingsBySubOrg(mainRolesIds, parentOrgTenantDomain);
        List<RoleV2> filteredRolesToBeShared = new ArrayList<>();

        if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() == sharingConfig.getMode().ordinal()) {
            List<RoleWithAudienceDO> roleWithAudienceDOList =
                    sharingConfig.getRoleWithAudienceDOList();
            for (RoleWithAudienceDO roleWithAudienceDO : roleWithAudienceDOList) {
                Optional<RoleV2> availableOrgRolesToBeShared = associatedRolesOfMainApplication.stream().findFirst()
                        .filter(roleInfo -> roleInfo.getName().equals(roleWithAudienceDO.getRoleName()));
                if (availableOrgRolesToBeShared.isPresent()) {
                    /* If the parent org is the same as the main org, then we don't need to check for the parent org
                     role association. Because there's no role association in the main org. It has the org that has
                     originated the role. */
                    if (mainOrganizationId.equals(parentOrgId)) {
                        filteredRolesToBeShared.add(availableOrgRolesToBeShared.get());
                    }
                    String roleId = availableOrgRolesToBeShared.get().getId();
                    if (mainOrgToParentOrgRoleAssociations.containsKey(roleId)) {
                        filteredRolesToBeShared.add(availableOrgRolesToBeShared.get());
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("The role " + roleWithAudienceDO.getRoleName() + " is not available in the " +
                                    "parent org. So it will not be shared.");
                        }

                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("The role " + roleWithAudienceDO.getRoleName() + " is not available in the " +
                                "main org. So it will not be shared.");
                    }
                }
            }
        } else {
            for (RoleV2 mainOrgAppRole : associatedRolesOfMainApplication) {
                if (mainOrganizationId.equals(parentOrgId)) {
                    filteredRolesToBeShared.add(mainOrgAppRole);
                } else {
                    if (mainOrgToParentOrgRoleAssociations.containsKey(mainOrgAppRole.getId())) {
                        filteredRolesToBeShared.add(mainOrgAppRole);
                    }
                }
            }
        }

        List<RoleV2> filteredRolesToBeAdded = new ArrayList<>();
        for (RoleV2 roleToBeShared : filteredRolesToBeShared) {
            RoleV2 role = new RoleV2();
            role.setId(roleToBeShared.getId());
            role.setName(roleToBeShared.getName());
            filteredRolesToBeAdded.add(role);
        }
        createSharedRolesWithOrgAudience(filteredRolesToBeAdded, mainAppTenantDomain, sharedAppOrgId);
        return filteredRolesToBeAdded.stream()
                .map(RoleV2::getId)
                .collect(Collectors.toList());
    }

    private void createSharedRolesWithOrgAudience(List<RoleV2> rolesList, String mainAppTenantDomain,
                                                  String sharedAppOrgId)
            throws IdentityRoleManagementException, OrganizationManagementException {

        if (rolesList == null) {
            return;
        }
        String sharedAppTenantDomain = getOrganizationManager().resolveTenantDomain(sharedAppOrgId);
        for (RoleV2 role : rolesList) {
            // Check if the role exists in the application shared org.
            boolean roleExistsInSharedOrg =
                    getRoleManagementServiceV2().isExistingRoleName(role.getName(), RoleConstants.ORGANIZATION,
                            sharedAppOrgId, sharedAppTenantDomain);
            Map<String, String> mainRoleToSharedRoleMappingInSharedOrg =
                    getRoleManagementServiceV2().getMainRoleToSharedRoleMappingsBySubOrg(
                            Collections.singletonList(role.getId()), sharedAppTenantDomain);
            boolean roleRelationshipExistsInSharedOrg =
                    MapUtils.isNotEmpty(mainRoleToSharedRoleMappingInSharedOrg);
            if (roleExistsInSharedOrg && !roleRelationshipExistsInSharedOrg) {
                // Add relationship between main role and shared role.
                String roleIdInSharedOrg =
                        getRoleManagementServiceV2().getRoleIdByName(role.getName(), RoleConstants.ORGANIZATION,
                                sharedAppOrgId, sharedAppTenantDomain);
                getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(role.getId(),
                        roleIdInSharedOrg, mainAppTenantDomain, sharedAppTenantDomain);
            } else if (!roleExistsInSharedOrg && !roleRelationshipExistsInSharedOrg) {
                // Create the role in the shared org.
                RoleBasicInfo sharedRole =
                        getRoleManagementServiceV2().addRole(role.getName(), Collections.emptyList(),
                                Collections.emptyList(), Collections.emptyList(), RoleConstants.ORGANIZATION,
                                sharedAppOrgId, sharedAppTenantDomain);
                // Add relationship between main role and shared role.
                getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(role.getId(),
                        sharedRole.getId(), mainAppTenantDomain, sharedAppTenantDomain);
            }
        }
    }

    /**
     * Resolves the tenant domain for a given organization ID.
     *
     * @param organizationId The ID of the organization.
     * @return The tenant domain.
     * @throws OrganizationManagementException If an error occurs while resolving the tenant domain.
     */
    private String resolveTenantDomain(String organizationId) throws OrganizationManagementException {

        return getOrganizationManager().resolveTenantDomain(organizationId);
    }

    /**
     * Fetches an organization by its ID.
     *
     * @param organizationId          The ID of the organization.
     * @param fetchChildOrganizations Whether to fetch child organizations.
     * @return The Organization object.
     * @throws OrganizationManagementException If an error occurs while fetching the organization.
     */
    private Organization getOrganization(String organizationId, boolean fetchChildOrganizations)
            throws OrganizationManagementException {

        return getOrganizationManager().getOrganization(organizationId, fetchChildOrganizations, false);
    }

    /**
     * Retrieves all application roles for a given main application ID and tenant domain.
     *
     * @param mainAppId      The ID of the main application.
     * @param mainTenantDomain The tenant domain of the main application.
     * @return A list of {@link RoleBasicInfo} representing the application roles.
     * @throws IdentityRoleManagementException If an error occurs during role retrieval.
     */
    private List<RoleBasicInfo> getMainApplicationRoles(String mainAppId, String mainTenantDomain)
            throws IdentityRoleManagementException {

        String filter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + mainAppId;
        return getRoleManagementServiceV2().getRoles(filter, null, 0, null, null, mainTenantDomain);
    }

    /**
     * Filters a list of main organization's application roles based on selection criteria.
     * Only roles matching name, audience name, and APPLICATION audience type from the criteria are returned.
     *
     * @param allMainOrgAppRoles List of all {@link RoleBasicInfo} from the main organization's application.
     * @param selectionCriteria  List of {@link RoleWithAudienceDO} specifying which roles to select.
     * @return A filtered list of {@link RoleBasicInfo} from mainOrgAppRoles that match the selection.
     */
    private List<RoleBasicInfo> filterSelectedMainAppRoles(List<RoleBasicInfo> allMainOrgAppRoles,
                                                           List<RoleWithAudienceDO> selectionCriteria) {
        List<RoleBasicInfo> filteredMainOrgAppRoles = new ArrayList<>();
        if (selectionCriteria == null || selectionCriteria.isEmpty()) {
            return filteredMainOrgAppRoles;
        }

        for (RoleWithAudienceDO criterion : selectionCriteria) {
            if (!RoleConstants.APPLICATION.equals(criterion.getAudienceType().toString())) {
                continue;
            }
            boolean roleAvailableInMainOrg = false;
            for (RoleBasicInfo mainOrgAppRole : allMainOrgAppRoles) {
                if (mainOrgAppRole.getName().equals(criterion.getRoleName()) &&
                        mainOrgAppRole.getAudienceName().equals(criterion.getAudienceName())) {
                    // Add the role from the main organization, not the criterion.
                    if (filteredMainOrgAppRoles.stream().noneMatch(r -> r.getId().equals(mainOrgAppRole.getId()))) {
                        filteredMainOrgAppRoles.add(mainOrgAppRole);
                    }
                    roleAvailableInMainOrg = true;
                    break;
                }
            }
            if (!roleAvailableInMainOrg) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The role '" + criterion.getRoleName() + "' with audience '" +
                            criterion.getAudienceName() + "' (type: APPLICATION) specified in selection criteria is" +
                            " not available in the main org. So it will not be shared.");
                }
            }
        }
        return filteredMainOrgAppRoles;
    }

    /**
     * Filters a list of application roles based on their availability/association in the parent organization of a
     * sub-organization. If the main organization is the direct parent of the sub-organization, no filtering is applied.
     *
     * @param appRolesFromMainOrg List of {@link RoleBasicInfo} (application roles from the main org, already
     *                            filtered by selection).
     * @param mainOrganizationId  The ID of the main organization.
     * @param subOrg              The sub-organization {@link Organization} object with parent info.
     * @return A list of {@link RoleBasicInfo} that are also available/associated in the parent organization.
     * @throws IdentityRoleManagementException If an error occurs during role management operations.
     * @throws OrganizationManagementException If an error occurs during organization management operations.
     */
    private List<RoleBasicInfo> filterAppRolesByParentOrgAssociation(List<RoleBasicInfo> appRolesFromMainOrg,
                                                                     String mainOrganizationId, Organization subOrg)
            throws IdentityRoleManagementException, OrganizationManagementException {

        if (appRolesFromMainOrg.isEmpty()) {
            return Collections.emptyList();
        }

        String subOrgParentId = subOrg.getParent().getId();
        if (mainOrganizationId.equals(subOrgParentId)) {
            // This is a direct child organization, no further filtering needed.
            return new ArrayList<>(appRolesFromMainOrg);
        }

        String parentOrgTenantDomain = resolveTenantDomain(subOrgParentId);
        List<String> mainRoleIds = appRolesFromMainOrg.stream().map(RoleBasicInfo::getId).collect(Collectors.toList());

        Map<String, String> mainOrgToParentOrgRoleAssociations = getRoleManagementServiceV2()
                .getMainRoleToSharedRoleMappingsBySubOrg(mainRoleIds, parentOrgTenantDomain);

        List<RoleBasicInfo> filteredRoles = new ArrayList<>();
        for (RoleBasicInfo mainOrgAppRole : appRolesFromMainOrg) {
            if (mainOrgToParentOrgRoleAssociations.containsKey(mainOrgAppRole.getId())) {
                filteredRoles.add(mainOrgAppRole);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The role '" + mainOrgAppRole.getName() + "' (ID: " + mainOrgAppRole.getId() + ") from" +
                            " main org '" + mainOrganizationId + "' is not available/associated in the parent org '" +
                            subOrgParentId + "'. So it will not be shared with sub-org '" + subOrg.getId() + "'.");
                }
            }
        }
        return filteredRoles;
    }

    /**
     * Provisions application roles in the shared organization. Creates roles if they don't exist
     * and establishes a relationship with the main organization's role.
     * Populates 'successfullyProvisionedOrExistingRoles' with roles that are intended to be shared
     * and either already exist or were newly created.
     *
     * @param rolesToShare                         List of {@link RoleBasicInfo} to be shared/provisioned.
     * @param mainTenantDomain                     Tenant domain of the main organization.
     * @param sharedAppId                          ID of the shared application.
     * @param sharedAppTenantDomain                Tenant domain of the shared organization.
     * @param successfullyProvisionedOrExistingRoles (Output) List to track roles that are successfully shared.
     * @throws IdentityRoleManagementException If an error occurs during role management operations.
     */
    private void provisionOrVerifyApplicationRolesInSharedOrg(List<RoleBasicInfo> rolesToShare,
                                                              String mainTenantDomain, String sharedAppId,
                                                              String sharedAppTenantDomain, List<RoleBasicInfo>
                                                                      successfullyProvisionedOrExistingRoles)
            throws IdentityRoleManagementException {

        for (RoleBasicInfo roleToBeShared : rolesToShare) {
            boolean isRoleAlreadyExist = getRoleManagementServiceV2().isExistingRoleName(roleToBeShared.getName(),
                    RoleConstants.APPLICATION, sharedAppId, sharedAppTenantDomain);

            if (isRoleAlreadyExist) {
                successfullyProvisionedOrExistingRoles.add(roleToBeShared);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Role '" + roleToBeShared.getName() + "' already exists in the shared sub org: '" +
                            sharedAppTenantDomain + "' for app ID '" + sharedAppId + "'. Skipping creation but " +
                            "acknowledging for sharing.");
                }
                continue;
            }

            RoleBasicInfo subOrgRole = getRoleManagementServiceV2().addRole(roleToBeShared.getName(),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    RoleConstants.APPLICATION, sharedAppId, sharedAppTenantDomain);
            getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(roleToBeShared.getId(),
                    subOrgRole.getId(), mainTenantDomain, sharedAppTenantDomain);
            successfullyProvisionedOrExistingRoles.add(subOrgRole);
        }
    }

    /**
     * Selectively creates or updates shared application roles in a sub-organization based on the
     * {@link ApplicationShareRolePolicy}.
     * It handles adding new roles, and removing roles that are no longer configured to be shared.
     *
     * @param mainAppId             ID of the main application.
     * @param mainOrganizationId    ID of the main organization.
     * @param sharedAppId           ID of the application in the sub-organization (shared application).
     * @param sharedOrganizationId  ID of the sub-organization where roles are shared.
     * @param applicationShareRolePolicy     Configuration defining which roles to share (ALL, SELECTED, NONE).
     * @return A list of IDs of the main application roles that are configured to be shared.
     * @throws IdentityRoleManagementException If a role management error occurs.
     * @throws OrganizationManagementException If an organization management error occurs.
     */
    private List<String> createSharedAppRolesSelectively(String mainAppId, String mainOrganizationId,
                                                        String sharedAppId, String sharedOrganizationId,
                                                        ApplicationShareRolePolicy applicationShareRolePolicy)
            throws IdentityRoleManagementException, OrganizationManagementException {

        String sharedAppTenantDomain = resolveTenantDomain(sharedOrganizationId);
        String mainTenantDomain = resolveTenantDomain(mainOrganizationId);

        String subOrgAppRolesFilter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + sharedAppId;
        List<RoleBasicInfo> alreadySharedSubOrgAppRoles = getRoleManagementServiceV2().getRoles(subOrgAppRolesFilter,
                null, 0, null, null, sharedAppTenantDomain);

        ApplicationShareRolePolicy currentConfig = applicationShareRolePolicy;
        if (currentConfig == null) {
            // Handle cases where role sharing config is unavailable (e.g., old app re-sharing)
            // Default to sharing ALL roles for backward compatibility.
            if (LOG.isDebugEnabled()) {
                LOG.debug("RoleSharingConfig is null for app " + mainAppId + " being shared with org " +
                        sharedOrganizationId + ". Defaulting to share ALL roles.");
            }
            currentConfig = new ApplicationShareRolePolicy.Builder().mode(ApplicationShareRolePolicy.Mode.ALL).build();
        }

        List<RoleBasicInfo> filteredRolesToBeShared;

        if (ApplicationShareRolePolicy.Mode.NONE.ordinal() == currentConfig.getMode().ordinal()) {
            // Mode is NONE: remove all previously shared roles for this app.
            if (!alreadySharedSubOrgAppRoles.isEmpty()) {
                LOG.warn("Role sharing mode is NONE. Removing all " + alreadySharedSubOrgAppRoles.size() +
                        " previously shared roles for app " + sharedAppId + " in org " + sharedOrganizationId);
                for (RoleBasicInfo alreadySharedRole : alreadySharedSubOrgAppRoles) {
                    getRoleManagementServiceV2().deleteRole(alreadySharedRole.getId(), sharedAppTenantDomain);
                }
            }
            return Collections.emptyList();
        }

        Organization subOrg = getOrganization(sharedOrganizationId, false);
        List<RoleBasicInfo> allMainOrgAppRoles = getMainApplicationRoles(mainAppId, mainTenantDomain);

        if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() == currentConfig.getMode().ordinal()) {
            List<RoleBasicInfo> selectedMainRoles = filterSelectedMainAppRoles(allMainOrgAppRoles,
                    currentConfig.getRoleWithAudienceDOList());
            filteredRolesToBeShared = filterAppRolesByParentOrgAssociation(selectedMainRoles, mainOrganizationId,
                    subOrg);
        } else {
            filteredRolesToBeShared = filterAppRolesByParentOrgAssociation(allMainOrgAppRoles, mainOrganizationId,
                    subOrg);
        }

        // This list will track roles that are successfully provisioned or found existing from the
        // 'filteredRolesToBeShared' list.
        List<RoleBasicInfo> provisionedOrExistingSharedRoles = new ArrayList<>();
        if (!filteredRolesToBeShared.isEmpty()) {
            provisionOrVerifyApplicationRolesInSharedOrg(filteredRolesToBeShared, mainTenantDomain, sharedAppId,
                    sharedAppTenantDomain, provisionedOrExistingSharedRoles);
        }
        // Remove roles from sub-org that are no longer meant to be shared based on the current configuration.
        for (RoleBasicInfo alreadySharedRoleInSubOrg : alreadySharedSubOrgAppRoles) {
            boolean shouldRoleBeKept = false;
            if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() == currentConfig.getMode().ordinal()) {
                /* For SELECTED mode, keep if the role name from sub-org is in the current selection criteria
                 (name match). This replicates the original logic's name-only check against the config list
                 for deletion. */
                shouldRoleBeKept = currentConfig.getRoleWithAudienceDOList().stream()
                        .anyMatch(criterion -> criterion.getRoleName().equals(alreadySharedRoleInSubOrg.getName()) &&
                                RoleConstants.APPLICATION.equals(criterion.getAudienceType().toString()));
                // Original didn't check audienceName on criterion here for deletion.
            } else if (ApplicationShareRolePolicy.Mode.ALL.ordinal() == currentConfig.getMode().ordinal()) {
                // For ALL mode, keep if the role from sub-org matches one of the successfully verified roles.
                shouldRoleBeKept = provisionedOrExistingSharedRoles.stream()
                        .anyMatch(role -> role.getName().equals(alreadySharedRoleInSubOrg.getName()) &&
                                role.getAudienceName().equals(alreadySharedRoleInSubOrg.getAudienceName()));
            }

            if (!shouldRoleBeKept) {
                LOG.warn("Deleting stale role '" + alreadySharedRoleInSubOrg.getName() + "' (ID: " +
                        alreadySharedRoleInSubOrg.getId() + ") from shared app " + sharedAppId + " in org " +
                        sharedOrganizationId);
                getRoleManagementServiceV2().deleteRole(alreadySharedRoleInSubOrg.getId(), sharedAppTenantDomain);
            }
        }
        return filteredRolesToBeShared.stream().map(RoleBasicInfo::getId).collect(Collectors.toList());
    }

    /**
     * Adds new application roles to a shared application, only if the application's role sharing mode is 'SELECTED'.
     * This method does not remove existing roles; it only adds new ones specified.
     *
     * @param mainAppId            ID of the main application.
     * @param mainOrganizationId   ID of the main organization.
     * @param sharedAppId          ID of the shared application.
     * @param sharedOrganizationId ID of the organization hosting the shared application.
     * @param newRoles             List of {@link RoleWithAudienceDO} representing roles to be added.
     * @throws IdentityRoleManagementException If a role management error occurs.
     * @throws OrganizationManagementException If an organization management error occurs.
     */
    private void addAppRolesToSharedApp(String mainAppId, String mainOrganizationId, String sharedAppId,
                                        String sharedOrganizationId, List<RoleWithAudienceDO> newRoles)
            throws IdentityRoleManagementException, OrganizationManagementException {

        String sharedAppTenantDomain = resolveTenantDomain(sharedOrganizationId);
        String mainTenantDomain = resolveTenantDomain(mainOrganizationId);

        ServiceProvider sharedServiceProvider;
        try {
            sharedServiceProvider = getApplicationManagementService().getApplicationByResourceId(sharedAppId,
                    sharedAppTenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityRoleManagementException("Error while getting the application by resource id: " +
                    sharedAppId, e);
        }

        ApplicationShareRolePolicy.Mode appAssociatedRoleSharingMode = getAppAssociatedRoleSharingMode(
                sharedServiceProvider);
        if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() != appAssociatedRoleSharingMode.ordinal()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Role sharing mode is not set to SELECTED for the application: " + sharedAppId +
                        ". Only SELECTED mode is supported for role updates via addAppRolesToSharedApp.");
            }
            return;
        }

        if (newRoles == null || newRoles.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No new roles specified for addition to shared app " + sharedAppId);
            }
            return;
        }

        Organization subOrg = getOrganization(sharedOrganizationId, false);
        List<RoleBasicInfo> allMainOrgAppRoles = getMainApplicationRoles(mainAppId, mainTenantDomain);

        List<RoleBasicInfo> selectedMainRolesToAdd = filterSelectedMainAppRoles(allMainOrgAppRoles, newRoles);
        List<RoleBasicInfo> filteredRolesToBeAdded = filterAppRolesByParentOrgAssociation(selectedMainRolesToAdd,
                mainOrganizationId, subOrg);

        if (!filteredRolesToBeAdded.isEmpty()) {
            addNewApplicationRolesToSharedOrgInternal(filteredRolesToBeAdded, mainTenantDomain, sharedAppId,
                    sharedAppTenantDomain);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No roles to add to shared app " + sharedAppId + " after filtering.");
            }
        }
    }

    /**
     * Internal helper to add new application roles to a shared org. Skips if role already exists.
     */
    private void addNewApplicationRolesToSharedOrgInternal(List<RoleBasicInfo> rolesToAdd,
                                                           String mainTenantDomain, String sharedAppId,
                                                           String sharedAppTenantDomain)
            throws IdentityRoleManagementException {
        for (RoleBasicInfo roleToBeShared : rolesToAdd) {
            boolean isRoleAlreadyExist = getRoleManagementServiceV2().isExistingRoleName(roleToBeShared.getName(),
                    RoleConstants.APPLICATION, sharedAppId, sharedAppTenantDomain);
            if (isRoleAlreadyExist) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Role '" + roleToBeShared.getName() + "' already exists in the shared sub org: '" +
                            sharedAppTenantDomain + "' for app ID '" + sharedAppId + "'. Skipping creation.");
                }
                continue;
            }
            RoleBasicInfo subOrgRole = getRoleManagementServiceV2().addRole(roleToBeShared.getName(),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    RoleConstants.APPLICATION, sharedAppId, sharedAppTenantDomain);
            getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(roleToBeShared.getId(),
                    subOrgRole.getId(), mainTenantDomain, sharedAppTenantDomain);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Added role '" + subOrgRole.getName() + "' to shared app " + sharedAppId);
            }
        }
    }


    /**
     * Removes specified application roles from a shared application and its child organizations' shared applications.
     * Only proceeds if the shared application's role sharing mode is 'SELECTED'.
     *
     * @param mainApplicationId    ID of the main application.
     * @param sharedAppId          ID of the shared application.
     * @param sharedOrganizationId ID of the organization hosting the shared application.
     * @param rolesToBeRemoved     List of {@link RoleWithAudienceDO} specifying roles to remove.
     * @throws OrganizationManagementException If an organization management error occurs.
     * @throws IdentityRoleManagementException If a role management error occurs.
     */
    private void removeAppRolesFromSharedApp(String mainApplicationId, String mainOrganizationId, String sharedAppId,
                                             String sharedOrganizationId, List<RoleWithAudienceDO> rolesToBeRemoved)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String sharedAppTenantDomain = resolveTenantDomain(sharedOrganizationId);
        ServiceProvider sharedServiceProvider;
        try {
            sharedServiceProvider = getApplicationManagementService().getApplicationByResourceId(sharedAppId,
                    sharedAppTenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityRoleManagementClientException("Error while getting the application by resource id: " +
                    sharedAppId, e);
        }

        ApplicationShareRolePolicy.Mode appAssociatedRoleSharingMode = getAppAssociatedRoleSharingMode(
                sharedServiceProvider);
        if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() != appAssociatedRoleSharingMode.ordinal()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Role sharing mode is not set to SELECTED for the application: " + sharedAppId +
                        ". Only SELECTED mode supports role removal.");
            }
            return;
        }

        if (rolesToBeRemoved == null || rolesToBeRemoved.isEmpty()) {
            LOG.warn("No roles specified for removal from shared app " + sharedAppId);
            return;
        }
        Organization subOrg = getOrganization(sharedOrganizationId, true);
        deleteSharedAppRolesInternal(sharedAppTenantDomain, sharedAppId, rolesToBeRemoved);

        List<ChildOrganizationDO> childOrganizations = subOrg.getChildOrganizations();
        if (childOrganizations != null) {
            for (ChildOrganizationDO childOrganization : childOrganizations) {
                String childOrgId = childOrganization.getId();
                try {
                    String childOrgTenantDomain = resolveTenantDomain(childOrgId);
                    ServiceProvider childOrgApp = getOrgApplicationManager().resolveSharedApplicationByMainAppUUID(
                            mainApplicationId, mainOrganizationId, childOrgId);
                    deleteSharedAppRolesInternal(childOrgTenantDomain, childOrgApp.getApplicationResourceId(),
                            rolesToBeRemoved);
                } catch (OrganizationManagementException | IdentityRoleManagementException e) {
                    LOG.error("Error removing roles from child organization " + childOrgId + " for app " +
                            sharedAppId, e);
                }
            }
        }
    }

    /**
     * Internal helper to delete specified application roles from a shared application in a given tenant.
     *
     * @param tenantDomain          The tenant domain of the shared application.
     * @param applicationResourceId The ID of the application in this tenant.
     * @param rolesToDelete         List of {@link RoleWithAudienceDO} criteria for roles to delete.
     * @throws IdentityRoleManagementException If a role management error occurs.
     */
    private void deleteSharedAppRolesInternal(String tenantDomain, String applicationResourceId,
                                              List<RoleWithAudienceDO> rolesToDelete)
            throws IdentityRoleManagementException {

        if (rolesToDelete == null || rolesToDelete.isEmpty()) {
            return;
        }
        String appRolesFilter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + applicationResourceId;
        List<RoleBasicInfo> existingAppRoles = getRoleManagementServiceV2().getRoles(appRolesFilter, null,
                0, null, null, tenantDomain);

        if (existingAppRoles.isEmpty()) {
            return;
        }

        for (RoleWithAudienceDO roleCriterion : rolesToDelete) {
            // Ensure the criterion is for an APPLICATION role before attempting to match and delete.
            if (!RoleConstants.APPLICATION.equals(roleCriterion.getAudienceType().toString())) {
                continue;
            }
            for (RoleBasicInfo existingRole : existingAppRoles) {
                boolean isRoleNameEqual = existingRole.getName().equals(roleCriterion.getRoleName());
                // In this context, existingRole.getAudienceName() IS applicationResourceId.
                // So, roleCriterion.getAudienceName() must also be applicationResourceId for this to match.
                // This implies the RoleWithAudienceDO for deletion is constructed with audienceName = sharedAppId.
                boolean isAudienceNameEqual = existingRole.getAudienceName().equals(roleCriterion.getAudienceName());

                if (isRoleNameEqual && isAudienceNameEqual) {
                    getRoleManagementServiceV2().deleteRole(existingRole.getId(), tenantDomain);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Deleted role '" + existingRole.getName() + "' (ID: " + existingRole.getId() +
                                ") from app " + applicationResourceId + " in tenant " + tenantDomain);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Adds new organization-level roles to a shared application, only if the application's role sharing mode
     * is 'SELECTED'. These roles are typically associated with the main application and then shared.
     *
     * @param mainAppId            ID of the main application (source of roles).
     * @param mainOrganizationId   ID of the main organization.
     * @param sharedAppId          ID of the shared application (target).
     * @param sharedOrganizationId ID of the organization hosting the shared application.
     * @param newRoles             List of {@link RoleWithAudienceDO} representing org roles to be added/shared.
     * @throws IdentityRoleManagementException      If a role management error occurs.
     * @throws OrganizationManagementException      If an organization management error occurs.
     * @throws IdentityApplicationManagementException If an application management error occurs.
     */
    private void addOrgRolesToSharedApp(String mainAppId, String mainOrganizationId, String sharedAppId,
                                       String sharedOrganizationId, List<RoleWithAudienceDO> newRoles)
            throws IdentityRoleManagementException, OrganizationManagementException,
            IdentityApplicationManagementException {

        String sharedAppTenantDomain = resolveTenantDomain(sharedOrganizationId);
        String mainAppTenantDomain = resolveTenantDomain(mainOrganizationId);

        ServiceProvider sharedServiceProvider;
        try {
            sharedServiceProvider = getApplicationManagementService().getApplicationByResourceId(sharedAppId,
                    sharedAppTenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityRoleManagementClientException("Error while getting the application by resource id: " +
                    sharedAppId, e);
        }

        ApplicationShareRolePolicy.Mode appAssociatedRoleSharingMode = getAppAssociatedRoleSharingMode(
                sharedServiceProvider);

        if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() != appAssociatedRoleSharingMode.ordinal()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Role sharing mode is not set to SELECTED for the application: " + sharedAppId +
                        ". Only SELECTED mode is supported for org role updates.");
            }
            return;
        }

        if (newRoles == null || newRoles.isEmpty()) {
            LOG.warn("No new org roles provided to add to shared app " + sharedAppId);
            return;
        }

        List<RoleV2> associatedRolesOfMainApplication = getApplicationMgtService().getAssociatedRolesOfApplication(
                mainAppId, mainAppTenantDomain);

        if (associatedRolesOfMainApplication.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Main application " + mainAppId + " has no associated org roles. Cannot share any.");
            }
            return;
        }

        List<RoleV2> orgRolesToShareCandidates = new ArrayList<>();
        // Filter newRoles based on availability in main application's associated roles.
        for (RoleWithAudienceDO roleCriterion : newRoles) {
            // Assuming for org roles, we primarily match by name from the criterion.
            // AudienceType in RoleWithAudienceDO should ideally be ORGANIZATION for clarity.
            if (!RoleConstants.ORGANIZATION.equals(roleCriterion.getAudienceType().toString())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Role '" + roleCriterion.getRoleName() + "' is not an ORGANIZATION type. Skipping.");
                }
                continue;
            }
            Optional<RoleV2> mainOrgRoleOpt = associatedRolesOfMainApplication.stream()
                    .filter(roleInfo -> roleInfo.getName().equals(roleCriterion.getRoleName()))
                    .findFirst();

            if (mainOrgRoleOpt.isPresent()) {
                orgRolesToShareCandidates.add(mainOrgRoleOpt.get());
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The org role '" + roleCriterion.getRoleName() + "' specified in newRoles is not " +
                            "available as an associated role of the main application '" + mainAppId + "'. So it will" +
                            " not be shared.");
                }
            }
        }

        if (orgRolesToShareCandidates.isEmpty()) {
            LOG.warn("No org roles from newRoles were found in the main application's associated roles. " +
                    "Nothing to share.");
            return;
        }

        Organization sharedOrg = getOrganization(sharedOrganizationId, false);
        String parentOrgId = sharedOrg.getParent().getId();
        List<RoleV2> filteredRolesToBeShared = new ArrayList<>();

        if (mainOrganizationId.equals(parentOrgId)) {
            // Main organization is a direct parent, all candidates can be shared (if they are org roles).
            filteredRolesToBeShared.addAll(orgRolesToShareCandidates);
        } else {
            String parentOrgTenantDomain = resolveTenantDomain(parentOrgId);
            List<String> candidateMainRoleIds = orgRolesToShareCandidates.stream()
                    .map(RoleV2::getId)
                    .collect(Collectors.toList());
            if (!candidateMainRoleIds.isEmpty()) {
                Map<String, String> mainOrgToParentOrgRoleAssociations = getRoleManagementServiceV2()
                        .getMainRoleToSharedRoleMappingsBySubOrg(candidateMainRoleIds, parentOrgTenantDomain);

                for (RoleV2 candidateRole : orgRolesToShareCandidates) {
                    if (mainOrgToParentOrgRoleAssociations.containsKey(candidateRole.getId())) {
                        filteredRolesToBeShared.add(candidateRole);
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("The org role '" + candidateRole.getName() + "' (ID: " + candidateRole.getId() +
                                    ") is available in main app but not associated/available in the parent org '" +
                                    parentOrgId + "'. So it will not be shared with sub-org '" + sharedOrganizationId +
                                    "'.");
                        }
                    }
                }
            }
        }

        if (filteredRolesToBeShared.isEmpty()) {
            LOG.warn("After all filtering, no org roles are left to be shared with app " + sharedAppId + " in org " +
                    sharedOrganizationId);
            return;
        }
        createSharedRolesWithOrgAudience(filteredRolesToBeShared, mainAppTenantDomain, sharedOrganizationId);
        LOG.warn("Successfully processed sharing of " + filteredRolesToBeShared.size() + " org roles for app " +
                sharedAppId);
    }

    private void createSharedRolesOnNewRoleCreation(Map<String, Object> eventProperties)
            throws IdentityEventException {

        try {
            String mainRoleUUID = (String) eventProperties.get(IdentityEventConstants.EventProperty.ROLE_ID);
            String mainRoleName = (String) eventProperties.get(IdentityEventConstants.EventProperty.ROLE_NAME);
            String roleTenantDomain = (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
            String roleAudienceType = (String) eventProperties.get(IdentityEventConstants.EventProperty.AUDIENCE);
            String roleAudienceId = (String) eventProperties.get(IdentityEventConstants.EventProperty.AUDIENCE_ID);
            String roleOrgId = getOrganizationManager().resolveOrganizationId(roleTenantDomain);
            if (OrganizationManagementUtil.isOrganization(roleTenantDomain)) {
                return;
            }
            switch (roleAudienceType.toLowerCase()) {
                case RoleConstants.APPLICATION:
                    /*
                     If the audienced application is a shared application, create the role in
                     the shared apps' org space.
                     */
                    List<SharedApplication> sharedApplications =
                            getOrgApplicationManager().getSharedApplications(roleOrgId, roleAudienceId);
                    int noOfSharedApps = sharedApplications.size();
                    for (int i = 0; i < noOfSharedApps; i++) {
                        final int taskId = i;
                        CompletableFuture.runAsync(() -> {
                            try {
                                String sharedApplicationId = sharedApplications.get(taskId).getSharedApplicationId();
                                String sharedOrganizationId = sharedApplications.get(taskId).getOrganizationId();
                                String shareAppTenantDomain =
                                        getOrganizationManager().resolveTenantDomain(sharedOrganizationId);

                                ServiceProvider sharedServiceProvider = getApplicationManagementService()
                                        .getApplicationByResourceId(sharedApplicationId, shareAppTenantDomain);
                                ServiceProviderProperty[] spProperties = sharedServiceProvider.getSpProperties();
                                boolean isRoleSharingModeAll = false;
                                for (ServiceProviderProperty spProperty : spProperties) {
                                    if (ROLE_SHARING_MODE.equals(spProperty.getName())) {
                                        if (spProperty.getValue() != null && spProperty.getValue().equalsIgnoreCase(
                                                ApplicationShareRolePolicy.Mode.ALL.name())) {
                                            isRoleSharingModeAll = true;
                                            break;
                                        }
                                    }
                                }
                                if (!isRoleSharingModeAll) {
                                    return;
                                }
                                String associatedUserName =
                                        PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                                try {
                                    PrivilegedCarbonContext.startTenantFlow();
                                    PrivilegedCarbonContext.getThreadLocalCarbonContext()
                                            .setTenantDomain(shareAppTenantDomain, true);
                                    PrivilegedCarbonContext.getThreadLocalCarbonContext()
                                            .setUsername(associatedUserName);
                                    RoleBasicInfo sharedRoleInfo =
                                        getRoleManagementServiceV2().addRole(mainRoleName, Collections.emptyList(),
                                                Collections.emptyList(),
                                                Collections.emptyList(), RoleConstants.APPLICATION, sharedApplicationId,
                                                shareAppTenantDomain);
                                    // Add relationship between main role and shared role.
                                    getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(mainRoleUUID,
                                            sharedRoleInfo.getId(), roleTenantDomain, shareAppTenantDomain);
                                } finally {
                                    PrivilegedCarbonContext.endTenantFlow();
                                }
                            } catch (IdentityRoleManagementException | OrganizationManagementException e) {
                                LOG.error("Error occurred while creating shared role in organization with id: " +
                                        sharedApplications.get(taskId).getOrganizationId(), e);
                            } catch (IdentityApplicationManagementException e) {
                                LOG.error(String.format(ERROR_CODE_INVALID_APPLICATION.getMessage(),
                                        sharedApplications.get(taskId).getSharedApplicationId(),
                                        sharedApplications.get(taskId).getOrganizationId()), e);
                            }
                        }, executorService).exceptionally(throwable -> {
                            LOG.error(String.format(
                                    "Exception occurred during creating a shared role: %s in organization: %s",
                                    mainRoleName, sharedApplications.get(taskId).getOrganizationId()), throwable);
                            return null;
                        });
                    }
                    break;
                case RoleConstants.ORGANIZATION:
                    ApplicationBasicInfo[] applicationBasicInfo =
                            getApplicationMgtService().getApplicationBasicInfoBySPProperty(roleTenantDomain,
                                    PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername(),
                                    ALLOWED_AUDIENCE_FOR_ASSOCIATED_ROLES, RoleConstants.ORGANIZATION);
                    List<String> applicationIdList = new ArrayList<>();
                    for (ApplicationBasicInfo basicInfo : applicationBasicInfo) {
                        applicationIdList.add(basicInfo.getUuid());
                    }

                    List<BasicOrganization> applicationSharedOrganizations = new ArrayList<>();
                    for (String applicationId : applicationIdList) {
                        String shareAppTenantDomain = getOrganizationManager().resolveTenantDomain(roleOrgId);
                        ServiceProvider sharedServiceProvider = getApplicationManagementService()
                                .getApplicationByResourceId(applicationId, shareAppTenantDomain);
                        ServiceProviderProperty[] spProperties = sharedServiceProvider.getSpProperties();
                        boolean isRoleSharingModeAll = false;
                        for (ServiceProviderProperty spProperty : spProperties) {
                            if (ROLE_SHARING_MODE.equals(spProperty.getName())) {
                                if (spProperty.getValue() != null && spProperty.getValue().equalsIgnoreCase(
                                        ApplicationShareRolePolicy.Mode.ALL.name())) {
                                    isRoleSharingModeAll = true;
                                    break;
                                }
                            }
                        }
                        if (!isRoleSharingModeAll) {
                            return;
                        }
                        List<BasicOrganization> applicationSharedOrganizationsCopy = getOrgApplicationManager().
                                getApplicationSharedOrganizations(roleOrgId, applicationId);

                        for (BasicOrganization organizationCopy : applicationSharedOrganizationsCopy) {
                            boolean found = false;
                            for (BasicOrganization organization : applicationSharedOrganizations) {
                                if (organization.getId().equals(organizationCopy.getId())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                applicationSharedOrganizations.add(organizationCopy);
                            }
                        }

                    }
                    for (BasicOrganization organization : applicationSharedOrganizations) {
                        String shareAppTenantDomain =
                                getOrganizationManager().resolveTenantDomain(organization.getId());
                        if (!getRoleManagementServiceV2().isExistingRoleName(mainRoleName, RoleConstants.ORGANIZATION,
                                organization.getId(), shareAppTenantDomain)) {
                            RoleBasicInfo sharedRoleInfo = getRoleManagementServiceV2().addRole(mainRoleName,
                                    Collections.emptyList(),
                                    Collections.emptyList(),
                                    Collections.emptyList(), RoleConstants.ORGANIZATION, organization.getId(),
                                    shareAppTenantDomain);
                            getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(mainRoleUUID,
                                    sharedRoleInfo.getId(), roleTenantDomain, shareAppTenantDomain);
                        } else {
                            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                            String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().
                                    getTenantDomain();
                            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                                    IdentityUtil.getInitiatorId(username, tenantDomain),
                                    LoggerUtils.Target.User.name(), mainRoleName, LoggerUtils.Target.Role.name(),
                                    LogConstants.UserManagement.ADD_ROLE_ACTION)
                                    .data(buildAuditData(roleOrgId, null, organization.getId(), mainRoleName,
                                    mainRoleUUID, "Role conflict"));
                            LoggerUtils.triggerAuditLogEvent(auditLogBuilder, true);
                            LOG.warn(String.format("Organization %s has a non shared role with name %s, ",
                                    organization.getId(), mainRoleName));
                        }
                    }
                    break;
                default:
                    LOG.error("Unsupported audience type: " + roleAudienceType);
            }
        } catch (OrganizationManagementException | IdentityApplicationManagementException |
                 IdentityRoleManagementException e) {
            throw new IdentityEventException("Error occurred while retrieving shared applications.", e);
        }
    }

    private void checkSharingRoleConflicts(Map<String, Object> eventProperties) throws IdentityEventException {

        String parentOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID);
        String parentApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID);
        String sharedOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID);
        String sharedApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID);

        try {
            String sharedAppTenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
            String mainAppTenantDomain = getOrganizationManager().resolveTenantDomain(parentOrganizationId);
            String allowedAudienceForRoleAssociationInMainApp = getApplicationMgtService().
                    getAllowedAudienceForRoleAssociation(parentApplicationId, mainAppTenantDomain);
            if (RoleConstants.ORGANIZATION.equals(allowedAudienceForRoleAssociationInMainApp.toLowerCase())) {
                List<RoleV2> associatedRolesOfApplication = getApplicationMgtService().
                        getAssociatedRolesOfApplication(parentApplicationId, mainAppTenantDomain);
                for (RoleV2 roleV2 : associatedRolesOfApplication) {
                    boolean roleExistsInSharedOrg = getRoleManagementServiceV2().isExistingRoleName(roleV2.getName(),
                            RoleConstants.ORGANIZATION, sharedOrganizationId, sharedAppTenantDomain);
                    Map<String, String> mainRoleToSharedRoleMappingInSharedOrg =
                            getRoleManagementServiceV2().getMainRoleToSharedRoleMappingsBySubOrg(
                                    Collections.singletonList(roleV2.getId()), sharedAppTenantDomain);
                    boolean roleRelationshipExistsInSharedOrg =
                            MapUtils.isNotEmpty(mainRoleToSharedRoleMappingInSharedOrg);
                    if (roleExistsInSharedOrg && !roleRelationshipExistsInSharedOrg) {
                        // If the role exists in the shared org, but the relationship does not exist then this role is
                        // created directly in the sub organization level. So this is a conflict to share the role
                        // with same name and organization audience to the sub organization.
                        String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().
                                getTenantDomain();
                        AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                                IdentityUtil.getInitiatorId(username, tenantDomain),
                                LoggerUtils.Target.User.name(), roleV2.getName(), LoggerUtils.Target.Role.name(),
                                LogConstants.ApplicationManagement.CREATE_APPLICATION_ACTION).
                                data(buildAuditData(parentOrganizationId, parentApplicationId,
                                        sharedOrganizationId, roleV2.getName(), roleV2.getId(), "Role conflict"));
                        LoggerUtils.triggerAuditLogEvent(auditLogBuilder, true);
                        throw new IdentityEventException(String.format("Organization %s has a non shared role with " +
                                "name %s, ", sharedOrganizationId, roleV2.getName()));
                    }
                }
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException |
                 IdentityApplicationManagementException e) {
            throw new IdentityEventException(String.format("Error while sharing roles related to application %s.",
                    sharedApplicationId), e);
        }
    }

    private static RoleManagementService getRoleManagementServiceV2() {

        return OrganizationManagementHandlerDataHolder.getInstance().getRoleManagementServiceV2();
    }

    private static OrganizationManager getOrganizationManager() {

        return OrganizationManagementHandlerDataHolder.getInstance().getOrganizationManager();
    }

    private static OrgApplicationManager getOrgApplicationManager() {

        return OrganizationManagementHandlerDataHolder.getInstance().getOrgApplicationManager();
    }

    private static ApplicationManagementService getApplicationMgtService() {

        return OrganizationManagementHandlerDataHolder.getInstance().getApplicationManagementService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrganizationManagementHandlerDataHolder.getInstance().getApplicationManagementService();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationManagementHandlerDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private Map<String, String> buildAuditData(String parentOrganizationId, String parentApplicationId,
                                          String sharedOrganizationId, String roleName, String roleId,
                                          String failureReason) {

        Map<String, String> auditData = new HashMap<>();
        auditData.put(RoleConstants.PARENT_ORG_ID, parentOrganizationId);
        auditData.put("parentApplicationId", parentApplicationId);
        auditData.put(RoleConstants.SHARED_ORG_ID, sharedOrganizationId);
        auditData.put("roleId", roleId);
        auditData.put("roleName", roleName);
        auditData.put(RoleConstants.FAILURE_REASON, failureReason);
        return auditData;
    }
}

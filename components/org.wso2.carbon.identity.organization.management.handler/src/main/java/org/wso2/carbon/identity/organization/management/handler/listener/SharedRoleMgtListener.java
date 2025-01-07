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

package org.wso2.carbon.identity.organization.management.handler.listener;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.AssociatedRolesConfig;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;

/**
 * Application management listener to handle shared roles in organizations.
 */
public class SharedRoleMgtListener extends AbstractApplicationMgtListener {

    private static final Log LOG = LogFactory.getLog(SharedRoleMgtListener.class);
    private static final String REMOVED_APPLICATION_AUDIENCE_ROLES = "removedApplicationAudienceRoles";
    private static final String ADDED_APPLICATION_AUDIENCE_ROLES = "addedApplicationAudienceRoles";
    private static final String REMOVED_ORGANIZATION_AUDIENCE_ROLES = "removedOrganizationAudienceRoles";
    private static final String ADDED_ORGANIZATION_AUDIENCE_ROLES = "addedOrganizationAudienceRoles";
    private static final String SPACE = " ";
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    ApplicationManagementService applicationManagementService =
            OrganizationManagementHandlerDataHolder.getInstance().getApplicationManagementService();
    OrganizationManager organizationManager =
            OrganizationManagementHandlerDataHolder.getInstance().getOrganizationManager();
    OrgApplicationManager orgApplicationManager =
            OrganizationManagementHandlerDataHolder.getInstance().getOrgApplicationManager();
    RoleManagementService roleManagementService =
            OrganizationManagementHandlerDataHolder.getInstance().getRoleManagementServiceV2();

    @Override
    public int getDefaultOrderId() {

        return 49;
    }

    @Override
    public boolean doPreUpdateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        // Associated role changes on main applications in tenant need to be handled here.
        try {
            if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
                return true;
            }
            String applicationResourceId = serviceProvider.getApplicationResourceId();
            // Get the currently associated roles set from DB/cache.
            String existingAllowedAudienceForRoleAssociation =
                    applicationManagementService.getAllowedAudienceForRoleAssociation(applicationResourceId,
                            tenantDomain);
            List<RoleV2> existingAssociatedRolesList =
                    applicationManagementService.getAssociatedRolesOfApplication(applicationResourceId, tenantDomain);

            String updatedAllowedAudienceForRoleAssociation = ((serviceProvider.getAssociatedRolesConfig() == null) ||
                    (serviceProvider.getAssociatedRolesConfig().getAllowedAudience() == null)) ?
                    RoleConstants.APPLICATION : serviceProvider.getAssociatedRolesConfig().getAllowedAudience();

            // If the existing and updated audiences are both organization, no need to update the roles.
            if (RoleConstants.ORGANIZATION.equalsIgnoreCase(existingAllowedAudienceForRoleAssociation) &&
                    RoleConstants.ORGANIZATION.equalsIgnoreCase(updatedAllowedAudienceForRoleAssociation)) {
                return true;
            }

            List<RoleV2> updatedAssociatedRolesList = new ArrayList<>();
            if (serviceProvider.getAssociatedRolesConfig() != null &&
                    serviceProvider.getAssociatedRolesConfig().getRoles() != null) {

                switch (updatedAllowedAudienceForRoleAssociation.toLowerCase()) {
                    case RoleConstants.APPLICATION:
                        updatedAssociatedRolesList =
                                Arrays.asList(serviceProvider.getAssociatedRolesConfig().getRoles());
                        break;
                    case RoleConstants.ORGANIZATION:
                        RoleManagementService roleManagementService = OrganizationManagementHandlerDataHolder
                                .getInstance().getRoleManagementServiceV2();
                        List<RoleBasicInfo> chunkOfRoles;
                        int offset = 1;
                        int maximumPage = IdentityUtil.getMaximumItemPerPage();
                        List<RoleBasicInfo> allRoles = new ArrayList<>();
                        if (roleManagementService != null) {
                            do {
                                chunkOfRoles = roleManagementService.
                                        getRoles(RoleConstants.AUDIENCE + SPACE + RoleConstants.EQ + SPACE +
                                                        RoleConstants.ORGANIZATION, maximumPage, offset, null, null,
                                                tenantDomain);
                                if (!chunkOfRoles.isEmpty()) {
                                    allRoles.addAll(chunkOfRoles);
                                    offset += chunkOfRoles.size(); // Move to the next chunk
                                }
                            } while (chunkOfRoles.size() == maximumPage);

                            List<String> roleIds = allRoles.stream().map(RoleBasicInfo::getId).collect(Collectors.
                                    toList());
                            for (String roleId : roleIds) {
                                // Get all role details for each role id and create a RoleV2 object.
                                Role roleBasicInfo = roleManagementService.getRole(roleId, tenantDomain);
                                if (roleBasicInfo != null) {
                                    updatedAssociatedRolesList.add(new RoleV2(roleBasicInfo.getId(),
                                            roleBasicInfo.getName()));
                                }
                            }
                        }
                        break;
                }
            }

            if (CollectionUtils.isEmpty(existingAssociatedRolesList) &&
                    CollectionUtils.isEmpty(updatedAssociatedRolesList)) {
                // No change in roles list.
                return true;
            }

            // Creating a copy of the list to avoid concurrent modification to facilitate lambda operations.
            List<RoleV2> finalUpdatedAssociatedRolesList = updatedAssociatedRolesList;
            /*
            if old and new audiences are equals, need to handle the role diff.
             */
            if (existingAllowedAudienceForRoleAssociation.equalsIgnoreCase(updatedAllowedAudienceForRoleAssociation)) {
                switch (updatedAllowedAudienceForRoleAssociation.toLowerCase()) {
                    case RoleConstants.APPLICATION:
                        List<RoleV2> addedApplicationAudienceRoles = finalUpdatedAssociatedRolesList.stream()
                                .filter(updatedRole -> !existingAssociatedRolesList.contains(updatedRole))
                                .collect(Collectors.toList());

                        List<RoleV2> removedApplicationAudienceRoles = existingAssociatedRolesList.stream()
                                .filter(existingRole -> !finalUpdatedAssociatedRolesList.contains(existingRole))
                                .collect(Collectors.toList());
                        // Add to threadLocal.
                        IdentityUtil.threadLocalProperties.get()
                                .put(ADDED_APPLICATION_AUDIENCE_ROLES, addedApplicationAudienceRoles);
                        IdentityUtil.threadLocalProperties.get()
                                .put(REMOVED_APPLICATION_AUDIENCE_ROLES, removedApplicationAudienceRoles);
                        return true;
                    case RoleConstants.ORGANIZATION:
                        if (existingAssociatedRolesList.equals(finalUpdatedAssociatedRolesList)) {
                            // Nothing to change in shared applications' organizations.
                            return true;
                        }
                        // Get the added roles and removed roles.
                        List<RoleV2> addedOrganizationAudienceRoles = finalUpdatedAssociatedRolesList.stream()
                                .filter(updatedRole -> !existingAssociatedRolesList.contains(updatedRole))
                                .collect(Collectors.toList());

                        List<RoleV2> removedOrganizationAudienceRoles = existingAssociatedRolesList.stream()
                                .filter(existingRole -> !finalUpdatedAssociatedRolesList.contains(existingRole))
                                .collect(Collectors.toList());
                        // Add to threadLocal.
                        IdentityUtil.threadLocalProperties.get()
                                .put(ADDED_ORGANIZATION_AUDIENCE_ROLES, addedOrganizationAudienceRoles);
                        IdentityUtil.threadLocalProperties.get()
                                .put(REMOVED_ORGANIZATION_AUDIENCE_ROLES, removedOrganizationAudienceRoles);
                        return true;
                }
            }

            /*
            If audience has changed from application to organization, all previous associated roles will be deleted.
            For updated organization roles, create shared role in organizations below if applicable.
             */
            if (RoleConstants.APPLICATION.equalsIgnoreCase(existingAllowedAudienceForRoleAssociation) &&
                    RoleConstants.ORGANIZATION.equalsIgnoreCase(updatedAllowedAudienceForRoleAssociation)) {

                // Add to thread local.
                IdentityUtil.threadLocalProperties.get()
                        .put(REMOVED_APPLICATION_AUDIENCE_ROLES, existingAssociatedRolesList);
                IdentityUtil.threadLocalProperties.get()
                        .put(ADDED_ORGANIZATION_AUDIENCE_ROLES, finalUpdatedAssociatedRolesList);
                return true;
            }

            /*
             If audience has changed from organization to application, need to remove the organization roles.
             Nothing to handle in application audience roles because they will be added/deleted in shared orgs
             based on role creation/deletion.
             */
            if (RoleConstants.ORGANIZATION.equalsIgnoreCase(existingAllowedAudienceForRoleAssociation) &&
                    RoleConstants.APPLICATION.equalsIgnoreCase(updatedAllowedAudienceForRoleAssociation)) {

                // Add to thread local.
                IdentityUtil.threadLocalProperties.get()
                        .put(REMOVED_ORGANIZATION_AUDIENCE_ROLES, existingAssociatedRolesList);
                return true;
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityApplicationManagementException(
                    String.format("Error while checking shared roles to be updated related to application %s update.",
                            serviceProvider.getApplicationID()), e);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityApplicationManagementException(
                    String.format("Error while retrieving organization roles to be updated related " +
                            "to application %s update.", serviceProvider.getApplicationID()), e);

        }
        return true;
    }

    @Override
    public boolean doPostUpdateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        try {
            if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
                return true;
            }
            Object addedAppRoles = IdentityUtil.threadLocalProperties.get().get(ADDED_APPLICATION_AUDIENCE_ROLES);
            if (addedAppRoles != null) {
                List<RoleV2> addedAppRolesList = (List<RoleV2>) addedAppRoles;
                handleAddedApplicationAudienceRolesOnAppUpdate(addedAppRolesList, serviceProvider, tenantDomain);
            }

            Object removedAppRoles = IdentityUtil.threadLocalProperties.get().get(REMOVED_APPLICATION_AUDIENCE_ROLES);
            if (removedAppRoles != null) {
                List<RoleV2> removedAppRolesList = (List<RoleV2>) removedAppRoles;
                handleRemovedApplicationAudienceRolesOnAppUpdate(removedAppRolesList, tenantDomain);
            }

            Object addedOrgRoles = IdentityUtil.threadLocalProperties.get().get(ADDED_ORGANIZATION_AUDIENCE_ROLES);
            if (addedOrgRoles != null) {
                List<RoleV2> addedOrgRolesList = (List<RoleV2>) addedOrgRoles;
                List<RoleV2> namesResolvedAddedRolesList = addedOrgRolesList.stream()
                        .map(role -> {
                            try {
                                String roleName = roleManagementService.getRoleNameByRoleId(role.getId(), tenantDomain);
                                if (roleName != null) {
                                    return new RoleV2(role.getId(), roleName);
                                }
                                return null;
                            } catch (Exception e) {
                                LOG.error("Failed to resolve role name of role id: " + role.getId());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull) // Filter out null values (roles that couldn't be resolved)
                        .collect(Collectors.toList());

                handleAddedOrganizationAudienceRolesOnAppUpdate(namesResolvedAddedRolesList, serviceProvider,
                        tenantDomain);
            }

            Object removedOrgRoles = IdentityUtil.threadLocalProperties.get().get(REMOVED_ORGANIZATION_AUDIENCE_ROLES);
            if (removedOrgRoles != null) {
                List<RoleV2> removedOrgRolesList = (List<RoleV2>) removedOrgRoles;
                handleRemovedOrganizationAudienceRolesOnAppUpdate(removedOrgRolesList, serviceProvider, tenantDomain);
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            throw new IdentityApplicationManagementException(
                    String.format("Error while updating shared roles related to application %s update.",
                            serviceProvider.getApplicationID()), e);
        } finally {
            IdentityUtil.threadLocalProperties.get().remove(ADDED_APPLICATION_AUDIENCE_ROLES);
            IdentityUtil.threadLocalProperties.get().remove(REMOVED_APPLICATION_AUDIENCE_ROLES);
            IdentityUtil.threadLocalProperties.get().remove(ADDED_ORGANIZATION_AUDIENCE_ROLES);
            IdentityUtil.threadLocalProperties.get().remove(REMOVED_ORGANIZATION_AUDIENCE_ROLES);
        }
        return true;
    }

    private void handleRemovedOrganizationAudienceRolesOnAppUpdate(List<RoleV2> removedOrgRolesList,
                                                                   ServiceProvider serviceProvider, String tenantDomain)
            throws OrganizationManagementException, IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(removedOrgRolesList)) {
            return;
        }
        String mainAppId = serviceProvider.getApplicationResourceId();
        String mainAppOrgId = organizationManager.resolveOrganizationId(tenantDomain);
        List<SharedApplication> sharedApplications =
                orgApplicationManager.getSharedApplications(mainAppOrgId, mainAppId);
        if (CollectionUtils.isEmpty(sharedApplications)) {
            return;
        }
        for (SharedApplication sharedApplication : sharedApplications) {
            CompletableFuture.runAsync(() -> {
                String sharedAppOrgId = sharedApplication.getOrganizationId();
                try {
                    handleOrganizationAudiencedSharedRoleDeletion(removedOrgRolesList,
                            serviceProvider.getApplicationResourceId(),
                            tenantDomain, sharedAppOrgId);
                } catch (IdentityRoleManagementException | OrganizationManagementException e) {
                    LOG.error(String.format("Exception occurred during deleting roles from organization %s",
                            sharedApplication.getOrganizationId()), e);
                }
            }, executorService).exceptionally(throwable -> {
                LOG.error(String.format("Exception occurred during deleting roles from organization %s",
                        sharedApplication.getOrganizationId()), throwable);
                return null;
            });
        }
    }

    private void handleAddedOrganizationAudienceRolesOnAppUpdate(List<RoleV2> addedOrgRolesList,
                                                                 ServiceProvider serviceProvider, String tenantDomain)
            throws OrganizationManagementException, IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(addedOrgRolesList)) {
            return;
        }
        String mainAppId = serviceProvider.getApplicationResourceId();
        String mainAppOrgId = organizationManager.resolveOrganizationId(tenantDomain);
        List<SharedApplication> sharedApplications =
                orgApplicationManager.getSharedApplications(mainAppOrgId, mainAppId);
        if (CollectionUtils.isEmpty(sharedApplications)) {
            return;
        }

        for (SharedApplication sharedApplication : sharedApplications) {
            CompletableFuture.runAsync(() -> {
                String sharedAppOrgId = sharedApplication.getOrganizationId();
                try {
                    String shareAppTenantDomain = organizationManager.resolveTenantDomain(sharedAppOrgId);
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(shareAppTenantDomain, true);
                    createSharedRolesWithOrgAudience(addedOrgRolesList, tenantDomain, sharedAppOrgId);
                } catch (IdentityRoleManagementException | OrganizationManagementException e) {
                    LOG.error(String.format("Exception occurred while adding shared roles to organization: %s",
                            sharedApplication.getOrganizationId()), e);
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }, executorService).exceptionally(throwable -> {
                LOG.error(String.format("Exception occurred while adding shared roles to organization: %s",
                        sharedApplication.getOrganizationId()), throwable);
                return null;
            });
        }
    }

    private void createSharedRolesWithOrgAudience(List<RoleV2> rolesList, String mainAppTenantDomain,
                                                  String sharedAppOrgId)
            throws IdentityRoleManagementException, OrganizationManagementException {

        if (rolesList == null) {
            return;
        }
        String sharedAppTenantDomain = organizationManager.resolveTenantDomain(sharedAppOrgId);
        for (RoleV2 role : rolesList) {
            // Check if the role exists in the application shared org.
            boolean roleExistsInSharedOrg =
                    roleManagementService.isExistingRoleName(role.getName(), RoleConstants.ORGANIZATION,
                            sharedAppOrgId, sharedAppTenantDomain);
            Map<String, String> mainRoleToSharedRoleMappingInSharedOrg =
                    roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                            Collections.singletonList(role.getId()), sharedAppTenantDomain);
            boolean roleRelationshipExistsInSharedOrg =
                    MapUtils.isNotEmpty(mainRoleToSharedRoleMappingInSharedOrg);
            if (roleExistsInSharedOrg && !roleRelationshipExistsInSharedOrg) {
                // Add relationship between main role and shared role.
                String roleIdInSharedOrg =
                        roleManagementService.getRoleIdByName(role.getName(), RoleConstants.ORGANIZATION,
                                sharedAppOrgId, sharedAppTenantDomain);
                roleManagementService.addMainRoleToSharedRoleRelationship(role.getId(),
                        roleIdInSharedOrg, mainAppTenantDomain, sharedAppTenantDomain);
            } else if (!roleExistsInSharedOrg && !roleRelationshipExistsInSharedOrg) {
                // Create the role in the shared org.
                RoleBasicInfo sharedRole = roleManagementService.addRole(role.getName(), Collections.emptyList(),
                                Collections.emptyList(), Collections.emptyList(), RoleConstants.ORGANIZATION,
                                sharedAppOrgId, sharedAppTenantDomain);
                // Add relationship between main role and shared role.
                roleManagementService.addMainRoleToSharedRoleRelationship(role.getId(),
                        sharedRole.getId(), mainAppTenantDomain, sharedAppTenantDomain);
            }
        }
    }

    private void handleRemovedApplicationAudienceRolesOnAppUpdate(List<RoleV2> removedAppRolesList, String tenantDomain)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(removedAppRolesList)) {
            return;
        }
        /*
        Delete the application audience roles from parent organization. Deleting their shared roles also handled inside.
         */
        for (RoleV2 removedRole : removedAppRolesList) {
            roleManagementService.deleteRole(removedRole.getId(), tenantDomain);
        }
    }

    private void handleAddedApplicationAudienceRolesOnAppUpdate(List<RoleV2> addedAppRolesList,
                                                                ServiceProvider serviceProvider, String tenantDomain)
            throws OrganizationManagementException, IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(addedAppRolesList)) {
            return;
        }
        // Get shared applications of the given main app, and share the role.
        String mainAppId = serviceProvider.getApplicationResourceId();
        String mainAppOrgId = organizationManager.resolveOrganizationId(tenantDomain);

        List<SharedApplication> sharedApplications =
                orgApplicationManager.getSharedApplications(mainAppOrgId, mainAppId);
        if (CollectionUtils.isEmpty(sharedApplications)) {
            return;
        }

        for (RoleV2 parentRole : addedAppRolesList) {
            for (SharedApplication sharedApplication : sharedApplications) {
                String sharedAppOrgId = sharedApplication.getOrganizationId();
                String sharedAppTenantDomain = organizationManager.resolveTenantDomain(sharedAppOrgId);
                String parentAppRoleName = parentRole.getName();
                // Create the role in the shared org.
                RoleBasicInfo subOrgRole =
                        roleManagementService.addRole(parentAppRoleName, Collections.emptyList(),
                                Collections.emptyList(), Collections.emptyList(), RoleConstants.APPLICATION,
                                sharedApplication.getSharedApplicationId(),
                                sharedAppTenantDomain);
                // Add relationship between main role and the shared role.
                roleManagementService.addMainRoleToSharedRoleRelationship(parentRole.getId(), subOrgRole.getId(),
                        tenantDomain, sharedAppTenantDomain);
            }
        }
    }

    @Override
    public boolean doPreDeleteApplication(String applicationName, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        try {
            // If the tenant is not an organization, no need to handle shared roles.
            if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                return true;
            }

            ServiceProvider serviceProvider = getApplicationByName(applicationName, tenantDomain);
            if (serviceProvider == null) {
                return false;
            }

            // If the application is not a fragment app in the sub organization level, no need to handle shared roles.
            boolean isFragmentApp = Arrays.stream(serviceProvider.getSpProperties())
                    .anyMatch(property -> IS_FRAGMENT_APP.equals(property.getName()) &&
                            Boolean.parseBoolean(property.getValue()));
            if (!isFragmentApp) {
                // Given app is a sub org level application.
                return true;
            }

            String sharedAppId = serviceProvider.getApplicationResourceId();
            String sharedAppOrgId = organizationManager.resolveOrganizationId(tenantDomain);
            // Resolve the main application details.
            String mainAppId = orgApplicationManager.getMainApplicationIdForGivenSharedApp(sharedAppId, sharedAppOrgId);
            if (mainAppId == null) {
                return false;
            }
            int mainAppTenantId = applicationManagementService.getTenantIdByApp(mainAppId);
            String mainAppTenantDomain = IdentityTenantUtil.getTenantDomain(mainAppTenantId);

            String allowedAudienceForRoleAssociationInMainApp =
                    applicationManagementService.getAllowedAudienceForRoleAssociation(mainAppId, mainAppTenantDomain);
            boolean hasAppAudiencedRoles =
                    RoleConstants.APPLICATION.equalsIgnoreCase(allowedAudienceForRoleAssociationInMainApp);
            if (hasAppAudiencedRoles) {
                // Handle role deletion in application deletion post actions.
                return true;
            }

            // Handing organization audienced roles associated case.
            List<RoleV2> associatedRolesOfMainApplication = applicationManagementService
                    .getAssociatedRolesOfApplication(mainAppId, mainAppTenantDomain);
            handleOrganizationAudiencedSharedRoleDeletion(associatedRolesOfMainApplication, mainAppId,
                    mainAppTenantDomain, sharedAppOrgId);
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            throw new IdentityApplicationManagementException(
                    "Error while deleting organization roles associated to the app.", e);
        }
        return super.doPreDeleteApplication(applicationName, tenantDomain, userName);
    }

    private void handleOrganizationAudiencedSharedRoleDeletion(List<RoleV2> rolesList, String mainApplicationId,
                                                               String mainApplicationTenantDomain,
                                                               String sharedAppOrgId)
            throws IdentityRoleManagementException, OrganizationManagementException {

        String mainApplicationOrgId = organizationManager.resolveOrganizationId(mainApplicationTenantDomain);
        if (mainApplicationOrgId == null) {
            mainApplicationOrgId = SUPER_ORG_ID;
        }
        String sharedAppTenantDomain = organizationManager.resolveTenantDomain(sharedAppOrgId);
        List<String> mainAppRoleIds =
                rolesList.stream().map(RoleV2::getId).collect(Collectors.toList());
        Map<String, String> mainRoleToSharedRoleMappingsInSubOrg =
                roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(mainAppRoleIds, sharedAppTenantDomain);

        // Get each role associated applications.
        for (String mainAppRoleId : mainAppRoleIds) {
            List<String> associatedApplicationsIds =
                    roleManagementService.getAssociatedApplicationByRoleId(mainAppRoleId,
                            mainApplicationTenantDomain);
            String sharedRoleId = mainRoleToSharedRoleMappingsInSubOrg.get(mainAppRoleId);
            if (StringUtils.isBlank(sharedRoleId)) {
                // There is no role available in the shared org. May be due to role creation issue.
                continue;
            }
            /*
            If this private method is called from application update post listener, the role already removed
            from the application. associatedApplicationsIds is empty means there are no any other applications.

             If this private method is called from application deletion post listener,
             and if the only associated application is the main app in this flow, this condition is satisfied.
             Hence, deleting the shared roles.
             */
            if (CollectionUtils.isEmpty(associatedApplicationsIds) || (associatedApplicationsIds.size() == 1 &&
                    mainApplicationId.equals(associatedApplicationsIds.get(0)))) {
                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedAppTenantDomain, true);
                    roleManagementService.deleteRole(sharedRoleId, sharedAppTenantDomain);
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            } else if (associatedApplicationsIds.size() > 1) {
                boolean isRoleUsedByAnotherSharedApp = false;
                for (String associatedApplicationId : associatedApplicationsIds) {
                    if (associatedApplicationId.equals(mainApplicationId)) {
                        continue;
                    }
                    boolean applicationSharedWithGivenOrganization =
                            orgApplicationManager.isApplicationSharedWithGivenOrganization(associatedApplicationId,
                                    mainApplicationOrgId, sharedAppOrgId);
                    if (applicationSharedWithGivenOrganization) {
                        isRoleUsedByAnotherSharedApp = true;
                        break;
                    }
                }
                if (!isRoleUsedByAnotherSharedApp) {
                    // Delete the role in org.
                    roleManagementService.deleteRole(sharedRoleId, sharedAppTenantDomain);
                }
            }
        }
    }

    private ServiceProvider getApplicationByName(String name, String tenantDomain)
            throws IdentityApplicationManagementException {

        return applicationManagementService.getServiceProvider(name, tenantDomain);
    }

    @Override
    public boolean doPostGetAllowedAudienceForRoleAssociation(AssociatedRolesConfig allowedAudienceForRoleAssociation,
                                                              String applicationUUID, String tenantDomain)
            throws IdentityApplicationManagementException {

        String mainAppId = applicationManagementService.getMainAppId(applicationUUID);
        // If the main application id is null, then this is the main application. We can skip this operation
        // based on that.
        if (StringUtils.isEmpty(mainAppId)) {
            return true;
        }
        // Resolve the allowed audience for associated roles of shared application from main application details.
        int mainAppTenantId = applicationManagementService.getTenantIdByApp(mainAppId);
        String mainAppTenantDomain = IdentityTenantUtil.getTenantDomain(mainAppTenantId);
        String resolvedAllowedAudienceFromMainApp =
                applicationManagementService.getAllowedAudienceForRoleAssociation(mainAppId, mainAppTenantDomain);
        allowedAudienceForRoleAssociation.setAllowedAudience(resolvedAllowedAudienceFromMainApp);
        return true;
    }

    @Override
    public boolean doPostGetAssociatedRolesOfApplication(List<RoleV2> associatedRolesOfApplication,
                                                         String applicationUUID, String tenantDomain)
            throws IdentityApplicationManagementException {

        try {
            String mainAppId = applicationManagementService.getMainAppId(applicationUUID);
            // If the main application id is null, then this is the main application. We can skip this operation based
            // on that.
            if (StringUtils.isEmpty(mainAppId)) {
                return true;
            }
            // Resolve the associated roles of shared application from main application details.
            int mainAppTenantId = applicationManagementService.getTenantIdByApp(mainAppId);
            String mainAppTenantDomain = IdentityTenantUtil.getTenantDomain(mainAppTenantId);
            List<RoleV2> resolvedAssociatedRolesFromMainApp =
                    applicationManagementService.getAssociatedRolesOfApplication(mainAppId, mainAppTenantDomain);
            List<String> mainAppRoleIds =
                    resolvedAssociatedRolesFromMainApp.stream().map(RoleV2::getId).collect(Collectors.toList());
            Map<String, String> mainRoleToSharedRoleMappingsInSubOrg =
                    roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(mainAppRoleIds, tenantDomain);
            List<RoleV2> associatedRolesOfSharedApplication = mainRoleToSharedRoleMappingsInSubOrg.entrySet().stream()
                    .map(entry -> {
                        String sharedRoleId = entry.getValue();
                        String mainRoleId = entry.getKey();

                        // Find the main role by ID and retrieve its name.
                        String mainRoleName = resolvedAssociatedRolesFromMainApp.stream()
                                .filter(role -> role.getId().equals(mainRoleId))
                                .findFirst()
                                .map(RoleV2::getName)
                                .orElse(null);

                        RoleV2 sharedRole = new RoleV2();
                        sharedRole.setId(sharedRoleId);
                        sharedRole.setName(mainRoleName);
                        return sharedRole;
                    })
                    .collect(Collectors.toList());
            associatedRolesOfApplication.clear();
            associatedRolesOfApplication.addAll(associatedRolesOfSharedApplication);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityApplicationManagementException(String.format(
                    "Error while fetching the allowed audience for role association of application with: %s.",
                    applicationUUID), e);
        }
        return true;

    }

    private String resolveEveryoneOrganizationRole(String tenantDomain) throws IdentityRoleManagementException {

        try {
            String internalEveryoneRole = OrganizationManagementHandlerDataHolder.getInstance().getRealmService()
                    .getTenantUserRealm(IdentityTenantUtil.getTenantId(tenantDomain)).getRealmConfiguration()
                    .getEveryOneRoleName();
            return UserCoreUtil.removeDomainFromName(internalEveryoneRole);
        } catch (UserStoreException e) {
            throw new IdentityRoleManagementException(String.format(
                    "Error while fetching the internal everyone role of the tenant with: %s.", tenantDomain), e);
        }
    }
}

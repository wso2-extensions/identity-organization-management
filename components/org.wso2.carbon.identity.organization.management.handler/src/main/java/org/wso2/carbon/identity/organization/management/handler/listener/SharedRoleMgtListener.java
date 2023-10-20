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

package org.wso2.carbon.identity.organization.management.handler.listener;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
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
import org.wso2.carbon.identity.role.v2.mgt.core.AssociatedApplication;
import org.wso2.carbon.identity.role.v2.mgt.core.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleBasicInfo;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

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
                    getApplicationMgtService().getAllowedAudienceForRoleAssociation(applicationResourceId,
                            tenantDomain);
            List<RoleV2> existingAssociatedRolesList =
                    getApplicationMgtService().getAssociatedRolesOfApplication(applicationResourceId, tenantDomain);

            String updatedAllowedAudienceForRoleAssociation =
                    serviceProvider.getAssociatedRolesConfig() == null ? RoleConstants.ORGANIZATION :
                            serviceProvider.getAssociatedRolesConfig().getAllowedAudience();
            List<RoleV2> updatedAssociatedRolesList =
                    serviceProvider.getAssociatedRolesConfig() == null ? Collections.emptyList() :
                            serviceProvider.getAssociatedRolesConfig().getRoles();

            if (CollectionUtils.isEmpty(existingAssociatedRolesList) &&
                    CollectionUtils.isEmpty(updatedAssociatedRolesList)) {
                // No change in roles list.
                return true;
            }

            /*
            if old and new audiences are equals, need to handle the role diff.
             */
            if (existingAllowedAudienceForRoleAssociation.equalsIgnoreCase(updatedAllowedAudienceForRoleAssociation)) {
                switch (updatedAllowedAudienceForRoleAssociation) {
                    case RoleConstants.APPLICATION:
                        List<RoleV2> addedApplicationAudienceRoles = updatedAssociatedRolesList.stream()
                                .filter(updatedRole -> !existingAssociatedRolesList.contains(updatedRole))
                                .collect(Collectors.toList());

                        List<RoleV2> removedApplicationAudienceRoles = existingAssociatedRolesList.stream()
                                .filter(existingRole -> !updatedAssociatedRolesList.contains(existingRole))
                                .collect(Collectors.toList());
                        // Add to threadLocal.
                        IdentityUtil.threadLocalProperties.get()
                                .put(ADDED_APPLICATION_AUDIENCE_ROLES, addedApplicationAudienceRoles);
                        IdentityUtil.threadLocalProperties.get()
                                .put(REMOVED_APPLICATION_AUDIENCE_ROLES, removedApplicationAudienceRoles);
                        return true;
                    default:
                        if (existingAssociatedRolesList.equals(updatedAssociatedRolesList)) {
                            // Nothing to change in shared applications' organizations.
                            return true;
                        }
                        // Get the added roles and removed roles.
                        List<RoleV2> addedOrganizationAudienceRoles = updatedAssociatedRolesList.stream()
                                .filter(updatedRole -> !existingAssociatedRolesList.contains(updatedRole))
                                .collect(Collectors.toList());

                        List<RoleV2> removedOrganizationAudienceRoles = existingAssociatedRolesList.stream()
                                .filter(existingRole -> !updatedAssociatedRolesList.contains(existingRole))
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
                        .put(ADDED_ORGANIZATION_AUDIENCE_ROLES, updatedAssociatedRolesList);
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
                handleAddedOrganizationAudienceRolesOnAppUpdate(addedOrgRolesList, serviceProvider, tenantDomain);

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
        String mainAppOrgId = getOrganizationManager().resolveOrganizationId(tenantDomain);
        List<SharedApplication> sharedApplications =
                getOrgApplicationManager().getSharedApplications(mainAppOrgId, mainAppId);
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
        String mainAppOrgId = getOrganizationManager().resolveOrganizationId(tenantDomain);
        List<SharedApplication> sharedApplications =
                getOrgApplicationManager().getSharedApplications(mainAppOrgId, mainAppId);
        if (CollectionUtils.isEmpty(sharedApplications)) {
            return;
        }

        for (SharedApplication sharedApplication : sharedApplications) {
            CompletableFuture.runAsync(() -> {
                String sharedAppOrgId = sharedApplication.getOrganizationId();
                try {
                    createSharedRolesWithOrgAudience(addedOrgRolesList, tenantDomain, sharedAppOrgId);
                } catch (IdentityRoleManagementException | OrganizationManagementException e) {
                    LOG.error(String.format("Exception occurred while adding shared roles to organization: %s",
                            sharedApplication.getOrganizationId()), e);
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

    private void handleRemovedApplicationAudienceRolesOnAppUpdate(List<RoleV2> removedAppRolesList, String tenantDomain)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(removedAppRolesList)) {
            return;
        }
        /*
        Delete the application audience roles from parent organization. Deleting their shared roles also handled inside.
         */
        for (RoleV2 removedRole : removedAppRolesList) {
            getRoleManagementServiceV2().deleteRole(removedRole.getId(), tenantDomain);
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
        String mainAppOrgId = getOrganizationManager().resolveOrganizationId(tenantDomain);

        List<SharedApplication> sharedApplications =
                getOrgApplicationManager().getSharedApplications(mainAppOrgId, mainAppId);
        if (CollectionUtils.isEmpty(sharedApplications)) {
            return;
        }

        for (RoleV2 parentRole : addedAppRolesList) {
            for (SharedApplication sharedApplication : sharedApplications) {
                String sharedAppOrgId = sharedApplication.getOrganizationId();
                String sharedAppTenantDomain = getOrganizationManager().resolveTenantDomain(sharedAppOrgId);
                String parentAppRoleName = parentRole.getName();
                // Create the role in the shared org.
                RoleBasicInfo subOrgRole =
                        getRoleManagementServiceV2().addRole(parentAppRoleName, Collections.emptyList(),
                                Collections.emptyList(), Collections.emptyList(), RoleConstants.APPLICATION,
                                sharedApplication.getSharedApplicationId(),
                                sharedAppTenantDomain);
                // Add relationship between main role and the shared role.
                getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(parentRole.getId(), subOrgRole.getId(),
                        tenantDomain, sharedAppTenantDomain);
            }
        }
    }

    @Override
    public boolean doPreDeleteApplication(String applicationName, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        try {
            // If the deleting application is an application of tenant(i.e primary org) nothing to do here.
            if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                return true;
            }

            ServiceProvider sharedApplication = getApplicationByName(applicationName, tenantDomain);
            if (sharedApplication == null) {
                return false;
            }
            String sharedAppId = sharedApplication.getApplicationResourceId();
            String sharedAppOrgId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            // Resolve the main application details.
            String mainAppId =
                    getOrgApplicationManager().getMainApplicationIdForGivenSharedApp(sharedAppId, sharedAppOrgId);
            if (mainAppId == null) {
                return false;
            }
            int mainAppTenantId = getApplicationMgtService().getTenantIdByApp(mainAppId);
            String mainAppTenantDomain = IdentityTenantUtil.getTenantDomain(mainAppTenantId);

            String allowedAudienceForRoleAssociationInMainApp =
                    getApplicationMgtService().getAllowedAudienceForRoleAssociation(mainAppId, mainAppTenantDomain);
            boolean hasAppAudiencedRoles =
                    RoleConstants.APPLICATION.equalsIgnoreCase(allowedAudienceForRoleAssociationInMainApp);
            if (hasAppAudiencedRoles) {
                // Handle role deletion in application deletion post actions.
                return true;
            }

            // Handing organization audienced roles associated case.
            List<RoleV2> associatedRolesOfMainApplication = getApplicationMgtService()
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

        String mainApplicationOrgId = getOrganizationManager().resolveOrganizationId(mainApplicationTenantDomain);
        if (mainApplicationOrgId == null) {
            mainApplicationOrgId = SUPER_ORG_ID;
        }
        String sharedAppTenantDomain = getOrganizationManager().resolveTenantDomain(sharedAppOrgId);
        List<String> mainAppRoleIds =
                rolesList.stream().map(RoleV2::getId).collect(Collectors.toList());
        Map<String, String> mainRoleToSharedRoleMappingsInSubOrg = getRoleManagementServiceV2()
                .getMainRoleToSharedRoleMappingsBySubOrg(mainAppRoleIds, sharedAppTenantDomain);

        // Get each role associated applications.
        for (String mainAppRoleId : mainAppRoleIds) {
            List<String> associatedApplicationsIds =
                    getRoleManagementServiceV2().getAssociatedApplicationByRoleId(mainAppRoleId,
                            mainApplicationTenantDomain);
            if (associatedApplicationsIds == null) {
                continue;
            }
            String sharedRoleId = mainRoleToSharedRoleMappingsInSubOrg.get(mainAppRoleId);
            if (StringUtils.isBlank(sharedRoleId)) {
                // There is no role available in the shared org. May be due to role creation issue.
                continue;
            }
            /*
            If the only associated application is the main app in this flow, delete the role in
            the org.
             */
            if (associatedApplicationsIds.size() == 1 && mainApplicationId.equals(associatedApplicationsIds.get(0))) {
                // Delete the role in org.
                getRoleManagementServiceV2().deleteRole(sharedRoleId, sharedAppTenantDomain);
                break;
            } else if (associatedApplicationsIds.size() > 1) {
                boolean isRoleUsedByAnotherSharedApp = false;
                for (String associatedApplicationId : associatedApplicationsIds) {
                    if (associatedApplicationId.equals(mainApplicationId)) {
                        continue;
                    }
                    boolean applicationSharedWithGivenOrganization =
                            getOrgApplicationManager().isApplicationSharedWithGivenOrganization(associatedApplicationId,
                                    mainApplicationOrgId, sharedAppOrgId);
                    if (applicationSharedWithGivenOrganization) {
                        isRoleUsedByAnotherSharedApp = true;
                        break;
                    }
                }
                if (!isRoleUsedByAnotherSharedApp) {
                    // Delete the role in org.
                    getRoleManagementServiceV2().deleteRole(sharedRoleId, sharedAppTenantDomain);
                    break;
                }
            }
        }
    }

    private ServiceProvider getApplicationByName(String name, String tenantDomain)
            throws IdentityApplicationManagementException {

        return getApplicationMgtService().getServiceProvider(name, tenantDomain);
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

    private static RoleManagementService getRoleManagementServiceV2() {

        return OrganizationManagementHandlerDataHolder.getInstance().getRoleManagementServiceV2();
    }
}

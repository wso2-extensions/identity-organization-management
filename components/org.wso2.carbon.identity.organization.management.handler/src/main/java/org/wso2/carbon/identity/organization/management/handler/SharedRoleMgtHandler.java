/*
 * Copyright (c) 2023-2024, WSO2 LLC. (http://www.wso2.com).
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.RoleV2;
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
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.utils.AuditLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION_ROLE_CONFLICT;

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
            default:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unsupported event: " + eventName);
                }
                break;
        }
    }

    private void createSharedRolesOnApplicationSharing(Map<String, Object> eventProperties)
            throws IdentityEventException {

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
            String allowedAudienceForRoleAssociationInMainApp =
                    getApplicationMgtService().getAllowedAudienceForRoleAssociation(parentApplicationId,
                            mainAppTenantDomain);
            switch (allowedAudienceForRoleAssociationInMainApp.toLowerCase()) {
                case RoleConstants.APPLICATION:
                    // Create the roles, and add the relationship.
                    createSharedRolesWithAppAudience(parentApplicationId, mainAppTenantDomain, sharedApplicationId,
                            sharedAppTenantDomain);
                    break;
                default:
                    // Create the role if not exists, and add the relationship.
                    List<RoleV2> associatedRolesOfApplication =
                            getApplicationMgtService().getAssociatedRolesOfApplication(
                                    parentApplicationId, mainAppTenantDomain);
                    createSharedRolesWithOrgAudience(associatedRolesOfApplication, mainAppTenantDomain,
                            sharedOrganizationId);
                    break;
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException |
                 IdentityApplicationManagementException e) {
            throw new IdentityEventException(
                    String.format("Error while sharing roles related to application %s.", sharedApplicationId), e);
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

    private void createSharedRolesWithAppAudience(String mainAppId, String mainAppTenantDomain, String sharedAppId,
                                                  String sharedAppTenantDomain) throws IdentityRoleManagementException {

        // Get parent organization's roles which has application audience.
        String filter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + mainAppId;
        List<RoleBasicInfo> parentOrgRoles =
                getRoleManagementServiceV2().getRoles(filter, null, 0, null, null, mainAppTenantDomain);
        for (RoleBasicInfo parentOrgRole : parentOrgRoles) {
            String parentOrgRoleName = parentOrgRole.getName();
            // Create the role in the shared org.
            RoleBasicInfo subOrgRole = getRoleManagementServiceV2().addRole(parentOrgRoleName, Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), RoleConstants.APPLICATION, sharedAppId,
                    sharedAppTenantDomain);
            // Add relationship between main role and the shared role.
            getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(parentOrgRole.getId(), subOrgRole.getId(),
                    mainAppTenantDomain, sharedAppTenantDomain);
        }
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

                    // Iterate through the list of organizations
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
                            if (LoggerUtils.isEnableV2AuditLogs()) {
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
                            }
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
                        if (LoggerUtils.isEnableV2AuditLogs()) {
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
                        }
                        String errorMessage = String.format(
                                ERROR_CODE_ERROR_SHARING_APPLICATION_ROLE_CONFLICT.getMessage(),
                                sharedOrganizationId, roleV2.getName());
                        throw new IdentityEventException(ERROR_CODE_ERROR_SHARING_APPLICATION_ROLE_CONFLICT.getCode(),
                                errorMessage);
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

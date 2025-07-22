/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.listener;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManagerImpl;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.getAppAssociatedRoleSharingMode;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.setIsAppSharedProperty;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.IS_APP_SHARED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;

/**
 * This class contains the implementation of the handler for post organization creation.
 * This handler will be used to add shared applications to newly created organizations.
 */
public class OrganizationCreationHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationCreationHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        String organizationId = (String) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION_ID);

        if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
            Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            try {
                addSharedApplicationsToOrganization(organization);
            } catch (IdentityApplicationManagementException | OrganizationManagementException |
                     ResourceSharingPolicyMgtException e) {
                throw new IdentityEventException("An error occurred while creating shared applications in the new " +
                        "organization", e);
            }
        }

        if (Constants.EVENT_PRE_DELETE_ORGANIZATION.equals(eventName)) {
            try {
                handleMainApplicationUpdateForPreDeleteOrganization(organizationId);
            } catch (IdentityApplicationManagementException | OrganizationManagementException e) {
                throw new IdentityEventException("An error occurred while retrieving main applications of " +
                        "fragment applications configured for organization with ID: " + organizationId, e);
            }
        }

        if (Constants.EVENT_POST_DELETE_ORGANIZATION.equals(eventName)) {
            try {
                handleSharedAppDeletionForPostDeleteOrganization(organizationId);
                handleMainApplicationUpdateForPostDeleteOrganization();
            } catch (OrganizationManagementException | IdentityApplicationManagementException e) {
                throw new IdentityEventException("An error occurred while updating main application based " +
                        "on the organizations that it is shared with during an organization deletion.", e);
            }

        }
    }

    private void addSharedApplicationsToOrganization(Organization organization)
            throws IdentityApplicationManagementException, OrganizationManagementException,
            ResourceSharingPolicyMgtException {

        // App sharing is skipped if the organization is a primary organization.
        String orgId = organization.getId();
        if (getOrganizationManager().isPrimaryOrganization(orgId)) {
            return;
        }
        String parentOrgId = organization.getParent().getId();
        if (parentOrgId == null) {
            parentOrgId = SUPER_ORG_ID;
        }
        String parentOrgHandle = getOrganizationManager().resolveTenantDomain(parentOrgId);
        List<String> ancestorOrganizationIds = getOrganizationManager().getAncestorOrganizationIds(parentOrgId);
        List<ResourceSharingPolicy> resourceSharingPolicies = getResourceSharingPolicyHandlerService()
                .getResourceSharingPoliciesByResourceType(ancestorOrganizationIds, ResourceType.APPLICATION.name());

        List<String> alreadyHandledSharedAppIds = new ArrayList<>();
        for (ResourceSharingPolicy resourceSharingPolicy : resourceSharingPolicies) {
            PolicyEnum sharingPolicy = resourceSharingPolicy.getSharingPolicy();
            if (!isValidApplicationSharePolicy(sharingPolicy)) {
                continue;
            }
            String mainOrganizationId = resourceSharingPolicy.getInitiatingOrgId();
            String mainTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
            String mainApplicationId = resourceSharingPolicy.getResourceId();
            boolean isMainOrganization = parentOrgId.equals(mainOrganizationId);
            ServiceProvider sharedApplication = resolveSharedApplication(orgId, parentOrgId, parentOrgHandle,
                    mainOrganizationId, mainApplicationId);
            if (sharedApplication == null) {
                continue;
            }
            ApplicationShareRolePolicy.Mode roleSharingMode = getAppAssociatedRoleSharingMode(sharedApplication);
            ApplicationShareRolePolicy.Builder roleSharingConfigBuilder = new ApplicationShareRolePolicy.Builder()
                    .mode(roleSharingMode);
            if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() == roleSharingMode.ordinal()) {
                List<RoleWithAudienceDO> roleWithAudienceDOs = new ArrayList<>();
                List<SharedResourceAttribute> sharedResourceAttributes =
                        getResourceSharingPolicyHandlerService().getSharedResourceAttributesBySharingPolicyId(
                                resourceSharingPolicy.getResourceSharingPolicyId());

                for (SharedResourceAttribute sharedResourceAttribute : sharedResourceAttributes) {
                    if (SharedAttributeType.ROLE.ordinal() != sharedResourceAttribute.getSharedAttributeType()
                            .ordinal()) {
                        continue;
                    }
                    String mainRoleId = sharedResourceAttribute.getSharedAttributeId();
                    RoleBasicInfo mainRoleBasicInfo;
                    try {
                        mainRoleBasicInfo = getRoleManagementServiceV2().getRoleBasicInfoById(mainRoleId,
                                mainTenantDomain);
                        RoleWithAudienceDO.AudienceType audienceType =
                                RoleWithAudienceDO.AudienceType.fromValue(
                                        mainRoleBasicInfo.getAudience());
                        RoleWithAudienceDO roleWithAudienceDO =
                                new RoleWithAudienceDO(mainRoleBasicInfo.getName(),
                                        mainRoleBasicInfo.getAudienceName(), audienceType);
                        roleWithAudienceDOs.add(roleWithAudienceDO);
                    } catch (IdentityRoleManagementException e) {
                        LOG.error("Failed to retrieve the role with ID: " + mainRoleId + " in tenant domain: " +
                                mainTenantDomain + ". Skipping sharing of this role for application: " +
                                mainApplicationId + " to organization: " + orgId + ".", e);
                    }
                }
                roleSharingConfigBuilder = roleSharingConfigBuilder.roleWithAudienceDOList(roleWithAudienceDOs);
            }
            ServiceProvider mainApplication;
            if (isMainOrganization) {
                mainApplication = sharedApplication;
            } else {
                mainApplication = getApplicationManagementService().getApplicationByResourceId(
                        mainApplicationId, mainTenantDomain);
            }
            getOrgApplicationManager().shareApplicationWithPolicy(mainOrganizationId, mainApplication, orgId,
                    PolicyEnum.SELECTED_ORG_ONLY, roleSharingConfigBuilder.build(), null);
            alreadyHandledSharedAppIds.add(mainApplicationId);

            if (isMainOrganization) {
                boolean isAppShared = isAppShared(mainApplication);
                if (!isAppShared) {
                    // Update the `isAppShared` property of the main application to true.
                    updateApplicationWithIsAppSharedProperty(true, mainApplication);
                }
            }
        }

        // NOTE: The below code is to handle the backward compatibility of the applications that are shared with
        // all children organizations using the `shareWithAllChildren` property.
        String primaryOrganizationId = getOrganizationManager().getPrimaryOrganizationId(organization.getId());
        if (primaryOrganizationId == null) {
            primaryOrganizationId = SUPER_ORG_ID;
        }
        ApplicationBasicInfo[] applicationBasicInfos;
        applicationBasicInfos = getApplicationManagementService().getApplicationBasicInfoBySPProperty(
                getOrganizationManager().resolveTenantDomain(primaryOrganizationId), getAuthenticatedUsername(),
                SHARE_WITH_ALL_CHILDREN, "true");

        for (ApplicationBasicInfo applicationBasicInfo : applicationBasicInfos) {
            String mainApplicationId = applicationBasicInfo.getUuid();
            if (alreadyHandledSharedAppIds.contains(mainApplicationId)) {
                continue;
            }
            ServiceProvider mainApplication;
            mainApplication = getApplicationManagementService()
                    .getServiceProvider(applicationBasicInfo.getApplicationId());
            if (mainApplication != null) {
                // Check whether the application is shared with the parent organization.
                ServiceProvider sharedApplication = resolveSharedApplication(orgId, parentOrgId, parentOrgHandle,
                        primaryOrganizationId, mainApplicationId);
                if (sharedApplication == null) {
                    continue;
                }
                ApplicationShareRolePolicy roleSharingConfigBuilder = new ApplicationShareRolePolicy.Builder()
                        .mode(ApplicationShareRolePolicy.Mode.ALL).build();

                // Share the application to the newly created organization.
                getOrgApplicationManager().shareApplicationWithPolicy(primaryOrganizationId, mainApplication, orgId,
                        PolicyEnum.SELECTED_ORG_ONLY, roleSharingConfigBuilder, null);

                // Add the resource sharing policy for the main application.
                String ownerTenantDomain = getOrganizationManager().resolveTenantDomain(primaryOrganizationId);
                getOrgApplicationManager().addOrUpdatePolicy(mainApplication.getApplicationResourceId(),
                        primaryOrganizationId, primaryOrganizationId, ownerTenantDomain,
                        PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS, roleSharingConfigBuilder);

                // Check whether the application is shared with any child organization using `isAppShared` property.
                boolean isAppShared = isAppShared(mainApplication);
                if (!isAppShared) {
                    // Update the `isAppShared` property of the main application to true if it hasn't been shared
                    // previously.
                    updateApplicationWithIsAppSharedProperty(true, mainApplication);
                }
            }
        }
    }

    private ServiceProvider resolveSharedApplication(String orgId, String parentOrgId, String parentOrgHandle,
                                                     String mainOrganizationId, String mainApplicationId)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        String sharedAppId;
        boolean isMainOrganization = parentOrgId.equals(mainOrganizationId);

        if (isMainOrganization) {
            // The original application is available in the parent organization. Not a fragment application.
            sharedAppId = mainApplicationId;
        } else {
            Optional<String> sharedAppIdOptional = resolveSharedApp(mainApplicationId, mainOrganizationId, parentOrgId);
            if (!sharedAppIdOptional.isPresent()) {
                LOG.error("No shared application found in the parent organization for organization: " + orgId +
                        ". Skipping sharing of the main application with ID: " + mainApplicationId);
                return null;
            }
            sharedAppId = sharedAppIdOptional.get();
        }
        ServiceProvider sharedApplication = getApplicationManagementService().getApplicationByResourceId(
                sharedAppId, parentOrgHandle);
        if (sharedApplication == null) {
            LOG.error("No shared application found in the parent organization for organization: " + orgId +
                    ". Skipping sharing of the main application with ID: " + mainApplicationId);
            return null;
        }
        return sharedApplication;
    }

    private boolean isValidApplicationSharePolicy(PolicyEnum policyEnum) {

        return PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN.equals(policyEnum) ||
               PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS.equals(policyEnum);
    }

    private void handleMainApplicationUpdateForPreDeleteOrganization(String organizationId)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        OrgApplicationManagerUtil.clearB2BApplicationIds();

        String tenantDomain = getOrganizationManager().resolveTenantDomain(organizationId);
        if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
            return;
        }
        List<String> mainAppIds = new ArrayList<>();
        try {
            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(username);
            ApplicationBasicInfo[] applicationBasicInfos = getApplicationManagementService()
                    .getAllApplicationBasicInfo(tenantDomain, getAuthenticatedUsername());
            for (ApplicationBasicInfo applicationBasicInfo : applicationBasicInfos) {
                ServiceProvider orgApplication = getApplicationManagementService().getServiceProvider(
                        applicationBasicInfo.getApplicationId());
                boolean isFragmentApp = Arrays.stream(orgApplication.getSpProperties())
                        .anyMatch(property -> IS_FRAGMENT_APP.equals(property.getName()) &&
                                Boolean.parseBoolean(property.getValue()));
                /*
                 Only adding the main app IDs of the fragment applications since there can be applications which
                 are directly created in the sub organization level.
                */
                if (isFragmentApp) {
                    String mainAppId = getApplicationManagementService()
                            .getMainAppId(orgApplication.getApplicationResourceId());
                    mainAppIds.add(mainAppId);
                }
            }
            OrgApplicationManagerUtil.setB2BApplicationIds(mainAppIds);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private void handleMainApplicationUpdateForPostDeleteOrganization() throws IdentityApplicationManagementException,
            OrganizationManagementException {

        List<String> mainAppIds = OrgApplicationManagerUtil.getB2BApplicationIds();
        if (CollectionUtils.isEmpty(mainAppIds)) {
            return;
        }
        try {
            // All the applications have the same tenant ID. Therefore, tenant ID of the first application is used.
            int rootTenantId = getApplicationManagementService().getTenantIdByApp(mainAppIds.get(0));
            String rootTenantDomain = IdentityTenantUtil.getTenantDomain(rootTenantId);
            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(rootTenantDomain, true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(username);
            for (String mainAppId : mainAppIds) {
                boolean hasSharedApps = getOrgApplicationManager().hasSharedApps(mainAppId);
                // Since the application doesn't have any shared applications, isAppShared service provider property
                // should be set to false.
                if (!hasSharedApps) {
                    ServiceProvider mainApplication = getApplicationManagementService()
                            .getApplicationByResourceId(mainAppId, rootTenantDomain);
                    updateApplicationWithIsAppSharedProperty(false, mainApplication);
                }
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
            OrgApplicationManagerUtil.clearB2BApplicationIds();
        }
    }

    /**
     * Handle shared application deletion for post delete organization.
     *
     * @param organizationId ID of the organization.
     */
    private void handleSharedAppDeletionForPostDeleteOrganization(String organizationId)
            throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            return;
        }
        getOrgApplicationMgtDAO().deleteSharedAppLinks(organizationId);
    }

    private void updateApplicationWithIsAppSharedProperty(boolean isAppShared, ServiceProvider mainApplication)
            throws IdentityApplicationManagementException {

        setIsAppSharedProperty(mainApplication, isAppShared);
        boolean systemApplication = OrgApplicationManagerUtil.isSystemApplication(mainApplication.getApplicationName());
        try {
            if (systemApplication) {
                IdentityApplicationManagementUtil.setAllowUpdateSystemApplicationThreadLocal(true);
            }
            getApplicationManagementService().updateApplication(mainApplication,
                    mainApplication.getTenantDomain(), getAuthenticatedUsername());
        } finally {
            if (systemApplication) {
                IdentityApplicationManagementUtil.removeAllowUpdateSystemApplicationThreadLocal();
            }
        }
    }

    private Optional<String> resolveSharedApp(String mainAppId, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException {

        return getOrgApplicationMgtDAO().getSharedApplicationResourceId(mainAppId, ownerOrgId, sharedOrgId);
    }

    /**
     * Return the value of the `isAppShared` property of the main application.
     *
     * @param mainApplication The main application service provider object.
     * @return True if the `isAppShared` property of the main application is set as true.
     */
    private boolean isAppShared(ServiceProvider mainApplication) {

        return Arrays.stream(mainApplication.getSpProperties())
                .anyMatch(serviceProviderProperty -> IS_APP_SHARED.equalsIgnoreCase(serviceProviderProperty.getName())
                        && Boolean.parseBoolean(serviceProviderProperty.getValue()));
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
    }

    private OrgApplicationManager getOrgApplicationManager() {

        return new OrgApplicationManagerImpl();
    }

    private OrgApplicationMgtDAO getOrgApplicationMgtDAO() {

        return OrgApplicationMgtDataHolder.getInstance().getOrgApplicationMgtDAO();
    }

    private OrganizationManager getOrganizationManager() {

        return OrgApplicationMgtDataHolder.getInstance().getOrganizationManager();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrgApplicationMgtDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private static RoleManagementService getRoleManagementServiceV2() {

        return OrgApplicationMgtDataHolder.getInstance().getRoleManagementServiceV2();
    }

}

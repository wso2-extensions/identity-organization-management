/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
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
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ROLE_SHARING_MODE;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
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

        String parentOrgId = organization.getParent().getId();
        if (parentOrgId == null) {
            parentOrgId = SUPER_ORG_ID;
        }
        List<String> alreadyHandledSharedAppIds = new ArrayList<>();

        // Get applications from the parent organization that are avaialble on policy table
        // In Each service provider,
        // Get the roleMode
        // Get parent org shared Roles, if the mode is not none (Which is available on the attribute table, it has
        // the main role IDs, so get associations with the parent org)
        // Create the RoleSharing Object.
        // Invoke application sharing with policy
        List<String> parentOrgList = new ArrayList<>();
        parentOrgList.add(parentOrgId);
        List<ResourceSharingPolicy> resourceSharingPolicies = getResourceSharingPolicyHandlerService()
                .getResourceSharingPolicies(parentOrgList);

        for (ResourceSharingPolicy resourceSharingPolicy : resourceSharingPolicies) {
            if (ResourceType.APPLICATION.ordinal() != resourceSharingPolicy.getResourceType().ordinal()) {
                // Only need to handle application sharing policies.
                continue;
            }
            String mainOrganizationId = resourceSharingPolicy.getInitiatingOrgId();
            String mainTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
            String mainApplicationId = resourceSharingPolicy.getResourceId();
            String sharedAppId;
            if (mainOrganizationId.equals(parentOrgId)) {
                // That mean original application is available in the parent org. Not a fragment application.
                sharedAppId = mainApplicationId;
            } else {
                Optional<String> sharedAppIdOptional = resolveSharedApp(mainApplicationId, mainOrganizationId,
                        parentOrgId);
                if (!sharedAppIdOptional.isPresent()) {
                    // No shared application found for the main application.
                    // TODO: Should we throw an error here and fail org creation?
                    continue;
                }
                sharedAppId = sharedAppIdOptional.get();
            }
            String sharedOrgId = resourceSharingPolicy.getPolicyHoldingOrgId();
            String sharedOrgName = getOrganizationManager().resolveTenantDomain(sharedOrgId);
            ServiceProvider sharedApplication = getApplicationManagementService().getApplicationByResourceId(
                    sharedAppId, sharedOrgName);
            PolicyEnum sharingPolicy = resourceSharingPolicy.getSharingPolicy();
            boolean validApplicationSharePolicy = isValidApplicationSharePolicy(sharingPolicy);
            if (!validApplicationSharePolicy) {
                // TODO: Add notification
                // Only handle selected organization with all existing and future children and all existing and
                // future organizations.
                LOG.warn("Invalid application sharing policy: " + sharingPolicy + " for application: " +
                        mainApplicationId + " in organization: " + sharedOrgName);
                continue;
            }
            List<RoleWithAudienceDO> roleWithAudienceDOs = new ArrayList<>();
            List<SharedResourceAttribute> sharedResourceAttributes =
                    getResourceSharingPolicyHandlerService().getSharedResourceAttributesBySharingPolicyId(
                            resourceSharingPolicy.getResourceSharingPolicyId());

            ApplicationShareRolePolicy.Mode roleSharingMode = ApplicationShareRolePolicy.Mode.ALL;
            for (ServiceProviderProperty spProperty : sharedApplication.getSpProperties()) {
                if (ROLE_SHARING_MODE.equalsIgnoreCase(spProperty.getName())) {
                    String roleSharingModeString = spProperty.getValue();
                    roleSharingMode = ApplicationShareRolePolicy.Mode.valueOf(roleSharingModeString);
                    break;
                }
            }
            if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() == roleSharingMode.ordinal()) {

                // That mean the original application is available in the parent org. Not a fragment application.
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
                        // TODO: Add notification
                        LOG.error("Failed to get the role with ID: " + mainRoleId + " in tenant domain: " +
                                mainTenantDomain + ". Hence skipping application sharing.", e);
                    }
                }
            }

            ApplicationShareRolePolicy.Builder roleSharingConfigBuilder = new ApplicationShareRolePolicy.Builder()
                    .mode(roleSharingMode);
            if (roleSharingMode == ApplicationShareRolePolicy.Mode.SELECTED) {
                roleSharingConfigBuilder = roleSharingConfigBuilder.roleWithAudienceDOList(roleWithAudienceDOs);
            }
            ServiceProvider mainApplicationFromSharedApp;
            boolean isMainOrganization = false;
            if (sharedOrgId != null) {
                byte[] sharedOrgIdBytes = sharedOrgId.getBytes(StandardCharsets.UTF_8);
                byte[] mainOrganizationIdBytes = mainOrganizationId.getBytes(StandardCharsets.UTF_8);
                isMainOrganization = MessageDigest.isEqual(sharedOrgIdBytes, mainOrganizationIdBytes);
            }
            if (isMainOrganization) {
                // This happen only with future sharing policies. When new organization is added as immediate
                // children of the main organization.
                mainApplicationFromSharedApp = sharedApplication;
            } else {
                mainApplicationFromSharedApp = getMainApplicationFromSharedApp(sharedAppId, sharedOrgId,
                        mainTenantDomain);
            }
            getOrgApplicationManager().shareApplicationWithPolicy(mainOrganizationId, mainApplicationFromSharedApp,
                    organization.getId(), sharingPolicy, roleSharingConfigBuilder.build(), null);
            alreadyHandledSharedAppIds.add(sharedAppId);

            if (isMainOrganization) {
                boolean isAppShared = isAppShared(mainApplicationFromSharedApp);
                if (!isAppShared) {
                    // Update the `isAppShared` property of the main application to true if it hasn't been shared
                    // previously.
                    updateApplicationWithIsAppSharedProperty(true, mainApplicationFromSharedApp);
                }
            }
        }

        // NOTE: The below code is to handle the backward compatibility of the applications that are shared with
        // all children organizations using the `shareWithAllChildren` property.

        ApplicationBasicInfo[] applicationBasicInfos;
        applicationBasicInfos = getApplicationManagementService().getAllApplicationBasicInfo(
                getOrganizationManager().resolveTenantDomain(parentOrgId), getAuthenticatedUsername());

        for (ApplicationBasicInfo applicationBasicInfo : applicationBasicInfos) {
            if (alreadyHandledSharedAppIds.contains(applicationBasicInfo.getApplicationResourceId())) {
                // Skip the applications that are already handled by the resource sharing policy.
                continue;
            }
            if (getOrgApplicationMgtDAO().isFragmentApplication(applicationBasicInfo.getApplicationId())) {
                Optional<SharedApplicationDO> sharedApplicationDO;
                sharedApplicationDO = getOrgApplicationMgtDAO().getSharedApplication(
                        applicationBasicInfo.getApplicationId(), parentOrgId);

                if (sharedApplicationDO.isPresent() && sharedApplicationDO.get().shareWithAllChildren()) {
                    Optional<MainApplicationDO> mainApplicationDO;
                    mainApplicationDO = getOrgApplicationMgtDAO().getMainApplication(
                            sharedApplicationDO.get().getFragmentApplicationId(),
                            sharedApplicationDO.get().getOrganizationId());
                    if (mainApplicationDO.isPresent()) {
                        String tenantDomain = getOrganizationManager().resolveTenantDomain(
                                mainApplicationDO.get().getOrganizationId());
                        ServiceProvider mainApplication = getApplicationManagementService()
                                .getApplicationByResourceId(mainApplicationDO.get().getMainApplicationId(),
                                        tenantDomain);
                        String ownerOrgIdOfMainApplication = mainApplicationDO.get().getOrganizationId();
                        getOrgApplicationManager().shareApplicationWithPolicy(ownerOrgIdOfMainApplication,
                                mainApplication, organization.getId(),
                                PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN,
                                new ApplicationShareRolePolicy.Builder()
                                        .mode(ApplicationShareRolePolicy.Mode.ALL).build(), null);
                    }
                }
            } else {
                ServiceProvider mainApplication;
                mainApplication = getApplicationManagementService().getServiceProvider(
                        applicationBasicInfo.getApplicationId());
                if (mainApplication != null && Arrays.stream(mainApplication.getSpProperties())
                        .anyMatch(serviceProviderProperty -> SHARE_WITH_ALL_CHILDREN.equalsIgnoreCase(
                                serviceProviderProperty.getName()) && Boolean.parseBoolean(
                                        serviceProviderProperty.getValue()))) {
                    getOrgApplicationManager().shareApplicationWithPolicy(parentOrgId,
                            mainApplication, organization.getId(),
                            PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN,
                            new ApplicationShareRolePolicy.Builder()
                                    .mode(ApplicationShareRolePolicy.Mode.ALL).build(), null);
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
    }

    private boolean isValidApplicationSharePolicy(PolicyEnum policyEnum) {
        // Use constants to avoid direct ordinal comparison which can trigger security warnings.
        return PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN.equals(policyEnum) ||
               PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS.equals(policyEnum);
        // As of now these are the only two FUTURE sharing policies supported for application sharing.
    }

    private ServiceProvider getMainApplicationFromSharedApp(String sharedAppId, String sharedOrgId, String mainOrgName)
            throws OrganizationManagementException, IdentityApplicationManagementException {

        Optional<MainApplicationDO> mainApplicationDO = getOrgApplicationMgtDAO().getMainApplication(sharedAppId,
                sharedOrgId);
        if (!mainApplicationDO.isPresent()) {
            throw new OrganizationManagementException("Main application not found for the shared application: " +
                    sharedAppId);
        }
        String mainAppId = mainApplicationDO.get().getMainApplicationId();
        return getApplicationManagementService().getApplicationByResourceId(mainAppId, mainOrgName);
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

/*
 * Copyright (c) 2022-2023, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.AssociatedRolesConfig;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.common.model.script.AuthenticationScriptConfig;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_FRAGMENT_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_SHARE_FOR_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.setShareWithAllChildrenProperty;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_SUB_ORG_CANNOT_CREATE_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isB2BApplicationRoleSupportEnabled;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isSubOrganization;

/**
 * Application listener to restrict actions on shared applications and fragment applications.
 */
public class FragmentApplicationMgtListener extends AbstractApplicationMgtListener {

    private static final Log LOG = LogFactory.getLog(FragmentApplicationMgtListener.class);

    @Override
    public int getDefaultOrderId() {

        return 50;
    }

    @Override
    public boolean isEnable() {

        IdentityEventListenerConfig identityEventListenerConfig = IdentityUtil.readEventListenerProperty
                (ApplicationMgtListener.class.getName(), this.getClass().getName());

        if (identityEventListenerConfig == null) {
            return false;
        }

        if (StringUtils.isNotBlank(identityEventListenerConfig.getEnable())) {
            return Boolean.parseBoolean(identityEventListenerConfig.getEnable());
        }
        return false;

    }

    @Override
    public boolean doPreCreateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        try {
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            int organizationDepthInHierarchy =
                    getOrganizationManager().getOrganizationDepthInHierarchy(organizationId);
            if (isSubOrganization(organizationDepthInHierarchy) &&
                    !isSharedAppFromInternalProcess(serviceProvider, tenantDomain)) {
                throw new IdentityApplicationManagementClientException(
                        ERROR_CODE_SUB_ORG_CANNOT_CREATE_APP.getCode(),
                        ERROR_CODE_SUB_ORG_CANNOT_CREATE_APP.getMessage());
            }
        } catch (OrganizationManagementClientException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Organization not found for the tenant: " + tenantDomain);
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityApplicationManagementException(
                    "An error occurred while getting depth of the organization", e);
        }
        return true;
    }

    @Override
    public boolean doPreUpdateApplication(ServiceProvider serviceProvider, String tenantDomain,
                                          String userName) throws IdentityApplicationManagementException {

        /* If the application is a fragment application, only certain configurations are allowed to be updated since
        the organization login authenticator needs some configurations unchanged. Hence, the listener will override
        any configs changes that are required for organization login. */
        ServiceProvider existingApplication =
                getApplicationByResourceId(serviceProvider.getApplicationResourceId(), tenantDomain);
        if (existingApplication != null && Arrays.stream(existingApplication.getSpProperties())
                .anyMatch(p -> IS_FRAGMENT_APP.equalsIgnoreCase(p.getName()) && Boolean.parseBoolean(p.getValue()))) {
            serviceProvider.setSpProperties(existingApplication.getSpProperties());
            serviceProvider.setInboundAuthenticationConfig(existingApplication.getInboundAuthenticationConfig());
            LocalAndOutboundAuthenticationConfig localAndOutBoundAuthenticationConfig =
                    serviceProvider.getLocalAndOutBoundAuthenticationConfig();
            if (localAndOutBoundAuthenticationConfig != null &&
                    localAndOutBoundAuthenticationConfig.getAuthenticationScriptConfig() != null) {
                AuthenticationScriptConfig authenticationScriptConfig =
                        localAndOutBoundAuthenticationConfig.getAuthenticationScriptConfig();
                if (authenticationScriptConfig.isEnabled() &&
                        !StringUtils.isBlank(authenticationScriptConfig.getContent())) {
                    throw new IdentityApplicationManagementClientException(
                            "Authentication script configuration not allowed for shared applications.");
                }
            }
        }

        // Updating the shareWithAllChildren flag of application is blocked.
        if (existingApplication != null
                && !IdentityUtil.threadLocalProperties.get().containsKey(UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN)) {
            Optional<ServiceProviderProperty> shareWithAllChildren =
                    Arrays.stream(existingApplication.getSpProperties())
                            .filter(p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()))
                            .findFirst();
            shareWithAllChildren.ifPresent(serviceProviderProperty ->
                    setShareWithAllChildrenProperty(serviceProvider,
                            Boolean.parseBoolean(serviceProviderProperty.getValue())));
        }

        return super.doPreUpdateApplication(serviceProvider, tenantDomain, userName);
    }

    @Override
    public boolean doPostGetServiceProvider(ServiceProvider serviceProvider, String applicationName,
                                            String tenantDomain) throws IdentityApplicationManagementException {

        // If the application is a shared application, updates to the application are allowed
        if (serviceProvider != null && Arrays.stream(serviceProvider.getSpProperties())
                .anyMatch(p -> IS_FRAGMENT_APP.equalsIgnoreCase(p.getName()) && Boolean.parseBoolean(p.getValue()))) {
            Optional<MainApplicationDO> mainApplicationDO;
            try {
                String sharedOrgId = getOrganizationManager().resolveOrganizationId(tenantDomain);
                mainApplicationDO = getOrgApplicationMgtDAO()
                        .getMainApplication(serviceProvider.getApplicationResourceId(), sharedOrgId);
                // Add the skip logout consent property to true for the shared application
                serviceProvider.getLocalAndOutBoundAuthenticationConfig().setSkipLogoutConsent(true);
                if (mainApplicationDO.isPresent()) {
                    /* Add User Attribute Section related configurations from the
                    main application to the shared application
                     */
                    String mainApplicationTenantDomain = getOrganizationManager()
                            .resolveTenantDomain(mainApplicationDO.get().getOrganizationId());
                    ServiceProvider mainApplication = getApplicationByResourceId
                            (mainApplicationDO.get().getMainApplicationId(), mainApplicationTenantDomain);
                    ClaimMapping[] filteredClaimMappings =
                            Arrays.stream(mainApplication.getClaimConfig().getClaimMappings())
                                    .filter(claim -> !claim.getLocalClaim().getClaimUri()
                                            .startsWith("http://wso2.org/claims/runtime/"))
                                    .toArray(ClaimMapping[]::new);
                    if (isB2BApplicationRoleSupportEnabled()) {
                        // Add application roles to the filtered claim mappings (if any
                        filteredClaimMappings = addApplicationRolesToFilteredClaimMappings(filteredClaimMappings);
                    }
                    ClaimConfig claimConfig = new ClaimConfig();
                    claimConfig.setClaimMappings(filteredClaimMappings);
                    claimConfig.setAlwaysSendMappedLocalSubjectId(
                            mainApplication.getClaimConfig().isAlwaysSendMappedLocalSubjectId());
                    serviceProvider.setClaimConfig(claimConfig);
                    if (serviceProvider.getLocalAndOutBoundAuthenticationConfig() != null
                            && mainApplication.getLocalAndOutBoundAuthenticationConfig() != null) {
                        serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                                .setUseTenantDomainInLocalSubjectIdentifier(mainApplication
                                        .getLocalAndOutBoundAuthenticationConfig()
                                        .isUseTenantDomainInLocalSubjectIdentifier());
                        serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                                .setUseUserstoreDomainInLocalSubjectIdentifier(mainApplication
                                        .getLocalAndOutBoundAuthenticationConfig()
                                        .isUseUserstoreDomainInLocalSubjectIdentifier());
                        serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                                .setUseUserstoreDomainInRoles(mainApplication
                                        .getLocalAndOutBoundAuthenticationConfig().isUseUserstoreDomainInRoles());
                    }

                    // Set application's associated roles.
                    AssociatedRolesConfig associatedRolesConfigOfMainApp = mainApplication.getAssociatedRolesConfig();
                    if (associatedRolesConfigOfMainApp != null) {
                        AssociatedRolesConfig associatedRolesConfigForSharedApp =
                                getAssociatedRolesConfigForSharedApp(associatedRolesConfigOfMainApp);
                        serviceProvider.setAssociatedRolesConfig(associatedRolesConfigForSharedApp);
                    }
                }
            } catch (OrganizationManagementException e) {
                throw new IdentityApplicationManagementException
                        ("Error while retrieving the fragment application details.", e);
            }
        }
        return super.doPostGetServiceProvider(serviceProvider, applicationName, tenantDomain);
    }

    private AssociatedRolesConfig getAssociatedRolesConfigForSharedApp(
            AssociatedRolesConfig associatedRolesConfigOfMainApp) {

        String allowedAudience = associatedRolesConfigOfMainApp.getAllowedAudience();
        List<RoleV2> mainAppRoles = associatedRolesConfigOfMainApp.getRoles();
        AssociatedRolesConfig associatedRolesConfigForSharedApp = new AssociatedRolesConfig();
        associatedRolesConfigForSharedApp.setAllowedAudience(allowedAudience);
        // TODO resolve to shared role id reference and re build.
        List<RoleV2> associatedRolesOfSharedApp = mainAppRoles;
        associatedRolesConfigForSharedApp.setRoles(associatedRolesOfSharedApp);
        return associatedRolesConfigForSharedApp;
    }

    @Override
    public boolean doPreDeleteApplication(String applicationName, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        ServiceProvider application = getApplicationByName(applicationName, tenantDomain);
        if (application == null) {
            return false;
        }

        // If the application is a fragment application and the main application is shared with all its descendants,
        // the application can be deleted only if the main application is being deleted or if main application delete
        // sharing with all organizations.
        if (Arrays.stream(application.getSpProperties())
                .anyMatch(p -> IS_FRAGMENT_APP.equalsIgnoreCase(p.getName()) && Boolean.parseBoolean(p.getValue()))) {
            Optional<SharedApplicationDO> sharedApplicationDO;
            try {
                sharedApplicationDO = getOrgApplicationMgtDAO().getSharedApplication(application.getApplicationID(),
                        tenantDomain);
                if (sharedApplicationDO.isPresent()) {
                    if (IdentityUtil.threadLocalProperties.get().containsKey(DELETE_MAIN_APPLICATION) ||
                        IdentityUtil.threadLocalProperties.get().containsKey(DELETE_SHARE_FOR_MAIN_APPLICATION) ||
                        (!sharedApplicationDO.get().shareWithAllChildren() &&
                                IdentityUtil.threadLocalProperties.get().containsKey(DELETE_FRAGMENT_APPLICATION))) {
                        return true;
                    }
                    return false;
                }
            } catch (OrganizationManagementException e) {
                throw new IdentityApplicationManagementException(
                        format("Unable to delete fragment application with resource id: %s ",
                                application.getApplicationResourceId()));
            }
        }
        try {
            // If an application has fragment applications, delete all its fragment applications.
            if (getOrgApplicationMgtDAO().hasFragments(application.getApplicationResourceId())) {
                String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
                if (organizationId == null) {
                    organizationId = SUPER_ORG_ID;
                }

                List<SharedApplicationDO> sharedApplications = getOrgApplicationMgtDAO().getSharedApplications(
                        organizationId, application.getApplicationResourceId());
                IdentityUtil.threadLocalProperties.get().put(DELETE_MAIN_APPLICATION, true);
                String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                for (SharedApplicationDO sharedApplicationDO: sharedApplications) {
                    getApplicationMgtService().deleteApplication(application.getApplicationName(),
                            sharedApplicationDO.getOrganizationId(), username);
                }
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityApplicationManagementException("Error in validating the application for deletion.", e);
        } finally {
            IdentityUtil.threadLocalProperties.get().remove(DELETE_MAIN_APPLICATION);
        }

        return super.doPreDeleteApplication(applicationName, tenantDomain, userName);
    }

    private ServiceProvider getApplicationByResourceId(String applicationResourceId, String tenantDomain)
            throws IdentityApplicationManagementException {

        return getApplicationMgtService().getApplicationByResourceId(applicationResourceId, tenantDomain);
    }

    private ServiceProvider getApplicationByName(String name, String tenantDomain)
            throws IdentityApplicationManagementException {

        return getApplicationMgtService().getServiceProvider(name, tenantDomain);
    }

    private ApplicationManagementService getApplicationMgtService() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
    }

    private OrgApplicationMgtDAO getOrgApplicationMgtDAO() {

        return OrgApplicationMgtDataHolder.getInstance().getOrgApplicationMgtDAO();
    }

    private OrganizationManager getOrganizationManager() {

        return OrgApplicationMgtDataHolder.getInstance().getOrganizationManager();
    }

    /**
     * Add application roles claim mapping to the filtered claim mappings.
     *
     * @param filteredClaimMappings ClaimMappings array be used to add application roles claim mapping.
     * @return ClaimMappings array with application roles claim mapping.
     */
    private ClaimMapping[] addApplicationRolesToFilteredClaimMappings(ClaimMapping[] filteredClaimMappings) {

        if (filteredClaimMappings == null) {
            return filteredClaimMappings;
        }
        for (ClaimMapping claimMapping : filteredClaimMappings) {
            if (OrgApplicationMgtConstants.APP_ROLES_CLAIM_URI.equals(claimMapping.getLocalClaim().getClaimUri())) {
                return filteredClaimMappings;  // Return original array if the claim already exists
            }
        }
        ClaimMapping appRoleClaimMapping = new ClaimMapping();
        Claim localAppRoleClaim = new Claim();
        localAppRoleClaim.setClaimUri(OrgApplicationMgtConstants.APP_ROLES_CLAIM_URI);
        Claim fedAppRoleClaim = new Claim();
        fedAppRoleClaim.setClaimUri(OrgApplicationMgtConstants.APP_ROLES_CLAIM_URI);
        appRoleClaimMapping.setLocalClaim(localAppRoleClaim);
        appRoleClaimMapping.setRemoteClaim(fedAppRoleClaim);
        appRoleClaimMapping.setRequested(true);

        ClaimMapping[] claimMappings = new ClaimMapping[filteredClaimMappings.length + 1];
        System.arraycopy(filteredClaimMappings, 0, claimMappings, 0, filteredClaimMappings.length);
        claimMappings[filteredClaimMappings.length] = appRoleClaimMapping;

        return claimMappings;  // Return the updated array
    }

    /**
     * Check whether the service provider app is a shared application for a sub-organization by an internal process.
     * In that process, the isFragmentApp attribute is set to true, and request initiated tenant domain and the
     * service provider belonging tenant domain would be different.
     *
     * @param serviceProvider The service provider app which is going to be provisioned.
     * @param tenantDomain    The tenant domain which the service provider app is belongs to.
     * @return True if app is shared by an internal process of Asgardeo for sharing apps to sub organizations.
     */
    private boolean isSharedAppFromInternalProcess(ServiceProvider serviceProvider, String tenantDomain) {

        return serviceProvider.getSpProperties() != null && Arrays.stream(serviceProvider.getSpProperties())
                .anyMatch(property -> IS_FRAGMENT_APP.equals(property.getName()) &&
                        Boolean.parseBoolean(property.getValue())) &&
                !StringUtils.equals(IdentityTenantUtil.getTenantDomainFromContext(), tenantDomain);
    }
}

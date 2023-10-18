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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.AssociatedApplication;
import org.wso2.carbon.identity.role.v2.mgt.core.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;

/**
 * Application management listener to handle shared roles in organizations.
 */
public class SharedRoleMgtListener extends AbstractApplicationMgtListener {

    private static final Log LOG = LogFactory.getLog(SharedRoleMgtListener.class);

    @Override
    public int getDefaultOrderId() {

        return 49;
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
            // Get all the shared applications of the deleting app.
            String sharedAppOrgId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            // Resolve the main application details.
            String mainAppId =
                    getOrgApplicationManager().getMainApplicationIdForGivenSharedApp(sharedAppId, sharedAppOrgId);
            if (mainAppId == null) {
                return false;
            }
            int mainAppTenantId = getApplicationMgtService().getTenantIdByApp(mainAppId);
            String mainAppTenantDomain = IdentityTenantUtil.getTenantDomain(mainAppTenantId);
            String mainAppOrgId = getOrganizationManager().resolveOrganizationId(mainAppTenantDomain);
            if (mainAppOrgId == null) {
                mainAppOrgId = SUPER_ORG_ID;
            }
            String allowedAudienceForRoleAssociationInMainApp =
                    getApplicationMgtService().getAllowedAudienceForRoleAssociation(mainAppId, mainAppTenantDomain);
            boolean hasAppAudiencedRoles =
                    RoleConstants.APPLICATION.equalsIgnoreCase(allowedAudienceForRoleAssociationInMainApp);
            if (hasAppAudiencedRoles) {
                // Handle role deletion in application deletion post actions.
                return true;
            }

            // Handing organization audienced roles associated case.
            handleOrganizationAudiencedSharedRoleDeletion(mainAppId, mainAppTenantDomain, mainAppOrgId, tenantDomain,
                    sharedAppOrgId);
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            throw new IdentityApplicationManagementException(
                    "Error while deleting organization roles associated to the app.", e);
        }
        return super.doPreDeleteApplication(applicationName, tenantDomain, userName);
    }

    private void handleOrganizationAudiencedSharedRoleDeletion(String mainApplicationId,
                                                               String mainApplicationTenantDomain,
                                                               String mainApplicationOrgId,
                                                               String unsharedApplicationTenantDomain,
                                                               String appUnsharedOrgId)
            throws IdentityRoleManagementException, IdentityApplicationManagementException,
            OrganizationManagementException {

        List<RoleV2> associatedRolesOfMainApplication = getApplicationMgtService()
                .getAssociatedRolesOfApplication(mainApplicationId, mainApplicationTenantDomain);
        List<String> mainAppRoleIds =
                associatedRolesOfMainApplication.stream().map(RoleV2::getId).collect(Collectors.toList());
        Map<String, String> mainRoleToSharedRoleMappingsInSubOrg = getRoleManagementServiceV2()
                .getMainRoleToSharedRoleMappingsBySubOrg(mainAppRoleIds, unsharedApplicationTenantDomain);

        // Get each role associated applications.
        for (String mainAppRoleId : mainAppRoleIds) {
            // TODO use a service which return only associated role ids.
            Role role = getRoleManagementServiceV2().getRole(mainAppRoleId, mainApplicationTenantDomain);
            List<AssociatedApplication> associatedApplications = role.getAssociatedApplications();

            if (associatedApplications == null) {
                continue;
            }
            /*
            If the only associated application is the main app in this flow, delete the role in
            the app unsharing org.
             */
            if (associatedApplications.size() == 1 && mainApplicationId.equals(associatedApplications.get(0).getId())) {
                // Delete the role in app unsharing org.
                getRoleManagementServiceV2().deleteRole(mainRoleToSharedRoleMappingsInSubOrg.get(mainAppRoleId),
                        unsharedApplicationTenantDomain);
                break;
            } else if (associatedApplications.size() > 1) {
                boolean isRoleUsedByAnotherSharedApp = false;
                for (AssociatedApplication associatedApplication : associatedApplications) {
                    if (associatedApplication.getId().equals(mainApplicationId)) {
                        continue;
                    }
                    boolean applicationSharedWithGivenOrganization =
                            getOrgApplicationManager().isApplicationSharedWithGivenOrganization(
                                    associatedApplication.getId(), mainApplicationOrgId, appUnsharedOrgId);
                    if (applicationSharedWithGivenOrganization) {
                        isRoleUsedByAnotherSharedApp = true;
                        break;
                    }
                }
                if (!isRoleUsedByAnotherSharedApp) {
                    // Delete the role in app unsharing org.
                    getRoleManagementServiceV2().deleteRole(mainRoleToSharedRoleMappingsInSubOrg.get(mainAppRoleId),
                            unsharedApplicationTenantDomain);
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

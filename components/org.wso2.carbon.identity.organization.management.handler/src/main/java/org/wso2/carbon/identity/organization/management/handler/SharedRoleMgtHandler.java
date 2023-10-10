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

package org.wso2.carbon.identity.organization.management.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.role.v2.mgt.core.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleBasicInfo;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Event handler to manage shared roles in sub-organizations.
 */
public class SharedRoleMgtHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(SharedRoleMgtHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        switch (eventName) {
            case OrgApplicationMgtConstants.EVENT_POST_SHARE_APPLICATION:
                /*
                If the main application use application audienced roles, create the role for sub org space,
                and add the relationship.
                 */
                createSubOrgRolesOnAppSharing(eventProperties);
                break;
            case OrgApplicationMgtConstants.EVENT_POST_DELETE_SHARED_APPLICATION:
                // TODO: no need to handle here if application audienced roles get
                // deleted if the application is deleted.
                break;
            case OrgApplicationMgtConstants.EVENT_POST_DELETE_ALL_SHARED_APPLICATIONS:
                // TODO: no need to handle here if application audienced roles get
                //  deleted if the application is deleted.
                break;
            case IdentityEventConstants.Event.POST_ADD_ROLE_V2_EVENT:
                createSubOrgRolesOnNewRoleCreation(eventProperties);
                break;
            case Constants.EVENT_POST_ADD_ORGANIZATION:
                /*
                 If the org is a sub organization and if primary org has roles with organization audience,
                 create them in the sub org as well.
                 */
                createSubOrgRolesOnNewOrgCreation(eventProperties);
                break;
        }
    }

    private void createSubOrgRolesOnNewOrgCreation(Map<String, Object> eventProperties) {

        try {
            Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            String organizationId = organization.getId();
            String organizationName = organization.getName();
            ParentOrganizationDO parentOrg = organization.getParent();
            if (getOrganizationManager().isPrimaryOrganization(organizationId)) {
                return;
            }

        } catch (OrganizationManagementServerException e) {
            throw new RuntimeException(e);
        }

    }

    private void createSubOrgRolesOnNewRoleCreation(Map<String, Object> eventProperties) {

        try {
            String mainRoleUUID = (String) eventProperties.get(IdentityEventConstants.EventProperty.ROLE_ID);
            String mainRoleName = (String) eventProperties.get(IdentityEventConstants.EventProperty.ROLE_NAME);
            String roleTenantDomain = (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
            String roleAudienceType = (String) eventProperties.get(IdentityEventConstants.EventProperty.AUDIENCE);
            String roleAudienceId = (String) eventProperties.get(IdentityEventConstants.EventProperty.AUDIENCE_ID);
            String roleOrgId = getOrganizationManager().resolveOrganizationId(roleTenantDomain);
            boolean isPrimaryOrganization = getOrganizationManager().isPrimaryOrganization(roleOrgId);
            if (!isPrimaryOrganization) {
                return;
            }
            switch (roleAudienceType) {
                case RoleConstants.APPLICATION:
                    // If the audienced application is a shared application, create the role in the shared apps.
                    List<SharedApplication> sharedApplications =
                            getOrgApplicationManager().getSharedApplications(roleOrgId, roleAudienceId);
                    for (SharedApplication sharedApplication : sharedApplications) {
                        String sharedApplicationId = sharedApplication.getSharedApplicationId();
                        String sharedOrganizationId = sharedApplication.getOrganizationId();
                        String shareAppTenantDomain =
                                getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
                        RoleBasicInfo sharedRoleInfo =
                                getRoleManagementServiceV2().addRole(mainRoleName, Collections.emptyList(),
                                        Collections.emptyList(),
                                        Collections.emptyList(), RoleConstants.APPLICATION, sharedApplicationId,
                                        shareAppTenantDomain);
                        // Add relationship between main role and shared role.
                        getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(mainRoleUUID,
                                sharedRoleInfo.getId(), roleTenantDomain, shareAppTenantDomain);
                    }
                    break;
                case RoleConstants.ORGANIZATION:
                    // If the audienced organization is a shared organization, create the role in the shared orgs.
                    getOrganizationManager().getChildOrganizations(roleOrgId, true).forEach(childOrg -> {
                        try {
                            String sharedOrganizationId = childOrg.getId();
                            String shareAppTenantDomain =
                                    getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
                            RoleBasicInfo sharedRoleInfo =
                                    getRoleManagementServiceV2().addRole(mainRoleName, Collections.emptyList(),
                                            Collections.emptyList(), Collections.emptyList(),
                                            RoleConstants.ORGANIZATION, sharedOrganizationId,
                                            shareAppTenantDomain);
                            // Add relationship between main role and shared role.
                            getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(mainRoleUUID,
                                    sharedRoleInfo.getId(), roleTenantDomain, shareAppTenantDomain);
                        } catch (OrganizationManagementException e) {
                            //TODO : handle exception
                            throw new RuntimeException(e);
                        } catch (IdentityRoleManagementException e) {
                            //TODO : handle exception
                            throw new RuntimeException(e);
                        }
                    });
                    break;
                default:
                    LOG.error("Unsupported audience type: " + roleAudienceType);
            }
        } catch (OrganizationManagementException e) {
            // TODO : handle exception
            LOG.debug(e.getMessage());
        } catch (IdentityRoleManagementException e) {
            // TODO : handle exception
            throw new RuntimeException(e);
        }
    }

    private void createSubOrgRolesOnAppSharing(Map<String, Object> eventProperties) {

        String parentOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID);
        String parentApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID);
        String sharedOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID);
        String sharedApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID);
        boolean hasAppAudiencedRoles = true;
        // TODO: check application is using the application audience roles.
        if (hasAppAudiencedRoles) {
            // Create the role if not exists, and add the relationship.
            try {
                String mainApplicationTenantDomain = getOrganizationManager().resolveTenantDomain(parentOrganizationId);
                String sharedApplicationTenantDomain =
                        getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
                RoleManagementService roleManagementServiceV2 = getRoleManagementServiceV2();
                roleManagementServiceV2.shareRoles(parentApplicationId, mainApplicationTenantDomain,
                        sharedApplicationId,
                        sharedApplicationTenantDomain);
            } catch (OrganizationManagementException | IdentityRoleManagementException e) {
                // TODO: handle exception
                throw new RuntimeException(e);
            }
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
}

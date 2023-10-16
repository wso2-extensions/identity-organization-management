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
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
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
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.role.v2.mgt.core.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleBasicInfo;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

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
            default:
                LOG.debug("Unsupported event: " + eventName);
                break;
        }
    }

    private void createSubOrgRolesOnNewOrgCreation(Map<String, Object> eventProperties) throws IdentityEventException {

        try {
            Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            String organizationId = organization.getId();
            if (getOrganizationManager().isPrimaryOrganization(organizationId)) {
                return;
            }
            String subOrgTenantDomain = getOrganizationManager().resolveTenantDomain(organizationId);
            ParentOrganizationDO parentOrg = organization.getParent();
            String parentOrgId = parentOrg.getId();
            // Get parent organization's roles which has organization audience.
            String filter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + parentOrg.getId();
            String parenTenantDomain = getOrganizationManager().resolveTenantDomain(parentOrgId);
            List<RoleBasicInfo> parentOrgRoles =
                    getRoleManagementServiceV2().getRoles(filter, null, 0, null, null, parenTenantDomain);
            for (RoleBasicInfo parentOrgRole : parentOrgRoles) {
                String parentOrgRoleName = parentOrgRole.getName();
                // Create the role in the sub org.
                RoleBasicInfo subOrgRole =
                        getRoleManagementServiceV2().addRole(parentOrgRoleName, Collections.emptyList(),
                                Collections.emptyList(), Collections.emptyList(), RoleConstants.ORGANIZATION,
                                organizationId, subOrgTenantDomain);
                // Add relationship between parent org role and sub org role.
                getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(parentOrgRole.getId(),
                        subOrgRole.getId(), parenTenantDomain, subOrgTenantDomain);
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException("Error occurred while resolving organization id from tenant domain.", e);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException("Error occurred while adding main role to shared role relationship.", e);
        }
    }

    private void createSubOrgRolesOnNewRoleCreation(Map<String, Object> eventProperties) throws IdentityEventException {

        try {
            String mainRoleUUID = (String) eventProperties.get(IdentityEventConstants.EventProperty.ROLE_ID);
            String mainRoleName = (String) eventProperties.get(IdentityEventConstants.EventProperty.ROLE_NAME);
            String roleTenantDomain = (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
            String roleAudienceType = (String) eventProperties.get(IdentityEventConstants.EventProperty.AUDIENCE);
            String roleAudienceId = (String) eventProperties.get(IdentityEventConstants.EventProperty.AUDIENCE_ID);
            String roleOrgId = getOrganizationManager().resolveOrganizationId(roleTenantDomain);
            boolean isPrimaryOrganization = getOrganizationManager().isPrimaryOrganization(roleOrgId);
            if (!isPrimaryOrganization && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(roleTenantDomain)) {
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
            throw new IdentityEventException("Error occurred while retrieving shared applications.", e);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException("Error occurred while adding main role to shared role relationship.", e);
        }
    }

    private void createSubOrgRolesOnAppSharing(Map<String, Object> eventProperties) throws IdentityEventException {

        String parentOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID);
        String parentApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID);
        String sharedOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID);
        String sharedApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID);
        try {
            String mainApplicationTenantDomain = getOrganizationManager().resolveTenantDomain(parentOrganizationId);
            String allowedAudienceForRoleAssociation =
                    OrganizationManagementHandlerDataHolder.getInstance().getApplicationManagementService()
                            .getAllowedAudienceForRoleAssociation(parentApplicationId, mainApplicationTenantDomain);
            boolean hasAppAudiencedRoles =
                    RoleConstants.APPLICATION.equalsIgnoreCase(allowedAudienceForRoleAssociation);
            if (!hasAppAudiencedRoles) {
                return;
            }
            // Create the role if not exists, and add the relationship.
            String sharedApplicationTenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
            // Get parent organization's roles which has application audience.
            String filter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + parentApplicationId;
            List<RoleBasicInfo> parentOrgRoles =
                    getRoleManagementServiceV2().getRoles(filter, null, 0, null, null,
                            mainApplicationTenantDomain);
            for (RoleBasicInfo parentOrgRole : parentOrgRoles) {
                String parentOrgRoleName = parentOrgRole.getName();
                // Create the role in the sub org.
                RoleBasicInfo subOrgRole =
                        getRoleManagementServiceV2().addRole(parentOrgRoleName, Collections.emptyList(),
                                Collections.emptyList(), Collections.emptyList(), RoleConstants.APPLICATION,
                                sharedApplicationId, sharedApplicationTenantDomain);
                // Add relationship between parent org role and sub org role.
                getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(parentOrgRole.getId(),
                        subOrgRole.getId(), mainApplicationTenantDomain, sharedApplicationTenantDomain);
            }
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityEventException("Error occurred checking main application allowed role audience.", e);
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException("Error occurred while resolving tenant domain from organization id.", e);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException("Error occurred while adding main role to shared role relationship.", e);
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

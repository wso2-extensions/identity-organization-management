/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.tenant.association.listeners;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.AbstractIdentityTenantMgtListener;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.models.User;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.management.tenant.association.Constants;
import org.wso2.carbon.identity.organization.management.tenant.association.internal.TenantAssociationDataHolder;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ORG_ADMINISTRATOR_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ORG_CREATOR_ROLE;
import static org.wso2.carbon.identity.organization.management.tenant.association.Constants.MINIMUM_PERMISSIONS_REQUIRED_FOR_ORG_CREATOR_VIEW;

/**
 * This class contains the implementation of the tenant management listener.  This listener will be used to add tenant
 * associations between the tenant creator and tenant, during the tenant creation flow.
 */
public class TenantAssociationManagementListener extends AbstractIdentityTenantMgtListener {

    private static final Log LOG = LogFactory.getLog(TenantAssociationManagementListener.class);

    @Override
    public void onTenantCreate(TenantInfoBean tenantInfo) {

        if (!isEnable()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Organization management related TenantAssociationManagementListener is not enabled.");
            }
            return;
        }
        int tenantId = tenantInfo.getTenantId();
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Organization management related TenantAssociationManagementListener fired for tenant creation " +
                            "for Tenant ID: " + tenantId);
        }
        try {
            RealmService realmService = TenantAssociationDataHolder.getRealmService();
            Tenant tenant = realmService.getTenantManager().getTenant(tenantId);
            // Association will be created only if the tenant created with an organization id.
            String organizationID = tenant.getAssociatedOrganizationUUID();
            if (organizationID == null || getOrganizationManager().getOrganizationDepthInHierarchy(organizationID) == -1) {
                Organization organization = new Organization();
                if (StringUtils.isBlank(organizationID)) {
                    organizationID = UUID.randomUUID().toString();
                }
                organization.setId(organizationID);
                organization.setName(tenantInfo.getTenantDomain());
                organization.setStatus(OrganizationManagementConstants.OrganizationStatus.ACTIVE.name());
                organization.setType(OrganizationManagementConstants.OrganizationTypes.TENANT.name());
                getOrganizationManager().addRootOrganization(tenant.getId(), organization);
                return;
            }
            // If the organization uses carbon roles, this organization association is not required.
            if (!Utils.useOrganizationRolesForValidation(organizationID)) {
                return;
            }
            String adminUUID = tenant.getAdminUserId();
            if (StringUtils.isBlank(adminUUID)) {
                // If realms were not migrated after https://github.com/wso2/product-is/issues/14001.
                adminUUID = realmService.getTenantUserRealm(tenantId).getRealmConfiguration().getAdminUserName();
            }
            String tenantUuid = tenant.getTenantUniqueID();
            if (StringUtils.isBlank(tenantUuid)) {
                LOG.error("Tenant UUID was not found for tenant: " + tenantId + ". Therefore, tenant association " +
                        "will not be set.");
                return;
            }
            if (StringUtils.isBlank(adminUUID)) {
                LOG.error(
                        "User UUID is empty. Therefore, tenant association will not be set with tenant: " + tenantUuid);
                return;
            }
            Role organizationCreatorRole = buildOrgCreatorRole(adminUUID);
            Role administratorRole = buildAdministratorRole(adminUUID);
            TenantAssociationDataHolder.getRoleManager().createRole(organizationID, organizationCreatorRole);
            TenantAssociationDataHolder.getRoleManager().createRole(organizationID, administratorRole);
        } catch (UserStoreException | OrganizationManagementException e) {
            String error = "Error occurred while adding user-tenant association for the tenant id: " + tenantId;
            LOG.error(error, e);
        }
    }

    private Role buildOrgCreatorRole(String adminUUID) {

        Role organizationCreatorRole = new Role();
        organizationCreatorRole.setDisplayName(ORG_CREATOR_ROLE);
        User orgCreator = new User(adminUUID);
        organizationCreatorRole.setUsers(Collections.singletonList(orgCreator));
        // Set permissions for org-creator role.
        ArrayList<String> orgCreatorRolePermissions = new ArrayList<>();
        // Adding mandatory permissions for the org-creator role.
        orgCreatorRolePermissions.add(Constants.ORG_MGT_PERMISSION);
        orgCreatorRolePermissions.add(Constants.ORG_ROLE_MGT_PERMISSION);
        /*
        Adding the bear minimum permission set that org creator should have to logged in to the console and view
        user, groups, roles, SP, IDP sections.
         */
        orgCreatorRolePermissions.addAll(MINIMUM_PERMISSIONS_REQUIRED_FOR_ORG_CREATOR_VIEW);
        // Add user create permission to organization creator to delegate permissions to other org users.
        // This permission is assigned until https://github.com/wso2/product-is/issues/14439 is fixed
        orgCreatorRolePermissions.add(Constants.USER_MGT_CREATE_PERMISSION);
        organizationCreatorRole.setPermissions(orgCreatorRolePermissions);
        return organizationCreatorRole;
    }

    private Role buildAdministratorRole(String adminUUID) {

        Role organizationAdministratorRole = new Role();
        organizationAdministratorRole.setDisplayName(ORG_ADMINISTRATOR_ROLE);
        User orgAdministrator = new User(adminUUID);
        organizationAdministratorRole.setUsers(Collections.singletonList(orgAdministrator));
        // Set permissions for org-administrator role.
        ArrayList<String> orgAdministratorRolePermissions = new ArrayList<>();
        // Setting all administrative permissions for the Administrator role
        orgAdministratorRolePermissions.add(Constants.ADMINISTRATOR_ROLE_PERMISSION);
        organizationAdministratorRole.setPermissions(orgAdministratorRolePermissions);
        return organizationAdministratorRole;
    }

    private OrganizationManager getOrganizationManager() {

        return TenantAssociationDataHolder.getOrganizationManager();
    }
}

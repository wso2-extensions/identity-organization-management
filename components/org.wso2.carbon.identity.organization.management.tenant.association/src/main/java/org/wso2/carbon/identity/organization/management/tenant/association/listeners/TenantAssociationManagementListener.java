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
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.tenant.association.internal.TenantAssociationDataHolder;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.UUID;

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
            if (organizationID == null ||
                    getOrganizationManager().getOrganizationDepthInHierarchy(organizationID) == -1) {
                Organization organization = new Organization();
                if (StringUtils.isBlank(organizationID)) {
                    organizationID = UUID.randomUUID().toString();
                }
                organization.setId(organizationID);
                organization.setName(tenantInfo.getTenantDomain());
                organization.setStatus(OrganizationManagementConstants.OrganizationStatus.ACTIVE.name());
                organization.setType(OrganizationManagementConstants.OrganizationTypes.TENANT.name());
                getOrganizationManager().addRootOrganization(tenant.getId(), organization);
            }
        } catch (UserStoreException | OrganizationManagementException e) {
            String error = "Error occurred while adding user-tenant association for the tenant id: " + tenantId;
            LOG.error(error, e);
        }
    }

    private OrganizationManager getOrganizationManager() {

        return TenantAssociationDataHolder.getOrganizationManager();
    }
}

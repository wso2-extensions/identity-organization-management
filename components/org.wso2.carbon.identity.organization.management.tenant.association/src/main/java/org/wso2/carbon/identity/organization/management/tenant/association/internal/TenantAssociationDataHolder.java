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

package org.wso2.carbon.identity.organization.management.tenant.association.internal;

import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * This class holds the services required for tenant association management services.
 */
public class TenantAssociationDataHolder {

    private RealmService realmService;
    private RoleManager roleManager;
    private OrganizationManager organizationManager;
    
    private static final TenantAssociationDataHolder INSTANCE = new TenantAssociationDataHolder();

    private TenantAssociationDataHolder() {

    }
    
    public static TenantAssociationDataHolder getInstance() {

        return INSTANCE;
    }

    /**
     * Get the RealmService.
     *
     * @return RealmService.
     */
    public RealmService getRealmService() {

        if (realmService == null) {
            throw new RuntimeException("RealmService was not set during the TenantAssociationServiceComponent startup");
        }
        return realmService;
    }

    /**
     * Set the RealmService.
     *
     * @param realmService RealmService.
     */
    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * Get the organization role manager service.
     *
     * @return Organization role manager service.
     */
    public RoleManager getRoleManager() {

        return roleManager;
    }

    /**
     * Set the organization role manager service.
     *
     * @param roleManager Organization role manager service.
     */
    public void setRoleManager(RoleManager roleManager) {

        this.roleManager = roleManager;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }
}

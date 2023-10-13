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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.internal;

import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Data holder for organization user sharing management.
 */
public class OrganizationUserSharingDataHolder {

    private static final OrganizationUserSharingDataHolder instance = new OrganizationUserSharingDataHolder();
    private RealmService realmService;
    private OrganizationManager organizationManager;
    private RoleManager roleManager;

    public static OrganizationUserSharingDataHolder getInstance() {

        return instance;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * Get the organization role manager service.
     *
     * @return Organization role manager service.
     */
    public RoleManager getRoleManager() {

        return this.roleManager;
    }

    /**
     * Set the organization role manager service.
     *
     * @param roleManager Organization role manager service.
     */
    public void setRoleManager(RoleManager roleManager) {

        this.roleManager = roleManager;
    }
}

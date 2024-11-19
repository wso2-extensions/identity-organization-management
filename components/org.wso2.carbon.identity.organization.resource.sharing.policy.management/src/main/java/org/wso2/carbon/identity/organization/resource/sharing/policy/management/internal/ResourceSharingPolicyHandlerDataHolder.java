/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.internal;

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Data holder for resource sharing policy management.
 */
public class ResourceSharingPolicyHandlerDataHolder {

    private static final ResourceSharingPolicyHandlerDataHolder instance =
            new ResourceSharingPolicyHandlerDataHolder();
    private RealmService realmService;
    private OrganizationManager organizationManager;
    private RoleManagementService roleManagementService;
    private ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService;
    private ApplicationManagementService applicationManagementService;
    private RoleManager roleManager;

    public static ResourceSharingPolicyHandlerDataHolder getInstance() {

        return instance;
    }

    /**
     * Get the Realm Service.
     *
     * @return RealmService instance.
     */
    public RealmService getRealmService() {

        return realmService;
    }

    /**
     * Set the Realm Service.
     *
     * @param realmService Instance of RealmService to set.
     */
    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * Get the Organization Manager service.
     *
     * @return OrganizationManager instance.
     */
    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    /**
     * Set the Organization Manager service.
     *
     * @param organizationManager Instance of OrganizationManager to set.
     */
    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    /**
     * Get the Role Management Service.
     *
     * @return RoleManagementService instance.
     */
    public RoleManagementService getRoleManagementService() {

        return roleManagementService;
    }

    /**
     * Set the Role Management Service.
     *
     * @param roleManagementService Instance of RoleManagementService to set.
     */
    public void setRoleManagementService(RoleManagementService roleManagementService) {

        this.roleManagementService = roleManagementService;
    }

    /**
     * Get the Resource Sharing Policy Handler service.
     *
     * @return ResourceSharingPolicyHandlerService instance.
     */
    public ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return resourceSharingPolicyHandlerService;
    }

    /**
     * Set the Resource Sharing Policy Handler service.
     *
     * @param resourceSharingPolicyHandlerService Instance of ResourceSharingPolicyHandlerService to set.
     */
    public void setResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        this.resourceSharingPolicyHandlerService = resourceSharingPolicyHandlerService;
    }

    /**
     * Get the Application Management Service.
     *
     * @return ApplicationManagementService instance.
     */
    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    /**
     * Set the Application Management Service.
     *
     * @param applicationManagementService Instance of ApplicationManagementService to set.
     */
    public void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }

    /**
     * Get the Role Manager Service.
     *
     * @return RoleManager instance.
     */
    public RoleManager getRoleManager() {

        return roleManager;
    }

    /**
     * Set the Role Manager Service.
     *
     * @param roleManager Instance of RoleManager to set.
     */
    public void setRoleManager(RoleManager roleManager) {

        this.roleManager = roleManager;
    }
}

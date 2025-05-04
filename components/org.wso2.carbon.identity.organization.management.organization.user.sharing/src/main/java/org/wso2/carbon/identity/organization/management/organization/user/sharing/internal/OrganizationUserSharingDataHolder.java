/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.service.AsyncOperationStatusMgtService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Data holder for organization user sharing management.
 */
public class OrganizationUserSharingDataHolder {

    private static final OrganizationUserSharingDataHolder instance = new OrganizationUserSharingDataHolder();
    private RealmService realmService;
    private OrganizationManager organizationManager;
    private RoleManagementService roleManagementService;
    private OrganizationUserSharingService organizationUserSharingService;
    private ApplicationManagementService applicationManagementService;
    private RoleManager roleManager;
    private ClaimMetadataManagementService claimManagementService;
    private OrgResourceResolverService orgResourceResolverService;
    private ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService;
    private AsyncOperationStatusMgtService asyncOperationStatusMgtService;

    public static OrganizationUserSharingDataHolder getInstance() {

        return instance;
    }

    /**
     * Get the organization manager service.
     *
     * @return Organization manager service.
     */
    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    /**
     * Set the organization manager service.
     *
     * @param organizationManager Organization manager service.
     */
    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    /**
     * Get the realm service.
     *
     * @return Realm service.
     */
    public RealmService getRealmService() {

        return realmService;
    }

    /**
     * Set the realm service.
     *
     * @param realmService RealmService service.
     */
    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * Get the organization role manager service.
     *
     * @return Organization role manager service.
     */
    public RoleManagementService getRoleManagementService() {

        return roleManagementService;
    }

    /**
     * Set the organization role manager service.
     *
     * @param roleManagementService Organization role manager service.
     */
    public void setRoleManagementService(RoleManagementService roleManagementService) {

        this.roleManagementService = roleManagementService;
    }

    /**
     * Get the organization user sharing service.
     *
     * @return OrganizationUserSharingService organization user sharing service.
     */
    public OrganizationUserSharingService getOrganizationUserSharingService() {

        return organizationUserSharingService;
    }

    /**
     * Set the organization user sharing service.
     *
     * @param organizationUserSharingService Organization user sharing service.
     */
    public void setOrganizationUserSharingService(OrganizationUserSharingService organizationUserSharingService) {

        this.organizationUserSharingService = organizationUserSharingService;
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

    /**
     * Get the application management service.
     *
     * @return Application management service.
     */
    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    /**
     * Set the application management service.
     *
     * @param applicationManagementService Application management service.
     */
    public void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }

    /**
     * Get the claim management service.
     *
     * @return ClaimMetadataManagementService claim management service.
     */
    public ClaimMetadataManagementService getClaimManagementService() {

        return claimManagementService;
    }

    /**
     * Set the claim management service.
     *
     * @param claimManagementService ClaimMetadataManagementService claim management service.
     */
    public void setClaimManagementService(ClaimMetadataManagementService claimManagementService) {

        this.claimManagementService = claimManagementService;
    }

    /**
     * Get the organization resource resolver service.
     *
     * @return OrgResourceResolverService organization resource resolver service.
     */
    public OrgResourceResolverService getOrgResourceResolverService() {

        return orgResourceResolverService;
    }

    /**
     * Set the organization resource resolver service.
     *
     * @param orgResourceResolverService OrgResourceResolverService organization resource resolver service.
     */
    public void setOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        this.orgResourceResolverService = orgResourceResolverService;
    }

    /**
     * Get the resource sharing policy handler service.
     *
     * @return ResourceSharingPolicyHandlerService resource sharing policy handler service.
     */
    public ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return resourceSharingPolicyHandlerService;
    }

    /**
     * Set the resource sharing policy handler service.
     *
     * @param resourceSharingPolicyHandlerService ResourceSharingPolicyHandlerService resource
     *                                            sharing policy handler service.
     */
    public void setResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        this.resourceSharingPolicyHandlerService = resourceSharingPolicyHandlerService;
    }

    /**
     * Get the async operation status management service.
     *
     * @return AsyncOperationStatusMgtService async operation status management service.
     */
    public AsyncOperationStatusMgtService getAsyncOperationStatusMgtService() {

        return asyncOperationStatusMgtService;
    }

    /**
     * Set the async operation status management service.
     *
     * @param asyncOperationStatusMgtService AsyncOperationStatusMgtService async operation
     *                                          status management service.
     */
    public void setAsyncOperationStatusMgtService(AsyncOperationStatusMgtService asyncOperationStatusMgtService) {

        this.asyncOperationStatusMgtService = asyncOperationStatusMgtService;
    }
}

/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.handler.internal;

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Organization management handler data holder.
 */
public class OrganizationManagementHandlerDataHolder {

    private static final OrganizationManagementHandlerDataHolder instance =
            new OrganizationManagementHandlerDataHolder();

    private IdentityEventService identityEventService;
    private IdentityGovernanceService identityGovernanceService;
    private OrganizationManager organizationManager;
    private RoleManagementService roleManagementServiceV2;
    private OrgApplicationManager orgApplicationManager;
    private ApplicationManagementService applicationManagementService;
    private RealmService realmService;
    private ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService;

    public static OrganizationManagementHandlerDataHolder getInstance() {

        return instance;
    }

    /**
     * Get {@link IdentityEventService}.
     *
     * @return IdentityEventService.
     */
    public IdentityEventService getIdentityEventService() {

        return identityEventService;
    }

    /**
     * Set {@link IdentityEventService}.
     *
     * @param identityEventService Instance of {@link IdentityEventService}.
     */
    public void setIdentityEventService(IdentityEventService identityEventService) {

        this.identityEventService = identityEventService;
    }

    /**
     * Get {@link IdentityGovernanceService}.
     *
     * @return IdentityGovernanceService.
     */
    public IdentityGovernanceService getIdentityGovernanceService() {

        return identityGovernanceService;
    }

    /**
     * Set {@link IdentityGovernanceService}.
     *
     * @param identityGovernanceService Instance of {@link IdentityGovernanceService}.
     */
    public void setIdentityGovernanceService(IdentityGovernanceService identityGovernanceService) {

        this.identityGovernanceService = identityGovernanceService;
    }

    /**
     * Get {@link OrganizationManager}.
     *
     * @return organization manager instance {@link OrganizationManager}.
     */
    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    /**
     * Set {@link OrganizationManager}.
     *
     * @param organizationManager Instance of {@link OrganizationManager}.
     */
    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    /**
     * Set {@link RoleManagementService}.
     *
     * @param roleManagementServiceV2 Instance of {@link RoleManagementService}.
     */
    public void setRoleManagementServiceV2(RoleManagementService roleManagementServiceV2) {

        this.roleManagementServiceV2 = roleManagementServiceV2;
    }

    /**
     * Get {@link RoleManagementService}.
     *
     * @return role management service instance {@link RoleManagementService}.
     */
    public RoleManagementService getRoleManagementServiceV2() {

        return roleManagementServiceV2;
    }

    /**
     * Get {@link OrgApplicationManager}.
     *
     * @return Org application manager instance {@link OrgApplicationManager}.
     */
    public OrgApplicationManager getOrgApplicationManager() {

        return orgApplicationManager;
    }

    /**
     * Set {@link OrgApplicationManager}.
     *
     * @param orgApplicationManager Instance of {@link OrgApplicationManager}.
     */
    public void setOrgApplicationManager(OrgApplicationManager orgApplicationManager) {

        this.orgApplicationManager = orgApplicationManager;
    }

    /**
     * Get {@link ApplicationManagementService}.
     *
     * @return Application management instance {@link ApplicationManagementService}.
     */
    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    /**
     * Set {@link ApplicationManagementService}.
     *
     * @param applicationManagementService Instance of {@link ApplicationManagementService}.
     */
    public void setApplicationManagementService(
            ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }

    /**
     * Get {@link RealmService}.
     *
     * @return Realm service {@link RealmService}.
     */
    public RealmService getRealmService() {

        return realmService;
    }

    /**
     * Set {@link RealmService}.
     *
     * @param realmService Instance of {@link RealmService}.
     */
    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * Get {@link ResourceSharingPolicyHandlerService}.
     *
     * @return ResourceSharingPolicyHandlerService {@link ResourceSharingPolicyHandlerService}.
     */
    public ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return resourceSharingPolicyHandlerService;
    }

    /**
     * Set {@link ResourceSharingPolicyHandlerService}.
     *
     * @param resourceSharingPolicyHandlerService Instance of {@link ResourceSharingPolicyHandlerService}.
     */
    public void setResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        this.resourceSharingPolicyHandlerService = resourceSharingPolicyHandlerService;
    }
}


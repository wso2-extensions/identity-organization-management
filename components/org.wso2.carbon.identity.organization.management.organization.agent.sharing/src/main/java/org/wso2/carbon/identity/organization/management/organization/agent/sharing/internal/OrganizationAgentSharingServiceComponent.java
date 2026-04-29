/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.service.AsyncOperationStatusMgtService;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.AgentSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.AgentSharingPolicyHandlerServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.OrganizationAgentSharingService;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.OrganizationAgentSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * OSGi service component for Organization Management - Agent Sharing bundle.
 */
@Component(
        name = "identity.organization.management.organization.agent.sharing.component",
        immediate = true
)
public class OrganizationAgentSharingServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationAgentSharingServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        BundleContext bundleContext = componentContext.getBundleContext();
        OrganizationAgentSharingService organizationAgentSharingService = new OrganizationAgentSharingServiceImpl();
        OrganizationAgentSharingDataHolder.getInstance()
                .setOrganizationAgentSharingService(organizationAgentSharingService);
        bundleContext.registerService(OrganizationAgentSharingService.class.getName(), organizationAgentSharingService,
                null);
        AgentSharingPolicyHandlerService agentSharingPolicyHandlerService = new AgentSharingPolicyHandlerServiceImpl();
        bundleContext.registerService(AgentSharingPolicyHandlerService.class.getName(),
                agentSharingPolicyHandlerService, null);
        LOG.debug("OrganizationAgentSharingServiceComponent activated successfully.");
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        OrganizationAgentSharingDataHolder.getInstance().setRealmService(realmService);
        LOG.debug("Set the Realm Service.");
    }

    protected void unsetRealmService(RealmService realmService) {

        OrganizationAgentSharingDataHolder.getInstance().setRealmService(null);
        LOG.debug("Unset the Realm Service.");
    }

    @Reference(
            name = "organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManagementService")
    protected void setOrganizationManagementService(OrganizationManager organizationManager) {

        OrganizationAgentSharingDataHolder.getInstance().setOrganizationManager(organizationManager);
        LOG.debug("Set Organization Management Service.");
    }

    protected void unsetOrganizationManagementService(OrganizationManager organizationManager) {

        OrganizationAgentSharingDataHolder.getInstance().setOrganizationManager(null);
        LOG.debug("Unset Organization Management Service.");
    }

    @Reference(
            name = "RoleManagementService",
            service = RoleManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagementService")
    protected void setRoleManagementService(RoleManagementService roleManagementService) {

        OrganizationAgentSharingDataHolder.getInstance().setRoleManagementService(roleManagementService);
        LOG.debug("Set Role Management Service.");
    }

    protected void unsetRoleManagementService(RoleManagementService roleManagementService) {

        OrganizationAgentSharingDataHolder.getInstance().setRoleManagementService(null);
        LOG.debug("Unset Role Management Service.");
    }

    @Reference(
            name = "RoleManager",
            service = RoleManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagerService")
    protected void setRoleManagerService(RoleManager roleManager) {

        OrganizationAgentSharingDataHolder.getInstance().setRoleManager(roleManager);
        LOG.debug("Set Role Manager Service.");
    }

    protected void unsetRoleManagerService(RoleManager roleManager) {

        OrganizationAgentSharingDataHolder.getInstance().setRoleManager(null);
        LOG.debug("Unset Role Manager Service.");
    }

    @Reference(name = "identity.application.management.component",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrganizationAgentSharingDataHolder.getInstance().setApplicationManagementService(applicationManagementService);
        LOG.debug("Set Application Management Service.");
    }

    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrganizationAgentSharingDataHolder.getInstance().setApplicationManagementService(null);
        LOG.debug("Unset Application Management Service.");
    }

    @Reference(
            name = "claim.metadata.management.service",
            service = ClaimMetadataManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetClaimMetadataManagementService"
    )
    protected void setClaimMetadataManagementService(ClaimMetadataManagementService claimManagementService) {

        OrganizationAgentSharingDataHolder.getInstance().setClaimManagementService(claimManagementService);
        LOG.debug("Set Claim Metadata Management Service.");
    }

    protected void unsetClaimMetadataManagementService(ClaimMetadataManagementService claimManagementService) {

        OrganizationAgentSharingDataHolder.getInstance().setClaimManagementService(null);
        LOG.debug("Unset Claim Metadata Management Service.");
    }

    @Reference(
            name = "org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service",
            service = OrgResourceResolverService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrgResourceResolverService")
    protected void setOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        OrganizationAgentSharingDataHolder.getInstance().setOrgResourceResolverService(orgResourceResolverService);
        LOG.debug("Set Org Resource Resolver Service.");
    }

    protected void unsetOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        OrganizationAgentSharingDataHolder.getInstance().setOrgResourceResolverService(null);
        LOG.debug("Unset Org Resource Resolver Service.");
    }

    @Reference(
            name = "resource.sharing.policy.handler.service",
            service = ResourceSharingPolicyHandlerService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetResourceSharingPolicyHandlerService"
    )
    protected void setResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        OrganizationAgentSharingDataHolder.getInstance()
                .setResourceSharingPolicyHandlerService(resourceSharingPolicyHandlerService);
        LOG.debug("Set Resource Sharing Policy Handler Service.");
    }

    protected void unsetResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        OrganizationAgentSharingDataHolder.getInstance().setResourceSharingPolicyHandlerService(null);
        LOG.debug("Unset Resource Sharing Policy Handler Service.");
    }

    @Reference(
            name = "async.operation.status.mgt.service",
            service = AsyncOperationStatusMgtService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAsyncOperationStatusMgtService"
    )
    protected void setAsyncOperationStatusMgtService(AsyncOperationStatusMgtService asyncOperationStatusMgtService) {

        OrganizationAgentSharingDataHolder.getInstance()
                .setAsyncOperationStatusMgtService(asyncOperationStatusMgtService);
        LOG.debug("Set Async Operation Status Mgt Service.");
    }

    protected void unsetAsyncOperationStatusMgtService(AsyncOperationStatusMgtService asyncOperationStatusMgtService) {

        OrganizationAgentSharingDataHolder.getInstance().setAsyncOperationStatusMgtService(null);
        LOG.debug("Unset Async Operation Status Mgt Service.");
    }
}

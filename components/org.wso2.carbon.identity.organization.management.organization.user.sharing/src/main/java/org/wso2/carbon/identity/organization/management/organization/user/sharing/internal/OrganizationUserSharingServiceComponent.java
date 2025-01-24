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
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.listener.SharedUserOperationEventListener;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.listener.SharedUserProfileUpdateGovernanceEventListener;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.listener.SharingOrganizationCreatorUserEventHandler;
import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * OSGi service component for Organization Management - User Sharing bundle.
 */
@Component(
        name = "identity.organization.management.organization.user.sharing.component",
        immediate = true
)
public class OrganizationUserSharingServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationUserSharingServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        BundleContext bundleContext = componentContext.getBundleContext();
        OrganizationUserSharingService organizationUserSharingService = new OrganizationUserSharingServiceImpl();
        OrganizationUserSharingDataHolder.getInstance()
                .setOrganizationUserSharingService(organizationUserSharingService);
        bundleContext.registerService(OrganizationUserSharingService.class.getName(), organizationUserSharingService,
                null);
        bundleContext.registerService(UserOperationEventListener.class.getName(),
                new SharedUserOperationEventListener(), null);
        bundleContext.registerService(UserOperationEventListener.class.getName(),
                new SharedUserProfileUpdateGovernanceEventListener(), null);
        bundleContext.registerService(AbstractEventHandler.class.getName(),
                new SharingOrganizationCreatorUserEventHandler(), null);
        LOG.info("OrganizationUserSharingServiceComponent activated successfully.");
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        OrganizationUserSharingDataHolder.getInstance().setRealmService(realmService);
        LOG.debug("Set the Realm Service");
    }

    protected void unsetRealmService(RealmService realmService) {

        OrganizationUserSharingDataHolder.getInstance().setRealmService(null);
        LOG.debug("Unset the Realm Service.");
    }

    @Reference(
            name = "organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManagementService")
    protected void setOrganizationManagementService(OrganizationManager organizationManager) {

        OrganizationUserSharingDataHolder.getInstance().setOrganizationManager(organizationManager);
        LOG.debug("Set Organization Management Service");
    }

    protected void unsetOrganizationManagementService(OrganizationManager organizationManager) {

        OrganizationUserSharingDataHolder.getInstance().setOrganizationManager(null);
        LOG.debug("Unset Organization Management Service");
    }

    @Reference(
            name = "RoleManagementService",
            service = RoleManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagementService")
    protected void setRoleManagementService(RoleManagementService roleManagementService) {

        OrganizationUserSharingDataHolder.getInstance().setRoleManagementService(roleManagementService);
    }

    protected void unsetRoleManagementService(RoleManagementService roleManagementService) {

        OrganizationUserSharingDataHolder.getInstance().setRoleManagementService(null);
    }

    @Reference(
            name = "RoleManager",
            service = RoleManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagerService")
    protected void setRoleManagerService(RoleManager roleManagerService) {

        OrganizationUserSharingDataHolder.getInstance().setRoleManager(roleManagerService);
    }

    protected void unsetRoleManagerService(RoleManager roleManagerService) {

        OrganizationUserSharingDataHolder.getInstance().setRoleManager(null);
    }

    @Reference(name = "identity.application.management.component",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrganizationUserSharingDataHolder.getInstance().setApplicationManagementService(applicationManagementService);
    }

    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrganizationUserSharingDataHolder.getInstance().setApplicationManagementService(null);
    }

    @Reference(
            name = "claim.metadata.management.service",
            service = ClaimMetadataManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetClaimMetadataManagementService"
    )
    protected void setClaimMetadataManagementService(ClaimMetadataManagementService claimManagementService) {

        OrganizationUserSharingDataHolder.getInstance().setClaimManagementService(claimManagementService);
        LOG.debug("Set Claim Metadata Management Service.");
    }

    protected void unsetClaimMetadataManagementService(ClaimMetadataManagementService claimManagementService) {

        OrganizationUserSharingDataHolder.getInstance().setClaimManagementService(null);
        LOG.debug("Unset Claim Metadata Management Service.");
    }

    @Reference(
            name = "org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service",
            service = OrgResourceResolverService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrgResourceResolverService")
    protected void setOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        OrganizationUserSharingDataHolder.getInstance().setOrgResourceResolverService(orgResourceResolverService);
        LOG.debug("Set Org Resource Resolver Service.");
    }

    protected void unsetOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        OrganizationUserSharingDataHolder.getInstance().setOrgResourceResolverService(null);
        LOG.debug("Unset Org Resource Resolver Service.");
    }
}

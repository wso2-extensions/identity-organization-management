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
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.handler.GovernanceConfigUpdateHandler;
import org.wso2.carbon.identity.organization.management.handler.SharedRoleMgtHandler;
import org.wso2.carbon.identity.organization.management.handler.listener.SharedRoleMgtListener;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Organization management handler service component.
 */
@Component(
        name = "identity.organization.management.handler.component",
        immediate = true
)
public class OrganizationManagementHandlerServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementHandlerServiceComponent.class);

    /**
     * Register the organization mgt listener implementation in the OSGI context.
     *
     * @param componentContext OSGi service component context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(AbstractEventHandler.class, new GovernanceConfigUpdateHandler(), null);
            bundleContext.registerService(AbstractEventHandler.class, new SharedRoleMgtHandler(), null);
            bundleContext.registerService(ApplicationMgtListener.class.getName(), new SharedRoleMgtListener(), null);
            LOG.debug("Organization management handler component activated successfully.");
        } catch (Throwable e) {
            LOG.error("Error while activating organization management handler module.", e);
        }
    }

    @Reference(
            name = "identity.event.service",
            service = IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService"
    )
    protected void setIdentityEventService(IdentityEventService identityEventService) {

        OrganizationManagementHandlerDataHolder.getInstance().setIdentityEventService(identityEventService);
        LOG.debug("IdentityEventService set in organization management handler.");
    }

    protected void unsetIdentityEventService(IdentityEventService identityEventService) {

        OrganizationManagementHandlerDataHolder.getInstance().setIdentityEventService(null);
    }

    @Reference(
            name = "IdentityGovernanceService",
            service = org.wso2.carbon.identity.governance.IdentityGovernanceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityGovernanceService")
    protected void setIdentityGovernanceService(IdentityGovernanceService governanceManager) {

        OrganizationManagementHandlerDataHolder.getInstance().setIdentityGovernanceService(governanceManager);
    }

    protected void unsetIdentityGovernanceService(IdentityGovernanceService governanceManager) {

        OrganizationManagementHandlerDataHolder.getInstance().setIdentityGovernanceService(null);
    }

    @Reference(name = "identity.organization.management.component",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService",
            service = org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagementServiceV2")
    protected void setRoleManagementServiceV2(RoleManagementService roleManagementService) {

        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(roleManagementService);
        LOG.debug("RoleManagementServiceV2 set in OrganizationManagementHandlerService bundle.");
    }

    protected void unsetRoleManagementServiceV2(RoleManagementService roleManagementService) {

        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(null);
        LOG.debug("RoleManagementServiceV2 unset in OrganizationManagementHandlerService bundle.");
    }

    @Reference(
            name = "org.wso2.carbon.identity.organization.management.application.OrgApplicationManager",
            service = org.wso2.carbon.identity.organization.management.application.OrgApplicationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrgApplicationManagementService")
    protected void setOrgApplicationManagementService(OrgApplicationManager orgApplicationManagementService) {

        OrganizationManagementHandlerDataHolder.getInstance().setOrgApplicationManager(orgApplicationManagementService);
        LOG.debug("OrgApplication management service set in OrganizationManagementHandlerService bundle.");
    }

    protected void unsetOrgApplicationManagementService(OrgApplicationManager orgApplicationManagementService) {

        OrganizationManagementHandlerDataHolder.getInstance().setOrgApplicationManager(null);
        LOG.debug("OrgApplication management service unset in OrganizationManagementHandlerService bundle.");
    }

    @Reference(
            name = "org.wso2.carbon.identity.application.mgt.ApplicationManagementService",
            service = org.wso2.carbon.identity.application.mgt.ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(applicationManagementService);
        LOG.debug("Application management service set in OrganizationManagementHandlerService bundle.");
    }

    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrganizationManagementHandlerDataHolder.getInstance().setApplicationManagementService(null);
        LOG.debug("Application management service unset in OrganizationManagementHandlerService bundle.");
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        OrganizationManagementHandlerDataHolder.getInstance().setRealmService(realmService);
        LOG.debug("Realm service set in OrganizationManagementHandlerService bundle.");
    }

    protected void unsetRealmService(RealmService realmService) {

        OrganizationManagementHandlerDataHolder.getInstance().setRealmService(null);
        LOG.debug("Realm service unset in OrganizationManagementHandlerService bundle.");
    }

    @Reference(
            name = "ResourceSharingPolicyHandlerService",
            service = ResourceSharingPolicyHandlerService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetResourceSharingPolicyHandlerService")
    protected void setResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        OrganizationManagementHandlerDataHolder.getInstance()
                .setResourceSharingPolicyHandlerService(resourceSharingPolicyHandlerService);
    }

    protected void unsetResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        OrganizationManagementHandlerDataHolder.getInstance().setResourceSharingPolicyHandlerService(null);
    }
}

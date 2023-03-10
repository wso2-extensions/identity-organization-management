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

package org.wso2.carbon.identity.organization.management.role.management.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.role.management.service.RoleManagerImpl;
import org.wso2.carbon.identity.organization.management.role.management.service.listener.OrganizationUserOperationEventListener;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * OSGi service component for Organization Management - Role Management bundle.
 */
@Component(
        name = "carbon.organization.management.role.management.component",
        immediate = true
)
public class RoleManagementServiceComponent {

    private static final Log LOG = LogFactory.getLog(RoleManagementServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(RoleManager.class.getName(), new RoleManagerImpl(), null);
            bundleContext.registerService(UserOperationEventListener.class.getName(),
                    new OrganizationUserOperationEventListener(), null);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Organization Management - Role Management component activated successfully.");
            }
        } catch (Throwable e) {
            LOG.error("Error while activating Organization Management - Role Management Component", e);
        }
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the Realm Service");
        }
        RoleManagementDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Unset the Realm Service.");
        }
        RoleManagementDataHolder.getInstance().setRealmService(null);
    }

    @Reference(
            name = "organization.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager"
    )
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the organization management service.");
        }
        RoleManagementDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Unset organization management service.");
        }
        RoleManagementDataHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(
            name = "organization.user.resident.resolver.service",
            service = OrganizationUserResidentResolverService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationUserResidentResolverService"
    )
    protected void setOrganizationUserResidentResolverService(
            OrganizationUserResidentResolverService organizationUserResidentResolverService) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the organization user resident resolver service.");
        }
        RoleManagementDataHolder.getInstance()
                .setOrganizationUserResidentResolverService(organizationUserResidentResolverService);
    }

    protected void unsetOrganizationUserResidentResolverService(
            OrganizationUserResidentResolverService organizationUserResidentResolverService) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Unset organization user resident resolver service.");
        }
        RoleManagementDataHolder.getInstance().setOrganizationUserResidentResolverService(null);
    }
}

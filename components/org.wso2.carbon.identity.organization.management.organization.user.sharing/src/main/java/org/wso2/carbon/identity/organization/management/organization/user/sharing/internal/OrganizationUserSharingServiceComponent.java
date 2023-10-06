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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.listener.SharedUserOperationEventListener;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.listener.SharingOrganizationCreatorUserEventHandler;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
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
        bundleContext.registerService(OrganizationUserSharingService.class.getName(),
                new OrganizationUserSharingServiceImpl(), null);
        bundleContext.registerService(UserOperationEventListener.class.getName(),
                new SharedUserOperationEventListener(), null);
        bundleContext.registerService(AbstractEventHandler.class.getName(),
                new SharingOrganizationCreatorUserEventHandler(), null);
        LOG.info("Shared Organization User Listener activated successfully.");
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
}

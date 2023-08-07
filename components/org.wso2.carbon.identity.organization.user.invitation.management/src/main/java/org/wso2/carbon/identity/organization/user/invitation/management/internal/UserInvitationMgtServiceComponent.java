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

package org.wso2.carbon.identity.organization.user.invitation.management.internal;

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
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.user.invitation.management.InvitationCoreService;
import org.wso2.carbon.identity.organization.user.invitation.management.InvitationCoreServiceImpl;
import org.wso2.carbon.identity.organization.user.invitation.management.handler.UserInvitationEventHandler;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * OSGi service component for Organization Management - Role Management bundle.
 */
@Component(
        name = "carbon.organization.organization.user.invitation.management.component",
        immediate = true
)
public class UserInvitationMgtServiceComponent {

    private static final Log LOG = LogFactory.getLog(UserInvitationMgtServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(InvitationCoreService.class.getName(),
                    new InvitationCoreServiceImpl(), null);
            LOG.info("Organization User Invitation Mgt component activated successfully.");
            bundleContext.registerService(AbstractEventHandler.class.getName(),
                    new UserInvitationEventHandler(), null);
            LOG.info("Organization User Invitation Handler activated successfully.");
        } catch (Throwable e) {
            LOG.error("Error while activating Organization User Invitation Mgt Component", e);
        }
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        UserInvitationMgtDataHolder.getInstance().setRealmService(realmService);
        LOG.debug("Set the Realm Service");
    }

    protected void unsetRealmService(RealmService realmService) {

        UserInvitationMgtDataHolder.getInstance().setRealmService(null);
        LOG.debug("Unset the Realm Service.");
    }

    @Reference(
            name = "identity.event.service",
            service = IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService"
    )
    protected void setIdentityEventService(IdentityEventService identityEventService) {

        UserInvitationMgtDataHolder.getInstance().setIdentityEventService(identityEventService);
        LOG.debug("Set Identity Event Service.");
    }

    protected void unsetIdentityEventService(IdentityEventService identityEventService) {

        UserInvitationMgtDataHolder.getInstance().setIdentityEventService(null);
        LOG.debug("Unset Identity Event Service.");
    }

    @Reference(
            name = "organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManagementService")
    protected void setOrganizationManagementService(OrganizationManager organizationManager) {

        UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(organizationManager);
        LOG.debug("Set Organization Management Service");

    }

    protected void unsetOrganizationManagementService(OrganizationManager organizationManager) {

        UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(null);
        LOG.debug("Unset Organization Management Service");
    }
}

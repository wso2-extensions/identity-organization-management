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

package org.wso2.carbon.identity.organization.management.role.mgt.core.internal;

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
import org.wso2.carbon.identity.organization.management.role.mgt.core.OrganizationUserRoleManager;
import org.wso2.carbon.identity.organization.management.role.mgt.core.OrganizationUserRoleManagerImpl;
import org.wso2.carbon.identity.organization.management.role.mgt.core.dao.OrganizationUserRoleMgtDAOImpl;
import org.wso2.carbon.identity.organization.management.role.mgt.core.handler.OrganizationUserRoleAuditLogger;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * OSGi service component for organization and user role management core bundle.
 */
@Component(
        name = "carbon.organization.and.user.role.mgt.component",
        immediate = true
)
public class OrganizationUserRoleMgtServiceComponent {

    private static final Log log = LogFactory.getLog(OrganizationUserRoleMgtServiceComponent.class);

    /**
     * Register Organization and User Role Manager service in the OSGI context.
     *
     * @param componentContext OSGi service component context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            OrganizationUserRoleMgtDataHolder.getInstance()
                    .setOrganizationAndUserRoleMgtDAO(new OrganizationUserRoleMgtDAOImpl());
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(OrganizationUserRoleManager.class.getName(),
                    new OrganizationUserRoleManagerImpl(), null);
            bundleContext.registerService(
                    AbstractEventHandler.class.getName(), new OrganizationUserRoleAuditLogger(),
                    null);
            if (log.isDebugEnabled()) {
                log.debug("Organization and User Role Management component activated successfully.");
            }
        } catch (Throwable e) {
            log.error("Error while activating Organization and User Role Management module.", e);
        }
    }

    @Reference(
            name = "realm.service",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        OrganizationUserRoleMgtDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Unset the Realm Service.");
        }
        OrganizationUserRoleMgtDataHolder.getInstance().setRealmService(null);
    }

    @Reference(
            name = "identity.event.service",
            service = IdentityEventService.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService"
    )
    protected void setIdentityEventService(IdentityEventService identityEventService) {

        OrganizationUserRoleMgtDataHolder.getInstance().setIdentityEventService(identityEventService);
    }

    protected void unsetIdentityEventService(IdentityEventService identityEventService) {

        OrganizationUserRoleMgtDataHolder.getInstance().setIdentityEventService(null);
    }
}

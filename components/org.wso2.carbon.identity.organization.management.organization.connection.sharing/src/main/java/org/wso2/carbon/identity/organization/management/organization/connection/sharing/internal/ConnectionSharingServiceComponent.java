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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.ConnectionSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.ConnectionSharingPolicyHandlerServiceImpl;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.idp.mgt.IdpManager;

/**
 * OSGi service component for the Organization Connection Sharing bundle.
 */
@Component(
        name = "identity.organization.management.organization.connection.sharing.component",
        immediate = true
)
public class ConnectionSharingServiceComponent {

    private static final Log LOG = LogFactory.getLog(ConnectionSharingServiceComponent.class);

    private ServiceRegistration<?> serviceRegistration;

    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            ConnectionSharingPolicyHandlerService connectionSharingPolicyHandlerService =
                    new ConnectionSharingPolicyHandlerServiceImpl();
            serviceRegistration = bundleContext.registerService(
                    ConnectionSharingPolicyHandlerService.class.getName(),
                    connectionSharingPolicyHandlerService, null);
            LOG.info("ConnectionSharingServiceComponent activated successfully.");
        } catch (Throwable e) {
            LOG.error("Failed to activate ConnectionSharingServiceComponent.", e);
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
        LOG.debug("ConnectionSharingServiceComponent deactivated.");
    }

    @Reference(
            name = "organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager"
    )
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        ConnectionSharingDataHolder.getInstance().setOrganizationManager(organizationManager);
        LOG.debug("Set Organization Management Service.");
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        ConnectionSharingDataHolder.getInstance().setOrganizationManager(null);
        LOG.debug("Unset Organization Management Service.");
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

        ConnectionSharingDataHolder.getInstance()
                .setResourceSharingPolicyHandlerService(resourceSharingPolicyHandlerService);
        LOG.debug("Set Resource Sharing Policy Handler Service.");
    }

    protected void unsetResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        ConnectionSharingDataHolder.getInstance().setResourceSharingPolicyHandlerService(null);
        LOG.debug("Unset Resource Sharing Policy Handler Service.");
    }

    @Reference(
            name = "idp.manager.service",
            service = IdpManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdpManager"
    )
    protected void setIdpManager(IdpManager idpManager) {

        ConnectionSharingDataHolder.getInstance().setIdpManager(idpManager);
        LOG.debug("Set IDP Manager Service.");
    }

    protected void unsetIdpManager(IdpManager idpManager) {

        ConnectionSharingDataHolder.getInstance().setIdpManager(null);
        LOG.debug("Unset IDP Manager Service.");
    }
}

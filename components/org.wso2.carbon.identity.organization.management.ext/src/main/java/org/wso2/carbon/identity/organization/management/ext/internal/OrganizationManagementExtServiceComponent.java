/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.ext.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.ext.OrganizationManagerListenerImpl;
import org.wso2.carbon.identity.organization.management.service.listener.OrganizationManagerListener;

/**
 * Organization management ext service component.
 */
@Component(
        name = "identity.organization.application.management.ext.component",
        immediate = true
)
public class OrganizationManagementExtServiceComponent {

    private static final Log log = LogFactory.getLog(OrganizationManagementExtServiceComponent.class);

    /**
     * Register the organization mgt listener implementation in the OSGI context.
     *
     * @param componentContext OSGi service component context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(OrganizationManagerListener.class.getName(),
                    new OrganizationManagerListenerImpl(), null);

            if (log.isDebugEnabled()) {
                log.debug("Organization management ext component activated successfully.");
            }
        } catch (Throwable e) {
            log.error("Error while activating organization management ext module.", e);
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

        OrganizationManagementExtDataHolder.getInstance().setIdentityEventService(identityEventService);
        if (log.isDebugEnabled()) {
            log.debug("IdentityEventService set in Central logger.");
        }
    }

    protected void unsetIdentityEventService(IdentityEventService identityEventService) {

        OrganizationManagementExtDataHolder.getInstance().setIdentityEventService(null);
    }
}

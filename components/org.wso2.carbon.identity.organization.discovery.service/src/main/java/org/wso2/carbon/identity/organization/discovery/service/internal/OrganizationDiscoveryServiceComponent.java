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

package org.wso2.carbon.identity.organization.discovery.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.discovery.service.EmailDomainDiscoveryFactory;
import org.wso2.carbon.identity.organization.discovery.service.OrganizationDiscoveryManager;
import org.wso2.carbon.identity.organization.discovery.service.OrganizationDiscoveryManagerImpl;
import org.wso2.carbon.identity.organization.discovery.service.OrganizationDiscoveryTypeFactory;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Service component class for the organization discovery service.
 */
@Component(
        name = "org.wso2.carbon.identity.organization.discovery.service",
        immediate = true)
public class OrganizationDiscoveryServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationDiscoveryServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        BundleContext bundleContext = componentContext.getBundleContext();
        bundleContext.registerService(OrganizationDiscoveryManager.class.getName(),
                new OrganizationDiscoveryManagerImpl(), null);
        OrganizationDiscoveryTypeFactory emailDomainDiscovery = new EmailDomainDiscoveryFactory();
        bundleContext.registerService(OrganizationDiscoveryTypeFactory.class.getName(), emailDomainDiscovery, null);
        LOG.info("Organization discovery service component activated successfully.");
    }

    @Reference(name = "identity.organization.management.component",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(name = "identity.organization.config.management.component",
            service = OrganizationConfigManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationConfigManager")
    protected void setOrganizationConfigManager(OrganizationConfigManager organizationConfigManager) {

        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationConfigManager(organizationConfigManager);
    }

    protected void unsetOrganizationConfigManager(OrganizationConfigManager organizationConfigManager) {

        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationConfigManager(null);
    }

    @Reference(
            name = "organization.discovery.type.component",
            service = OrganizationDiscoveryTypeFactory.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationDiscoveryTypeFactory"
    )
    protected void setOrganizationDiscoveryTypeFactory(OrganizationDiscoveryTypeFactory
                                                                   organizationDiscoveryTypeFactory) {

        OrganizationDiscoveryServiceHolder.getInstance().setDiscoveryTypeFactory(organizationDiscoveryTypeFactory);
    }

    protected void unsetOrganizationDiscoveryTypeFactory(OrganizationDiscoveryTypeFactory
                                                                 organizationDiscoveryTypeFactory) {

        OrganizationDiscoveryServiceHolder.getInstance().unbindDiscoveryTypeFactory(organizationDiscoveryTypeFactory);
    }
}

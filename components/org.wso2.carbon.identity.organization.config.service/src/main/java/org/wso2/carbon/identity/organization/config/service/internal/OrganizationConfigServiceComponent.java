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

package org.wso2.carbon.identity.organization.config.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManagerImpl;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Service component class for the organization configuration service.
 */
@Component(
        name = "org.wso2.carbon.identity.organization.config.service",
        immediate = true)
public class OrganizationConfigServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationConfigServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        OrganizationConfigManager organizationConfigManager = new OrganizationConfigManagerImpl();
        BundleContext bundleContext = componentContext.getBundleContext();
        bundleContext.registerService(OrganizationConfigManager.class.getName(), organizationConfigManager, null);
        OrganizationConfigServiceHolder.getInstance().setOrganizationConfigManager(organizationConfigManager);
        LOG.debug("Organization configuration service component activated successfully.");
    }

    @Reference(
            name = "resource.configuration.manager",
            service = ConfigurationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationManager"
    )
    protected void setConfigurationManager(ConfigurationManager configurationManager) {

        OrganizationConfigServiceHolder.getInstance().setConfigurationManager(configurationManager);
    }

    protected void unsetConfigurationManager(ConfigurationManager configurationManager) {

        OrganizationConfigServiceHolder.getInstance().setConfigurationManager(null);
    }

    @Reference(name = "identity.organization.management.component",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OrganizationConfigServiceHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OrganizationConfigServiceHolder.getInstance().setOrganizationManager(null);
    }
}

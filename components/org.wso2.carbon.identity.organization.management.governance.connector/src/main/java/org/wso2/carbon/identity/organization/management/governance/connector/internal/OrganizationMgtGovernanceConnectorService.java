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

package org.wso2.carbon.identity.organization.management.governance.connector.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.identity.governance.common.IdentityConnectorConfig;
import org.wso2.carbon.identity.organization.management.governance.connector.OrganizationMgtGovernanceConnectorImp;

/**
 * OSGi service component for organization management governance connector.
 */
@Component(name = "identity.organization.management.governance.connector",
        immediate = true)
public class OrganizationMgtGovernanceConnectorService {

    private static final Log LOG = LogFactory.getLog(OrganizationMgtGovernanceConnectorService.class);

    /**
     * Register Organization Management Initialization in the OSGi context.
     *
     * @param componentContext OSGi service component context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(IdentityConnectorConfig.class.getName(),
                    new OrganizationMgtGovernanceConnectorImp(), null);
            LOG.debug("Organization management governance connector activated successfully.");
        } catch (Exception e) {
            LOG.error("Error while activating Organization management governance connector.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        LOG.debug("Organization management governance connector is deactivated.");
    }
}

/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.executor.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.organization.management.executor.OrganizationProvisioningExecutor;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * OSGi service component for the organization management flow executors.
 */
@Component(
        name = "identity.organization.management.executor.component",
        immediate = true
)
public class OrganizationManagementExecutorServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementExecutorServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            context.getBundleContext().registerService(Executor.class.getName(),
                    new OrganizationProvisioningExecutor(), null);
            LOG.debug("Organization management executor bundle is activated successfully.");
        } catch (Exception e) {
            LOG.error("Error while activating the organization management executor component.", e);
        }
    }

    @Reference(
            name = "organization.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager"
    )
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OrganizationManagementExecutorDataHolder.getInstance().setOrganizationManager(organizationManager);
        LOG.debug("Set the organization management service.");
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OrganizationManagementExecutorDataHolder.getInstance().setOrganizationManager(null);
        LOG.debug("Unset the organization management service.");
    }
}

/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.capability.governance.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.organization.management.capability.governance.GovernancePolicyEvaluator;
import org.wso2.carbon.identity.organization.management.capability.governance.GovernancePolicyEvaluatorImpl;
import org.wso2.carbon.identity.organization.management.capability.governance.GovernancePolicyService;
import org.wso2.carbon.identity.organization.management.capability.governance.GovernancePolicyServiceImpl;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * OSGi service component for the Organization Capability Governance bundle.
 */
@Component(
        name = "identity.organization.management.capability.governance.component",
        immediate = true
)
public class GovernancePolicyServiceComponent {

    private static final Log LOG = LogFactory.getLog(GovernancePolicyServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        BundleContext bundleContext = componentContext.getBundleContext();
        bundleContext.registerService(GovernancePolicyService.class.getName(),
                new GovernancePolicyServiceImpl(), null);
        bundleContext.registerService(GovernancePolicyEvaluator.class.getName(),
                new GovernancePolicyEvaluatorImpl(), null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("GovernancePolicyServiceComponent activated successfully.");
        }
    }

    @Reference(
            name = "organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManagementService")
    protected void setOrganizationManagementService(OrganizationManager organizationManager) {

        GovernancePolicyDataHolder.getInstance().setOrganizationManager(organizationManager);
        LOG.debug("Set Organization Management Service");
    }

    protected void unsetOrganizationManagementService(OrganizationManager organizationManager) {

        GovernancePolicyDataHolder.getInstance().setOrganizationManager(null);
        LOG.debug("Unset Organization Management Service");
    }
}

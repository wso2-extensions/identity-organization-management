/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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
package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverServiceImpl;

/**
 * OSGi declarative services component of the organization resource hierarchy traverse service.
 */
@Component(
        name = "org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service",
        immediate = true)
public class OrgResourceHierarchyTraverseServiceComponent {

    private static final Log log = LogFactory.getLog(OrgResourceHierarchyTraverseServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(OrgResourceResolverService.class.getName(),
                    new OrgResourceResolverServiceImpl(), null);
            if (log.isDebugEnabled()) {
                log.debug("OrgResourceResolverService bundle is activated successfully.");
            }
        } catch (Exception e) {
            log.error("Error while activating OrgResourceResolverService bundle.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("OrgResourceResolverService bundle is deactivated");
        }
    }

    @Reference(name = "org.wso2.carbon.identity.organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OrgResourceHierarchyTraverseServiceDataHolder.getInstance().setOrganizationManager(organizationManager);
        log.debug("OrganizationManager set in OrgResourceManagementServiceComponent bundle.");
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OrgResourceHierarchyTraverseServiceDataHolder.getInstance().setOrganizationManager(null);
        log.debug("OrganizationManager unset in OrgResourceManagementServiceComponent bundle.");
    }

    @Reference(
            name = "org.wso2.carbon.identity.application.mgt",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrgResourceHierarchyTraverseServiceDataHolder.getInstance()
                .setApplicationManagementService(applicationManagementService);
        log.debug("ApplicationManagementService set in OrgResourceManagementServiceComponent bundle.");
    }

    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrgResourceHierarchyTraverseServiceDataHolder.getInstance().setApplicationManagementService(null);
        log.debug("ApplicationManagementService unset in OrgResourceManagementServiceComponent bundle.");
    }
}

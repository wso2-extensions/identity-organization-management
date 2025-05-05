/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.application.resource.hierarchy.traverse.service.internal;

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
import org.wso2.carbon.identity.organization.application.resource.hierarchy.traverse.service.OrgAppResourceResolverService;
import org.wso2.carbon.identity.organization.application.resource.hierarchy.traverse.service.OrgAppResourceResolverServiceImpl;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * OSGi component responsible for managing the activation and deactivation of the organization application resource
 * hierarchy traverse service.
 * <p>
 * It manages dynamic references to necessary services required by {@link OrgAppResourceResolverService} as well.
 */
@Component(
        name = "org.wso2.carbon.identity.organization.application.resource.hierarchy.traverse.service",
        immediate = true)
public class OrgAppResourceHierarchyTraverseServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrgAppResourceHierarchyTraverseServiceComponent.class);

    /**
     * Activates the OSGi component.
     * This method is called when the component is activated in the OSGi environment.
     */
    @Activate
    protected void activate(ComponentContext context) {

        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(OrgAppResourceResolverService.class.getName(),
                    new OrgAppResourceResolverServiceImpl(), null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("OrgAppResourceResolverService bundle is activated successfully.");
            }
        } catch (Exception e) {
            LOG.error("Error while activating OrgAppResourceResolverService bundle.", e);
        }
    }

    /**
     * Deactivates the OSGi component.
     * This method is called when the component is deactivated in the OSGi environment.
     */
    @Deactivate
    protected void deactivate() {

        if (LOG.isDebugEnabled()) {
            LOG.debug("OrgAppResourceResolverService bundle is deactivated successfully.");
        }
    }

    /**
     * Set the OrganizationManager service.
     *
     * @param organizationManager OrganizationManager instance.
     */
    @Reference(
            name = "org.wso2.carbon.identity.organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OrgAppResourceHierarchyTraverseServiceDataHolder.getInstance()
                .setOrganizationManager(organizationManager);
        if (LOG.isDebugEnabled()) {
            LOG.debug("OrganizationManager set in OrgAppResourceHierarchyTraverseServiceComponent bundle.");
        }
    }

    /**
     * Unset the OrganizationManager service.
     *
     * @param organizationManager OrganizationManager instance.
     */
    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OrgAppResourceHierarchyTraverseServiceDataHolder.getInstance()
                .setOrganizationManager(null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("OrganizationManager unset in OrgAppResourceHierarchyTraverseServiceComponent bundle.");
        }
    }

    /**
     * Set the ApplicationManagementService service.
     *
     * @param applicationManagementService ApplicationManagementService instance.
     */
    @Reference(
            name = "org.wso2.carbon.identity.application.mgt",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrgAppResourceHierarchyTraverseServiceDataHolder.getInstance()
                .setApplicationManagementService(applicationManagementService);
        if (LOG.isDebugEnabled()) {
            LOG.debug("ApplicationManagementService set in OrgAppResourceHierarchyTraverseServiceComponent bundle.");
        }
    }

    /**
     * Unset the ApplicationManagementService service.
     *
     * @param applicationManagementService ApplicationManagementService instance.
     */
    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrgAppResourceHierarchyTraverseServiceDataHolder.getInstance()
                .setApplicationManagementService(null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("ApplicationManagementService unset in OrgAppResourceHierarchyTraverseServiceComponent bundle.");
        }
    }
}

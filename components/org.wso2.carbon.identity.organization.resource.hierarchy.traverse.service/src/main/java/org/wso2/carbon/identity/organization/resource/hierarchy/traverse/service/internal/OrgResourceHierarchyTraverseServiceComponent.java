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
 * OSGi component responsible for managing the activation and deactivation of the organization resource hierarchy
 * traverse service.
 * <p>
 * It manages dynamic references to necessary services required by {@link OrgResourceResolverService} as well.
 */
@Component(
        name = "org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service",
        immediate = true)
public class OrgResourceHierarchyTraverseServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrgResourceHierarchyTraverseServiceComponent.class);

    /**
     * Activates the OSGi component by registering the {@link OrgResourceResolverService} service.
     * This method is called when the component is activated in the OSGi environment.
     *
     * @param context The ComponentContext instance that provides the OSGi environment context.
     */
    @Activate
    protected void activate(ComponentContext context) {

        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(OrgResourceResolverService.class.getName(),
                    new OrgResourceResolverServiceImpl(), null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("OrgResourceResolverService bundle is activated successfully.");
            }
        } catch (Exception e) {
            LOG.error("Error while activating OrgResourceResolverService bundle.", e);
        }
    }

    /**
     * Deactivates the OSGi component by cleaning up resources and logging the deactivation.
     * This method is called when the component is deactivated in the OSGi environment.
     *
     * @param context The ComponentContext instance that provides the OSGi environment context.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("OrgResourceResolverService bundle is deactivated");
        }
    }

    /**
     * Sets the OrganizationManager instance in the OrgResourceHierarchyTraverseServiceDataHolder.
     *
     * @param organizationManager The OrganizationManager instance to be assigned.
     */
    @Reference(name = "org.wso2.carbon.identity.organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OrgResourceHierarchyTraverseServiceDataHolder.getInstance().setOrganizationManager(organizationManager);
        LOG.debug("OrganizationManager set in OrgResourceManagementServiceComponent bundle.");
    }

    /**
     * Unsets the OrganizationManager instance in the OrgResourceHierarchyTraverseServiceDataHolder.
     *
     * @param organizationManager The OrganizationManager instance to be removed.
     */
    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OrgResourceHierarchyTraverseServiceDataHolder.getInstance().setOrganizationManager(null);
        LOG.debug("OrganizationManager unset in OrgResourceManagementServiceComponent bundle.");
    }

    /**
     * Sets the ApplicationManagementService instance in the OrgResourceHierarchyTraverseServiceDataHolder.
     *
     * @param applicationManagementService The ApplicationManagementService instance to be assigned.
     */
    @Reference(
            name = "org.wso2.carbon.identity.application.mgt",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrgResourceHierarchyTraverseServiceDataHolder.getInstance()
                .setApplicationManagementService(applicationManagementService);
        LOG.debug("ApplicationManagementService set in OrgResourceManagementServiceComponent bundle.");
    }

    /**
     * Unsets the ApplicationManagementService instance in the OrgResourceHierarchyTraverseServiceDataHolder.
     *
     * @param applicationManagementService The ApplicationManagementService instance to be removed.
     */
    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrgResourceHierarchyTraverseServiceDataHolder.getInstance().setApplicationManagementService(null);
        LOG.debug("ApplicationManagementService unset in OrgResourceManagementServiceComponent bundle.");
    }
}


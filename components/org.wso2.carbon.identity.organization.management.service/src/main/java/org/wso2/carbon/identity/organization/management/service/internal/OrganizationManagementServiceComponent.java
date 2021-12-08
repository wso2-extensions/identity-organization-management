/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManagerImpl;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.dao.impl.CachedBackedOrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.dao.impl.OrganizationManagementDAOImpl;

/**
 * OSGI service component for organization management core bundle.
 */
@Component(name = "identity.organization.management.component",
           immediate = true)
public class OrganizationManagementServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementServiceComponent.class);

    /**
     * Register Organization Manager service in the OSGI context.
     *
     * @param componentContext OSGI service component context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(OrganizationManager.class.getName(), new OrganizationManagerImpl(), null);

            OrganizationManagementDAO organizationMgtDao = new CachedBackedOrganizationManagementDAO
                    (new OrganizationManagementDAOImpl());
            OrganizationManagementDataHolder.getInstance().setOrganizationManagementDAO(organizationMgtDao);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Organization Management component activated successfully.");
            }
        } catch (Throwable e) {
            LOG.error("Error while activating Organization Management module.", e);
        }
    }
}

/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Service component class for organization management tomcat valve.
 */
@Component(
        name = "org.wso2.carbon.identity.organization.management.tomcat.service",
        immediate = true)
public class OrganizationManagementTomcatServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementTomcatServiceComponent.class);

    @Activate
    protected void activate(ComponentContext cxt) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("OrganizationManagementTomcatServiceComponent is activated");
        }
    }

    @Reference(name = "identity.organization.management.service..component",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the Organization Manager.");
        }
        OrganizationManagementTomcatDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsetting the Organization Manager.");
        }
        OrganizationManagementTomcatDataHolder.getInstance().setOrganizationManager(null);
    }
}

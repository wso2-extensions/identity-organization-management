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

package org.wso2.carbon.identity.organization.management.authz.valve.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.authz.service.AuthorizationManager;
import org.wso2.carbon.identity.core.handler.HandlerComparator;

import java.util.List;

/**
 * OSGi service component for organization management authorization valve.
 */
@Component(
        name = "org.wso2.carbon.identity.organization.management.authz.valve",
        immediate = true)
public class OrganizationManagementAuthzValveServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementAuthzValveServiceComponent.class);

    @Activate
    protected void activate(ComponentContext cxt) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Organization management authorization valve component is activated successfully.");
        }
    }

    @Reference(
            name = "org.wso2.carbon.identity.authz.service.manager.consume",
            service = org.wso2.carbon.identity.authz.service.AuthorizationManager.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAuthorizationManager")
    protected void setAuthorizationManager(AuthorizationManager authorizationManager) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("AuthorizationManager acquired.");
        }
        List<AuthorizationManager> authorizationManagerList = OrganizationManagementAuthzValveServiceHolder
                .getInstance().getAuthorizationManagerList();
        authorizationManagerList.add(authorizationManager);
        authorizationManagerList.sort(new HandlerComparator());
    }

    protected void unsetAuthorizationManager(AuthorizationManager authorizationManager) {

        List<AuthorizationManager> authorizationManagerList = OrganizationManagementAuthzValveServiceHolder
                .getInstance().getAuthorizationManagerList();
        authorizationManagerList.remove(authorizationManager);
    }
}

/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.organization.management.user.role.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Service component class for user role relations of organization management.
 */
@Component(
        name = "org.wso2.carbon.identity.organization.management.user.role.service",
        immediate = true)
public class OrganizationManagementUserRoleServiceComponent {

    private static final Log log = LogFactory.getLog(OrganizationManagementUserRoleServiceComponent.class);

    @Activate
    protected void activate(ComponentContext cxt) {

        if (log.isDebugEnabled()) {
            log.debug("OrganizationManagementUserRoleServiceComponent is activated");
        }
    }
}

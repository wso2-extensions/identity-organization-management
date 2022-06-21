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

package org.wso2.carbon.identity.organization.management.role.management.endpoint;

import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;

/**
 * Service holder class for role management related services.
 */
public class RoleManagementServiceHolder {

    private static RoleManagementServiceHolder instance = new RoleManagementServiceHolder();

    private RoleManager roleManager;

    private RoleManagementServiceHolder() {

    }

    public static RoleManagementServiceHolder getInstance() {

        return instance;
    }

    /**
     * Set RoleManager OSGi service.
     *
     * @param roleManager RoleManager.
     */
    public void setRoleManager(RoleManager roleManager) {

        RoleManagementServiceHolder.getInstance().roleManager = roleManager;
    }

    /**
     * Get RoleManager OSGi service.
     *
     * @return RoleManager
     */
    public RoleManager getRoleManager() {

        return RoleManagementServiceHolder.getInstance().roleManager;
    }
}

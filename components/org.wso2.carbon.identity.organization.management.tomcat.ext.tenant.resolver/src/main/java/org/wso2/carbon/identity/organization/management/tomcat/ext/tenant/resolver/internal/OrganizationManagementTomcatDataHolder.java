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

import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Organization management data holder.
 */
public class OrganizationManagementTomcatDataHolder {

    private static final OrganizationManagementTomcatDataHolder instance = new OrganizationManagementTomcatDataHolder();
    private OrganizationManager organizationManager;

    public static OrganizationManagementTomcatDataHolder getInstance() {

        return instance;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }
}

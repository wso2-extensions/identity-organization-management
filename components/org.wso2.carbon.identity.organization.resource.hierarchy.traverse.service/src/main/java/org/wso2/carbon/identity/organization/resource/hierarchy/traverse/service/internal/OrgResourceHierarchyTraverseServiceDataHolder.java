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

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * This class holds the data required for the organization resource hierarchy traverse service.
 */
public class OrgResourceHierarchyTraverseServiceDataHolder {

    private static final OrgResourceHierarchyTraverseServiceDataHolder instance =
            new OrgResourceHierarchyTraverseServiceDataHolder();

    private OrganizationManager organizationManager;
    private ApplicationManagementService applicationManagementService;

    private OrgResourceHierarchyTraverseServiceDataHolder() {

    }

    /**
     * Get the instance of OrgResourceManagementServiceDataHolder.
     *
     * @return OrgResourceManagementServiceDataHolder instance.
     */
    public static OrgResourceHierarchyTraverseServiceDataHolder getInstance() {

        return instance;
    }

    /**
     * Get the organization manager.
     *
     * @return Organization manager.
     */
    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    /**
     * Set the organization manager.
     *
     * @param organizationManager Organization manager instance.
     */
    public void setOrganizationManager(
            OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    /**
     * Get the application management service.
     *
     * @return Application management service.
     */
    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    /**
     * Set the application management service.
     *
     * @param applicationManagementService Application management service instance.
     */
    public void setApplicationManagementService(
            ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }
}

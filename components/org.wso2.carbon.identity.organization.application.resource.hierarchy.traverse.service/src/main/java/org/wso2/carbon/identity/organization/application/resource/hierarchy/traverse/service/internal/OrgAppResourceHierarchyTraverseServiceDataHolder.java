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

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Singleton class that serves as a centralized data holder for key service instances used in the organization
 * application resource hierarchy traversal process.
 */
public class OrgAppResourceHierarchyTraverseServiceDataHolder {

    private static final OrgAppResourceHierarchyTraverseServiceDataHolder INSTANCE =
            new OrgAppResourceHierarchyTraverseServiceDataHolder();

    private ApplicationManagementService applicationManagementService;
    private OrganizationManager organizationManager;

    private OrgAppResourceHierarchyTraverseServiceDataHolder() {
    }

    /**
     * Retrieves the singleton instance of the OrgAppResourceHierarchyTraverseServiceDataHolder class.
     *
     * @return The singleton instance of OrgAppResourceHierarchyTraverseServiceDataHolder.
     */
    public static OrgAppResourceHierarchyTraverseServiceDataHolder getInstance() {

        return INSTANCE;
    }

    /**
     * Retrieves the current instance of the ApplicationManagementService.
     *
     * @return The current ApplicationManagementService instance responsible for managing applications.
     */
    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    /**
     * Sets the ApplicationManagementService instance.
     *
     * @param applicationManagementService The ApplicationManagementService instance to be assigned.
     */
    public void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }

    /**
     * Retrieves the current instance of the OrganizationManager.
     *
     * @return The current OrganizationManager instance that manages organizational data.
     */
    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    /**
     * Sets the OrganizationManager instance.
     *
     * @param organizationManager The OrganizationManager instance to be assigned.
     */
    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }
}

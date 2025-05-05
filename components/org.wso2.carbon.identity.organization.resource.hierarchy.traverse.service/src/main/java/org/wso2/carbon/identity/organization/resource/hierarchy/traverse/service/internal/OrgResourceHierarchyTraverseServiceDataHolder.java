/*
 * Copyright (c) 2024-2025, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Singleton class that serves as a centralized data holder for key service instances used in the organization
 * resource hierarchy traversal process.
 */
public class OrgResourceHierarchyTraverseServiceDataHolder {

    private static final OrgResourceHierarchyTraverseServiceDataHolder INSTANCE =
            new OrgResourceHierarchyTraverseServiceDataHolder();

    private OrganizationManager organizationManager;

    private OrgResourceHierarchyTraverseServiceDataHolder() {

    }

    /**
     * Retrieves the Singleton instance of the OrgResourceHierarchyTraverseServiceDataHolder class.
     *
     * @return The singleton instance of OrgResourceHierarchyTraverseServiceDataHolder.
     */
    public static OrgResourceHierarchyTraverseServiceDataHolder getInstance() {

        return INSTANCE;
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

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

package org.wso2.carbon.identity.organization.management.application.dao;

import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Optional;

/**
 * This interface performs CRUD operations for Shared applications.
 */
public interface OrgApplicationMgtDAO {

    /**
     * Creates new entry for shared applications across organizations.
     *
     * @param ownerTenantId  tenant owns the application.
     * @param mainAppId      main application.
     * @param sharedTenantId tenant to whom the application is shared.
     * @param sharedAppId    shared application id.
     * @throws OrganizationManagementException the server exception is thrown in a failure to create the entry.
     */
    void addSharedApplication(int ownerTenantId, String mainAppId, int sharedTenantId, String sharedAppId)
            throws OrganizationManagementException;

    /**
     * Returns the Unique identifier of the shared application.
     *
     * @param ownerTenantId  tenant owns the application.
     * @param sharedTenantId tenant to whom the application is shared.
     * @param mainAppId      main application identifier.
     * @return Unique identifier of the shared application.
     * @throws OrganizationManagementException the server exception is thrown in a failure to retrieve the entry.
     */
    Optional<String> getSharedApplicationResourceId(int ownerTenantId, int sharedTenantId, String mainAppId)
            throws OrganizationManagementException;
}

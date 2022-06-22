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

package org.wso2.carbon.identity.organization.management.application;

import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.List;

/**
 * Interface for Organization Application Management.
 */
public interface OrgApplicationManager {

    /**
     * Share the application to all the child organizations or to a list of child organizations based on the user input.
     *
     * @param ownerOrgId Identifier of the organization owning the application.
     * @param mainAppId  Identifier of the main application.
     * @param sharedOrgs Optional list of identifiers of child organization to share the application.
     * @throws OrganizationManagementException on errors when sharing the application.
     */
    void shareOrganizationApplication(String ownerOrgId, String mainAppId, List<String> sharedOrgs)
            throws OrganizationManagementException;

    /**
     * Resolve the shared application id based on the organization link and the identifier of the main application.
     *
     * @param mainAppName Name of the main application.
     * @param ownerOrgId  Identifier of the organization owning the application.
     * @param sharedOrgId Identifier of the organization owning the shared application.
     * @return shared application {@link ServiceProvider}.
     * @throws OrganizationManagementException on errors when resolving the shared application id.
     */
    ServiceProvider resolveSharedApplication(String mainAppName, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException;
}

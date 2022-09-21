/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application;

import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;

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
     * Remove the shared (fragment) application for given organization to stop sharing the business application.
     *
     * @param organizationId       ID of the organization owning the primary application.
     * @param applicationId        ID of the primary application.
     * @param sharedOrganizationId organization ID which owns the fragment application.
     * @throws OrganizationManagementException on errors when removing the fragment application.
     */
    void deleteSharedApplication(String organizationId, String applicationId, String sharedOrganizationId)
            throws OrganizationManagementException;

    /**
     * Returns the list of organization with whom the primary application is shared.
     *
     * @param ownerOrgId ID of the organization owning the primary application.
     * @param mainAppId  ID of the primary application.
     * @throws OrganizationManagementException on errors when retrieving the list of organization owning the
     *                                         fragment applications.
     */
    List<BasicOrganization> getApplicationSharedOrganizations(String ownerOrgId, String mainAppId)
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

    /**
     * Share the application to an organization.
     *
     * @param ownerOrgId      Identifier of the organization owning the application.
     * @param sharedOrgId     Identifier of the organization to which the application being shared to.
     * @param mainApplication Identifier of the main application.
     * @throws OrganizationManagementException on errors when sharing the application.
     */
    void shareApplication(String ownerOrgId, String sharedOrgId, ServiceProvider mainApplication,
            boolean shareWithSubOrgs) throws OrganizationManagementException;
}

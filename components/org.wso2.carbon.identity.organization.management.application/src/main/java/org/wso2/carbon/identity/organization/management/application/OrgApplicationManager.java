/*
 * Copyright (c) 2022-2024, WSO2 LLC. (http://www.wso2.com).
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
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.service.exception.NotImplementedException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;

import java.util.List;
import java.util.Map;

/**
 * Interface for Organization Application Management.
 */
public interface OrgApplicationManager {

    /**
     * Share the application to all the child organizations or to a list of child organizations based on the user input.
     *
     * @param ownerOrgId           Identifier of the organization owning the application.
     * @param mainAppId            Identifier of the main application.
     * @param shareWithAllChildren Attribute indicating if the application is shared with all sub-organizations.
     * @param sharedOrgs           Optional list of identifiers of child organization to share the application.
     * @throws OrganizationManagementException on errors when sharing the application.
     */
    void shareOrganizationApplication(String ownerOrgId, String mainAppId, boolean shareWithAllChildren,
                                      List<String> sharedOrgs) throws OrganizationManagementException;

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
     * Returns the shared applications list of a given primary application, along with their organizations.
     *
     * @param ownerOrgId ID of the organization owning the primary application.
     * @param mainAppId  UUID of the primary application.
     * @return A list of shared applications details.
     * @throws OrganizationManagementException on errors occurred while retrieving the list of shared applications.
     */
    List<SharedApplication> getSharedApplications(String ownerOrgId, String mainAppId)
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
     * Resolve the shared application based on the organization link and the UUID of the main application.
     *
     * @param mainAppUUID UUID of the main application.
     * @param ownerOrgId  Identifier of the organization owning the application.
     * @param sharedOrgId Identifier of the organization owning the shared application.
     * @return shared application {@link ServiceProvider}.
     * @throws OrganizationManagementException If errors occurred when resolving the shared application id.
     */
    default ServiceProvider resolveSharedApplicationByMainAppUUID(String mainAppUUID, String ownerOrgId,
                                                                  String sharedOrgId)
            throws OrganizationManagementException {

        return null;
    }

    /**
     * Check whether the application is shared with the given organization.
     *
     * @param mainAppId   UUID of the main application.
     * @param ownerOrgId  Identifier of the organization owning the application.
     * @param sharedOrgId Identifier of the organization which checking whether app is shared or not.
     * @return True if the application is shared with the given organization.
     * @throws OrganizationManagementException If errors occurred when checking whether the application is shared
     *                                         with the given organization.
     */
    default boolean isApplicationSharedWithGivenOrganization(String mainAppId, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException {

        return false;
    }

    /**
     * Get the main application id for given shared application.
     *
     * @param sharedAppId Shared application id.
     * @param sharedOrgId Organization id of the shared application.
     * @return Main application id.
     * @throws OrganizationManagementException If errors occurred when getting the main application id.
     */
    default String getMainApplicationIdForGivenSharedApp(String sharedAppId, String sharedOrgId)
            throws OrganizationManagementException {

        return null;
    }

    /**
     * Share the application to an organization.
     *
     * @param ownerOrgId           Identifier of the organization owning the application.
     * @param sharedOrgId          Identifier of the organization to which the application being shared to.
     * @param mainApplication      The application which is shared with the child organizations.
     * @param shareWithAllChildren Boolean attribute indicating if the application is shared with all sub-organizations.
     * @throws OrganizationManagementException on errors when sharing the application.
     */
    void shareApplication(String ownerOrgId, String sharedOrgId, ServiceProvider mainApplication,
                          boolean shareWithAllChildren) throws OrganizationManagementException;

    /**
     * Get the shared ancestor application IDs for the given child application ID of the given child organization.
     *
     * @param childAppId The unique ID of the shared child application.
     * @param childOrgId The organization ID of the child.
     * @return Map containing shared ancestor application IDs and their organization IDs.
     * @throws OrganizationManagementException If errors occurred when retrieving the shared ancestor application IDs
     */
    default Map<String, String> getAncestorAppIds(String childAppId, String childOrgId)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "getAncestorAppIds method is not implemented in " + this.getClass().getName());
    }

    /**
     * Get shared child application IDs of the given parent application.
     *
     * @param parentAppId The unique ID of the root app / shared app in parent org.
     * @param parentOrgId The organization ID of the parent.
     * @param childOrgIds The organization ID list of the children.
     * @return The map containing organization ID and application ID of the shared child applications.
     * @throws OrganizationManagementException If errors occurred when retrieving the child app IDs.
     */
    default Map<String, String> getChildAppIds(String parentAppId, String parentOrgId, List<String> childOrgIds)
            throws OrganizationManagementException {

        throw new NotImplementedException("getChildAppIds method is not implemented in " + this.getClass().getName());
    }
}

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

package org.wso2.carbon.identity.organization.management.application.dao;

import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.exception.NotImplementedException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.List;
import java.util.Optional;

/**
 * This interface performs CRUD operations for Shared applications.
 */
public interface OrgApplicationMgtDAO {

    /**
     * Creates new entry for shared applications across organizations.
     *
     * @param mainAppId             Unique identifier of the main application.
     * @param ownerOrgId            The unique ID corresponding to the organization where the main application resides.
     * @param sharedAppId           Unique identifier of the shared application.
     * @param sharedOrgId           The unique ID of the organization, to whom the application is shared.
     * @param shareWithAllChildren  Attribute indicating if the application is shared with all child organizations.
     * @throws OrganizationManagementException the server exception is thrown in a failure to create the entry.
     */
    void addSharedApplication(String mainAppId, String ownerOrgId, String sharedAppId, String sharedOrgId,
            boolean shareWithAllChildren) throws OrganizationManagementException;

    /**
     * Creates new entry for shared applications across organizations. Here we don't use the shareWithAllChildren
     * parameter since with the new implementation, we handle it by policy management.
     *
     * @param mainAppResourceId  Unique identifier of the main application.
     * @param ownerOrgId         The unique ID corresponding to the organization where the main application resides.
     * @param shareAppResourceId Unique identifier of the shared application.
     * @param sharedOrgId        The unique ID of the organization, to whom the application is shared.
     * @throws OrganizationManagementException the server exception is thrown in a failure to create the entry.
     */
    default void addSharedApplication(String mainAppResourceId, String ownerOrgId, String shareAppResourceId,
                                      String sharedOrgId) throws OrganizationManagementException {

        addSharedApplication(mainAppResourceId, ownerOrgId, shareAppResourceId, sharedOrgId, false);
    }

    /**
     * Retrieve the list of shared applications entries for a given application.
     *
     * @param organizationId The unique ID corresponding to the organization where the main application resides.
     * @param applicationId  Unique identifier of the main application.
     * @return list of {@link SharedApplicationDO}s for a given application.
     * @throws OrganizationManagementException the server exception is thrown in a failure to create the entry.
     */
    List<SharedApplicationDO> getSharedApplications(String organizationId, String applicationId)
            throws OrganizationManagementException;

    /**
     * Retrieve main application for a given shared application.
     *
     * @param sharedAppId Unique identifier of the shared application.
     * @param sharedOrgId The unique ID of the organization, to whom the application is shared.
     * @return {@link MainApplicationDO} for a given shared application.
     * @throws OrganizationManagementException the server exception is thrown in a failure to retrieve the entry.
     */
    Optional<MainApplicationDO> getMainApplication(String sharedAppId, String sharedOrgId)
            throws OrganizationManagementException;

    /**
     * Retrieve shared application for a given shared application id.
     *
     * @param sharedAppId Unique identifier of the shared application.
     * @param sharedOrgId The unique ID of the organization, to whom the application is shared.
     * @return {@link SharedApplicationDO}
     * @throws OrganizationManagementException the server exception is thrown in a failure to retrieve the entry.
     */
    Optional<SharedApplicationDO> getSharedApplication(int sharedAppId, String sharedOrgId)
            throws OrganizationManagementException;

    /**
     * Returns the unique identifier of the shared application.
     *
     * @param mainAppId   Main application identifier.
     * @param ownerOrgId  The unique ID corresponding to the organization where the main application resides.
     * @param sharedOrgId The unique ID of the organization, to whom the application is shared.
     * @return Unique identifier of the shared application.
     * @throws OrganizationManagementException the server exception is thrown in a failure to retrieve the entry.
     */
    Optional<String> getSharedApplicationResourceId(String mainAppId, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException;

    /**
     * Returns whether the given application has fragment applications.
     *
     * @param applicationId Application ID.
     * @return boolean value indicating whether the given application has fragments created.
     * @throws OrganizationManagementException the server exception is thrown in a failure when checking the fragments
     *                                         of the application.
     */
    boolean hasFragments(String applicationId) throws OrganizationManagementException;

    /**
     * Returns whether the given application is a fragment application.
     *
     * @param applicationId Application ID.
     * @return boolean value indicating whether the given application is a fragment application.
     * @throws OrganizationManagementException the server exception is thrown in a failure when checking if the
     *                                         application is a fragment.
     */
    boolean isFragmentApplication(int applicationId) throws OrganizationManagementException;

    /**
     * Update the shareWithAllChildren value of shared applications for a given main application.
     *
     * @param mainApplicationId     The unique ID of the main application.
     * @param ownerOrganizationId   The unique ID corresponding to the organization where the main application resides.
     * @param shareWithAllChildren  Value to be updated.
     * @throws OrganizationManagementException the server exception is thrown in a failure when updating.
     */
    void updateShareWithAllChildren(String mainApplicationId, String ownerOrganizationId, boolean shareWithAllChildren)
            throws OrganizationManagementException;

    /**
     * Returns the unique identifiers of shared applications associated with the given main application
     * within the given shared organizations.
     *
     * @param mainAppId    The app ID of the main application.
     * @param ownerOrgId   The organization ID of the owner.
     * @param sharedOrgIds The list of app shared organization IDs.
     * @return The list of shared application IDs within the given shared organizations.
     * @throws OrganizationManagementException The server exception is thrown in a failure
     *                                         when retrieving the shared apps.
     */
    default List<SharedApplicationDO> getSharedApplications(String mainAppId, String ownerOrgId,
                                                            List<String> sharedOrgIds)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "getSharedApplications method is not implemented in " + this.getClass().getName());
    }

    /**
     * Returns the unique identifiers of shared applications associated with the given main application
     * within the given shared organizations.
     *
     * @param ownerOrgId       The main organizationId that the original application belongs to.
     * @param mainApplicationId The app resource ID of the main application.
     * @param sharedOrgIds     The list of app shared organization IDs.
     * @param expressionNodes  The list of expression nodes to filter the results.
     * @param sortOder         The order in which to sort the results.
     * @param limit            The maximum number of results to return.
     * @return The list of shared application IDs within the given shared organizations.
     * @throws OrganizationManagementException The server exception is thrown in a failure
     *                                         when retrieving the shared apps.
     */
    default List<SharedApplicationDO> getSharedApplications(String ownerOrgId, String mainApplicationId,
                                                           List<String> sharedOrgIds, List<ExpressionNode>
                                                                   expressionNodes, String sortOder, int limit)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "getSharedApplications method is not implemented in " + this.getClass().getName());
    }

    /**
     * Returns the basic information of the discoverable shared applications
     *
     * @param limit        Maximum no of applications to be returned in the result set (optional).
     * @param offset       Zero based index of the first application to be returned in the result set (optional).
     * @param filter       Filter to search for applications (optional).
     * @param sortOrder    Sort order, ascending or descending (optional).
     * @param sortBy       Attribute to sort from (optional).
     * @param tenantDomain Tenant domain.
     * @param rootOrgId    Root organization ID.
     * @return List of DiscoverableApplicationBasicInfo of applications matching the given criteria.
     * @throws OrganizationManagementException The server exception is thrown in a failure when retrieving the
     *                                         discoverable applications.
     */
    List<ApplicationBasicInfo> getDiscoverableSharedApplicationBasicInfo(int limit, int offset, String filter,
                                                                           String sortOrder, String sortBy,
                                                                           String tenantDomain, String rootOrgId)
            throws OrganizationManagementException;

    /**
     * Returns the count of discoverable shared applications matching given filter.
     *
     * @param filter       Filter to search for applications (optional).
     * @param tenantDomain Tenant domain.
     * @param rootOrgId    Root organization ID.
     * @return Count of discoverable applications matching given filter.
     * @throws OrganizationManagementException The server exception is thrown in a failure when retrieving the
     *                                         discoverable applications count.
     */
    int getCountOfDiscoverableSharedApplications(String filter, String tenantDomain, String rootOrgId)
            throws OrganizationManagementException;

    /**
     * Delete shared application links of an organization.
     *
     * @param organizationId    The unique ID of the organization.
     * @throws OrganizationManagementException the server exception is thrown in a failure when deleting the shared
     * applications.
     */
    default void deleteSharedAppLinks(String organizationId) throws OrganizationManagementException {

        return;
    }
}

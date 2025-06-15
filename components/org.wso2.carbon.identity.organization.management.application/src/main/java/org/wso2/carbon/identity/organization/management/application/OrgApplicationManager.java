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

import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationOrganizationNodePage;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareUpdateOperation;
import org.wso2.carbon.identity.organization.management.application.model.operation.GeneralApplicationShareOperation;
import org.wso2.carbon.identity.organization.management.application.model.operation.SelectiveShareApplicationOperation;
import org.wso2.carbon.identity.organization.management.service.exception.NotImplementedException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

import java.util.List;
import java.util.Map;

/**
 * Interface for Organization Application Management.
 */
public interface OrgApplicationManager {

    /**
     * Use {@link #shareApplicationWithSelectedOrganizations(String, String, List)} or
     * {@link #shareApplicationWithAllOrganizations(String, String, GeneralApplicationShareOperation)}
     * Share the application to all the child organizations or to a list of child organizations based on the user input.
     *
     * @param ownerOrgId           Identifier of the organization owning the application.
     * @param mainAppId            Identifier of the main application.
     * @param shareWithAllChildren Attribute indicating if the application is shared with all sub-organizations.
     * @param sharedOrgs           Optional list of identifiers of child organization to share the application.
     * @throws OrganizationManagementException on errors when sharing the application.
     */
    @Deprecated
    void shareOrganizationApplication(String ownerOrgId, String mainAppId, boolean shareWithAllChildren,
                                      List<String> sharedOrgs) throws OrganizationManagementException;

    /**
     * Selectively shares an application with specific sub-organizations.
     * <p>
     * This method allows you to share an application with specific organizations using either:
     * - SELECTED_ORG_ONLY: Shares with specified organizations only
     * - SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN: Shares with specified organizations and their existing
     * and future children
     * <p>
     * Features:
     * - Share with multiple organizations in a single operation.
     * - Configure roles that are shared with the organizations.
     * - Automatically enforces parent-child inheritance rules.
     *
     * @param mainOrganizationId The ID of the organization that owns the application
     * @param mainApplicationId The ID of the application to be shared
     * @param selectiveShareApplicationList List of organizations to share with, including their policies and roles.
     *                                      Important: You must include the complete organization hierarchy in this
     *                                      list. For example, to share with organization A.1.1, you must include
     *                                      both A.1 and A.1.1 in the list (order doesn't matter).
     * @throws OrganizationManagementException If any error occurs during the sharing process
     */
    default void shareApplicationWithSelectedOrganizations(String mainOrganizationId, String mainApplicationId,
                                                       List<SelectiveShareApplicationOperation>
                                                               selectiveShareApplicationList)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "selectiveShareOrganizationApplication method is not implemented in " + this.getClass().getName());
    }

    /**
     * Shares an application with all child organizations in a single operation.
     * <p>
     * This method allows you to share an application with all child organizations of the main organization.
     * and configure how roles should be applied universally to these organizations.
     *
     * @param mainOrganizationId ID of the organization that owns the application.
     * @param mainApplicationId ID of the application to be shared.
     * @param generalApplicationShare Contains the sharing policy and role configuration that will be
     *                               applied to all child organizations. Available policies:
     *                               - `ALL_EXISTING_ORGS_ONLY` - Share with existing child organizations only.
     *                               - `ALL_EXISTING_AND_FUTURE_ORGS` - Share with all existing and future child
     *                                organizations.
     * @throws OrganizationManagementException If the sharing operation fails.
     */
    default void shareApplicationWithAllOrganizations(String mainOrganizationId, String mainApplicationId,
                                                     GeneralApplicationShareOperation generalApplicationShare)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "generalOrganizationApplicationShare method is not implemented in " + this.getClass().getName());
    }

    /**
     * Shares an application with a specific organization using the given policy.
     * <p>
     * IMPORTANT: This method only shares with ONE organization at a time and does NOT cascade
     * the sharing to child organizations, even if a future-sharing policy is specified.
     * <p>
     * When to use this method:
     * - Use this method ONLY for single organization sharing with a specific policy
     * - For sharing with multiple organizations: use {@link #shareApplicationWithSelectedOrganizations}
     * - For sharing with all children: use {@link #shareApplicationWithAllOrganizations}
     * <p>
     * Hierarchical requirements:
     * - The application must already be shared with the parent organization
     * - The shared roles must be available in the parent organization
     *
     * @param ownerOrgId                 The organization ID that owns the application.
     * @param mainApplication            The application to be shared.
     * @param sharingOrgId               The organization ID to share the application with.
     * @param policyEnum                 The sharing policy to apply.
     * @param applicationShareRolePolicy The role sharing configuration.
     * @param operationId                (Optional) The async operation ID for tracking the sharing operation.
     *                                   Keep it null if you do not want to track the operation.
     * @throws OrganizationManagementException If the sharing operation fails
     */
    default void shareApplicationWithPolicy(String ownerOrgId, ServiceProvider mainApplication,
                                            String sharingOrgId, PolicyEnum policyEnum,
                                            ApplicationShareRolePolicy applicationShareRolePolicy, String operationId)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "shareApplicationWithPolicy method is not implemented in " + this.getClass().getName());
    }

    /**
     * Un-share the application from a list of organizations.
     *
     * @param mainOrganizationId     ID of the organization owning the primary application.
     * @param mainApplicationId      ID of the primary application.
     * @param sharedOrganizationList List of organization IDs to un-share the application from.
     * @throws OrganizationManagementException on errors when un-sharing the application.
     */
    default void unshareApplicationFromSelectedOrganizations(String mainOrganizationId, String mainApplicationId,
                                    List<String> sharedOrganizationList) throws OrganizationManagementException {

        throw new NotImplementedException(
                "unshareApplication method is not implemented in " + this.getClass().getName());
    }

    /**
     * Un-share the application from all organizations.
     *
     * @param mainOrganizationId     ID of the organization owning the primary application.
     * @param mainApplicationId ID of the primary application.
     * @throws OrganizationManagementException on errors when un-sharing the application.
     */
    default void unshareAllApplicationFromAllOrganizations(String mainOrganizationId, String mainApplicationId)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "unshareAllApplications method is not implemented in " + this.getClass().getName());
    }


    /**
     * Update the shared application with the given update operations.
     * As of now, this method supports updating the roles of the shared application.
     * You cannot use this to share the application with new organizations.
     *
     * @param mainOrganizationId  Main organization ID that owns the primary application.
     * @param mainApplicationId   Main application ID that needs to be updated.
     * @param updateOperationList List of update operations to be performed on the shared application. You have to
     *                            specify the operation type, path and values to be updated.
     * @throws OrganizationManagementException on errors when updating the shared application.
     */
    default void updateSharedApplication(String mainOrganizationId, String mainApplicationId,
                                         List<ApplicationShareUpdateOperation> updateOperationList)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "updateSharedApplication method is not implemented in " + this.getClass().getName());
    }

    /**
     * @deprecated Use {@link #unshareApplicationFromSelectedOrganizations(String, String, List)} or
     * {@link #unshareAllApplicationFromAllOrganizations(String, String)} instead.
     * Remove the shared (fragment) application for given organization to stop sharing the business application.
     *
     * @param organizationId       ID of the organization owning the primary application.
     * @param applicationId        ID of the primary application.
     * @param sharedOrganizationId organization ID which owns the fragment application.
     * @throws OrganizationManagementException on errors when removing the fragment application.
     */
    @Deprecated
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
     * Returns the list of organization with whom the primary application is shared. This method provides the
     * filtering, pagination and other options to retrieve the shared organizations.
     * Currently, this has the support to filter applications by organization id and name.
     *
     * @param mainOrganizationId ID of the main organization owning the main application.
     * @param mainApplicationId  Resource ID of the main application.
     * @param filter             (Optional) Filter to search for shared applications (optional). Currently, supports
     *                           filtering by organization id and parent organization id.
     *                           Ex: `id eq 088fb49c-46fa-48c1-a0a8-5538ee4b7ec5` or
     *                           `parentId eq 088fb49c-46fa-48c1-a0a8-5538ee4b7ec5`
     * @param beforeCursor        (Optional) The before cursor to get the previous page of results. This should
     *                           be the shared application id. NOTE: We always prioritize the before token over the
     *                           after cursor. Value cannot be 0.
     * @param afterCursor        (Optional) The after cursor to get the next page of results. This should be a shared
     *                           application id.
     * @param excludedAttributes (Optional) A comma separated list of attributes to be excluded from the result.
     *                           Currently, supports excluding `roles`.
     * @param limit              (Optional) The maximum number of results to be returned. If not specified
     *                           (that is, 0), it will return all the results.
     * @param recursive          (Optional) If true, it will return the shared organizations recursively. If false,
     *                           it will return only the immediate child organizations of the main organization.
     * @return A page of shared application organization nodes. It contains the shared application details along
     * with the next and previous cursor values
     * @throws OrganizationManagementException on errors occurred while retrieving the list of shared organizations.
     */
    default SharedApplicationOrganizationNodePage getApplicationSharedOrganizations(String mainOrganizationId,
                                                                                    String mainApplicationId,
                                                                                    String filter, int beforeCursor,
                                                                                    int afterCursor,
                                                                                    String excludedAttributes,
                                                                                    int limit, boolean recursive)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "getApplicationSharedOrganizations method is not implemented in " + this.getClass().getName());
    }

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
     * Use {@link #shareApplicationWithPolicy(String, ServiceProvider, String, PolicyEnum,
     * ApplicationShareRolePolicy, String)}
     * instead.
     * Share the application to an organization.
     *
     * @param ownerOrgId           Identifier of the organization owning the application.
     * @param sharedOrgId          Identifier of the organization to which the application being shared to.
     * @param mainApplication      The application which is shared with the child organizations.
     * @param shareWithAllChildren Boolean attribute indicating if the application is shared with all sub-organizations.
     * @throws OrganizationManagementException on errors when sharing the application.
     */
    @Deprecated
    void shareApplication(String ownerOrgId, String sharedOrgId, ServiceProvider mainApplication,
                          boolean shareWithAllChildren) throws OrganizationManagementException;

    /**
     * Use {@link #shareApplicationWithPolicy(String, ServiceProvider, String, PolicyEnum, ApplicationShareRolePolicy,
     * String)} instead.
     * Share the application to an organization.
     *
     * @param ownerOrgId           Identifier of the organization owning the application.
     * @param sharedOrgId          Identifier of the organization to which the application being shared to.
     * @param mainApplication      The application which is shared with the child organizations.
     * @param shareWithAllChildren Boolean attribute indicating if the application is shared with all sub-organizations.
     * @throws OrganizationManagementException on errors when sharing the application.
     */
    @Deprecated
    default void shareApplication(String ownerOrgId, String sharedOrgId, ServiceProvider mainApplication,
                          boolean shareWithAllChildren, String operationId) throws OrganizationManagementException {

        throw new NotImplementedException(
                "shareApplication method is not implemented in " + this.getClass().getName());
    };

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

    /**
     * Get the discoverable shared application basic info.
     *
     * @param limit        Maximum no of applications to be returned in the result set (optional).
     * @param offset       Zero based index of the first application to be returned in the result set (optional).
     * @param filter       Filter to search for applications (optional).
     * @param sortOrder    Sort order, ascending or descending (optional).
     * @param sortBy       Attribute to sort from (optional).
     * @param tenantDomain Tenant domain.
     * @return List of DiscoverableApplicationBasicInfo of applications matching the given criteria.
     * @throws OrganizationManagementException If an error occurred when retrieving the discoverable applications.
     */
    default List<ApplicationBasicInfo> getDiscoverableSharedApplicationBasicInfo(int limit, int offset, String filter,
                                                                           String sortOrder,
                                                                           String sortBy, String tenantDomain)
            throws OrganizationManagementException {

        return null;
    }

    /**
     * Get the count of discoverable shared applications.
     *
     * @param filter       Filter to search for applications (optional).
     * @param tenantDomain Tenant domain.
     * @return Count of discoverable applications matching given filter.
     * @throws OrganizationManagementException If an error occurred when retrieving the count of
     *                                         discoverable applications.
     */
    default int getCountOfDiscoverableSharedApplications(String filter, String tenantDomain)
            throws OrganizationManagementException {

        return 0;
    }

    /**
     * Check whether the main application has shared applications.
     *
     * @param mainApplicationId Main application ID.
     * @return True if the main application has shared applications.
     * @throws OrganizationManagementException If an error occurred when checking shared applications.
     */
    default boolean hasSharedApps(String mainApplicationId) throws OrganizationManagementException {

        return false;
    }
}

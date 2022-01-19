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

package org.wso2.carbon.identity.organization.management.service.dao;

import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;

import java.time.Instant;
import java.util.List;

/**
 * This interface performs CRUD operations for {@link Organization}
 */
public interface OrganizationManagementDAO {

    /**
     * Create new {@link Organization} in the database.
     *
     * @param tenantId     The tenant ID corresponding to the tenant where the organization should be created.
     * @param tenantDomain The tenant domain name corresponding to the tenant where the organization should be created.
     * @param organization The organization to be created.
     * @throws OrganizationManagementServerException The server exception thrown when creating an organization.
     */
    void addOrganization(int tenantId, String tenantDomain, Organization organization) throws
            OrganizationManagementServerException;

    /**
     * Check if the {@link Organization} exists by name in a given tenant.
     *
     * @param tenantId         The tenant ID corresponding to the tenant where the organization existence should be
     *                         checked.
     * @param organizationName The organization name.
     * @param tenantDomain     The tenant name corresponding to the tenant where the organization existence should be
     *                         checked.
     * @return true if the organization exists.
     * @throws OrganizationManagementServerException The server exception thrown when checking for the organization
     *                                               existence.
     */
    boolean isOrganizationExistByName(int tenantId, String organizationName, String tenantDomain) throws
            OrganizationManagementServerException;

    /**
     * Check if the {@link Organization} exists by organization Id in a given tenant.
     *
     * @param tenantId       The tenant ID corresponding to the tenant where the organization existence should be
     *                       checked.
     * @param organizationId The organization ID.
     * @param tenantDomain   The tenant name corresponding to the tenant where the organization existence should be
     *                       checked.
     * @return true if the organization exists.
     * @throws OrganizationManagementServerException The server exception thrown when checking for the organization
     *                                               existence.
     */
    boolean isOrganizationExistById(int tenantId, String organizationId, String tenantDomain) throws
            OrganizationManagementServerException;

    /**
     * Retrieve organization ID if the given organization name exists for the tenant.
     *
     * @param tenantId         The tenant ID corresponding to the tenant where the organization should be retrieved.
     * @param organizationName The organization name.
     * @param tenantDomain     The tenant name corresponding to the tenant where the organization should be retrieved.
     * @return the organization ID.
     * @throws OrganizationManagementServerException The server exception thrown when retrieving the organization ID.
     */
    String getOrganizationIdByName(int tenantId, String organizationName, String tenantDomain)
            throws OrganizationManagementServerException;

    /**
     * Retrieve {@link Organization} by ID in the given tenant.
     *
     * @param tenantId       The tenant ID corresponding to the tenant where the organization should be retrieved.
     * @param organizationId The organization ID.
     * @param tenantDomain   The tenant name corresponding to the tenant where the organization should be retrieved.
     * @return the organization object.
     * @throws OrganizationManagementServerException The server exception thrown when retrieving the organization.
     */
    Organization getOrganization(int tenantId, String organizationId, String tenantDomain) throws
            OrganizationManagementServerException;

    /**
     * Retrieve the IDs of the organizations residing in the given tenant.
     *
     * @param tenantId     The tenant ID corresponding to the tenant where the organizations should be retrieved.
     * @param tenantDomain The tenant name corresponding to the tenant where the organizations should be retrieved.
     * @return the list of organization IDs.
     * @throws OrganizationManagementServerException The server exception thrown when retrieving the organizations.
     */
    List<String> getOrganizationIds(int tenantId, String tenantDomain) throws
            OrganizationManagementServerException;

    /**
     * Delete {@link Organization} by ID.
     *
     * @param tenantId       The tenant ID corresponding to the tenant where the organization should be deleted.
     * @param organizationId The organization ID.
     * @param tenantDomain   The tenant name corresponding to the tenant where the organization should be deleted.
     * @throws OrganizationManagementServerException The server exception thrown when deleting the organization.
     */
    void deleteOrganization(int tenantId, String organizationId, String tenantDomain) throws
            OrganizationManagementServerException;

    /**
     * Check if an organization has child organizations.
     *
     * @param organizationId The organization ID.
     * @param tenantDomain   The tenant name corresponding to the tenant.
     * @return true if the organization has child organizations.
     * @throws OrganizationManagementServerException The server exception thrown when checking if an organization has
     *                                               child organizations.
     */
    boolean hasChildOrganizations(String organizationId, String tenantDomain) throws
            OrganizationManagementServerException;

    /**
     * Add, remove or replace organization fields and attributes.
     *
     * @param organizationId      The organization ID.
     * @param tenantDomain        The tenant name corresponding to the tenant.
     * @param lastModifiedInstant The last modified time.
     * @param patchOperations          The list of patch operations.
     * @throws OrganizationManagementServerException The server exception thrown when patching an organization.
     */
    void patchOrganization(String organizationId, String tenantDomain, Instant lastModifiedInstant,
                           List<PatchOperation> patchOperations) throws OrganizationManagementServerException;

    /**
     * Update {@link Organization} by ID.
     *
     * @param organizationId The organization ID.
     * @param tenantDomain   The tenant name corresponding to the tenant where the organization should be updated.
     * @param organization   The organization object.
     * @throws OrganizationManagementServerException The server exception thrown when updating an organization.
     */
    void updateOrganization(String organizationId, String tenantDomain, Organization organization) throws
            OrganizationManagementServerException;

    /**
     * Check if the organization has the given attribute.
     *
     * @param tenantDomain   The tenant name corresponding to the tenant where the organization resides.
     * @param organizationId The organization ID.
     * @param attributeKey   The attribute key of the organization.
     * @return true if the organization attribute exists.
     * @throws OrganizationManagementServerException The server exception thrown when checking if an organization
     *                                               attribute exists.
     */
    boolean isAttributeExistByKey(String tenantDomain, String organizationId, String attributeKey)
            throws OrganizationManagementServerException;

    /**
     * Retrieve the list of child organization IDs of a given organization.
     *
     * @param tenantId       The tenant ID corresponding to the tenant where the organization resides.
     * @param organizationId The organization ID.
     * @param tenantDomain   The tenant name corresponding to the tenant where the organization resides.
     * @param organization   The organization object.
     * @return the ID list of the child organizations.
     * @throws OrganizationManagementServerException The server exception thrown when retrieving the child
     *                                               organizations.
     */
    List<String> getChildOrganizationIds(int tenantId, String organizationId, String tenantDomain, Organization
            organization) throws OrganizationManagementServerException;
}

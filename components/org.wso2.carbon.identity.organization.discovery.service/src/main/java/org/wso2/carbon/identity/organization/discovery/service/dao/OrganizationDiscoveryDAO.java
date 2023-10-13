/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.discovery.service.dao;

import org.wso2.carbon.identity.organization.discovery.service.model.OrgDiscoveryAttribute;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.List;
import java.util.Map;

/**
 * This interface performs CRUD operations for organization discovery.
 */
public interface OrganizationDiscoveryDAO {

    /**
     * Add the organization discovery attributes of the given organization.
     *
     * @param organizationId      The organization ID.
     * @param discoveryAttributes The organization discovery attributes.
     * @throws OrganizationManagementServerException The server exception thrown when adding organization discovery
     *                                               attributes.
     */
    void addOrganizationDiscoveryAttributes(String organizationId, List<OrgDiscoveryAttribute> discoveryAttributes)
            throws OrganizationManagementServerException;

    /**
     * Check if the given discovery attribute is already mapped to an organization within the hierarchy.
     *
     * @param excludeCurrentOrganization Determines whether the current organization should be excluded or not for the
     *                                   data retrieval.
     * @param rootOrganizationId         The root organization ID.
     * @param currentOrganizationId      The current organization ID.
     * @param type                       The organization discovery attribute type.
     * @param values                     The organization discovery attribute values.
     * @return If the discovery attribute already exists within the hierarchy.
     * @throws OrganizationManagementServerException The server exception thrown when checking if the discovery
     *                                               attribute already exists within the hierarchy.
     */
    boolean isDiscoveryAttributeExistInHierarchy(boolean excludeCurrentOrganization, String rootOrganizationId,
                                                 String currentOrganizationId, String type, List<String> values)
            throws OrganizationManagementServerException;

    /**
     * Check if the given organization already has a discovery attribute mapped.
     *
     * @param organizationId The organization ID.
     * @return If the organization already has a discovery attribute mapped.
     * @throws OrganizationManagementServerException The server exception thrown when checking if the organization
     *                                               already has a discovery attribute mapped.
     */
    boolean isDiscoveryAttributeAddedToOrganization(String organizationId) throws OrganizationManagementServerException;

    /**
     * Fetch the discovery attributes of the organization.
     *
     * @param organizationId The organization ID.
     * @return The discovery attributes of the organization.
     * @throws OrganizationManagementServerException The server exception thrown when retrieving discovery attributes
     *                                               of the organization.
     */
    List<OrgDiscoveryAttribute> getOrganizationDiscoveryAttributes(String organizationId)
            throws OrganizationManagementServerException;

    /**
     * Delete the discovery attributes of the given organization.
     *
     * @param organizationId The organization ID.
     * @throws OrganizationManagementServerException The server exception thrown when deleting discovery attributes of
     *                                               the organization.
     */
    void deleteOrganizationDiscoveryAttributes(String organizationId) throws OrganizationManagementServerException;

    /**
     * Update the discovery attributes of the given organization.
     *
     * @param organizationId      The organization ID.
     * @param discoveryAttributes The organization discovery attributes.
     * @throws OrganizationManagementServerException The server exception thrown when updating discovery attributes of
     *                                               the organization.
     */
    void updateOrganizationDiscoveryAttributes(String organizationId, List<OrgDiscoveryAttribute> discoveryAttributes)
            throws OrganizationManagementServerException;

    /**
     * List the discovery attributes of all the organizations under the given root organization.
     *
     * @param rootOrganizationId The root organization ID.
     * @return The discovery attributes of the organizations.
     * @throws OrganizationManagementServerException The server exception thrown when listing discovery attributes of
     *                                               the organizations.
     */
    Map<String, List<OrgDiscoveryAttribute>> getOrganizationsDiscoveryAttributes(String rootOrganizationId) throws
            OrganizationManagementServerException;

    /**
     * Get the organization ID by discovery attribute in the hierarchy.
     *
     * @param attributeType      The organization discovery attribute type.
     * @param attributeValue     The organization discovery attribute value.
     * @param rootOrganizationId The root organization ID.
     * @return The organization ID.
     * @throws OrganizationManagementServerException The server exception thrown when fetching the organization ID.
     */
    String getOrganizationIdByDiscoveryAttribute(String attributeType, String attributeValue, String rootOrganizationId)
            throws OrganizationManagementServerException;
}

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

package org.wso2.carbon.identity.organization.discovery.service;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.organization.discovery.service.model.DiscoveryOrganizationsResult;
import org.wso2.carbon.identity.organization.discovery.service.model.OrgDiscoveryAttribute;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.List;
import java.util.Map;

/**
 * Interface for organization discovery management.
 */
public interface OrganizationDiscoveryManager {

    /**
     * Add the organization discovery attributes of the given organization.
     *
     * @param organizationId        The organization ID.
     * @param discoveryAttributes   The organization discovery attributes.
     * @param validateRootOrgAccess Determines whether validation is required for root organization access.
     * @return The added organization discovery attributes.
     * @throws OrganizationManagementException The exception thrown when adding organization discovery attributes.
     */
    List<OrgDiscoveryAttribute> addOrganizationDiscoveryAttributes(String organizationId, List<OrgDiscoveryAttribute>
            discoveryAttributes, boolean validateRootOrgAccess) throws OrganizationManagementException;

    /**
     * Fetch the discovery attributes of the organization.
     *
     * @param organizationId        The organization ID.
     * @param validateRootOrgAccess Determines whether validation is required for root organization access.
     * @return The discovery attributes of the organization.
     * @throws OrganizationManagementException The exception thrown when retrieving discovery attributes of the
     *                                         organization.
     */
    List<OrgDiscoveryAttribute> getOrganizationDiscoveryAttributes(String organizationId, boolean validateRootOrgAccess)
            throws OrganizationManagementException;

    /**
     * Delete the discovery attributes of the given organization.
     *
     * @param organizationId        The organization ID.
     * @param validateRootOrgAccess Determines whether validation is required for root organization access.
     * @throws OrganizationManagementException The exception thrown when deleting discovery attributes of the
     *                                         organization.
     */
    void deleteOrganizationDiscoveryAttributes(String organizationId, boolean validateRootOrgAccess)
            throws OrganizationManagementException;

    /**
     * Update the discovery attributes of the given organization.
     *
     * @param organizationId        The organization ID.
     * @param discoveryAttributes   The organization discovery attributes.
     * @param validateRootOrgAccess Determines whether validation is required for root organization access.
     * @return The updated organization discovery attributes.
     * @throws OrganizationManagementException The exception thrown when updating discovery attributes of the
     *                                         organization.
     */
    List<OrgDiscoveryAttribute> updateOrganizationDiscoveryAttributes(String organizationId, List<OrgDiscoveryAttribute>
            discoveryAttributes, boolean validateRootOrgAccess) throws OrganizationManagementException;

    /**
     * Check if the given discovery attribute is already mapped to an organization within the hierarchy.
     *
     * @param type  The organization discovery attribute type.
     * @param value The organization discovery attribute value.
     * @return If the discovery attribute already exists within the hierarchy.
     * @throws OrganizationManagementException The exception thrown when checking if the discovery attribute already
     *                                         exists within the hierarchy.
     */
    boolean isDiscoveryAttributeValueAvailable(String type, String value) throws OrganizationManagementException;

    /**
     * Check if the given discovery attribute is already mapped to an organization within the hierarchy under the given
     * organization.
     *
     * @param organizationId The organization ID.
     * @param type           The organization discovery attribute type.
     * @param value          The organization discovery attribute value.
     * @return If the discovery attribute already exists within the hierarchy.
     * @throws OrganizationManagementException The exception thrown when checking if the discovery attribute already
     *                                         exists within the hierarchy.
     */
    boolean isDiscoveryAttributeValueAvailable(String organizationId, String type, String value) throws
            OrganizationManagementException;

    /**
     * List the discovery attributes of all the organizations under the root organization.
     *
     * @param limit  The maximum number of records to be returned.
     * @param offset The number of records to skip for pagination.
     * @param filter The filter to be applied.
     * @return The discovery attributes of the organizations.
     * @throws OrganizationManagementException The exception thrown when listing discovery attributes of the
     *                                         organizations.
     */
    DiscoveryOrganizationsResult getOrganizationsDiscoveryAttributes(Integer limit, Integer offset, String filter)
            throws OrganizationManagementException;

    /**
     * Get attribute based organization discovery handlers.
     *
     * @return Handlers.
     */
    Map<String, AttributeBasedOrganizationDiscoveryHandler> getAttributeBasedOrganizationDiscoveryHandlers();

    /**
     * Get the organization ID by discovery attribute in the hierarchy.
     *
     * @param attributeType      The organization discovery attribute type.
     * @param discoveryInput     The organization discovery input.
     * @param rootOrganizationId The root organization ID.
     * @return The organization ID.
     * @throws OrganizationManagementException The server exception thrown when fetching the organization ID.
     */
    String getOrganizationIdByDiscoveryAttribute(String attributeType, String discoveryInput, String rootOrganizationId)
            throws OrganizationManagementException;

    /**
     * Get the organization ID by discovery attribute in the hierarchy.
     *
     * @param attributeType      The organization discovery attribute type.
     * @param discoveryInput     The organization discovery input.
     * @param rootOrganizationId The root organization ID.
     * @param context            The authentication context.
     * @return The organization ID.
     * @throws OrganizationManagementException The server exception thrown when fetching the organization ID.
     */
    default String getOrganizationIdByDiscoveryAttribute(String attributeType, String discoveryInput,
                                                         String rootOrganizationId, AuthenticationContext context)
            throws OrganizationManagementException {

        return getOrganizationIdByDiscoveryAttribute(attributeType, discoveryInput, rootOrganizationId);
    }
}

/*
 * Copyright (c) 2023-2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.config.service;

import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigException;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;
import org.wso2.carbon.identity.organization.config.service.model.OrganizationConfig;
import org.wso2.carbon.identity.organization.management.service.exception.NotImplementedException;

/**
 * Interface for organization configuration management.
 */
public interface OrganizationConfigManager {

    /**
     * Add the discovery configuration of the primary organization.
     *
     * @param discoveryConfig The discovery configuration.
     * @throws OrganizationConfigException The exception thrown when an error occurs while adding the discovery
     *                                     configuration.
     */
    void addDiscoveryConfiguration(DiscoveryConfig discoveryConfig) throws OrganizationConfigException;

    /**
     * Update the discovery configuration of the primary organization.
     *
     * @param discoveryConfig The discovery configuration.
     * @throws OrganizationConfigException The exception thrown when an error occurs while updating the discovery
     *                                     configuration.
     */
    default void updateDiscoveryConfiguration(DiscoveryConfig discoveryConfig) throws OrganizationConfigException {

        throw new NotImplementedException("updateDiscoveryConfiguration method is not implemented in " +
                this.getClass().getName());
    }

    /**
     * Fetch the discovery configuration of the primary organization.
     *
     * @return The discovery configuration.
     * @throws OrganizationConfigException The exception thrown when an error occurs while fetching the discovery
     *                                     configuration.
     */
    DiscoveryConfig getDiscoveryConfiguration() throws OrganizationConfigException;

    /**
     * Delete the discovery configuration of the primary organization.
     *
     * @throws OrganizationConfigException The exception thrown when an error occurs while deleting the discovery
     *                                     configuration.
     */
    void deleteDiscoveryConfiguration() throws OrganizationConfigException;

    /**
     * Fetch the discovery configuration by the tenant ID.
     *
     * @param tenantId The tenant ID.
     * @return The discovery configuration.
     * @throws OrganizationConfigException The exception thrown when an error occurs while fetching the discovery
     *                                     configuration.
     */
    DiscoveryConfig getDiscoveryConfigurationByTenantId(int tenantId) throws OrganizationConfigException;

    /**
     * Update all the organization configurations.
     *
     * @param organizationConfig The organization configuration.
     * @throws OrganizationConfigException The exception thrown when an error occurs while updating the organization
     *                                     configuration.
     */
    default void updateOrganizationConfiguration(OrganizationConfig organizationConfig)
            throws OrganizationConfigException {

    }

    /**
     * Fetch all the organization configurations.
     *
     * @return The organization configuration.
     * @throws OrganizationConfigException The exception thrown when an error occurs while fetching the organization
     *                                     configuration.
     */
    default OrganizationConfig getOrganizationConfiguration() throws OrganizationConfigException {

        throw new NotImplementedException(
                "getOrganizationConfiguration method is not implemented in " + this.getClass().getName());
    }
}

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

import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

/**
 * Interface for organization discovery type factories.
 */
public interface OrganizationDiscoveryTypeFactory {

    /**
     * Get the type of the organization discovery.
     *
     * @return The organization discovery type.
     */
    String getType();

    /**
     * Checks whether the organization discovery configuration is enabled in the given organization.
     *
     * @param organizationId The root organization ID.
     * @return If the organization discovery configuration is enabled.
     * @throws OrganizationManagementException The exception thrown when checking if organization discovery
     *                                         configuration is enabled.
     */
    boolean isDiscoveryConfigurationEnabled(String organizationId)
            throws OrganizationManagementException;
}

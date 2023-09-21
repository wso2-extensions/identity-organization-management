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

package org.wso2.carbon.identity.organization.config.service.constant;

/**
 * Contains constants related to organization configuration management.
 */
public class OrganizationConfigConstants {

    public static final String RESOURCE_TYPE_NAME = "ORGANIZATION_CONFIGURATION";
    public static final String RESOURCE_NAME = "OrganizationDiscovery";
    private static final String ORGANIZATION_CONFIGURATION_ERROR_CODE_PREFIX = "OCM-";

    /**
     * Enum for error messages related to organization management.
     */
    public enum ErrorMessages {

        // Client errors.
        ERROR_CODE_DISCOVERY_CONFIG_UPDATE_NOT_ALLOWED("60001", "Can't modify the organization discovery " +
                "configuration.", "Only the primary organization has the authority to modify the organization " +
                "discovery configuration."),
        ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST("60002", "No organization discovery configuration found.",
                "There is no organization discovery configuration for organization with ID: %s."),
        ERROR_CODE_DISCOVERY_CONFIG_CONFLICT("60003", "The organization discovery configuration already exists.",
                "The organization discovery configuration is already for available for the organization with id: %s."),

        // Server errors.
        ERROR_CODE_ERROR_ADDING_DISCOVERY_CONFIG("65001", "Unable to add the organization discovery " +
                "configuration.", "Server encountered an error while adding the organization discovery " +
                "configuration for organization with ID: %s."),
        ERROR_CODE_ERROR_RETRIEVING_DISCOVERY_CONFIG("65002", "Unable to retrieve the organization " +
                "discovery configuration.", "Server encountered an error while retrieving the organization discovery " +
                "configuration for organization with ID: %s."),
        ERROR_CODE_ERROR_DELETING_DISCOVERY_CONFIG("65003", "Unable to delete the organization discovery " +
                "configuration.", "Server encountered an error while deleting the organization discovery " +
                "configuration for the organization with id: %s");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return ORGANIZATION_CONFIGURATION_ERROR_CODE_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }
}

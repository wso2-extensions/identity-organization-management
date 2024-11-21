/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.constant;

/**
 * Constants for organization resource hierarchy traverse service.
 */
public class OrgResourceHierarchyTraverseConstants {

    private static final String ORGANIZATION_RESOURCE_HIERARCHY_TRAVERSE_ERROR_CODE_PREFIX = "ORHT-";

    private OrgResourceHierarchyTraverseConstants() {

    }

    /**
     * Enum for error messages related to organization resource hierarchy traverse service.
     */
    public enum ErrorMessages {

        // Server errors.
        ERROR_CODE_SERVER_ERROR_WHILE_RESOLVING_ANCESTOR_ORGANIZATIONS(
                "65001",
                "Unable to resolve ancestor organizations.",
                "Unexpected server error occurred " +
                        "while resolving ancestor organizations for organization with id: %s."),
        ERROR_CODE_SERVER_ERROR_WHILE_RESOLVING_ANCESTOR_APPLICATIONS(
                "65002",
                "Unable to resolve ancestor applications.",
                "Unexpected server error occurred while resolving ancestor applications for organization " +
                        "with id: %s for application with id: %s.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return ORGANIZATION_RESOURCE_HIERARCHY_TRAVERSE_ERROR_CODE_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }
}

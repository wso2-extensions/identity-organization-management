/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.executor;

/**
 * Constants related to the organization management flow executors.
 */
public class ExecutorConstants {

    /**
     * Enum for error messages.
     */
    protected enum ExecutorErrorMessages {

        ERROR_CODE_INVALID_ORGANIZATION_NAME("60001",
                "Invalid organization name.",
                "Organization name is not provided in the organization provisioning request of flow id: %s"),
        ERROR_CODE_ORGANIZATION_HANDLE_ALREADY_EXISTS("60002",
                "Organization handle already exists.",
                "The provided organization handle already exists. Please provide a different handle."),
        ERROR_CODE_ORGANIZATION_PROVISIONING_FAILURE("60003",
                "Error while provisioning organization.",
                "Error occurred while provisioning the organization in the request of flow id: %s"),
        ERROR_CODE_RESOLVE_PARENT_ORGANIZATION_FAILURE("65001",
                "Error while resolving the parent organization.",
                "Error occurred while resolving the parent organization of tenant: %s in the request of flow id: %s"),
        ERROR_CODE_ORGANIZATION_ONBOARD_FAILURE("65002",
                "Error while onboarding organization.",
                "Error occurred while onboarding the organization in the request of flow id: %s");

        private static final String ERROR_PREFIX = "OPE";
        private final String code;
        private final String message;
        private final String description;

        ExecutorErrorMessages(String code, String message, String description) {

            this.code = ERROR_PREFIX + "-" + code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }

        @Override
        public String toString() {

            return code + ":" + message;
        }
    }
}

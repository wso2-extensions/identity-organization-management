/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.constant;

/**
 * Contains constants related to organization application management.
 */
public class OrgApplicationMgtConstants {

    private static final String ORG_APPLICATION_MGT_ERROR_CODE_PREFIX = "OAM-";

    public static final String VIEW_SHARED_APP_ID = "SHARED_APP_ID";

    /**
     * Enum for error messages related to organization application management.
     */
    public enum ErrorMessages {
        ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION("65001", "Unable to resolve shared application",
                "Server encountered an error while resolving the application with ID: %s."),
        ERROR_CODE_ERROR_RETRIEVING_ORG_APPLICATION("65002", "Unable to retrieve the application",
                "Server encountered an error while retrieving the application with ID: %s."),
        ERROR_CODE_ERROR_RETRIEVING_SHARED_APP_ID("65003", "Unable to retrieve shared application ID.",
                "Server encountered an error while retrieving the shared application ID of " +
                        "parent application with ID: %s"),
        ERROR_CODE_ERROR_SHARING_APPLICATION("65004", "Unable to share the application.",
                "Server encountered an error while sharing the application."),;

        private final String code;
        private final String message;
        private final String description;

        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return ORG_APPLICATION_MGT_ERROR_CODE_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }
}

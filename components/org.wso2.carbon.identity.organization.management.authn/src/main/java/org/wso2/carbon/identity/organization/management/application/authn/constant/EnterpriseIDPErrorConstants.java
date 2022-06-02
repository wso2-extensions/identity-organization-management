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

package org.wso2.carbon.identity.organization.management.application.authn.constant;

/**
 * This class holds the constants related with enterprise login authenticator.
 */
public class EnterpriseIDPErrorConstants {

    public static final String ENTERPRISE_LOGIN_FAILURE = "enterpriseLoginFailure";
    public static final String ERROR_MESSAGE = "&authFailure=true&authFailureMsg=";

    public static final String REQUEST_ORG_PAGE_URL = "authenticationendpoint/org_name.do";
    public static final String REQUEST_ORG_PAGE_URL_CONFIG = "RequestOrganizationPage";


    /**
     * Relevant error messages and error codes.
     */
    public enum ErrorMessages {

        ORG_PARAMETER_NOT_FOUND("ELA-66001",
                "Cannot find the org parameter in the request."),
        ENTERPRISE_IDP_LOGIN_FAILED("ELA-66004", "Enterprise IDP Login failed."),
        ORG_NOT_FOUND("ELA-66005", "Organization could not be found.");
        private final String code;
        private final String message;

        /**
         * Create an Error Message.
         *
         * @param code    Relevant error code.
         * @param message Relevant error message.
         */
        ErrorMessages(String code, String message) {

            this.code = code;
            this.message = message;
        }

        /**
         * To get the code of specific error.
         *
         * @return Error code.
         */
        public String getCode() {

            return code;
        }

        /**
         * To get the message of specific error.
         *
         * @return Error message.
         */
        public String getMessage() {

            return message;
        }

        @Override
        public String toString() {

            return String.format("%s  - %s", code, message);
        }
    }
}

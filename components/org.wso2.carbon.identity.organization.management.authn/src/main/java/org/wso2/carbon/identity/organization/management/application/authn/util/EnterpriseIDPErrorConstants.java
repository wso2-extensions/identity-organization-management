/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.application.authn.util;

/**
 * This class holds the constants related with enterprise login authenticator.
 */
public class EnterpriseIDPErrorConstants {

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

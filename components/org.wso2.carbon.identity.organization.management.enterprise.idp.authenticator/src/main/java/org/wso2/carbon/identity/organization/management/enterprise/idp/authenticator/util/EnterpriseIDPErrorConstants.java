/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.enterprise.idp.authenticator.util;

/**
 * This class holds the constants related with enterprise login authenticator.
 */
public class EnterpriseIDPErrorConstants {

    /**
     * Relevant error messages and error codes.
     */
    public enum ErrorMessages {

        USERNAME_PARAMETER_NOT_FOUND("ELA-66001",
                "Cannot find the username parameter in the request."),
        USERNAME_IS_NOT_AN_EMAIL_ERROR("ELA-66002",
                "The username in the request should be an email address."),
        EMAIL_DOMAIN_IS_NOT_REGISTERED_FOR_ENTERPRISE_IDP_LOGIN("ELA-66003",
                "The email domain is not registered for Enterprise IDP Login."),
        ENTERPRISE_IDP_LOGIN_FAILED("ELA-66004", "Enterprise IDP Login failed."),
        ORG_NOT_FOUND("ELA-66005", "Organization could not be found."),
        EMAIL_DOMAIN_NOT_ASSOCIATED("ELA-66006",
                "The organization is not associated with the email domain.");
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

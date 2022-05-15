/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.enterprise.login.mgt.core.common;

import java.util.regex.Pattern;

/**
 * Class for constants and error messages.
 */
public class Constants {

    public static final String ENTERPRISE_LOGIN_MGT_SP_TEMPLATE_ID = "custom-application-oidc";
    public static final String ENTERPRISE_LOGIN_MGT_SP_NAME = "WSO2_LOGIN_FOR_";
    public static final String ENTERPRISE_LOGIN_MGT_SP_DESC = "WSO2 enterprise login management application for ";
    public static final String OAUTH2_VERSION = "OAuth-2.0";
    public static final Pattern EMAIL_DOMAIN_REGEX = Pattern.compile("^(?![+.\\-_])(?:(?![.+\\-_]{2})[\\w.+\\-]){1," +
            "245}(?<![+.\\-_])\\.[a-zA-Z]{2,10}$");
    public static final String EMAIL_DOMAIN = "EMAIL_DOMAIN";
    public static final String INBOUND_SP_RESOURCE_ID = "INBOUND_SP_ID";
    public static final String INBOUND_SP_TENANT_ID = "INBOUND_SP_TENANT_ID";
    public static final String OUTBOUND_SP_TENANT_ID = "OUTBOUND_SP_TENANT_ID";
    public static final String OUTBOUND_SP_RESOURCE_ID = "OUTBOUND_SP_ID";
    public static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";


    /**
     * Enum for enterprise login management related errors.
     * Error Code - code to identify the error.
     * Error Message - What went wrong.
     */
    public enum ErrorMessage {

        ERROR_CODE_SERVICE_NOT_FOUND("ELM-65001", "Service does not exists or invalid."),
        ERROR_CODE_SERVICE_NOT_PRESENT("ELM-65002", "Services not present."),
        ERROR_CODE_INVALID_ORG("ELM-65003", "The organization does not exist or invalid."),
        ERROR_CODE_SERVICE_ALREADY_REGISTERED("ELM-65004", "Service(s) already registered."),
        ERROR_CODE_SERVER_ERROR("ELM-65005", "Unexpected server error."),
        ERROR_CODE_EMPTY_ORG("ELM-65006", "Organization can not be empty."),
        ERROR_CODE_EMPTY_INBOUND_SP_TENANT("ELM-65007",
                "Organization of the service can not be empty."),
        ERROR_CODE_EMPTY_SERVICE("ELM-65008", "Service can not be empty."),
        ERROR_CODE_EMAIL_NOT_PRESENT("ELM-65009", "Email domain not present."),
        ERROR_CODE_ENTERPRISE_APP_NOT_EXIST("ELM-65010",
                "Error while accessing enterprise login configurations."),
        ERROR_PERSISTING_CONFIG_MAPPINGS("ELM-65011", "Error occurred while configuring enterprise login."),
        ERROR_RETRIEVE_CONFIG_MAPPINGS("ELM-65012", "Error occurred while retrieving configurations."),
        ERROR_DELETING_CONFIG_MAPPING("ELM-65013", "Error occurred while deleting configurations."),
        ERROR_RETRIEVING_ORG("ELM-65014", "Error occurred while retrieving organizations."),
        ERROR_DELETING_EMAIL_DOMAIN("ELM-65015", "Error occurred while deleting email domains."),
        ERROR_ADDING_EMAIL_DOMAIN("ELM-65016", "Error occurred while adding email domains."),
        ERROR_RETRIEVE_EMAIL_DOMAIN("ELM-65017", "Error occurred while retrieving email domains."),
        ERROR_RETRIEVE_OUTBOUND_SP("ELM-65018", "Error occurred while retrieving enterprise login " +
                "configurations."),
        ERROR_CODE_NO_CONFIGS("ELM-65019", "No existing configurations to update."),
        ERROR_CODE_INVALID_EMAIL_DOMAIN("ELM-65020", "Invalid email domain."),
        ERROR_CODE_NO_VALID_OUTBOUND_SP("ELM-65021", "Enterprise login configurations do not exist.");

        private final String code;
        private final String message;

        ErrorMessage(String code, String message) {

            this.code = code;
            this.message = message;
        }

        public String getCode() {

            return code;
        }

        public String getMessage() {

            return message;
        }

        @Override
        public String toString() {

            return code + " | " + message;
        }
    }
}

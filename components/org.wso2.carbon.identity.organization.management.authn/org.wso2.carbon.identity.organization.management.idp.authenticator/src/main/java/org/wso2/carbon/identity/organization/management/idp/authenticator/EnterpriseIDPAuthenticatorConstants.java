/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.idp.authenticator;

/**
 * Class for constants.
 */
public class EnterpriseIDPAuthenticatorConstants {

    private EnterpriseIDPAuthenticatorConstants() {
    }

    public static final String AUTHENTICATOR_NAME = "EnterpriseIDPAuthenticator";
    public static final String AUTHENTICATOR_FRIENDLY_NAME = "EnterpriseIDP";

    public static final String AUTHORIZATION_ENDPOINT_TENANTED_PATH = "t/{tenant}/oauth2/authorize";
    public static final String TOKEN_ENDPOINT_TENANTED_PATH = "t/{tenant}/oauth2/token";
    public static final String USERINFO_ENDPOINT_TENANTED_PATH = "t/{tenant}/oauth2/userinfo";
    public static final String TENANT_PLACEHOLDER = "{tenant}";

    public static final String EMAIL_DOMAIN_SEPARATOR = "@";
    public static final String USERNAME_PARAMETER = "username";
    public static final String ORGANIZATION_ATTRIBUTE = "Organization";
    public static final String ORGANIZATION_USER_ATTRIBUTE = "org";
    public static final String ACCOUNT_CHOOSE_PROMPT_PAGE = "/authenticationendpoint/accountChooser.jsp";
    public static final String ORG_PARAMETER = "org";
}

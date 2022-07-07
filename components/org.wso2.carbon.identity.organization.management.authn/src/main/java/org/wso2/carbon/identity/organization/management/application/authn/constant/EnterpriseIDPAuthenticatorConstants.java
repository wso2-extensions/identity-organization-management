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
 * Class for constants.
 */
public class EnterpriseIDPAuthenticatorConstants {

    private EnterpriseIDPAuthenticatorConstants() {
    }

    public static final String AUTHENTICATOR_NAME = "EnterpriseIDPAuthenticator";
    public static final String AUTHENTICATOR_FRIENDLY_NAME = "EnterpriseIDP";

    public static final String AUTHORIZATION_ENDPOINT_TENANTED_PATH = "o/{orgId}/oauth2/authorize";
    public static final String TOKEN_ENDPOINT_TENANTED_PATH = "o/{orgId}/oauth2/token";
    public static final String TENANT_PLACEHOLDER = "{tenant}";
    public static final String ORGANIZATION_ID_PLACEHOLDER = "{orgId}";

    public static final String ORGANIZATION_ATTRIBUTE = "Organization";
    public static final String ORGANIZATION_USER_ATTRIBUTE = "org";
    public static final String ORG_PARAMETER = "org";
    public static final String IDP_PARAMETER = "idp";
    public static final String AUTHENTICATOR_PARAMETER = "authenticator";
    public static final String ORG_LIST_PARAMETER = "orgList";
    public static final String CLIENT_ID_PARAMETER = "client_id";
    public static final String REDIRECT_URI_PARAMETER = "redirect_uri";
    public static final String RESPONSE_TYPE_PARAMETER = "response_type";
    public static final String SCOPE_PARAMETER = "scope";
    public static final String ORG_ID_PARAMETER = "orgId";
    public static final String ORG_NAME_PARAMETER = "orgName";

    public static final String ENTERPRISE_LOGIN_FAILURE = "enterpriseLoginFailure";
    public static final String ERROR_MESSAGE = "&authFailure=true&authFailureMsg=";

    public static final String REQUEST_ORG_PAGE_URL = "authenticationendpoint/org_name.do";
    public static final String REQUEST_ORG_SELECT_PAGE_URL = "authenticationendpoint/select_org.do";
    public static final String REQUEST_ORG_PAGE_URL_CONFIG = "RequestOrganizationPage";
    public static final String INBOUND_AUTH_TYPE_OAUTH = "oauth2";

    public static final String UTF_8 = "UTF-8";
    public static final String EQUAL_SIGN = "=";
    public static final String AMPERSAND_SIGN = "&";

}

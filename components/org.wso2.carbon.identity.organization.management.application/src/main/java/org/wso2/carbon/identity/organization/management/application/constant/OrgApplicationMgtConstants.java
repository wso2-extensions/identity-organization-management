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

package org.wso2.carbon.identity.organization.management.application.constant;

/**
 * Contains constants related to organization application management.
 */
public class OrgApplicationMgtConstants {

    public static final String TENANT = "TENANT";
    public static final String AUTH_TYPE_OAUTH_2 = "oauth2";
    public static final String IS_FRAGMENT_APP = "isFragmentApp";
    public static final String SHARE_WITH_ALL_CHILDREN = "shareWithAllChildren";

    public static final String ORGANIZATION_LOGIN_AUTHENTICATOR = "OrganizationAuthenticator";
    public static final String DELETE_FRAGMENT_APPLICATION = "deleteFragmentApplication";
    public static final String DELETE_MAIN_APPLICATION = "deleteMainApplication";
    public static final String UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN = "updateShareWithAllChildren";
    public static final String DELETE_SHARE_FOR_MAIN_APPLICATION = "deleteShareForMainApp";

    public static final String FEDERATED_ORG_CLAIM_URL =  "http://wso2.org/claims/runtime/federated_org";
    public static final String FEDERATED_ORG_CLAIM_DISPLAY_NAME = "federated_org";
    public static final String OIDC_CLAIM_DIALECT_URI = "http://wso2.org/oidc/claim";

}

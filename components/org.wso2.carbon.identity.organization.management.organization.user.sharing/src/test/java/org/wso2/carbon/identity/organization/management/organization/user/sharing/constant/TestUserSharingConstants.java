/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.constant;

/**
 * Constants related to the test cases of User Sharing Policy Handler service.
 */
public class TestUserSharingConstants {

    // Fields.
    public static final String FIELD_USER_ID_RESOLVER = "userIDResolver";
    public static final String TENANT_DOMAIN = "tenantDomain";
    public static final int TENANT_ID = -1234;
    public static final String USER_DOMAIN_PRIMARY = "PRIMARY";
    public static final String USER_NAME_PREFIX = "username-of-";
    public static final String PATH_SEPARATOR = "/";

    // Organizations.
    public static final String ORG_SUPER_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    public static final String ORG_1_ID = "c524c30a-cbd4-4169-ac9d-1ee3edf1bf16";
    public static final String ORG_2_ID = "cd5a1dcb-fff2-4c14-a073-c07b3caf1757";
    public static final String ORG_3_ID = "7cb4ab7e-9a25-44bd-a9e0-cf4e07d804dc";

    public static final String ORG_1_NAME = "org1";
    public static final String ORG_2_NAME = "org2";
    public static final String ORG_3_NAME = "org3";

    // Applications.
    public static final String APP_1_NAME = "App1";
    public static final String APP_2_NAME = "App2";

    public static final String APPLICATION_AUDIENCE = "APPLICATION";
    public static final String ORGANIZATION_AUDIENCE = "ORGANIZATION";

    // Roles.
    public static final String APP_ROLE_1_ID = "3f2a1d7e-89c6-4e3f-9c1b-8a4f6b3e5d91";
    public static final String APP_ROLE_1_NAME = "app-role-1";
    public static final String APP_ROLE_2_ID = "b7e9f3d2-6c45-4128-928c-5f91e3a8d7b2";
    public static final String APP_ROLE_2_NAME = "app-role-2";

    public static final String ORG_ROLE_1_ID = "e5c1a7f8-4d92-44b1-bc3e-8d2f6a9e1b74";
    public static final String ORG_ROLE_1_NAME = "org-role-1";
    public static final String ORG_ROLE_2_ID = "a3d6f2c8-9b71-482e-91c5-7e4d8b2a3f69";
    public static final String ORG_ROLE_2_NAME = "org-role-2";

    // Users.
    public static final String USER_1_ID = "448e57e7-ff6b-4c31-a1eb-2a0e2d635b2a";
    public static final String USER_2_ID = "558e57e7-ff6b-4c31-a1eb-2a0e2d635b2a";
    public static final String USER_3_ID = "668e57e7-ff6b-4c31-a1eb-2a0e2d635b2b";
    public static final String USER_4_ID = "a35cd594-8408-4692-a061-56730b62ba27";
    public static final String USER_5_ID = "c1f7d4a8-92b3-47e1-8c5d-6e9a3f2b7d64";

    // Validate Messages.
    public static final String VALIDATE_MSG_RESPONSE = "Response should not be null.";
    public static final String VALIDATE_MSG_RESPONSE_SHARED_ORGS_COUNT = "Mismatch in shared organizations count.";
    public static final String VALIDATE_MSG_SHARED_ORG_ID = "Mismatched in shared org id.";
    public static final String VALIDATE_MSG_SHARED_USER_ID = "Mismatched in shared user id.";
    public static final String VALIDATE_MSG_SHARED_TYPE = "Mismatched in shared type.";
    public static final String VALIDATE_MSG_SHARED_ORG_NAME = "Mismatched in shared org name.";
    public static final String VALIDATE_MSG_RESPONSE_SHARED_ROLES_COUNT = "Role count mismatch";
    public static final String VALIDATE_MSG_SHARED_ROLE_NAME = "Mismatch in shared role name.";
    public static final String VALIDATE_MSG_SHARED_ROLE_AUDIENCE_NAME = "Mismatch in shared role audience name.";
    public static final String VALIDATE_MSG_SHARED_ROLE_AUDIENCE_TYPE = "Mismatch in shared role audience type.";
    public static final String VALIDATE_MSG_EXCEPTION = "Simulated Exception";
}

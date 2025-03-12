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

    // Organizations.
    public static final String ORG_SUPER_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    public static final String ORG_1_ID = "c524c30a-cbd4-4169-ac9d-1ee3edf1bf16";
    public static final String ORG_2_ID = "cd5a1dcb-fff2-4c14-a073-c07b3caf1757";
    public static final String ORG_3_ID = "7cb4ab7e-9a25-44bd-a9e0-cf4e07d804dc";

    public static final String ORG_1_NAME = "org1";
    public static final String ORG_2_NAME = "org2";
    public static final String ORG_3_NAME = "org3";

    // Users.
    public static final String USER_1_ID = "448e57e7-ff6b-4c31-a1eb-2a0e2d635b2a";
    public static final String USER_2_ID = "558e57e7-ff6b-4c31-a1eb-2a0e2d635b2a";
    public static final String USER_3_ID = "668e57e7-ff6b-4c31-a1eb-2a0e2d635b2b";

    // Mocks.
    public static final String MOCKED_DATA_ACCESS_EXCEPTION = "Mocked DataAccessException";
    public static final String MOCKED_TRANSACTION_EXCEPTION = "Mocked TransactionException";

    // Validate Messages.
    public static final String VALIDATE_MESSAGE_RESPONSE = "Response should not be null.";
    public static final String VALIDATE_MESSAGE_RESPONSE_SHARED_ORGS = "Mismatch in shared organizations count";
}

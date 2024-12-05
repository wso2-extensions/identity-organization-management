/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;

/**
 * Constants related to the test cases of Resource Sharing policy Management component.
 */
public class TestResourceSharingConstants {

    //Organizations.
    public static final String UM_ID_ORGANIZATION_SUPER = "10084a8d-113f-4211-a0d5-efe36b082211";
    public static final String UM_ID_ORGANIZATION_ORG_ALL = "c524c30a-cbd4-4169-ac9d-1ee3edf1bf16";
    public static final String UM_ID_ORGANIZATION_ORG_ALL_CHILD1 = "cd5a1dcb-fff2-4c14-a073-c07b3caf1757";
    public static final String UM_ID_ORGANIZATION_ORG_IMMEDIATE = "7cb4ab7e-9a25-44bd-a9e0-cf4e07d804dc";
    public static final String UM_ID_ORGANIZATION_ORG_IMMEDIATE_CHILD1 = "440ad5b6-6a41-4da7-aadd-272995d0e5db";

    //Invalid Organizations.
    public static final String UM_ID_ORGANIZATION_INVALID = "abcdefgh-0123-ijkl-4563-mnopqrstuvwx";
    public static final String UM_ID_ORGANIZATION_INVALID_FORMAT = "12'3";

    //Resources.
    public static final String UM_ID_RESOURCE_1 = "448e57e7-ff6b-4c31-a1eb-2a0e2d635b2a";
    public static final String UM_ID_RESOURCE_2 = "558e57e7-ff6b-4c31-a1eb-2a0e2d635b2a";
    public static final String UM_ID_RESOURCE_3 = "668e57e7-ff6b-4c31-a1eb-2a0e2d635b2b";
    public static final String UM_ID_RESOURCE_4 = "778e57e7-ff6b-4c31-a1eb-2a0e2d635b2c";

    //Resources Types.
    public static final ResourceType RESOURCE_TYPE_RESOURCE_1 = ResourceType.USER;

    //Resource Attributes.
    public static final String UM_ID_RESOURCE_ATTRIBUTE_1 = "daea2340-4686-4929-b0c3-aad28237b065";
    public static final String UM_ID_RESOURCE_ATTRIBUTE_2 = "daea2341-4686-4929-b0c3-aad28237b065";

    //Resource Attributes Types.
    public static final SharedAttributeType SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1 = SharedAttributeType.ROLE;

    //Mocks
    public static final String MOCKED_DATA_ACCESS_EXCEPTION = "Mocked DataAccessException";
    public static final String MOCKED_TRANSACTION_EXCEPTION = "Mocked TransactionException";

}

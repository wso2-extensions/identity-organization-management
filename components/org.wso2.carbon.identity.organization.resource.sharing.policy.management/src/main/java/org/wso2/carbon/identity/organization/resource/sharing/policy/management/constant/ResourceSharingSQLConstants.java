/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant;

/**
 * SQL constants for resource sharing policy management.
 */
public class ResourceSharingSQLConstants {

    // SQL for creating a resource sharing policy
    public static final String CREATE_RESOURCE_SHARING_POLICY = "INSERT INTO UM_RESOURCE_SHARING_POLICY" +
            "(UM_RESOURCE_ID, UM_RESOURCE_TYPE, UM_INITIATED_ORG_ID, UM_POLICY_HOLDING_ORG_ID, UM_SHARING_POLICY) " +
            "VALUES(?, ?, ?, ?, ?)";

    // SQL for deleting a resource sharing policy
    public static final String DELETE_RESOURCE_SHARING_POLICY = "DELETE FROM UM_RESOURCE_SHARING_POLICY " +
            "WHERE UM_RESOURCE = ? AND UM_RESOURCE_TYPE = ? AND UM_INITIATED_ORG = ? AND UM_POLICY_HOLDING_ORG = ?";
}

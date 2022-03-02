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

package org.wso2.carbon.identity.organization.management.role.mgt.core.constants;

/**
 * Constant for Organization-User-Role event handler.
 */
public class OrganizationUserRoleEventConstants {

    public static final String PRE_ASSIGN_ORGANIZATION_USER_ROLE = "PRE_ASSIGN_ORGANIZATION_USER_ROLE";
    public static final String POST_ASSIGN_ORGANIZATION_USER_ROLE = "POST_ASSIGN_ORGANIZATION_USER_ROLE";
    public static final String PRE_REVOKE_ORGANIZATION_USER_ROLE = "PRE_REVOKE_ORGANIZATION_USER_ROLE";
    public static final String POST_REVOKE_ORGANIZATION_USER_ROLE = "POST_REVOKE_ORGANIZATION_USER_ROLE";

    public static final String USER_NAME = "username";
    public static final String TENANT_DOMAIN = "tenantDomain";
    public static final String USER_ID = "userId";
    public static final String ORGANIZATION_ID = "organizationId";
    public static final String DATA = "data";
    public static final String STATUS = "status";

    /**
     * Status of the user's Organization Management action.
     */
    public enum Status {
        SUCCESS("Success"),
        FAILURE("Failure");

        private final String status;

        Status (String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}


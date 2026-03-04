/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.RoleAssignmentMode;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

import java.util.List;

/**
 * Abstract class that contains the common fields for user share operations.
 */
public abstract class BaseUserShare {

    private String userId;
    private PolicyEnum policy;
    private List<String> roles;
    private RoleAssignmentMode roleAssignmentMode; // Used in user-sharing V2.

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public PolicyEnum getPolicy() {

        return policy;
    }

    public void setPolicy(PolicyEnum policy) {

        this.policy = policy;
    }

    public List<String> getRoles() {

        return roles;
    }

    public void setRoles(List<String> roles) {

        this.roles = roles;
    }

    public RoleAssignmentMode getRoleAssignmentMode() {

        return roleAssignmentMode;
    }

    public void setRoleAssignmentMode(RoleAssignmentMode roleAssignmentMode) {

        this.roleAssignmentMode = roleAssignmentMode;
    }
}

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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

/**
 * Data object that represents the application sharing mode, including the sharing policy and the role-sharing policy
 * configuration. in user-sharing v2.
 */
public class SharingModeDO {

    private PolicyEnum policy;
    private RoleAssignmentDO roleAssignment;

    public SharingModeDO(PolicyEnum policy, RoleAssignmentDO roleAssignment) {

        this.policy = policy;
        this.roleAssignment = roleAssignment;
    }

    public PolicyEnum getPolicy() {

        return policy;
    }

    public void setPolicy(PolicyEnum policy) {

        this.policy = policy;
    }

    public RoleAssignmentDO getRoleAssignment() {

        return roleAssignment;
    }

    public void setRoleAssignment(
            RoleAssignmentDO roleAssignment) {

        this.roleAssignment = roleAssignment;
    }

    /**
     * This class is used to build the SharingModeDO object.
     */
    public static class Builder {

        private PolicyEnum policy;
        private RoleAssignmentDO roleAssignment;

        public Builder policy(PolicyEnum policy) {

            this.policy = policy;
            return this;
        }

        public Builder applicationShareRolePolicy(RoleAssignmentDO roleAssignment) {

            this.roleAssignment = roleAssignment;
            return this;
        }

        public SharingModeDO build() {

            return new SharingModeDO(policy, roleAssignment);
        }
    }
}

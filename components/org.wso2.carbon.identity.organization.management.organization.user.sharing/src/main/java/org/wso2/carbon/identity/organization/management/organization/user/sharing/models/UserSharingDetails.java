/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.organization.management.organization.user.sharing.models;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model that contains the user sharing details data object.
 */
public class UserSharingDetails {

    private String userIdOfSharingUser;
    private String sharingInitOrgId;
    private String targetOrgId;
    private String userIdOfMainUser;
    private String usernameOfMainUser;
    private String orgIdOfMainUser;
    private String sharingType;
    private List<String> roleIds;
    private PolicyEnum policy;

    private UserSharingDetails(Builder builder) {

        this.userIdOfSharingUser = builder.sharingUserId;
        this.sharingInitOrgId = builder.sharingInitOrgId;
        this.targetOrgId = builder.targetOrgId;
        this.userIdOfMainUser = builder.userIdOfMainUser;
        this.usernameOfMainUser = builder.usernameOfMainUser;
        this.orgIdOfMainUser = builder.orgIdOfMainUser;
        this.sharingType = builder.sharingType;
        this.roleIds = builder.roleIds;
        this.policy = builder.policy;
    }

    public String getUserIdOfSharingUser() {

        return userIdOfSharingUser;
    }

    public String getSharingInitOrgId() {

        return sharingInitOrgId;
    }

    public String getTargetOrgId() {

        return targetOrgId;
    }

    public String getUserIdOfMainUser() {

        return userIdOfMainUser;
    }

    public String getUsernameOfMainUser() {

        return usernameOfMainUser;
    }

    public String getOrgIdOfMainUser() {

        return orgIdOfMainUser;
    }

    public String getSharingType() {

        return sharingType;
    }

    public List<String> getRoleIds() {

        return roleIds;
    }

    public PolicyEnum getPolicy() {

        return policy;
    }

    public void setUserIdOfSharingUser(String userIdOfSharingUser) {

        this.userIdOfSharingUser = userIdOfSharingUser;
    }

    public void setSharingInitOrgId(String sharingInitOrgId) {

        this.sharingInitOrgId = sharingInitOrgId;
    }

    public void setTargetOrgId(String targetOrgId) {

        this.targetOrgId = targetOrgId;
    }

    public void setUserIdOfMainUser(String userIdOfMainUser) {

        this.userIdOfMainUser = userIdOfMainUser;
    }

    public void setUsernameOfMainUser(String usernameOfMainUser) {

        this.usernameOfMainUser = usernameOfMainUser;
    }

    public void setOrgIdOfMainUser(String orgIdOfMainUser) {

        this.orgIdOfMainUser = orgIdOfMainUser;
    }

    public void setSharingType(String sharingType) {

        this.sharingType = sharingType;
    }

    public void setRoleIds(List<String> roleIds) {

        this.roleIds = roleIds;
    }

    public void setPolicy(PolicyEnum policy) {

        this.policy = policy;
    }

    /**
     * Builder class for UserSharingDetails.
     */
    public static class Builder {

        private String sharingUserId = "";
        private String sharingInitOrgId = "";
        private String targetOrgId = "";
        private String userIdOfMainUser = "";
        private String usernameOfMainUser = "";
        private String orgIdOfMainUser = "";
        private String sharingType = "";
        private List<String> roleIds = Collections.emptyList();
        private PolicyEnum policy = PolicyEnum.NO_SHARING;

        public Builder withUserIdOfSharingUser(String sharingUserId) {

            this.sharingUserId = sharingUserId != null ? sharingUserId : "";
            return this;
        }

        public Builder withSharingInitiatedOrgId(String sharingInitiatedOrgId) {

            this.sharingInitOrgId = sharingInitiatedOrgId != null ? sharingInitiatedOrgId : "";
            return this;
        }

        public Builder withTargetOrgId(String targetOrgId) {

            this.targetOrgId = targetOrgId != null ? targetOrgId : "";
            return this;
        }

        public Builder withUserIdOfMainUser(String userIdOfMainUser) {

            this.userIdOfMainUser = userIdOfMainUser != null ? userIdOfMainUser : "";
            return this;
        }

        public Builder withUsernameOfMainUser(String usernameOfMainUser) {

            this.usernameOfMainUser = usernameOfMainUser != null ? usernameOfMainUser : "";
            return this;
        }

        public Builder withOrganizationIdOfMainUser(String orgIdOfMainUser) {

            this.orgIdOfMainUser = orgIdOfMainUser != null ? orgIdOfMainUser : "";
            return this;
        }

        public Builder withSharingType(String sharingType) {

            this.sharingType = sharingType != null ? sharingType : "Not Specified";
            return this;
        }

        public Builder withRoleIds(List<String> roleIds) {
            this.roleIds = roleIds != null ? new ArrayList<>(roleIds) : Collections.emptyList();
            return this;
        }

        public Builder withPolicy(PolicyEnum appliedSharingPolicy) {

            this.policy =
                    appliedSharingPolicy != null ? appliedSharingPolicy : PolicyEnum.NO_SHARING; // Assumed default
            return this;
        }

        public UserSharingDetails build() {

            return new UserSharingDetails(this);
        }
    }

    public UserSharingDetails copy() {
        return new Builder()
                .withUserIdOfSharingUser(this.userIdOfSharingUser)
                .withSharingInitiatedOrgId(this.sharingInitOrgId)
                .withTargetOrgId(this.targetOrgId)
                .withUserIdOfMainUser(this.userIdOfMainUser)
                .withUsernameOfMainUser(this.usernameOfMainUser)
                .withOrganizationIdOfMainUser(this.orgIdOfMainUser)
                .withSharingType(this.sharingType)
                .withRoleIds(this.roleIds != null ? new ArrayList<>(this.roleIds) : null)
                .withPolicy(this.policy)
                .build();
    }
}

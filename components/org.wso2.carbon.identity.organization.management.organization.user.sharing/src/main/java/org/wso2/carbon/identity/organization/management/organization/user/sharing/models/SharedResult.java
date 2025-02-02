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

import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleWithAudienceDO;

/**
 * Model that represent each shared result with shared status.
 */
public class SharedResult {

    private String userId;
    private String userName;
    private String orgId;
    private String orgName;
    private String associatedUserId;
    private String associatedUserName;
    private String associatedOrgId;
    private String associatedOrgName;
    private SharingType sharingType;
    private RoleWithAudienceDO role;
    private SharedStatus status;
    private String statusDetail;
    private Throwable error;

    private SharedResult(Builder builder) {
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.orgId = builder.orgId;
        this.orgName = builder.orgName;
        this.associatedUserId = builder.associatedUserId;
        this.associatedUserName = builder.associatedUserName;
        this.associatedOrgId = builder.associatedOrgId;
        this.associatedOrgName = builder.associatedOrgName;
        this.sharingType = builder.sharingType;
        this.role = builder.role;
        this.status = builder.status;
        this.statusDetail = builder.statusDetail;
        this.error = builder.error;

    }

    /**
     * Converts the current instance into a Builder for modification.
     */
    public Builder toBuilder() {
        return new Builder()
                .userId(this.userId)
                .userName(this.userName)
                .orgId(this.orgId)
                .orgName(this.orgName)
                .associatedUserId(this.associatedUserId)
                .associatedUserName(this.associatedUserName)
                .associatedOrgId(this.associatedOrgId)
                .associatedOrgName(this.associatedOrgName)
                .sharingType(this.sharingType)
                .role(this.role)
                .status(this.status)
                .statusDetail(this.statusDetail)
                .error(this.error);
    }

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public String getUserName() {

        return userName;
    }

    public void setUserName(String userName) {

        this.userName = userName;
    }

    public String getOrgId() {

        return orgId;
    }

    public void setOrgId(String orgId) {

        this.orgId = orgId;
    }

    public String getOrgName() {

        return orgName;
    }

    public void setOrgName(String orgName) {

        this.orgName = orgName;
    }

    public String getAssociatedUserId() {

        return associatedUserId;
    }

    public void setAssociatedUserId(String associatedUserId) {

        this.associatedUserId = associatedUserId;
    }

    public String getAssociatedUserName() {

        return associatedUserName;
    }

    public void setAssociatedUserName(String associatedUserName) {

        this.associatedUserName = associatedUserName;
    }

    public String getAssociatedOrgId() {

        return associatedOrgId;
    }

    public void setAssociatedOrgId(String associatedOrgId) {

        this.associatedOrgId = associatedOrgId;
    }

    public String getAssociatedOrgName() {

        return associatedOrgName;
    }

    public void setAssociatedOrgName(String associatedOrgName) {

        this.associatedOrgName = associatedOrgName;
    }

    public SharingType getSharingType() {

        return sharingType;
    }

    public void setSharingType(SharingType sharingType) {

        this.sharingType = sharingType;
    }

    public RoleWithAudienceDO getRole() {

        return role;
    }

    public void setRole(RoleWithAudienceDO role) {

        this.role = role;
    }

    public SharedStatus getStatus() {

        return status;
    }

    public void setStatus(SharedStatus status) {

        this.status = status;
    }

    public String getStatusDetail() {

        return statusDetail;
    }

    public void setStatusDetail(String statusDetail) {

        this.statusDetail = statusDetail;
    }

    public Throwable getError() {

        return error;
    }

    public void setError(Throwable error) {

        this.error = error;
    }

    /**
     * Builder class for SharedResult.
     */
    public static class Builder {

        private String userId;
        private String userName;
        private String orgId;
        private String orgName;
        private String associatedUserId;
        private String associatedUserName;
        private String associatedOrgId;
        private String associatedOrgName;
        private SharingType sharingType;
        private RoleWithAudienceDO role;
        private SharedStatus status;
        private String statusDetail;
        private Throwable error;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder orgId(String orgId) {
            this.orgId = orgId;
            return this;
        }

        public Builder orgName(String orgName) {
            this.orgName = orgName;
            return this;
        }

        public Builder associatedUserId(String associatedUserId) {
            this.associatedUserId = associatedUserId;
            return this;
        }

        public Builder associatedUserName(String associatedUserName) {
            this.associatedUserName = associatedUserName;
            return this;
        }

        public Builder associatedOrgId(String associatedOrgId) {
            this.associatedOrgId = associatedOrgId;
            return this;
        }

        public Builder associatedOrgName(String associatedOrgName) {
            this.associatedOrgName = associatedOrgName;
            return this;
        }

        public Builder sharingType(SharingType sharingType) {
            this.sharingType = sharingType;
            return this;
        }

        public Builder role(RoleWithAudienceDO role) {
            this.role = role;
            return this;
        }

        public Builder status(SharedStatus status) {
            this.status = status;
            return this;
        }

        public Builder statusDetail(String statusDetail) {
            this.statusDetail = statusDetail;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public SharedResult build() {
            return new SharedResult(this);
        }
    }

    /**
     * Enum representing the possible statuses of a shared result.
     */
    public enum SharedStatus {
        SUCCESSFUL,
        FAILED
    }

    /**
     * Enum representing the result is from either sharing or unsharing.
     */
    public enum SharingType {
        SHARE,
        UNSHARE
    }
}

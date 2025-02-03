/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;

/**
 * Model class to represent the user associations created for the shared users.
 */
public class UserAssociation {

    private int id;
    private String userId;
    private String organizationId;
    private String associatedUserId;
    private String userResidentOrganizationId;
    private SharedType sharedType;

    public UserAssociation() {

    }

    public UserAssociation(String userId, String organizationId, String associatedUserId,
                           String userResidentOrganizationId,
                           SharedType sharedType) {

        this.userId = userId;
        this.organizationId = organizationId;
        this.associatedUserId = associatedUserId;
        this.userResidentOrganizationId = userResidentOrganizationId;
        this.sharedType = sharedType;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public void setOrganizationId(String organizationId) {

        this.organizationId = organizationId;
    }

    public String getAssociatedUserId() {

        return associatedUserId;
    }

    public void setAssociatedUserId(String associatedUserId) {

        this.associatedUserId = associatedUserId;
    }

    public String getUserResidentOrganizationId() {

        return userResidentOrganizationId;
    }

    public void setUserResidentOrganizationId(String userResidentOrganizationId) {

        this.userResidentOrganizationId = userResidentOrganizationId;
    }

    public SharedType getSharedType() {

        return sharedType;
    }

    public void setSharedType(SharedType sharedType) {

        this.sharedType = sharedType;
    }
}

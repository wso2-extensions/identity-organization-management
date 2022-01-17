/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.role.mgt.core.models;

/**
 * User object for User Role Mapping
 */
public class UserRoleMappingUser {
    private String userId;
    //Mandatory role if the role we are assigning is mandatory
    //Cascaded role if the role we are assigning is non-mandatory and included for sub organizations
    //if Mandatory role is true, then cascaded role is true.
    private boolean mandatoryRole;
    private boolean cascadedRole;

    public UserRoleMappingUser(String userId, boolean mandatoryRole, boolean cascadedRole) {
        this.userId = userId;
        this.mandatoryRole = mandatoryRole;
        this.cascadedRole = cascadedRole;
    }

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public boolean isMandatoryRole() {

        return mandatoryRole;
    }

    public void setMandatoryRole(boolean mandatoryRole) {

        this.mandatoryRole = mandatoryRole;
    }

    public boolean isCascadedRole() {
        return cascadedRole;
    }

    public void setCascadedRole(boolean cascadedRole) {
        this.cascadedRole = cascadedRole;
    }
}

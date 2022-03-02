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

package org.wso2.carbon.identity.organization.management.role.mgt.core.models;

import java.util.List;

/**
 * Class for Organization-User-Role mapping for event.
 */
public class OrganizationUserRoleMappingForEvent {

    private String organizationId;
    private String roleId;
    private String userId;
    private List<UserForUserRoleMapping> usersRoleInheritance;

    public OrganizationUserRoleMappingForEvent() {
    }

    public OrganizationUserRoleMappingForEvent(String organizationId, String userId, String roleId) {

        this.organizationId = organizationId;
        this.userId = userId;
        this.roleId = roleId;
    }

    public OrganizationUserRoleMappingForEvent(String organizationId, String roleId,
                                               List<UserForUserRoleMapping> usersRoleInheritance) {

        this.organizationId = organizationId;
        this.roleId = roleId;
        this.usersRoleInheritance = usersRoleInheritance;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public void setOrganizationId(String organizationId) {

        this.organizationId = organizationId;
    }

    public String getRoleId() {

        return roleId;
    }

    public void setRoleId(String roleId) {

        this.roleId = roleId;
    }

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public List<UserForUserRoleMapping> getUsersRoleInheritance() {

        return usersRoleInheritance;
    }

}

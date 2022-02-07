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


/**
 * Organization User Role Mapping.
 * <p>
 * =========================== Scenarios ===========================
 * Assume we have organizations A, B, C at three levels.
 * A is the parent organization of B and C is the child organization of B.
 * A --> B --> C
 * <p>
 * User U1 in A assigns a role R1. Does not let to propagate it to sub organizations.
 * (OrgId=ID(A), AssignedLevel=A, isForced=false)
 * <p>
 * User U1 in A assigns a role R1. But it is not mandatory. So user U1 in B or C can either
 * assign that role or let it pass. If user U1 in B wants it,
 * (OrgId=ID(A), AssignedLevel=A, isForced=false)
 * (OrgId=ID(B), AssignedLevel=B, isForced=false)
 * And then asks the user U1 in C whether he wants it or not.
 * <p>
 * If user U1 in C doesn't want it, the role won't be copied to the Organization C.
 * <p>
 * User U1 in A assigns a role R1. Say it to propagate and it is mandatory. So user U1 in B and C will automatically
 * add that role to their organizations.
 * (OrgId=ID(A), AssignedLevel=A, isForced=true)
 * (OrgId=ID(B), AssignedLevel=A, isForced=true)
 * (OrgId=ID(C), AssignedLevel=A, isForced=true)
 */
public class OrganizationUserRoleMapping {

    private String organizationId;
    private String userId;
    private String roleId;
    private String assignedLevelOrganizationId;
    private String assignedLevelOrganizationName;
    private boolean isForced;

    public OrganizationUserRoleMapping(String organizationId, String userId, String roleId,
                                       String assignedLevelOrganizationId, boolean isForced) {

        this.organizationId = organizationId;
        this.userId = userId;
        this.roleId = roleId;
        this.assignedLevelOrganizationId = assignedLevelOrganizationId;
        this.isForced = isForced;
    }

    public OrganizationUserRoleMapping(String organizationId, String userId, String roleId,
                                       String assignedLevelOrganizationId, String assignedLevelOrganizationName,
                                       boolean isForced) {

        this.organizationId = organizationId;
        this.userId = userId;
        this.roleId = roleId;
        this.assignedLevelOrganizationId = assignedLevelOrganizationId;
        this.assignedLevelOrganizationName = assignedLevelOrganizationName;
        this.isForced = isForced;
    }

    public OrganizationUserRoleMapping() {
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public void setOrganizationId(String organizationId) {

        this.organizationId = organizationId;
    }

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public String getRoleId() {

        return roleId;
    }

    public void setRoleId(String roleId) {

        this.roleId = roleId;
    }

    public String getAssignedLevelOrganizationId() {

        return assignedLevelOrganizationId;
    }

    public void setAssignedLevelOrganizationId(String assignedLevelOrganizationId) {

        this.assignedLevelOrganizationId = assignedLevelOrganizationId;
    }

    public boolean isForced() {

        return isForced;
    }

    public void setForced(boolean forced) {

        isForced = forced;
    }

    public String getAssignedLevelOrganizationName() {

        return assignedLevelOrganizationName;
    }

}

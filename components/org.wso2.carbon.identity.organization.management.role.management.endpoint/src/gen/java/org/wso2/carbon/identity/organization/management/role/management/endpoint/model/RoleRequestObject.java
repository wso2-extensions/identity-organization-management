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

package org.wso2.carbon.identity.organization.management.role.management.endpoint.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class RoleRequestObject  {
  
    private String displayName;
    private List<Object> users = null;

    private List<Object> groups = null;

    private List<String> permissions = null;


    /**
    **/
    public RoleRequestObject displayName(String displayName) {

        this.displayName = displayName;
        return this;
    }
    
    @ApiModelProperty(example = "loginRole", required = true, value = "")
    @JsonProperty("displayName")
    @Valid
    @NotNull(message = "Property displayName cannot be null.")

    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
    **/
    public RoleRequestObject users(List<Object> users) {

        this.users = users;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("users")
    @Valid
    public List<Object> getUsers() {
        return users;
    }
    public void setUsers(List<Object> users) {
        this.users = users;
    }

    public RoleRequestObject addUsersItem(Object usersItem) {
        if (this.users == null) {
            this.users = new ArrayList<>();
        }
        this.users.add(usersItem);
        return this;
    }

        /**
    **/
    public RoleRequestObject groups(List<Object> groups) {

        this.groups = groups;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("groups")
    @Valid
    public List<Object> getGroups() {
        return groups;
    }
    public void setGroups(List<Object> groups) {
        this.groups = groups;
    }

    public RoleRequestObject addGroupsItem(Object groupsItem) {
        if (this.groups == null) {
            this.groups = new ArrayList<>();
        }
        this.groups.add(groupsItem);
        return this;
    }

        /**
    **/
    public RoleRequestObject permissions(List<String> permissions) {

        this.permissions = permissions;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("permissions")
    @Valid
    public List<String> getPermissions() {
        return permissions;
    }
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public RoleRequestObject addPermissionsItem(String permissionsItem) {
        if (this.permissions == null) {
            this.permissions = new ArrayList<>();
        }
        this.permissions.add(permissionsItem);
        return this;
    }

    

    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoleRequestObject roleRequestObject = (RoleRequestObject) o;
        return Objects.equals(this.displayName, roleRequestObject.displayName) &&
            Objects.equals(this.users, roleRequestObject.users) &&
            Objects.equals(this.groups, roleRequestObject.groups) &&
            Objects.equals(this.permissions, roleRequestObject.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, users, groups, permissions);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class RoleRequestObject {\n");
        
        sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
        sb.append("    users: ").append(toIndentedString(users)).append("\n");
        sb.append("    groups: ").append(toIndentedString(groups)).append("\n");
        sb.append("    permissions: ").append(toIndentedString(permissions)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
    * Convert the given object to string with each line indented by 4 spaces
    * (except the first line).
    */
    private String toIndentedString(java.lang.Object o) {

        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n");
    }
}


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
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestGroupObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestUserObject;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class RolePutRequestObject  {
  
    private String displayName;
    private List<RolePutRequestUserObject> users = null;

    private List<RolePutRequestGroupObject> groups = null;

    private List<String> permissions = null;


    /**
    **/
    public RolePutRequestObject displayName(String displayName) {

        this.displayName = displayName;
        return this;
    }
    
    @ApiModelProperty(example = "loginRole", value = "")
    @JsonProperty("displayName")
    @Valid
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
    **/
    public RolePutRequestObject users(List<RolePutRequestUserObject> users) {

        this.users = users;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("users")
    @Valid
    public List<RolePutRequestUserObject> getUsers() {
        return users;
    }
    public void setUsers(List<RolePutRequestUserObject> users) {
        this.users = users;
    }

    public RolePutRequestObject addUsersItem(RolePutRequestUserObject usersItem) {
        if (this.users == null) {
            this.users = new ArrayList<>();
        }
        this.users.add(usersItem);
        return this;
    }

        /**
    **/
    public RolePutRequestObject groups(List<RolePutRequestGroupObject> groups) {

        this.groups = groups;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("groups")
    @Valid
    public List<RolePutRequestGroupObject> getGroups() {
        return groups;
    }
    public void setGroups(List<RolePutRequestGroupObject> groups) {
        this.groups = groups;
    }

    public RolePutRequestObject addGroupsItem(RolePutRequestGroupObject groupsItem) {
        if (this.groups == null) {
            this.groups = new ArrayList<>();
        }
        this.groups.add(groupsItem);
        return this;
    }

        /**
    **/
    public RolePutRequestObject permissions(List<String> permissions) {

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

    public RolePutRequestObject addPermissionsItem(String permissionsItem) {
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
        RolePutRequestObject rolePutRequestObject = (RolePutRequestObject) o;
        return Objects.equals(this.displayName, rolePutRequestObject.displayName) &&
            Objects.equals(this.users, rolePutRequestObject.users) &&
            Objects.equals(this.groups, rolePutRequestObject.groups) &&
            Objects.equals(this.permissions, rolePutRequestObject.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, users, groups, permissions);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class RolePutRequestObject {\n");
        
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


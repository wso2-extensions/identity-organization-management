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
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseObjectGroups;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseObjectUsers;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostResponseObjectMeta;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class RoleGetResponseObject  {
  
    private String displayName;
    private RolePostResponseObjectMeta meta;
    private String id;
    private List<RoleGetResponseObjectUsers> users = null;

    private List<RoleGetResponseObjectGroups> groups = null;

    private List<String> permissions = null;


    /**
    **/
    public RoleGetResponseObject displayName(String displayName) {

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
    public RoleGetResponseObject meta(RolePostResponseObjectMeta meta) {

        this.meta = meta;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("meta")
    @Valid
    public RolePostResponseObjectMeta getMeta() {
        return meta;
    }
    public void setMeta(RolePostResponseObjectMeta meta) {
        this.meta = meta;
    }

    /**
    **/
    public RoleGetResponseObject id(String id) {

        this.id = id;
        return this;
    }
    
    @ApiModelProperty(example = "4645709c-ea8c-4495-8590-e1fa0efe3de0", value = "")
    @JsonProperty("id")
    @Valid
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    /**
    **/
    public RoleGetResponseObject users(List<RoleGetResponseObjectUsers> users) {

        this.users = users;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("users")
    @Valid
    public List<RoleGetResponseObjectUsers> getUsers() {
        return users;
    }
    public void setUsers(List<RoleGetResponseObjectUsers> users) {
        this.users = users;
    }

    public RoleGetResponseObject addUsersItem(RoleGetResponseObjectUsers usersItem) {
        if (this.users == null) {
            this.users = new ArrayList<>();
        }
        this.users.add(usersItem);
        return this;
    }

        /**
    **/
    public RoleGetResponseObject groups(List<RoleGetResponseObjectGroups> groups) {

        this.groups = groups;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("groups")
    @Valid
    public List<RoleGetResponseObjectGroups> getGroups() {
        return groups;
    }
    public void setGroups(List<RoleGetResponseObjectGroups> groups) {
        this.groups = groups;
    }

    public RoleGetResponseObject addGroupsItem(RoleGetResponseObjectGroups groupsItem) {
        if (this.groups == null) {
            this.groups = new ArrayList<>();
        }
        this.groups.add(groupsItem);
        return this;
    }

        /**
    **/
    public RoleGetResponseObject permissions(List<String> permissions) {

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

    public RoleGetResponseObject addPermissionsItem(String permissionsItem) {
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
        RoleGetResponseObject roleGetResponseObject = (RoleGetResponseObject) o;
        return Objects.equals(this.displayName, roleGetResponseObject.displayName) &&
            Objects.equals(this.meta, roleGetResponseObject.meta) &&
            Objects.equals(this.id, roleGetResponseObject.id) &&
            Objects.equals(this.users, roleGetResponseObject.users) &&
            Objects.equals(this.groups, roleGetResponseObject.groups) &&
            Objects.equals(this.permissions, roleGetResponseObject.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, meta, id, users, groups, permissions);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class RoleGetResponseObject {\n");
        
        sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
        sb.append("    meta: ").append(toIndentedString(meta)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
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


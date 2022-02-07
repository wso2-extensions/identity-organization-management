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

package org.wso2.carbon.identity.organization.management.endpoint.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserAssignedMetaDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserNameDTO;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class UserDTO  {
  
    private String username;
    private String id;
    private UserNameDTO name;
    private List<UserAssignedMetaDTO> assignedMeta = null;


    /**
    **/
    public UserDTO username(String username) {

        this.username = username;
        return this;
    }
    
    @ApiModelProperty(example = "PRIMARY/Lia", required = true, value = "")
    @JsonProperty("username")
    @Valid
    @NotNull(message = "Property username cannot be null.")

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    /**
    **/
    public UserDTO id(String id) {

        this.id = id;
        return this;
    }
    
    @ApiModelProperty(example = "008bba85-451d-414b-87de-c03b5a1f4217", required = true, value = "")
    @JsonProperty("id")
    @Valid
    @NotNull(message = "Property id cannot be null.")

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    /**
    **/
    public UserDTO name(UserNameDTO name) {

        this.name = name;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("name")
    @Valid
    public UserNameDTO getName() {
        return name;
    }
    public void setName(UserNameDTO name) {
        this.name = name;
    }

    /**
    **/
    public UserDTO assignedMeta(List<UserAssignedMetaDTO> assignedMeta) {

        this.assignedMeta = assignedMeta;
        return this;
    }
    
    @ApiModelProperty(example = "[{\"forced\":true,\"assignedAt\":{\"orgId\":\"b4526d91-a8bf-43d2-8b14-c548cf73065b\",\"orgName\":\"WSO2\"}},{\"forced\":false,\"assignedAt\":{\"orgId\":\"c4526761-a8bf-43d2-8b14-c548cf7306fc\",\"orgName\":\"WSO2BR\"}}]", value = "")
    @JsonProperty("assignedMeta")
    @Valid
    public List<UserAssignedMetaDTO> getAssignedMeta() {
        return assignedMeta;
    }
    public void setAssignedMeta(List<UserAssignedMetaDTO> assignedMeta) {
        this.assignedMeta = assignedMeta;
    }

    public UserDTO addAssignedMetaItem(UserAssignedMetaDTO assignedMetaItem) {
        if (this.assignedMeta == null) {
            this.assignedMeta = new ArrayList<>();
        }
        this.assignedMeta.add(assignedMetaItem);
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
        UserDTO userDTO = (UserDTO) o;
        return Objects.equals(this.username, userDTO.username) &&
            Objects.equals(this.id, userDTO.id) &&
            Objects.equals(this.name, userDTO.name) &&
            Objects.equals(this.assignedMeta, userDTO.assignedMeta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, id, name, assignedMeta);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class UserDTO {\n");
        
        sb.append("    username: ").append(toIndentedString(username)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    assignedMeta: ").append(toIndentedString(assignedMeta)).append("\n");
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


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
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class UserRoleMappingUsersDTO  {
  
    private String userId;
    private Boolean mandatory;
    private Boolean includeSubOrgs;

    /**
    **/
    public UserRoleMappingUsersDTO userId(String userId) {

        this.userId = userId;
        return this;
    }
    
    @ApiModelProperty(example = "c4c3a320-e381-463a-846f-1c2bd083d38c", required = true, value = "")
    @JsonProperty("userId")
    @Valid
    @NotNull(message = "Property userId cannot be null.")

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
    **/
    public UserRoleMappingUsersDTO mandatory(Boolean mandatory) {

        this.mandatory = mandatory;
        return this;
    }
    
    @ApiModelProperty(example = "true", value = "")
    @JsonProperty("mandatory")
    @Valid
    public Boolean getMandatory() {
        return mandatory;
    }
    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
    **/
    public UserRoleMappingUsersDTO includeSubOrgs(Boolean includeSubOrgs) {

        this.includeSubOrgs = includeSubOrgs;
        return this;
    }
    
    @ApiModelProperty(example = "true", value = "")
    @JsonProperty("includeSubOrgs")
    @Valid
    public Boolean getIncludeSubOrgs() {
        return includeSubOrgs;
    }
    public void setIncludeSubOrgs(Boolean includeSubOrgs) {
        this.includeSubOrgs = includeSubOrgs;
    }



    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserRoleMappingUsersDTO userRoleMappingUsersDTO = (UserRoleMappingUsersDTO) o;
        return Objects.equals(this.userId, userRoleMappingUsersDTO.userId) &&
            Objects.equals(this.mandatory, userRoleMappingUsersDTO.mandatory) &&
            Objects.equals(this.includeSubOrgs, userRoleMappingUsersDTO.includeSubOrgs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, mandatory, includeSubOrgs);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class UserRoleMappingUsersDTO {\n");
        
        sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
        sb.append("    mandatory: ").append(toIndentedString(mandatory)).append("\n");
        sb.append("    includeSubOrgs: ").append(toIndentedString(includeSubOrgs)).append("\n");
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


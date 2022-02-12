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

public class UserRoleOperationDTO  {
  
    private String op;
    private String path;
    private Boolean value;

    /**
    **/
    public UserRoleOperationDTO op(String op) {

        this.op = op;
        return this;
    }
    
    @ApiModelProperty(example = "replace", required = true, value = "")
    @JsonProperty("op")
    @Valid
    @NotNull(message = "Property op cannot be null.")

    public String getOp() {
        return op;
    }
    public void setOp(String op) {
        this.op = op;
    }

    /**
    **/
    public UserRoleOperationDTO path(String path) {

        this.path = path;
        return this;
    }
    
    @ApiModelProperty(example = "/includeSubOrganizations", required = true, value = "")
    @JsonProperty("path")
    @Valid
    @NotNull(message = "Property path cannot be null.")

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    /**
    **/
    public UserRoleOperationDTO value(Boolean value) {

        this.value = value;
        return this;
    }
    
    @ApiModelProperty(example = "false", required = true, value = "")
    @JsonProperty("value")
    @Valid
    @NotNull(message = "Property value cannot be null.")

    public Boolean getValue() {
        return value;
    }
    public void setValue(Boolean value) {
        this.value = value;
    }



    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserRoleOperationDTO userRoleOperationDTO = (UserRoleOperationDTO) o;
        return Objects.equals(this.op, userRoleOperationDTO.op) &&
            Objects.equals(this.path, userRoleOperationDTO.path) &&
            Objects.equals(this.value, userRoleOperationDTO.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, path, value);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class UserRoleOperationDTO {\n");
        
        sb.append("    op: ").append(toIndentedString(op)).append("\n");
        sb.append("    path: ").append(toIndentedString(path)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
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


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
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class RoleGetResponseUserObject  {
  
    private String $ref;
    private String id;
    private String display;

    /**
    **/
    public RoleGetResponseUserObject $ref(String $ref) {

        this.$ref = $ref;
        return this;
    }
    
    @ApiModelProperty(example = "https://localhost:9443/t/carbon.super/o/carbon/api/identity/organization-mgt/v1.0/organizations/48e31bc5-1669-4de1-bb22-c71e443aeb8b/users/3a12bae9-4386-44be-befd-caf349297f45", value = "")
    @JsonProperty("$ref")
    @Valid
    public String get$Ref() {
        return $ref;
    }
    public void set$Ref(String $ref) {
        this.$ref = $ref;
    }

    /**
    **/
    public RoleGetResponseUserObject id(String id) {

        this.id = id;
        return this;
    }
    
    @ApiModelProperty(example = "3a12bae9-4386-44be-befd-caf349297f45", value = "")
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
    public RoleGetResponseUserObject display(String display) {

        this.display = display;
        return this;
    }
    
    @ApiModelProperty(example = "kim", value = "")
    @JsonProperty("display")
    @Valid
    public String getDisplay() {
        return display;
    }
    public void setDisplay(String display) {
        this.display = display;
    }



    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoleGetResponseUserObject roleGetResponseUserObject = (RoleGetResponseUserObject) o;
        return Objects.equals(this.$ref, roleGetResponseUserObject.$ref) &&
            Objects.equals(this.id, roleGetResponseUserObject.id) &&
            Objects.equals(this.display, roleGetResponseUserObject.display);
    }

    @Override
    public int hashCode() {
        return Objects.hash($ref, id, display);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class RoleGetResponseUserObject {\n");
        
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    display: ").append(toIndentedString(display)).append("\n");
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


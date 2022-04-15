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

public class RolePutResponseObjectMeta  {
  
    private String location;
    private String resourceType;

    /**
    **/
    public RolePutResponseObjectMeta location(String location) {

        this.location = location;
        return this;
    }
    
    @ApiModelProperty(example = "https://localhost:9443/t/carbon.super/o/organization/api/identity/organizations/48e31bc5-1669-4de1-bb22-c71e443aeb8b/roles/4645709c-ea8c-4495-8590-e1fa0fe3de0", value = "")
    @JsonProperty("location")
    @Valid
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    /**
    **/
    public RolePutResponseObjectMeta resourceType(String resourceType) {

        this.resourceType = resourceType;
        return this;
    }
    
    @ApiModelProperty(example = "role", value = "")
    @JsonProperty("resourceType")
    @Valid
    public String getResourceType() {
        return resourceType;
    }
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }



    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RolePutResponseObjectMeta rolePutResponseObjectMeta = (RolePutResponseObjectMeta) o;
        return Objects.equals(this.location, rolePutResponseObjectMeta.location) &&
            Objects.equals(this.resourceType, rolePutResponseObjectMeta.resourceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, resourceType);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class RolePutResponseObjectMeta {\n");
        
        sb.append("    location: ").append(toIndentedString(location)).append("\n");
        sb.append("    resourceType: ").append(toIndentedString(resourceType)).append("\n");
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


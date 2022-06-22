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

public class ChildOrganization  {
  
    private String id;
    private String ref;

    /**
    **/
    public ChildOrganization id(String id) {

        this.id = id;
        return this;
    }
    
    @ApiModelProperty(example = "d8f9780e-3a9a-4ae0-8d94-1a2d1aa3ec14", required = true, value = "")
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
    public ChildOrganization ref(String ref) {

        this.ref = ref;
        return this;
    }
    
    @ApiModelProperty(example = "o/root4a8d-113f-4211-a0d5-efe36b082211/api/server/v1/organizations/d8f9780e-3a9a-4ae0-8d94-1a2d1aa3ec14", required = true, value = "")
    @JsonProperty("ref")
    @Valid
    @NotNull(message = "Property ref cannot be null.")

    public String getRef() {
        return ref;
    }
    public void setRef(String ref) {
        this.ref = ref;
    }



    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChildOrganization childOrganization = (ChildOrganization) o;
        return Objects.equals(this.id, childOrganization.id) &&
            Objects.equals(this.ref, childOrganization.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ref);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class ChildOrganization {\n");
        
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    ref: ").append(toIndentedString(ref)).append("\n");
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


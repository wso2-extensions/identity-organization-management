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
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleObj;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class RolesListResponseObject  {
  
    private Integer totalResults;
    private Integer startIndex;
    private Integer itemsPerPage;
    private List<RoleObj> resources = null;


    /**
    **/
    public RolesListResponseObject totalResults(Integer totalResults) {

        this.totalResults = totalResults;
        return this;
    }
    
    @ApiModelProperty(example = "3", value = "")
    @JsonProperty("totalResults")
    @Valid
    public Integer getTotalResults() {
        return totalResults;
    }
    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    /**
    **/
    public RolesListResponseObject startIndex(Integer startIndex) {

        this.startIndex = startIndex;
        return this;
    }
    
    @ApiModelProperty(example = "1", value = "")
    @JsonProperty("startIndex")
    @Valid
    public Integer getStartIndex() {
        return startIndex;
    }
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    /**
    **/
    public RolesListResponseObject itemsPerPage(Integer itemsPerPage) {

        this.itemsPerPage = itemsPerPage;
        return this;
    }
    
    @ApiModelProperty(example = "3", value = "")
    @JsonProperty("itemsPerPage")
    @Valid
    public Integer getItemsPerPage() {
        return itemsPerPage;
    }
    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    /**
    **/
    public RolesListResponseObject resources(List<RoleObj> resources) {

        this.resources = resources;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("resources")
    @Valid
    public List<RoleObj> getResources() {
        return resources;
    }
    public void setResources(List<RoleObj> resources) {
        this.resources = resources;
    }

    public RolesListResponseObject addResourcesItem(RoleObj resourcesItem) {
        if (this.resources == null) {
            this.resources = new ArrayList<>();
        }
        this.resources.add(resourcesItem);
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
        RolesListResponseObject rolesListResponseObject = (RolesListResponseObject) o;
        return Objects.equals(this.totalResults, rolesListResponseObject.totalResults) &&
            Objects.equals(this.startIndex, rolesListResponseObject.startIndex) &&
            Objects.equals(this.itemsPerPage, rolesListResponseObject.itemsPerPage) &&
            Objects.equals(this.resources, rolesListResponseObject.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalResults, startIndex, itemsPerPage, resources);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class RolesListResponseObject {\n");
        
        sb.append("    totalResults: ").append(toIndentedString(totalResults)).append("\n");
        sb.append("    startIndex: ").append(toIndentedString(startIndex)).append("\n");
        sb.append("    itemsPerPage: ").append(toIndentedString(itemsPerPage)).append("\n");
        sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
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


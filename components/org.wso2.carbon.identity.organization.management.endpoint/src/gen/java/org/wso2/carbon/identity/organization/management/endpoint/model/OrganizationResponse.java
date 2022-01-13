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
import org.wso2.carbon.identity.organization.management.endpoint.model.Attribute;
import org.wso2.carbon.identity.organization.management.endpoint.model.ParentOrganization;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class OrganizationResponse  {
  
    private String id;
    private String name;
    private String description;
    private String created;
    private String lastModified;
    private ParentOrganization parent;
    private List<Attribute> attributes = null;


    /**
    **/
    public OrganizationResponse id(String id) {

        this.id = id;
        return this;
    }
    
    @ApiModelProperty(example = "06c1f4e2-3339-44e4-a825-96585e3653b1", required = true, value = "")
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
    public OrganizationResponse name(String name) {

        this.name = name;
        return this;
    }
    
    @ApiModelProperty(example = "ABC Builders", required = true, value = "")
    @JsonProperty("name")
    @Valid
    @NotNull(message = "Property name cannot be null.")

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    /**
    **/
    public OrganizationResponse description(String description) {

        this.description = description;
        return this;
    }
    
    @ApiModelProperty(example = "Building constructions", value = "")
    @JsonProperty("description")
    @Valid
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    /**
    **/
    public OrganizationResponse created(String created) {

        this.created = created;
        return this;
    }
    
    @ApiModelProperty(example = "2021-10-25T12:31:53.406Z", value = "")
    @JsonProperty("created")
    @Valid
    public String getCreated() {
        return created;
    }
    public void setCreated(String created) {
        this.created = created;
    }

    /**
    **/
    public OrganizationResponse lastModified(String lastModified) {

        this.lastModified = lastModified;
        return this;
    }
    
    @ApiModelProperty(example = "2021-10-25T12:31:53.406Z", value = "")
    @JsonProperty("lastModified")
    @Valid
    public String getLastModified() {
        return lastModified;
    }
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    /**
    **/
    public OrganizationResponse parent(ParentOrganization parent) {

        this.parent = parent;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("parent")
    @Valid
    public ParentOrganization getParent() {
        return parent;
    }
    public void setParent(ParentOrganization parent) {
        this.parent = parent;
    }

    /**
    **/
    public OrganizationResponse attributes(List<Attribute> attributes) {

        this.attributes = attributes;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("attributes")
    @Valid
    public List<Attribute> getAttributes() {
        return attributes;
    }
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public OrganizationResponse addAttributesItem(Attribute attributesItem) {
        if (this.attributes == null) {
            this.attributes = new ArrayList<>();
        }
        this.attributes.add(attributesItem);
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
        OrganizationResponse organizationResponse = (OrganizationResponse) o;
        return Objects.equals(this.id, organizationResponse.id) &&
            Objects.equals(this.name, organizationResponse.name) &&
            Objects.equals(this.description, organizationResponse.description) &&
            Objects.equals(this.created, organizationResponse.created) &&
            Objects.equals(this.lastModified, organizationResponse.lastModified) &&
            Objects.equals(this.parent, organizationResponse.parent) &&
            Objects.equals(this.attributes, organizationResponse.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, created, lastModified, parent, attributes);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class OrganizationResponse {\n");
        
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    created: ").append(toIndentedString(created)).append("\n");
        sb.append("    lastModified: ").append(toIndentedString(lastModified)).append("\n");
        sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
        sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
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


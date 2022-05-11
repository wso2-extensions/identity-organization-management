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

package org.wso2.carbon.identity.organization.management.role.management.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import java.io.InputStream;
import java.util.List;

import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolesListResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.OrganizationsApiService;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import io.swagger.annotations.*;

import javax.validation.constraints.*;

@Path("/organizations")
@Api(description = "The organizations API")

public class OrganizationsApi  {

    @Autowired
    private OrganizationsApiService delegate;

    @Valid
    @POST
    @Path("/{organization-id}/roles")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Create a role inside an organization.", notes = "This API creates a role inside an organization, assigns users, groups and permissions, and returns the details of the created role including its unique id.", response = RolePostResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Valid role is created.", response = RolePostResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 409, message = "Conflict response.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response createRole(@ApiParam(value = "ID of the organization.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "Represents display name, set of permissions, set of groups, set of users that are to be assigned to the role." ,required=true) @Valid RolePostRequest rolePostRequest) {

        return delegate.createRole(organizationId,  rolePostRequest );
    }

    @Valid
    @GET
    @Path("/{organization-id}/roles")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Get roles inside an organization.", notes = "This API returns roles in an organization based on the provided filter, sort and pagination parameters", response = RolesListResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response.", response = RolesListResponse.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesGet(@ApiParam(value = "ID of the organization.",required=true) @PathParam("organization-id") String organizationId,     @Valid@ApiParam(value = "Condition to filter the retrieval of records.")  @QueryParam("filter") String filter,     @Valid @Min(0)@ApiParam(value = "Maximum number of records to be returned. (Should be greater than 0)")  @QueryParam("limit") Integer limit,     @Valid@ApiParam(value = "Points to the next range of data to be returned.")  @QueryParam("after") String after,     @Valid@ApiParam(value = "Points to the previous range of data that can be retrieved.")  @QueryParam("before") String before) {

        return delegate.organizationsOrganizationIdRolesGet(organizationId,  filter,  limit,  after,  before );
    }

    @Valid
    @DELETE
    @Path("/{organization-id}/roles/{role-id}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Delete a role inside an organization.", notes = "This API deletes a particular role inside an organization using its unique ID.", response = Void.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Role is deleted.", response = Void.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdDelete(@ApiParam(value = "ID of the role.",required=true) @PathParam("role-id") String roleId, @ApiParam(value = "ID of the organization.",required=true) @PathParam("organization-id") String organizationId) {

        return delegate.organizationsOrganizationIdRolesRoleIdDelete(roleId,  organizationId );
    }

    @Valid
    @GET
    @Path("/{organization-id}/roles/{role-id}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Get Role by ID", notes = "This API returns the role details of a particular role using its unique id.", response = RoleGetResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Valid role is found.", response = RoleGetResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdGet(@ApiParam(value = "ID of the role.",required=true) @PathParam("role-id") String roleId, @ApiParam(value = "ID of the organization.",required=true) @PathParam("organization-id") String organizationId) {

        return delegate.organizationsOrganizationIdRolesRoleIdGet(roleId,  organizationId );
    }

    @Valid
    @PATCH
    @Path("/{organization-id}/roles/{role-id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update Role - PATCH", notes = "This API updates the role details and returns the updated role details inside a PATCH operation.", response = RolePatchResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Role is Updated.", response = RolePatchResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdPatch(@ApiParam(value = "ID of the role.",required=true) @PathParam("role-id") String roleId, @ApiParam(value = "ID of the organization.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "This represents a set of values that need to be changed in the role." ,required=true) @Valid RolePatchRequest rolePatchRequest) {

        return delegate.organizationsOrganizationIdRolesRoleIdPatch(roleId,  organizationId,  rolePatchRequest );
    }

    @Valid
    @PUT
    @Path("/{organization-id}/roles/{role-id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update Role - PUT", notes = "This API updates the role details and returns the updated role details using a PUT operation.", response = RolePutResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Role is successfully updated.", response = RolePutResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 406, message = "Not acceptable.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdPut(@ApiParam(value = "ID of the role.",required=true) @PathParam("role-id") String roleId, @ApiParam(value = "ID of the organization.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "This represents a set of values that need to be changed in the role." ,required=true) @Valid RolePutRequest rolePutRequest) {

        return delegate.organizationsOrganizationIdRolesRoleIdPut(roleId,  organizationId,  rolePutRequest );
    }

}

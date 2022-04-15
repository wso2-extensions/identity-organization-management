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

package org.wso2.carbon.identity.organization.management.role.management.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import java.io.InputStream;
import java.util.List;

import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolesListResponseObject;
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
    @ApiOperation(value = "Create a role inside an organization.", notes = "This API creates a role inside an organization and returns the details of the created role including its unique id.", response = RolePostResponseObject.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Valid role is created.", response = RolePostResponseObject.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 409, message = "Conflict response.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response createRole(@ApiParam(value = "ID of the organization where the role is going to be created.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "This represents a set of permissions going to be assigned to the role." ,required=true) @Valid RoleRequestObject roleRequestObject) {

        return delegate.createRole(organizationId,  roleRequestObject );
    }

    @Valid
    @GET
    @Path("/{organization-id}/roles")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Get roles inside an organization.", notes = "This API returs roles according to the specified filter, sort and pagination parameters.", response = RolesListResponseObject.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Valid roles are found.", response = RolesListResponseObject.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesGet(@ApiParam(value = "ID of the organization where roles are getting retrieved.",required=true) @PathParam("organization-id") String organizationId,     @Valid@ApiParam(value = "Filter expression for filtering.")  @QueryParam("filter") String filter,     @Valid@ApiParam(value = "The 1-based index of the first query result")  @QueryParam("startIndex") Integer startIndex,     @Valid@ApiParam(value = "Specifies the desired maximum number of query results per page.")  @QueryParam("count") Integer count) {

        return delegate.organizationsOrganizationIdRolesGet(organizationId,  filter,  startIndex,  count );
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
    public Response organizationsOrganizationIdRolesRoleIdDelete(@ApiParam(value = "ID of the organization where the role is.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "ID of the role.",required=true) @PathParam("role-id") String roleId) {

        return delegate.organizationsOrganizationIdRolesRoleIdDelete(organizationId,  roleId );
    }

    @Valid
    @GET
    @Path("/{organization-id}/roles/{role-id}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Get Role by ID", notes = "This API returns the role details of a particular role using its unique id.", response = RoleGetResponseObject.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Valid role is found.", response = RoleGetResponseObject.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdGet(@ApiParam(value = "ID of the organization where the role is.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "ID of the role.",required=true) @PathParam("role-id") String roleId) {

        return delegate.organizationsOrganizationIdRolesRoleIdGet(organizationId,  roleId );
    }

    @Valid
    @PATCH
    @Path("/{organization-id}/roles/{role-id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update Role - PATCH", notes = "This API updates the role details and returns the updated role details inside a PATCH operation.", response = RolePatchResponseObject.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Role is Updated.", response = RolePatchResponseObject.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 406, message = "Not acceptable.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdPatch(@ApiParam(value = "ID of the organization where the role is.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "ID of the role.",required=true) @PathParam("role-id") String roleId, @ApiParam(value = "This represents a set of values that need to be changed in the role." ) @Valid RolePatchRequestObject rolePatchRequestObject) {

        return delegate.organizationsOrganizationIdRolesRoleIdPatch(organizationId,  roleId,  rolePatchRequestObject );
    }

    @Valid
    @PUT
    @Path("/{organization-id}/roles/{role-id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update Role - PUT", notes = "This API updates the role details and returns the updated role details using a PUT operation.", response = RolePutResponseObject.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Role is Updated.", response = RolePutResponseObject.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 406, message = "Not acceptable.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdPut(@ApiParam(value = "ID of the organization where the role is.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "ID of the role.",required=true) @PathParam("role-id") String roleId, @ApiParam(value = "This represents a set of values that need to be changed in the role." ) @Valid RolePutRequestObject rolePutRequestObject) {

        return delegate.organizationsOrganizationIdRolesRoleIdPut(organizationId,  roleId,  rolePutRequestObject );
    }

}

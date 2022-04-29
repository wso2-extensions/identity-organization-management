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

package org.wso2.carbon.identity.organization.management.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import java.io.InputStream;
import java.util.List;

import org.wso2.carbon.identity.organization.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.endpoint.model.GetOrganizationResponse;
import java.util.List;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPOSTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPUTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPatchRequestItem;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationsResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.RoleDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleMappingDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleOperationDTO;

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
    @GET
    
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Retrieve organizations created for this tenant which matches the defined search criteria, if any.", notes = "This API is used to search and retrieve organizations created for this tenant.", response = OrganizationsResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response", response = OrganizationsResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class),
        @ApiResponse(code = 501, message = "Not Implemented.", response = Error.class)
    })
    public Response organizationsGet(    @Valid@ApiParam(value = "Condition to filter the retrieval of records.")  @QueryParam("filter") String filter,     @Valid @Min(0)@ApiParam(value = "Maximum number of records to be returned. (Should be greater than 0)")  @QueryParam("limit") Integer limit,     @Valid@ApiParam(value = "Points to the next range of data to be returned.")  @QueryParam("after") String after,     @Valid@ApiParam(value = "Points to the previous range of data that can be retrieved.")  @QueryParam("before") String before) {

        return delegate.organizationsGet(filter,  limit,  after,  before );
    }

    @Valid
    @DELETE
    @Path("/{organization-id}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Delete an organization by using the organization's ID.", notes = "This API provides the capability to delete an organization by giving its ID.", response = Void.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Successfully deleted", response = Void.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdDelete(@ApiParam(value = "ID of the organization to be deleted.",required=true) @PathParam("organization-id") String organizationId) {

        return delegate.organizationsOrganizationIdDelete(organizationId );
    }

    @Valid
    @GET
    @Path("/{organization-id}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Get an existing organization, identified by the organization ID.", notes = "This API is used to get an existing organization identified by the organization ID.", response = GetOrganizationResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response", response = GetOrganizationResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdGet(@ApiParam(value = "ID of the organization.",required=true) @PathParam("organization-id") String organizationId,     @Valid@ApiParam(value = "Returns the organization details along with the child organization IDs belonging to this organization.", defaultValue="false") @DefaultValue("false")  @QueryParam("showChildren") Boolean showChildren) {

        return delegate.organizationsOrganizationIdGet(organizationId,  showChildren );
    }

    @Valid
    @PATCH
    @Path("/{organization-id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Patch an organization property by ID. Patch is supported only for key-value pairs.", notes = "This API provides the capability to update an organization property using patch request. Organization patch is supported only for key-value pairs.", response = OrganizationResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Ok", response = Void.class),
        @ApiResponse(code = 200, message = "Successful response", response = OrganizationResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdPatch(@ApiParam(value = "ID of the organization to be patched.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "" ,required=true) @Valid List<OrganizationPatchRequestItem> organizationPatchRequestItem) {

        return delegate.organizationsOrganizationIdPatch(organizationId,  organizationPatchRequestItem );
    }

    @Valid
    @PUT
    @Path("/{organization-id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update an organization by ID.", notes = "This API provides the capability to update an organization by its id.", response = OrganizationResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Ok", response = Void.class),
        @ApiResponse(code = 200, message = "Successful response", response = OrganizationResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdPut(@ApiParam(value = "ID of the organization to be updated.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "" ,required=true) @Valid OrganizationPUTRequest organizationPUTRequest) {

        return delegate.organizationsOrganizationIdPut(organizationId,  organizationPUTRequest );
    }

    @Valid
    @POST
    @Path("/{organization-id}/roles")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Create a user role mapping for an organization. ", notes = "This API is used to create user role mappings for an organization. ", response = Void.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Created", response = Void.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesPost(@ApiParam(value = "ID of the organization which the user-role mappings are added.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "This represents user role mappings." ,required=true) @Valid UserRoleMappingDTO userRoleMappingDTO) {

        return delegate.organizationsOrganizationIdRolesPost(organizationId,  userRoleMappingDTO );
    }

    @Valid
    @GET
    @Path("/{organization-id}/roles/{role-id}/users")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Retrieve the list of users who have specific role against an organization. ", notes = "This API  is used to get the user list of an organization with a specific role. ", response = UserDTO.class, responseContainer = "List", authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Ok", response = UserDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdUsersGet(@ApiParam(value = "ID of the organization which, the users will be returned.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "ID of the role which, the user will be returned.",required=true) @PathParam("role-id") String roleId,     @Valid@ApiParam(value = "Number of items to be skipped before starting to collect the result set. (Should be 0 or positive)")  @QueryParam("offset") Integer offset,     @Valid@ApiParam(value = "Max number of items to be returned. (Should be greater than 0)")  @QueryParam("limit") Integer limit,     @Valid@ApiParam(value = "Comma separated list of SCIM user attributes to be returned in the response.")  @QueryParam("attributes") String attributes,     @Valid@ApiParam(value = "SCIM filtering to fine tune the search results. Supported operations are 'eq', 'co', 'sw', 'ew', and 'and'.")  @QueryParam("filter") String filter) {

        return delegate.organizationsOrganizationIdRolesRoleIdUsersGet(organizationId,  roleId,  offset,  limit,  attributes,  filter );
    }

    @Valid
    @DELETE
    @Path("/{organization-id}/roles/{role-id}/users/{user-id}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Delete an organization user role mapping ", notes = "This API is used to delete user role mappings for an organization. ", response = Void.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "No Content", response = Void.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdUsersUserIdDelete(@ApiParam(value = "ID of the organization of which, the user role mappings will be deleted.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "ID of the role of which, the user will be deleted.",required=true) @PathParam("role-id") String roleId, @ApiParam(value = "ID of the user.",required=true) @PathParam("user-id") String userId,     @Valid@ApiParam(value = "The deletion should proceed to sub orgs or not.")  @QueryParam("includeSubOrganizations") Boolean includeSubOrganizations) {

        return delegate.organizationsOrganizationIdRolesRoleIdUsersUserIdDelete(organizationId,  roleId,  userId,  includeSubOrganizations );
    }

    @Valid
    @PATCH
    @Path("/{organization-id}/roles/{role-id}/users/{user-id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update an organization user role mapping. ", notes = "This API is used to update the inheritance of a user role assigned over an organization. This will allow to change the includeSubOrganizations property of the role mapping. ", response = Void.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Ok", response = Void.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdRolesRoleIdUsersUserIdPatch(@ApiParam(value = "ID of the organization of which, the user role mappings will be updated.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "ID of the role of which, the user will be updated.",required=true) @PathParam("role-id") String roleId, @ApiParam(value = "ID of the user.",required=true) @PathParam("user-id") String userId, @ApiParam(value = "This represents the patch operation." ,required=true) @Valid List<UserRoleOperationDTO> userRoleOperationDTO) {

        return delegate.organizationsOrganizationIdRolesRoleIdUsersUserIdPatch(organizationId,  roleId,  userId,  userRoleOperationDTO );
    }

    @Valid
    @GET
    @Path("/{organization-id}/users/{user-id}/roles")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Retrive the list of roles that a particular user has against an organization. ", notes = "This API is used to get the list of roles for a user for an organization. ", response = RoleDTO.class, responseContainer = "List", authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization Role Management", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Ok", response = RoleDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsOrganizationIdUsersUserIdRolesGet(@ApiParam(value = "ID of the organization of which, the users will be returned.",required=true) @PathParam("organization-id") String organizationId, @ApiParam(value = "ID of the user.",required=true) @PathParam("user-id") String userId) {

        return delegate.organizationsOrganizationIdUsersUserIdRolesGet(organizationId,  userId );
    }

    @Valid
    @POST
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Create a new organization.", notes = "This API is used to create the organization defined in the user input.", response = OrganizationResponse.class, authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {
            
        })
    }, tags={ "Organization" })
    @ApiResponses(value = { 
        @ApiResponse(code = 201, message = "Successful response", response = OrganizationResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response organizationsPost(@ApiParam(value = "This represents the organization to be added." ,required=true) @Valid OrganizationPOSTRequest organizationPOSTRequest) {

        return delegate.organizationsPost(organizationPOSTRequest );
    }

}

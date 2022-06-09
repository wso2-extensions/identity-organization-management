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
import org.wso2.carbon.identity.organization.management.endpoint.OrganizationsApiService;

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

    @Valid
    @POST
    @Path("/{organization-id}/applications/{application-id}/share")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Share application from the parent tenant to given organization ", notes = "This API creates an internal application to delegate access from ", authorizations = {
        @Authorization(value = "BasicAuth"),
        @Authorization(value = "OAuth2", scopes = {

        })
    }, tags={ "Organization Application Management" })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Ok", response = Void.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "Requested resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response shareOrgApplication(
            @ApiParam(value = "ID of the parent organization where the application is created." ,required=true)
            @PathParam("organization-id") String organizationId,
            @ApiParam(value = "ID of the application which will be shared to child organizations.",required=true)
            @PathParam("application-id") String applicationId,
            @ApiParam(value = "Array of IDs of child organizations, if not provided application will be shared to all child organizations.") @Valid List<String> childOrgs) {

        return delegate.shareOrgApplication(organizationId, applicationId, childOrgs);
    }

}

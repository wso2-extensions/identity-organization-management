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

package org.wso2.carbon.identity.organization.management.endpoint.service;

import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.management.endpoint.model.Attribute;
import org.wso2.carbon.identity.organization.management.endpoint.model.BasicOrganizationResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.ChildOrganization;
import org.wso2.carbon.identity.organization.management.endpoint.model.GetOrganizationResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPOSTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPUTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPatchRequestItem;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleMappingDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleOperationDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.ParentOrganization;
import org.wso2.carbon.identity.organization.management.endpoint.util.RoleMgtEndpointUtils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtClientException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.Role;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.RoleMember;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserRoleMappingUser;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserRoleOperation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.ChildOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.organization.management.endpoint.constants.OrganizationManagementEndpointConstants.ORGANIZATION_ROLES_PATH;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.getResourceLocation;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.handleClientErrorResponse;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.handleServerErrorResponse;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.buildURIForBody;

/**
 * Perform organization management related operations.
 */
public class OrganizationManagementService {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementService.class);

    /**
     * Retrieve organization IDs.
     *
     * @return The list of organization IDs.
     */
    public Response getOrganizations() {

        try {
            List<String> organizationIds = getOrganizationManager().getOrganizationIds();
            return Response.ok().entity(getOrganizationsResponse(organizationIds)).build();
        } catch (OrganizationManagementClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Delete an organization.
     *
     * @param organizationId Unique identifier for the requested organization to be deleted.
     * @param force          Enforces the forceful deletion of child organizations belonging to this organization.
     * @return Organization deletion response.
     */
    public Response deleteOrganization(String organizationId, Boolean force) {

        try {
            getOrganizationManager().deleteOrganization(organizationId, force);
            return Response.noContent().build();
        } catch (OrganizationManagementClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Fetch an organization.
     *
     * @param organizationId Unique identifier for the requested organization to be fetched.
     * @param showChildren   If true, includes child organization IDs belonging to this organization in the response.
     * @return Requested organization details.
     */
    public Response getOrganization(String organizationId, Boolean showChildren) {

        try {
            Organization organization = getOrganizationManager().getOrganization(organizationId, showChildren);
            return Response.ok().entity(getOrganizationResponseWithChildren(organization)).build();
        } catch (OrganizationManagementClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Patch an organization.
     *
     * @param organizationId               Unique identifier for the requested organization to be patched.
     * @param organizationPatchRequestItem The list of organization details to be patched.
     * @return The patched organization.
     */
    public Response patchOrganization(String organizationId, List<OrganizationPatchRequestItem>
            organizationPatchRequestItem) {

        try {
            Organization organization = getOrganizationManager().patchOrganization(organizationId,
                    organizationPatchRequestItem.stream().map(op ->
                            new PatchOperation(op.getOperation() == null ? null : op.getOperation().toString(),
                                    op.getPath(), op.getValue())).collect(Collectors.toList()));
            return Response.ok().entity(getOrganizationResponse(organization)).build();
        } catch (OrganizationManagementClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Update an organization.
     *
     * @param organizationId         Unique identifier for the requested organization to be updated.
     * @param organizationPUTRequest The organization update request.
     * @return The updated organization.
     */
    public Response updateOrganization(String organizationId, OrganizationPUTRequest organizationPUTRequest) {

        try {
            Organization updatedOrganization = getUpdatedOrganization(organizationId, organizationPUTRequest);
            return Response.ok().entity(getOrganizationResponse(updatedOrganization)).build();
        } catch (OrganizationManagementClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Add an organization.
     *
     * @param organizationPOSTRequest Add organization request.
     * @return The newly created organization.
     */
    public Response addOrganization(OrganizationPOSTRequest organizationPOSTRequest) {

        try {
            Organization organization = getOrganizationManager().addOrganization(getOrganizationFromPostRequest
                    (organizationPOSTRequest));
            String organizationId = organization.getId();
            return Response.created(getResourceLocation(organizationId)).entity
                    (getOrganizationResponse(organization)).build();
        } catch (OrganizationManagementClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return handleServerErrorResponse(e, LOG);
        }
    }

    public Response addOrganizationUserRoleMappings(String organizationId, UserRoleMappingDTO
            userRoleMappingDTO) {
        try {
            UserRoleMapping newUserRoleMappings = new UserRoleMapping(userRoleMappingDTO.getRoleId(),
                    userRoleMappingDTO.getUsers()
                            .stream()
                            .map(mapping -> new UserRoleMappingUser(mapping.getUserId(), mapping.getMandatory(),
                                    mapping.getIncludeSubOrgs()))
                            .collect(Collectors.toList()));
            RoleMgtEndpointUtils.getOrganizationUserRoleManager().addOrganizationUserRoleMappings(organizationId, newUserRoleMappings);
            return Response.created(getOrganizationRoleResourceURI(organizationId)).build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return RoleMgtEndpointUtils.handleBadRequestResponse(e, LOG);
        } catch (Throwable throwable) {
            return RoleMgtEndpointUtils.handleUnexpectedServerError(throwable, LOG);
        }
    }

    public Response getUsersFromOrganizationAndRole(String organizationId, String roleId,
                                                                   Integer offset, Integer limit, String attributes,
                                                                   String filter) {

        try {
            if ((limit != null && limit < 1) && (offset != null && offset < 0)) {
                throw handleClientException(OrganizationUserRoleMgtConstants.ErrorMessages
                        .INVALID_ORGANIZATION_ROLE_USERS_GET_REQUEST, null);
            }
            // If pagination parameters are not set, then set them to -1
            limit = limit == null ?  Integer.valueOf(-1) : limit;
            offset = offset == null ? Integer.valueOf(-1) : offset;
            List<String> requestedAttributes = attributes == null ? new ArrayList<>() :
                    Arrays.stream(attributes.split(",")).map(String :: trim).collect(Collectors.toList());
            if (!requestedAttributes.contains("userName")) {
                requestedAttributes.add("userName");
            }
            List<RoleMember> roleMembers = RoleMgtEndpointUtils.getOrganizationUserRoleManager()
                    .getUsersByOrganizationAndRole(organizationId, roleId, offset, limit, requestedAttributes, filter);
            return Response.ok().entity(roleMembers.stream().map(RoleMember::getUserAttributes)
                    .collect(Collectors.toList())).build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return RoleMgtEndpointUtils.handleBadRequestResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return RoleMgtEndpointUtils.handleServerErrorResponse(e, LOG);
        } catch (Throwable e) {
            return RoleMgtEndpointUtils.handleUnexpectedServerError(e, LOG);
        }
    }

    public Response deleteOrganizationUserRoleMapping(String organizationId, String roleId,
                                                                            String userId, Boolean includeSubOrgs) {

        try {
            RoleMgtEndpointUtils.getOrganizationUserRoleManager()
                    .deleteOrganizationsUserRoleMapping(organizationId, userId, roleId, includeSubOrgs);
            return Response.noContent().build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return RoleMgtEndpointUtils.handleBadRequestResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return RoleMgtEndpointUtils.handleServerErrorResponse(e, LOG);
        } catch (Throwable throwable) {
            return RoleMgtEndpointUtils.handleUnexpectedServerError(throwable, LOG);
        }
    }

    public Response patchOrganizationUserRoleMapping(String organizationId, String roleId,
                                                                           String userId,
                                                                           List<UserRoleOperationDTO>
                                                                                   userRoleOperationDTO) {
        try {
            RoleMgtEndpointUtils.getOrganizationUserRoleManager().patchOrganizationsUserRoleMapping(organizationId, roleId, userId,
                    userRoleOperationDTO.stream().map(op -> new UserRoleOperation(op.getOp(), op.getPath(),
                                    op.getValue()))
                            .collect(Collectors.toList()));
            return Response.noContent().build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return RoleMgtEndpointUtils.handleBadRequestResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return RoleMgtEndpointUtils.handleServerErrorResponse(e, LOG);
        } catch (Throwable throwable) {
            return RoleMgtEndpointUtils.handleUnexpectedServerError(throwable, LOG);
        }
    }

    public Response getRolesFromOrganizationAndUser(String organizationId, String userId) {
        try {
            List<Role> roles = RoleMgtEndpointUtils.getOrganizationUserRoleManager().getRolesByOrganizationAndUser(organizationId, userId);
            return Response.ok().entity(roles).build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return RoleMgtEndpointUtils.handleBadRequestResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return RoleMgtEndpointUtils.handleServerErrorResponse(e, LOG);
        } catch (Throwable throwable) {
            return RoleMgtEndpointUtils.handleUnexpectedServerError(throwable, LOG);
        }
    }


    private URI getOrganizationRoleResourceURI(String organizationId) throws URISyntaxException {

        return new URI(String.format(ORGANIZATION_ROLES_PATH, organizationId));
    }

    private Organization getOrganizationFromPostRequest(OrganizationPOSTRequest organizationPOSTRequest) {

        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        organization.setName(organizationPOSTRequest.getName());
        organization.setDescription(organizationPOSTRequest.getDescription());
        organization.getParent().setId(organizationPOSTRequest.getParentId());
        List<Attribute> organizationAttributes = organizationPOSTRequest.getAttributes();
        if (CollectionUtils.isNotEmpty(organizationAttributes)) {
            organization.setAttributes(organizationAttributes.stream().map(attribute ->
                    new OrganizationAttribute(attribute.getKey(), attribute.getValue())).collect(Collectors.toList()));
        }
        return organization;
    }

    private OrganizationResponse getOrganizationResponse(Organization organization) {

        OrganizationResponse organizationResponse = new OrganizationResponse();
        organizationResponse.setId(organization.getId());
        organizationResponse.setName(organization.getName());
        organizationResponse.setDescription(organization.getDescription());
        organizationResponse.setCreated(organization.getCreated().toString());
        organizationResponse.setLastModified(organization.getLastModified().toString());
        ParentOrganizationDO parentOrganizationDO = organization.getParent();
        if (parentOrganizationDO != null) {
            organizationResponse.setParent(getParentOrganization(parentOrganizationDO));
        }

        List<Attribute> attributeList = getOrganizationAttributes(organization);
        if (!attributeList.isEmpty()) {
            organizationResponse.setAttributes(attributeList);
        }
        return organizationResponse;
    }

    private GetOrganizationResponse getOrganizationResponseWithChildren(Organization organization) {

        GetOrganizationResponse organizationResponse = new GetOrganizationResponse();
        organizationResponse.setId(organization.getId());
        organizationResponse.setName(organization.getName());
        organizationResponse.setDescription(organization.getDescription());
        organizationResponse.setCreated(organization.getCreated().toString());
        organizationResponse.setLastModified(organization.getLastModified().toString());
        ParentOrganizationDO parentOrganizationDO = organization.getParent();
        if (parentOrganizationDO != null) {
            organizationResponse.setParent(getParentOrganization(parentOrganizationDO));
        }

        setOrganizationChildren(organization, organizationResponse);

        List<Attribute> attributeList = getOrganizationAttributes(organization);
        if (!attributeList.isEmpty()) {
            organizationResponse.setAttributes(attributeList);
        }
        return organizationResponse;
    }

    private List<Attribute> getOrganizationAttributes(Organization organization) {

        List<Attribute> attributeList = new ArrayList<>();
        for (OrganizationAttribute attributeModel : organization.getAttributes()) {
            Attribute attribute = new Attribute();
            attribute.setKey(attributeModel.getKey());
            attribute.setValue(attributeModel.getValue());
            attributeList.add(attribute);
        }
        return attributeList;
    }

    private ParentOrganization getParentOrganization(ParentOrganizationDO parentOrganizationDO) {

        ParentOrganization parentOrganization = new ParentOrganization();
        parentOrganization.setId(parentOrganizationDO.getId());
        parentOrganization.setSelf(parentOrganizationDO.getSelf());
        return parentOrganization;
    }

    private void setOrganizationChildren(Organization organization, GetOrganizationResponse organizationResponse) {

        if (CollectionUtils.isNotEmpty(organization.getChildOrganizations())) {
            List<ChildOrganization> childOrganizations = new ArrayList<>();
            for (ChildOrganizationDO childOrganizationDO : organization.getChildOrganizations()) {
                ChildOrganization childOrganization = new ChildOrganization();
                childOrganization.setId(childOrganizationDO.getId());
                childOrganization.setSelf(childOrganizationDO.getSelf());
                childOrganizations.add(childOrganization);
            }
            if (!childOrganizations.isEmpty()) {
                organizationResponse.setChildren(childOrganizations);
            }
        }
    }

    private List<BasicOrganizationResponse> getOrganizationsResponse(List<String> organizationIds) throws
            OrganizationManagementServerException {

        List<BasicOrganizationResponse> organizationDTOs = new ArrayList<>();
        for (String org : organizationIds) {
            BasicOrganizationResponse organizationDTO = new BasicOrganizationResponse();
            organizationDTO.setId(org);
            organizationDTO.setSelf(buildURIForBody(org));

            organizationDTOs.add(organizationDTO);
        }
        return organizationDTOs;
    }

    private Organization getUpdatedOrganization(String organizationId, OrganizationPUTRequest organizationPUTRequest)
            throws OrganizationManagementException {

        Organization oldOrganization = getOrganizationManager().getOrganization(organizationId, false);
        String currentOrganizationName = oldOrganization.getName();
        Organization organization = createOrganizationClone(oldOrganization);

        organization.setName(organizationPUTRequest.getName());
        organization.setDescription(organizationPUTRequest.getDescription());
        List<Attribute> organizationAttributes = organizationPUTRequest.getAttributes();
        if (CollectionUtils.isNotEmpty(organizationAttributes)) {
            organization.setAttributes(organizationAttributes.stream().map(attribute ->
                    new OrganizationAttribute(attribute.getKey(), attribute.getValue())).collect(Collectors.toList()));
        } else {
            organization.setAttributes(null);
        }
        return getOrganizationManager().updateOrganization(organizationId, currentOrganizationName, organization);
    }

    private Organization createOrganizationClone(Organization organization) {

        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(organization), Organization.class);
    }

    private OrganizationManager getOrganizationManager() {

        return (OrganizationManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService
                (OrganizationManager.class, null);
    }


}

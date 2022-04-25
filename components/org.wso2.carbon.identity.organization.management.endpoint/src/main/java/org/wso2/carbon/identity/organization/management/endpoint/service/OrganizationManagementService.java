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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.endpoint.exceptions.OrganizationManagementEndpointException;
import org.wso2.carbon.identity.organization.management.endpoint.model.Attribute;
import org.wso2.carbon.identity.organization.management.endpoint.model.BasicOrganizationResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.ChildOrganization;
import org.wso2.carbon.identity.organization.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.endpoint.model.GetOrganizationResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.Link;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPOSTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPUTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPatchRequestItem;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationsResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.ParentOrganization;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleMappingDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleMappingUsersDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleOperationDTO;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtClientException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.Role;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserForUserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserRoleOperation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.ChildOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.organization.management.endpoint.constants.OrganizationManagementEndpointConstants.ASC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.endpoint.constants.OrganizationManagementEndpointConstants.DESC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.buildURIForPagination;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.getError;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.getOrganizationRoleResourceURI;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.getOrganizationUserRoleManager;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.getResourceLocation;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.handleClientErrorResponse;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.handleServerErrorResponse;
import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.handleUnexpectedServerError;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ADD_ORG_ROLE_USER_REQUEST_NULL_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ADD_ORG_ROLE_USER_REQUEST_NULL_USERS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.FORCED_FIELD_NULL;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.INVALID_FORCED_AND_INCLUDE_SUB_ORGS_VALUES;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.INVALID_ORGANIZATION_ROLE_USERS_GET_REQUEST;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.USER_ID_NULL;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_PAGINATION_PARAMETER_NEGATIVE_LIMIT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ROOT;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.buildURIForBody;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.generateUniqueID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;

/**
 * Perform organization management related operations.
 */
public class OrganizationManagementService {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementService.class);

    /**
     * Retrieve organization IDs.
     *
     * @param filter The filter string.
     * @param limit  The maximum number of records to be returned.
     * @param after  The pointer to next page.
     * @param before The pointer to previous page.
     * @return The list of organization IDs.
     */
    public Response getOrganizations(String filter, Integer limit, String after, String before) {

        try {
            limit = validateLimit(limit);
            String sortOrder = StringUtils.isNotBlank(before) ? ASC_SORT_ORDER : DESC_SORT_ORDER;
            List<BasicOrganization> organizations = getOrganizationManager().getOrganizations(limit + 1, after,
                    before, sortOrder, filter);
            return Response.ok().entity(getOrganizationsResponse(limit, after, before, filter, organizations)).build();
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
     * @return Organization deletion response.
     */
    public Response deleteOrganization(String organizationId) {

        try {
            getOrganizationManager().deleteOrganization(organizationId);
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
            Organization organization = getOrganizationManager().getOrganization(organizationId,
                    Boolean.TRUE.equals(showChildren));
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

    /**
     * Adding new organization-user-role mappings.
     *
     * @param organizationId     ID of organization
     * @param userRoleMappingDTO DTO of user-role mapping
     * @return the response
     */
    public Response addOrganizationUserRoleMappings(String organizationId, UserRoleMappingDTO
            userRoleMappingDTO) {

        try {
            validateAddOrganizationUserRoleMappingRequestBody(organizationId, userRoleMappingDTO);
            UserRoleMapping newUserRoleMappings = new UserRoleMapping(userRoleMappingDTO.getRoleId(),
                    userRoleMappingDTO.getUsers()
                            .stream()
                            .map(mapping -> new UserForUserRoleMapping(mapping.getUserId(), mapping.getForced(),
                                    mapping.getIncludeSubOrganizations() == null ? mapping.getForced() :
                                            mapping.getIncludeSubOrganizations()))
                            .collect(Collectors.toList()));
            getOrganizationUserRoleManager()
                    .addOrganizationUserRoleMappings(organizationId, newUserRoleMappings);
            return Response.created(getOrganizationRoleResourceURI(organizationId)).build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return handleServerErrorResponse(e, LOG);
        } catch (Throwable throwable) {
            return handleUnexpectedServerError(throwable, LOG);
        }
    }

    /**
     * Get users from an organization which have been assigned a particular role.
     *
     * @param organizationId ID of the organization.
     * @param roleId         ID of role.
     * @param offset         offset.
     * @param limit          limit.
     * @param attributes     attributes.
     * @param filter         filter.
     * @return a list of users.
     */
    public Response getUsersFromOrganizationAndRole(String organizationId, String roleId,
                                                    Integer offset, Integer limit, String attributes,
                                                    String filter) {

        try {
            if ((limit != null && limit < 1) && (offset != null && offset < 0)) {
                throw handleClientException(INVALID_ORGANIZATION_ROLE_USERS_GET_REQUEST, null);
            }
            // If pagination parameters are not set, then set them to -1
            limit = limit == null ? Integer.valueOf(-1) : limit;
            offset = offset == null ? Integer.valueOf(-1) : offset;
            List<String> requestedAttributes = attributes == null ? new ArrayList<>() :
                    Arrays.stream(attributes.split(",")).map(String::trim).collect(Collectors.toList());
            if (!requestedAttributes.contains("userName")) {
                requestedAttributes.add("userName");
            }
            List<Map<String, Object>> usersWithUserAttributesList = getOrganizationUserRoleManager()
                    .getUsersByOrganizationAndRole(organizationId, roleId, offset, limit, requestedAttributes, filter);
            return Response.ok().entity(usersWithUserAttributesList).build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return handleServerErrorResponse(e, LOG);
        } catch (Throwable throwable) {
            return handleUnexpectedServerError(throwable, LOG);
        }
    }

    /**
     * Delete organization-user-role mappings.
     *
     * @param organizationId ID of the organization.
     * @param roleId         ID of role.
     * @param userId         ID of user.
     * @param includeSubOrgs whether including sub-organizations or not.
     * @return whether the deletion is successful or not.
     */
    public Response deleteOrganizationUserRoleMapping(String organizationId, String roleId,
                                                      String userId, Boolean includeSubOrgs) {

        try {
            getOrganizationUserRoleManager()
                    .deleteOrganizationsUserRoleMapping(organizationId, userId, roleId,
                            includeSubOrgs == null ? false : includeSubOrgs);
            return Response.noContent().build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return handleServerErrorResponse(e, LOG);
        } catch (Throwable throwable) {
            return handleUnexpectedServerError(throwable, LOG);
        }
    }

    /**
     * Patching the forced field of organization-user-role mappings.
     *
     * @param organizationId       ID of the organization.
     * @param roleId               ID of role.
     * @param userId               ID of user.
     * @param userRoleOperationDTO DTO of user role operation.
     * @return whether the patch operation is successful or not.
     */
    public Response patchOrganizationUserRoleMapping(String organizationId, String roleId,
                                                     String userId,
                                                     List<UserRoleOperationDTO>
                                                             userRoleOperationDTO) {
        try {
            getOrganizationUserRoleManager()
                    .patchOrganizationsUserRoleMapping(organizationId, roleId, userId,
                            userRoleOperationDTO.stream().map(op -> new UserRoleOperation(op.getOp(), op.getPath(),
                                            op.getValue()))
                                    .collect(Collectors.toList()));
            return Response.noContent().build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return handleServerErrorResponse(e, LOG);
        } catch (Throwable throwable) {
            return handleUnexpectedServerError(throwable, LOG);
        }
    }

    /**
     * Get roles assigned by a user within an organization.
     *
     * @param organizationId ID of the organization.
     * @param userId         ID of user.
     * @return A list of roles.
     */
    public Response getRolesFromOrganizationAndUser(String organizationId, String userId) {
        try {
            List<Role> roles = getOrganizationUserRoleManager()
                    .getRolesByOrganizationAndUser(organizationId, userId);
            return Response.ok().entity(roles).build();
        } catch (OrganizationUserRoleMgtClientException e) {
            return handleClientErrorResponse(e, LOG);
        } catch (OrganizationUserRoleMgtException e) {
            return handleServerErrorResponse(e, LOG);
        } catch (Throwable throwable) {
            return handleUnexpectedServerError(throwable, LOG);
        }
    }


    private Organization getOrganizationFromPostRequest(OrganizationPOSTRequest organizationPOSTRequest) {

        Organization organization = new Organization();
        organization.setId(generateUniqueID());
        organization.setName(organizationPOSTRequest.getName());
        organization.setDescription(organizationPOSTRequest.getDescription());
        organization.setStatus(OrganizationManagementConstants.OrganizationStatus.ACTIVE.toString());
        String parentId = organizationPOSTRequest.getParentId();
        if (StringUtils.isNotBlank(parentId)) {
            organization.getParent().setId(parentId);
        } else {
            organization.getParent().setId(ROOT);
        }
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

        OrganizationResponse.StatusEnum status;
        try {
            status = OrganizationResponse.StatusEnum.valueOf(organization.getStatus());
        } catch (IllegalArgumentException e) {
            status = OrganizationResponse.StatusEnum.DISABLED;
        }
        organizationResponse.setStatus(status);

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

        GetOrganizationResponse.StatusEnum status;
        try {
            status = GetOrganizationResponse.StatusEnum.valueOf(organization.getStatus());
        } catch (IllegalArgumentException e) {
            status = GetOrganizationResponse.StatusEnum.DISABLED;
        }
        organizationResponse.setStatus(status);

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
        parentOrganization.setRef(parentOrganizationDO.getRef());
        return parentOrganization;
    }

    private void setOrganizationChildren(Organization organization, GetOrganizationResponse organizationResponse) {

        if (CollectionUtils.isNotEmpty(organization.getChildOrganizations())) {
            List<ChildOrganization> childOrganizations = new ArrayList<>();
            for (ChildOrganizationDO childOrganizationDO : organization.getChildOrganizations()) {
                ChildOrganization childOrganization = new ChildOrganization();
                childOrganization.setId(childOrganizationDO.getId());
                childOrganization.setRef(childOrganizationDO.getRef());
                childOrganizations.add(childOrganization);
            }
            if (!childOrganizations.isEmpty()) {
                organizationResponse.setChildren(childOrganizations);
            }
        }
    }

    private int validateLimit(Integer limit) throws OrganizationManagementClientException {

        if (limit == null) {
            int defaultItemsPerPage = IdentityUtil.getDefaultItemsPerPage();
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Given limit is null. Therefore the default limit is set to %s.",
                        defaultItemsPerPage));
            }
            return defaultItemsPerPage;
        }

        if (limit < 0) {
            throw handleClientException(ERROR_CODE_INVALID_PAGINATION_PARAMETER_NEGATIVE_LIMIT);
        }

        int maximumItemsPerPage = IdentityUtil.getMaximumItemPerPage();
        if (limit > maximumItemsPerPage) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Given limit exceeds the maximum limit. Therefore the limit is set to %s.",
                        maximumItemsPerPage));
            }
            return maximumItemsPerPage;
        }

        return limit;
    }

    private OrganizationsResponse getOrganizationsResponse(Integer limit, String after, String before, String filter,
                                                           List<BasicOrganization> organizations)
            throws OrganizationManagementServerException {

        OrganizationsResponse organizationsResponse = new OrganizationsResponse();

        if (CollectionUtils.isNotEmpty(organizations)) {
            boolean hasMoreItems = organizations.size() > limit;
            boolean needsReverse = StringUtils.isNotBlank(before);
            boolean isFirstPage = (StringUtils.isBlank(before) && StringUtils.isBlank(after)) ||
                    (StringUtils.isNotBlank(before) && !hasMoreItems);
            boolean isLastPage = !hasMoreItems && (StringUtils.isNotBlank(after) || StringUtils.isBlank(before));

            String url = "?limit=" + limit;
            if (StringUtils.isNotBlank(filter)) {
                try {
                    url += "&filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    LOG.error("Server encountered an error while building pagination URL for the response.", e);
                    Error error = getError(ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL.getCode(),
                            ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL.getMessage(),
                            ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL.getDescription());
                    throw new OrganizationManagementEndpointException(Response.Status.INTERNAL_SERVER_ERROR, error);
                }
            }

            if (hasMoreItems) {
                organizations.remove(organizations.size() - 1);
            }
            if (needsReverse) {
                Collections.reverse(organizations);
            }
            if (!isFirstPage) {
                String encodedString = Base64.getEncoder().encodeToString(organizations.get(0).getCreated()
                        .getBytes(StandardCharsets.UTF_8));
                Link link = new Link();
                link.setHref(URI.create(buildURIForPagination(url) + "&before=" + encodedString));
                link.setRel("previous");
                organizationsResponse.addLinksItem(link);
            }
            if (!isLastPage) {
                String encodedString = Base64.getEncoder().encodeToString(organizations.get(organizations.size() - 1)
                        .getCreated().getBytes(StandardCharsets.UTF_8));
                Link link = new Link();
                link.setHref(URI.create(buildURIForPagination(url) + "&after=" + encodedString));
                link.setRel("next");
                organizationsResponse.addLinksItem(link);
            }

            List<BasicOrganizationResponse> organizationDTOs = new ArrayList<>();
            for (BasicOrganization organization : organizations) {
                BasicOrganizationResponse organizationDTO = new BasicOrganizationResponse();
                organizationDTO.setId(organization.getId());
                organizationDTO.setName(organization.getName());
                organizationDTO.setRef(buildURIForBody(organization.getId()));
                organizationDTOs.add(organizationDTO);
            }
            organizationsResponse.setOrganizations(organizationDTOs);
        }
        return organizationsResponse;
    }

    private Organization getUpdatedOrganization(String organizationId, OrganizationPUTRequest organizationPUTRequest)
            throws OrganizationManagementException {

        Organization oldOrganization = getOrganizationManager().getOrganization(organizationId, false);
        String currentOrganizationName = oldOrganization.getName();
        Organization organization = createOrganizationClone(oldOrganization);

        organization.setName(organizationPUTRequest.getName());
        organization.setDescription(organizationPUTRequest.getDescription());

        OrganizationPUTRequest.StatusEnum statusEnum = organizationPUTRequest.getStatus();
        if (statusEnum != null) {
            String organizationStatus = statusEnum.toString();
            if (StringUtils.isNotBlank(organizationStatus)) {
                organization.setStatus(organizationStatus);
            }
        } else {
            organization.setStatus(null);
        }

        List<Attribute> organizationAttributes = organizationPUTRequest.getAttributes();
        if (CollectionUtils.isNotEmpty(organizationAttributes)) {
            organization.setAttributes(organizationAttributes.stream().map(attribute ->
                    new OrganizationAttribute(attribute.getKey(), attribute.getValue())).collect(Collectors.toList()));
        } else {
            organization.setAttributes(null);
        }
        return getOrganizationManager().updateOrganization(organizationId, currentOrganizationName, organization);
    }

    private void validateAddOrganizationUserRoleMappingRequestBody(String organizationId,
                                                                   UserRoleMappingDTO userRoleMappingDTO)
            throws OrganizationUserRoleMgtClientException {

        if (StringUtils.isBlank(userRoleMappingDTO.getRoleId())) {
            throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_NULL_ROLE_ID, organizationId);
        }
        List<UserRoleMappingUsersDTO> usersList = userRoleMappingDTO.getUsers();
        if (usersList == null) {
            throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_NULL_USERS, organizationId);
        }
        for (UserRoleMappingUsersDTO user : usersList) {
            if (StringUtils.isBlank(user.getUserId())) {
                throw handleClientException(USER_ID_NULL, organizationId);
            }
            if (user.getForced() == null) {
                throw handleClientException(FORCED_FIELD_NULL, organizationId);
            }
            if (user.getIncludeSubOrganizations() == null && !user.getForced()) {
                throw handleClientException(INVALID_FORCED_AND_INCLUDE_SUB_ORGS_VALUES, null);
            }
        }
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

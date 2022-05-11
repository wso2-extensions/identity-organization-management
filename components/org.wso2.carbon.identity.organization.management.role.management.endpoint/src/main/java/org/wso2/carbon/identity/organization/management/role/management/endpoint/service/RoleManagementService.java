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

package org.wso2.carbon.identity.organization.management.role.management.endpoint.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.exception.RoleManagementEndpointException;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.Link;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseGroup;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseUser;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleObj;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleObjMeta;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestGroup;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestUser;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestGroup;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestUser;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutResponseMeta;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolesListResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.util.RoleManagementEndpointUtils;
import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementClientException;
import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementException;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicGroup;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicUser;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constant.RoleManagementEndpointConstants.ASC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constant.RoleManagementEndpointConstants.DESC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_PAGINATION_PARAMETER_NEGATIVE_LIMIT;

/**
 * The service class for Role Management in Organization Management.
 */
public class RoleManagementService {

    private static final Log LOG = LogFactory.getLog(RoleManagementService.class);

    /**
     * Service for creating a role inside an organization.
     *
     * @param organizationId        The ID of the organization.
     * @param rolePostRequest The object created from request body.
     * @return The role creation response.
     */
    public Response createRole(String organizationId, RolePostRequest rolePostRequest) {

        try {
            Role role = RoleManagementEndpointUtils.getRoleManager().addRole(organizationId,
                    generateRoleFromPostRequest(rolePostRequest));
            String roleId = role.getId();
            URI roleURI = RoleManagementEndpointUtils.URIBuilder.ROLE_URI.buildURI(organizationId, roleId);
            return Response.created(roleURI).entity
                    (getRolePostResponse(role, roleURI)).build();
        } catch (RoleManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (RoleManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Service for deleting a role inside an organization.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of the role.
     * @return The role deletion response.
     */
    public Response deleteRole(String organizationId, String roleId) {

        try {
            RoleManagementEndpointUtils.getRoleManager().deleteRole(organizationId, roleId);
            return Response.noContent().build();
        } catch (RoleManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (RoleManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Service for getting a role using role ID and organization ID.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of the role.
     * @return The role corresponding to roleId and organizationId.
     */
    public Response getRoleUsingOrganizationIdAndRoleId(String organizationId, String roleId) {

        try {
            Role role = RoleManagementEndpointUtils.getRoleManager().getRoleById(organizationId, roleId);
            URI roleURI = RoleManagementEndpointUtils.URIBuilder.ROLE_URI.buildURI(organizationId, roleId);
            return Response.ok().entity
                    (getRoleGetResponse(organizationId, role, roleURI)).build();
        } catch (RoleManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (RoleManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Get the roles inside an organization.
     *
     * @param organizationId The ID of the organization.
     * @param filter         Param for filtering the results.
     * @param limit          Param for limiting the results.
     * @param before         The previous result page pointer.
     * @param after          The next result page pointer.
     * @return The roles inside an organization.
     */
    public Response getRolesOfOrganization(String organizationId, String filter, Integer limit, String before,
                                           String after) {

        try {
            int limitValue = validateLimit(limit);
            String sortOrder = StringUtils.isNotBlank(before) ? ASC_SORT_ORDER : DESC_SORT_ORDER;
            List<Role> roles = RoleManagementEndpointUtils.getRoleManager()
                    .getOrganizationRoles(limitValue + 1, after, before, sortOrder, filter, organizationId);
            return Response.ok().entity(getRoleListResponse(limitValue, after, before, filter, organizationId, roles))
                    .build();
        } catch (RoleManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (RoleManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Service for patching a role using role ID and organization ID.
     *
     * @param organizationId         The ID of the organization.
     * @param roleId                 The ID of the role.
     * @param rolePatchRequest The request object created using request body.
     * @return The role patch response.
     */
    public Response patchRole(String organizationId, String roleId,
                              RolePatchRequest rolePatchRequest) {

        try {
            List<RolePatchOperation> patchOperationList = rolePatchRequest.getOperations();
            List<PatchOperation> patchOperations = new ArrayList<>();

            for (RolePatchOperation rolePatchOperation : patchOperationList) {
                List<String> values = rolePatchOperation.getValue();
                PatchOperation patchOperation = new PatchOperation(rolePatchOperation.getOp().toString(),
                        rolePatchOperation.getPath(), values);
                patchOperations.add(patchOperation);
            }

            Role role = RoleManagementEndpointUtils.getRoleManager().patchRole(organizationId, roleId,
                    patchOperations);
            URI roleURI = RoleManagementEndpointUtils.URIBuilder.ROLE_URI.buildURI(organizationId, roleId);

            return Response.ok().entity(getRolePatchResponse(role, roleURI)).build();
        } catch (RoleManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (RoleManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Patching a role using PUT request.
     *
     * @param organizationId       The organization ID.
     * @param roleId               The role ID.
     * @param rolePutRequest The request object created using request body.
     * @return Put role response.
     */
    public Response putRole(String organizationId, String roleId, RolePutRequest rolePutRequest) {
        try {
            String displayName = rolePutRequest.getDisplayName();
            List<RolePutRequestUser> users = rolePutRequest.getUsers();
            List<RolePutRequestGroup> groups = rolePutRequest.getGroups();
            List<String> permissions = rolePutRequest.getPermissions();

            Role role = RoleManagementEndpointUtils.getRoleManager().putRole(organizationId, roleId,
                    new Role(roleId, displayName,
                            groups.stream().map(g -> new BasicGroup(g.getValue())).collect(Collectors.toList()),
                            users.stream().map(u -> new BasicUser(u.getValue())).collect(Collectors.toList()),
                            permissions));
            URI roleURI = RoleManagementEndpointUtils.URIBuilder.ROLE_URI.buildURI(organizationId, roleId);

            return Response.ok().entity(getRolePutResponse(role, roleURI)).build();
        } catch (RoleManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (RoleManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Generating a Role object from the RoleRequest.
     *
     * @param rolePostRequest The request object coming from the API.
     * @return A role object.
     */
    private Role generateRoleFromPostRequest(RolePostRequest rolePostRequest) {

        Role role = new Role();
        role.setId(Utils.generateUniqueId());
        role.setName(rolePostRequest.getDisplayName());
        role.setUsers(rolePostRequest.getUsers().stream().map(RolePostRequestUser::getValue)
                .map(BasicUser::new).collect(Collectors.toList()));
        role.setGroups(rolePostRequest.getGroups().stream().map(RolePostRequestGroup::getValue)
                .map(BasicGroup::new).collect(Collectors.toList()));
        role.setPermissions(rolePostRequest.getPermissions());

        return role;
    }

    /**
     * Generating a RolePostResponse object for the response.
     *
     * @param role    A role object.
     * @param roleURI The URI of the role.
     * @return A RolePostResponse.
     */
    private RolePostResponse getRolePostResponse(Role role, URI roleURI) {

        RoleObjMeta roleObjMeta = new RoleObjMeta();
        roleObjMeta.location(roleURI.toString());

        RolePostResponse response = new RolePostResponse();
        response.setId(role.getId());
        response.setDisplayName(role.getName());
        response.setMeta(roleObjMeta);

        return response;
    }

    /**
     * Generating  RoleGetResponse for the response.
     *
     * @param organizationId The ID of the organization.
     * @param role           A role object.
     * @param roleURI        The URI of the role.
     * @return A RoleGetResponse.
     */
    private RoleGetResponse getRoleGetResponse(String organizationId, Role role, URI roleURI) {

        RoleObjMeta roleObjMeta = new RoleObjMeta();
        roleObjMeta.location(roleURI.toString());

        RoleGetResponse response = new RoleGetResponse();
        response.setId(role.getId());
        response.setDisplayName(role.getName());
        response.setMeta(roleObjMeta);
        response.setPermissions(role.getPermissions());

        List<RoleGetResponseGroup> groups = new ArrayList<>();
        List<RoleGetResponseUser> users = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(role.getGroups())) {
            List<BasicGroup> roleGroups = role.getGroups();
            for (BasicGroup basicGroup : roleGroups) {
                RoleGetResponseGroup group = new RoleGetResponseGroup();
                group.value(basicGroup.getGroupId());
                group.displayName(basicGroup.getGroupName());
                group.$ref(RoleManagementEndpointUtils.URIBuilder.GROUP_URI.
                        buildURI(organizationId, basicGroup.getGroupId()).toString());
                groups.add(group);
            }
            response.setGroups(groups);
        }

        if (CollectionUtils.isNotEmpty(role.getUsers())) {
            List<BasicUser> roleUsers = role.getUsers();
            for (BasicUser basicUser : roleUsers) {
                RoleGetResponseUser user = new RoleGetResponseUser();
                user.id(basicUser.getId());
                user.displayName(basicUser.getUserName());
                user.$ref(RoleManagementEndpointUtils.URIBuilder.USER_URI.
                        buildURI(organizationId, basicUser.getId()).toString());
                users.add(user);
            }
            response.setUsers(users);
        }

        return response;
    }

    /**
     * Generate a response object for patch operation.
     *
     * @param role    The role which needed to be included in the response.
     * @param roleURI The URI of the role.
     * @return A RolePatchResponse.
     */
    private RolePatchResponse getRolePatchResponse(Role role, URI roleURI) {

        RolePutResponseMeta roleObjMeta = new RolePutResponseMeta();
        roleObjMeta.setLocation(roleURI.toString());

        RolePatchResponse response = new RolePatchResponse();
        response.setDisplayName(role.getName());
        response.setMeta(roleObjMeta);
        response.setId(role.getId());

        return response;
    }

    /**
     * Generate a response object for put operation.
     *
     * @param role    The role which needed to be included in the response.
     * @param roleURI The URI of the role.
     * @return A RolePutResponse.
     */
    private RolePutResponse getRolePutResponse(Role role, URI roleURI) {

        RolePutResponseMeta roleObjMeta = new RolePutResponseMeta();
        roleObjMeta.setLocation(roleURI.toString());

        RolePutResponse responseObject = new RolePutResponse();
        responseObject.setDisplayName(role.getName());
        responseObject.setMeta(roleObjMeta);
        responseObject.setId(role.getId());

        return responseObject;
    }

    /**
     * Generate a response object for get operation.
     *
     * @param limit Param for limiting the results.
     * @param after The next result page pointer.
     * @param before The previous result page pointer.
     * @param filter Param for filtering the results.
     * @param organizationId The ID of the organization.
     * @param roles List of roles.
     * @return The RoleListResponse.
     */
    private RolesListResponse getRoleListResponse(int limit, String after, String before, String filter,
                                                        String organizationId,
                                                        List<Role> roles) {

        RolesListResponse response = new RolesListResponse();
        if (CollectionUtils.isNotEmpty(roles)) {
            boolean hasMoreItems = roles.size() > limit;
            boolean needsReverse = StringUtils.isNotBlank(before);
            boolean isFirstPage = (StringUtils.isBlank(before) && StringUtils.isBlank(after)) ||
                    (StringUtils.isNotBlank(before) && !hasMoreItems);
            boolean isLastPage = !hasMoreItems && (StringUtils.isNotBlank(after) || StringUtils.isBlank(before));

            String url = "?limit=" + limit;
            if (org.apache.commons.lang.StringUtils.isNotBlank(filter)) {
                try {
                    url += "&filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    LOG.error("Server encountered an error while building pagination URL for the response.", e);
                    Error error = RoleManagementEndpointUtils.getError("", "", "");
                    throw new RoleManagementEndpointException(Response.Status.INTERNAL_SERVER_ERROR, error);
                }
            }

            if (hasMoreItems) {
                roles.remove(roles.size() - 1);
            }
            if (needsReverse) {
                Collections.reverse(roles);
            }
            if (!isFirstPage) {
                String encodedString = Base64.getEncoder().encodeToString(roles.get(0).getId()
                        .getBytes(StandardCharsets.UTF_8));
                Link link = new Link();
                link.setHref(URI.create(RoleManagementEndpointUtils.buildURIForPagination(url) +
                        "&before=" + encodedString));
                link.setRel("previous");
                response.addLinksItem(link);
            }
            if (!isLastPage) {
                String encodedString = Base64.getEncoder().encodeToString(roles.get(roles.size() - 1)
                        .getId().getBytes(StandardCharsets.UTF_8));
                Link link = new Link();
                link.setHref(URI.create(RoleManagementEndpointUtils.buildURIForPagination(url) +
                        "&after=" + encodedString));
                link.setRel("next");
                response.addLinksItem(link);
            }

            List<RoleObj> roleDTOs = new ArrayList<>();
            for (Role role : roles) {
                RoleObj roleObj = new RoleObj();
                RoleObjMeta roleObjMeta = new RoleObjMeta();
                roleObjMeta.setLocation(RoleManagementEndpointUtils.URIBuilder.ROLE_URI.buildURI(organizationId,
                        role.getId()).toString());
                roleObj.setId(role.getId());
                roleObj.setDisplayName(role.getName());
                roleObj.setMeta(roleObjMeta);
                roleDTOs.add(roleObj);
            }
            response.setRoles(roleDTOs);
        }
        return response;
    }

    /**
     *
     * @param limit The param for limiting results.
     * @return The limit.
     * @throws RoleManagementClientException This exception is thrown if the limit is not valid.
     */
    private int validateLimit(Integer limit) throws RoleManagementClientException {

        if (limit == null) {
            int defaultItemsPerPage = IdentityUtil.getDefaultItemsPerPage();
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Given limit is null. Therefore the default limit is set to %s.",
                        defaultItemsPerPage));
            }
            return defaultItemsPerPage;
        }

        if (limit < 0) {
            throw Utils.handleClientException(ERROR_CODE_INVALID_PAGINATION_PARAMETER_NEGATIVE_LIMIT);
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
}

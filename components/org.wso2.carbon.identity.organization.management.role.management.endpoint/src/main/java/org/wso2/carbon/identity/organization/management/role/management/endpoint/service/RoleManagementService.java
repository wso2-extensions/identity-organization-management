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
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseGroupObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseUserObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleObj;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleObjMeta;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchOperationObj;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchOperationObjValue;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestGroupObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestUserObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestGroupObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestUserObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutResponseObjectMeta;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolesListResponseObject;
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
     * @param rolePostRequestObject The object created from request body.
     * @return The role creation response.
     */
    public Response createRole(String organizationId, RolePostRequestObject rolePostRequestObject) {

        try {
            Role role = RoleManagementEndpointUtils.getRoleManager().addRole(organizationId,
                    generateRoleFromPostRequest(rolePostRequestObject));
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
     * @param rolePatchRequestObject The request object created using request body.
     * @return The role patch response.
     */
    public Response patchRole(String organizationId, String roleId,
                              RolePatchRequestObject rolePatchRequestObject) {

        try {
            List<RolePatchOperationObj> patchOperationObjs = rolePatchRequestObject.getOperations();
            List<PatchOperation> patchOperations = new ArrayList<>();

            for (RolePatchOperationObj rolePatchOperationObj : patchOperationObjs) {
                List<String> values = rolePatchOperationObj.getValue().stream().
                        map(RolePatchOperationObjValue::getValue).collect(Collectors.toList());
                PatchOperation patchOperation = new PatchOperation(rolePatchOperationObj.getOp().toString(),
                        rolePatchOperationObj.getPath(), values);
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
     * @param rolePutRequestObject The request object created using request body.
     * @return Put role response.
     */
    public Response putRole(String organizationId, String roleId, RolePutRequestObject rolePutRequestObject) {
        try {
            String displayName = rolePutRequestObject.getDisplayName();
            List<RolePutRequestUserObject> users = rolePutRequestObject.getUsers();
            List<RolePutRequestGroupObject> groups = rolePutRequestObject.getGroups();
            List<String> permissions = rolePutRequestObject.getPermissions();

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
     * Generating a Role object from the RoleRequestObject.
     *
     * @param rolePostRequestObject The request object coming from the API.
     * @return A role object.
     */
    private Role generateRoleFromPostRequest(RolePostRequestObject rolePostRequestObject) {

        Role role = new Role();
        role.setId(Utils.generateUniqueId());
        role.setName(rolePostRequestObject.getDisplayName());
        role.setUsers(rolePostRequestObject.getUsers().stream().map(RolePostRequestUserObject::getValue)
                .map(BasicUser::new).collect(Collectors.toList()));
        role.setGroups(rolePostRequestObject.getGroups().stream().map(RolePostRequestGroupObject::getValue)
                .map(BasicGroup::new).collect(Collectors.toList()));
        role.setPermissions(rolePostRequestObject.getPermissions());

        return role;
    }

    /**
     * Generating a RolePostResponseObject object for the response.
     *
     * @param role    A role object.
     * @param roleURI The URI of the role.
     * @return A RolePostResponseObject.
     */
    private RolePostResponseObject getRolePostResponse(Role role, URI roleURI) {

        RoleObjMeta roleObjMeta = new RoleObjMeta();
        roleObjMeta.location(roleURI.toString());

        RolePostResponseObject responseObject = new RolePostResponseObject();
        responseObject.setId(role.getId());
        responseObject.setDisplayName(role.getName());
        responseObject.setMeta(roleObjMeta);

        return responseObject;
    }

    /**
     * Generating  RoleGetResponseObject for the response.
     *
     * @param organizationId The ID of the organization.
     * @param role           A role object.
     * @param roleURI        The URI of the role.
     * @return A RoleGetResponseObject.
     */
    private RoleGetResponseObject getRoleGetResponse(String organizationId, Role role, URI roleURI) {

        RoleObjMeta roleObjMeta = new RoleObjMeta();
        roleObjMeta.location(roleURI.toString());

        RoleGetResponseObject responseObject = new RoleGetResponseObject();
        responseObject.setId(role.getId());
        responseObject.setDisplayName(role.getName());
        responseObject.setMeta(roleObjMeta);
        responseObject.setPermissions(role.getPermissions());

        List<RoleGetResponseGroupObject> groups = new ArrayList<>();
        List<RoleGetResponseUserObject> users = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(role.getGroups())) {
            List<BasicGroup> roleGroups = role.getGroups();
            for (BasicGroup basicGroup : roleGroups) {
                RoleGetResponseGroupObject group = new RoleGetResponseGroupObject();
                group.value(basicGroup.getGroupId());
                group.display(basicGroup.getGroupName());
                group.$ref(RoleManagementEndpointUtils.URIBuilder.GROUP_URI.
                        buildURI(organizationId, basicGroup.getGroupId()).toString());
                groups.add(group);
            }
            responseObject.setGroups(groups);
        }

        if (CollectionUtils.isNotEmpty(role.getUsers())) {
            List<BasicUser> roleUsers = role.getUsers();
            for (BasicUser basicUser : roleUsers) {
                RoleGetResponseUserObject user = new RoleGetResponseUserObject();
                user.id(basicUser.getId());
                user.display(basicUser.getUserName());
                user.$ref(RoleManagementEndpointUtils.URIBuilder.USER_URI.
                        buildURI(organizationId, basicUser.getId()).toString());
                users.add(user);
            }
            responseObject.setUsers(users);
        }

        return responseObject;
    }

    /**
     * Generate a response object for patch operation.
     *
     * @param role    The role which needed to be included in the response.
     * @param roleURI The URI of the role.
     * @return A RolePatchResponseObject.
     */
    private RolePatchResponseObject getRolePatchResponse(Role role, URI roleURI) {

        RolePutResponseObjectMeta roleObjMeta = new RolePutResponseObjectMeta();
        roleObjMeta.setLocation(roleURI.toString());

        RolePatchResponseObject responseObject = new RolePatchResponseObject();
        responseObject.setDisplayName(role.getName());
        responseObject.setMeta(roleObjMeta);
        responseObject.setId(role.getId());

        return responseObject;
    }

    /**
     * Generate a response object for put operation.
     *
     * @param role    The role which needed to be included in the response.
     * @param roleURI The URI of the role.
     * @return A RolePutResponseObject.
     */
    private RolePutResponseObject getRolePutResponse(Role role, URI roleURI) {

        RolePutResponseObjectMeta roleObjMeta = new RolePutResponseObjectMeta();
        roleObjMeta.setLocation(roleURI.toString());

        RolePutResponseObject responseObject = new RolePutResponseObject();
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
     * @return The RoleListResponseObject.
     */
    private RolesListResponseObject getRoleListResponse(int limit, String after, String before, String filter,
                                                        String organizationId,
                                                        List<Role> roles) {

        RolesListResponseObject responseObject = new RolesListResponseObject();
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
                responseObject.addLinksItem(link);
            }
            if (!isLastPage) {
                String encodedString = Base64.getEncoder().encodeToString(roles.get(roles.size() - 1)
                        .getId().getBytes(StandardCharsets.UTF_8));
                Link link = new Link();
                link.setHref(URI.create(RoleManagementEndpointUtils.buildURIForPagination(url) +
                        "&after=" + encodedString));
                link.setRel("next");
                responseObject.addLinksItem(link);
            }

            List<RoleObj> roleDTOs = new ArrayList<>();
            for (Role role : roles) {
                RoleObj roleObj = new RoleObj();
                RoleObjMeta roleObjMeta = new RoleObjMeta();
                roleObjMeta.setLocation(RoleManagementEndpointUtils.URIBuilder.ROLE_URI.buildURI(organizationId,
                        role.getId()).toString());
                roleObj.setDisplayName(role.getId());
                roleObj.setMeta(roleObjMeta);
                roleDTOs.add(roleObj);
            }
            responseObject.setRoles(roleDTOs);
        }
        return responseObject;
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

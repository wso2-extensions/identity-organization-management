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
import org.wso2.carbon.identity.organization.management.role.management.service.models.Group;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.models.User;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constant.RoleManagementEndpointConstants.GROUP_PATH;
import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constant.RoleManagementEndpointConstants.ROLE_PATH;
import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constant.RoleManagementEndpointConstants.USER_PATH;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_GROUP_URI;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_ROLE_URI;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_USER_URI;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_VALUE_NULL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_DISPLAY_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.generateUniqueID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;

/**
 * The service class for Role Management in Organization Management.
 */
public class RoleManagementService {

    private static final Log LOG = LogFactory.getLog(RoleManagementService.class);

    /**
     * Service for creating a role inside an organization.
     *
     * @param organizationId  The ID of the organization.
     * @param rolePostRequest The object created from request body.
     * @return The role creation response.
     */
    public Response createRole(String organizationId, RolePostRequest rolePostRequest) {

        try {
            Role role = RoleManagementEndpointUtils.getRoleManager().createRole(organizationId,
                    generateRoleFromPostRequest(rolePostRequest));
            URI roleURI = RoleManagementEndpointUtils.getUri(organizationId, role.getId(), ROLE_PATH,
                    ERROR_CODE_ERROR_BUILDING_ROLE_URI);
            return Response.created(roleURI).entity(getRolePostResponse(role, roleURI)).build();
        } catch (OrganizationManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
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
        } catch (OrganizationManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
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
            URI roleURI = RoleManagementEndpointUtils.getUri(organizationId, roleId, ROLE_PATH,
                    ERROR_CODE_ERROR_BUILDING_ROLE_URI);
            return Response.ok().entity(getRoleGetResponse(organizationId, role, roleURI)).build();
        } catch (OrganizationManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Get the roles inside an organization.
     *
     * @param organizationId The ID of the organization.
     * @param filter         Param for filtering the results.
     * @param limit          Param for limiting the results.
     * @return The roles inside an organization.
     */
    public Response getRolesOfOrganization(String organizationId, String filter, Integer limit) {

        try {
            int limitValue = validateLimit(limit);
            List<Role> roles = RoleManagementEndpointUtils.getRoleManager()
                    .getOrganizationRoles(limitValue, filter, organizationId);
            return Response.ok().entity(getRoleListResponse(organizationId, roles)).build();
        } catch (OrganizationManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Service for patching a role using role ID and organization ID.
     *
     * @param organizationId   The ID of the organization.
     * @param roleId           The ID of the role.
     * @param rolePatchRequest The request object created using request body.
     * @return The role patch response.
     */
    public Response patchRole(String organizationId, String roleId, RolePatchRequest rolePatchRequest) {

        try {
            List<RolePatchOperation> patchOperationList = rolePatchRequest.getOperations();
            List<PatchOperation> patchOperations = new ArrayList<>();

            for (RolePatchOperation rolePatchOperation : patchOperationList) {
                List<String> values = rolePatchOperation.getValue();
                String patchOp = rolePatchOperation.getOp().toString();
                if (StringUtils.equalsIgnoreCase(patchOp, PATCH_OP_REMOVE)) {
                    PatchOperation patchOperation = new PatchOperation(StringUtils.strip(rolePatchOperation.getOp()
                            .toString()), StringUtils.strip(rolePatchOperation.getPath()));
                    patchOperations.add(patchOperation);
                } else if (CollectionUtils.isNotEmpty(values) && (StringUtils.equalsIgnoreCase(patchOp, PATCH_OP_ADD) ||
                        StringUtils.equalsIgnoreCase(patchOp, PATCH_OP_REPLACE))) {
                    PatchOperation patchOperation = new PatchOperation(StringUtils.strip(rolePatchOperation.getOp()
                            .toString()), StringUtils.strip(rolePatchOperation.getPath()), values);
                    patchOperations.add(patchOperation);
                } else {
                    // Invalid patch operations cannot be sent due to swagger validation.
                    // But, if values are not passed along with ADD and REPLACE operations, an error is thrown.
                    throw handleClientException(ERROR_CODE_PATCH_VALUE_NULL);
                }
            }
            Role role = RoleManagementEndpointUtils.getRoleManager().patchRole(organizationId, roleId,
                    patchOperations);
            URI roleURI = RoleManagementEndpointUtils.getUri(organizationId, roleId, ROLE_PATH,
                    ERROR_CODE_ERROR_BUILDING_ROLE_URI);
            return Response.ok().entity(getRolePatchResponse(role, roleURI)).build();
        } catch (OrganizationManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
            return RoleManagementEndpointUtils.handleServerErrorResponse(e, LOG);
        }
    }

    /**
     * Patching a role using PUT request.
     *
     * @param organizationId The organization ID.
     * @param roleId         The role ID.
     * @param rolePutRequest The request object created using request body.
     * @return Put role response.
     */
    public Response putRole(String organizationId, String roleId, RolePutRequest rolePutRequest) {

        try {
            if (StringUtils.isBlank(rolePutRequest.getDisplayName())) {
                throw handleClientException(ERROR_CODE_ROLE_DISPLAY_NAME_NULL);
            }
            String displayName = rolePutRequest.getDisplayName();
            List<RolePutRequestUser> users = rolePutRequest.getUsers();
            List<RolePutRequestGroup> groups = rolePutRequest.getGroups();
            List<String> permissions = rolePutRequest.getPermissions();

            Role role = RoleManagementEndpointUtils.getRoleManager().putRole(organizationId, roleId,
                    new Role(roleId, displayName,
                            groups.stream().map(group -> new Group(group.getValue())).collect(Collectors.toList()),
                            users.stream().map(user -> new User(user.getValue())).collect(Collectors.toList()),
                            permissions));
            URI roleURI = RoleManagementEndpointUtils.getUri(organizationId, roleId, ROLE_PATH,
                    ERROR_CODE_ERROR_BUILDING_ROLE_URI);

            return Response.ok().entity(getRolePutResponse(role, roleURI)).build();
        } catch (OrganizationManagementClientException e) {
            return RoleManagementEndpointUtils.handleClientErrorResponse(e, LOG);
        } catch (OrganizationManagementException e) {
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
        role.setId(generateUniqueID());
        role.setDisplayName(StringUtils.strip(rolePostRequest.getDisplayName()));
        role.setUsers(rolePostRequest.getUsers().stream().map(RolePostRequestUser::getValue)
                .map(User::new).collect(Collectors.toList()));
        role.setGroups(rolePostRequest.getGroups().stream().map(RolePostRequestGroup::getValue)
                .map(Group::new).collect(Collectors.toList()));
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
        response.setDisplayName(role.getDisplayName());
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
        response.setDisplayName(role.getDisplayName());
        response.setMeta(roleObjMeta);
        response.setPermissions(role.getPermissions());

        if (CollectionUtils.isNotEmpty(role.getGroups())) {
            response.setGroups(getGroupsForResponseObject(role.getGroups(), organizationId));
        }

        if (CollectionUtils.isNotEmpty(role.getUsers())) {
            response.setUsers(getUsersForResponseObject(role.getUsers(), organizationId));
        }

        return response;
    }

    /**
     * Set the groups for the response if they exist.
     *
     * @param roleGroups     The groups assigned to a role.
     * @param organizationId The organizationId.
     * @return The RoleGetResponseGroup list.
     */
    private List<RoleGetResponseGroup> getGroupsForResponseObject(List<Group> roleGroups, String organizationId) {

        List<RoleGetResponseGroup> groups = new ArrayList<>();
        for (Group basicGroup : roleGroups) {
            RoleGetResponseGroup group = new RoleGetResponseGroup();
            group.value(basicGroup.getGroupId());
            group.display(basicGroup.getGroupName());
            group.$ref(RoleManagementEndpointUtils.getUri(organizationId, basicGroup.getGroupId(), GROUP_PATH,
                    ERROR_CODE_ERROR_BUILDING_GROUP_URI).toString());
            groups.add(group);
        }
        return groups;
    }

    /**
     * Set the users for the response if they exist.
     *
     * @param roleUsers      The users assigned to a role.
     * @param organizationId The organizationId.
     * @return The RoleGetResponseUser list.
     */
    private List<RoleGetResponseUser> getUsersForResponseObject(List<User> roleUsers, String organizationId) {
        List<RoleGetResponseUser> users = new ArrayList<>();
        for (User basicUser : roleUsers) {
            RoleGetResponseUser user = new RoleGetResponseUser();
            user.value(basicUser.getId());
            user.display(basicUser.getUserName());
            user.$ref(RoleManagementEndpointUtils.getUri(organizationId, basicUser.getId(), USER_PATH,
                    ERROR_CODE_ERROR_BUILDING_USER_URI).toString());
            users.add(user);
        }
        return users;
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
        response.setDisplayName(role.getDisplayName());
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
        responseObject.setDisplayName(role.getDisplayName());
        responseObject.setMeta(roleObjMeta);
        responseObject.setId(role.getId());

        return responseObject;
    }

    /**
     * Generate a response object for get operation.
     *
     * @param organizationId The ID of the organization.
     * @param roles          List of roles.
     * @return The RoleListResponse.
     */
    private RolesListResponse getRoleListResponse(String organizationId, List<Role> roles) {

        RolesListResponse response = new RolesListResponse();
        if (CollectionUtils.isNotEmpty(roles)) {
            List<RoleObj> roleDTOs = new ArrayList<>();
            for (Role role : roles) {
                RoleObj roleObj = new RoleObj();
                RoleObjMeta roleObjMeta = new RoleObjMeta();
                roleObjMeta.setLocation(RoleManagementEndpointUtils.getUri(organizationId, role.getId(), ROLE_PATH,
                        ERROR_CODE_ERROR_BUILDING_ROLE_URI).toString());
                roleObj.setId(role.getId());
                roleObj.setDisplayName(role.getDisplayName());
                roleObj.setMeta(roleObjMeta);
                roleDTOs.add(roleObj);
            }
            response.setRoles(roleDTOs);
        }
        return response;
    }

    /**
     * @param limit The param for limiting results.
     * @return The limit.
     * @throws OrganizationManagementClientException This exception is thrown if the limit is not valid.
     */
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
            limit = 0;
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

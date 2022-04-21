/*
 *
 *  * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.role.management.endpoint.service;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseObjectGroups;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseObjectUsers;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleObjMeta;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestGroupObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestUserObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.util.RoleManagementEndpointUtils;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementClientException;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementException;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicGroup;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicUser;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

/**
 * The service class for Role Management in Organization Management.
 */
public class OrganizationManagementService {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementService.class);

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

    public Response patchRole(String organizationId, String roleId,
                              RolePatchRequestObject rolePatchRequestObject) {

        try {
            List<PatchOperation> patchOperations = new ArrayList<>();

            Role role = RoleManagementEndpointUtils.getRoleManager().patchRole(organizationId, roleId,
                    patchOperations);
            return Response.ok().build();
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

        List<RoleGetResponseObjectGroups> groups = new ArrayList<>();
        List<RoleGetResponseObjectUsers> users = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(role.getGroups())) {
            List<BasicGroup> roleGroups = role.getGroups();
            for (BasicGroup basicGroup : roleGroups) {
                RoleGetResponseObjectGroups group = new RoleGetResponseObjectGroups();
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
                RoleGetResponseObjectUsers user = new RoleGetResponseObjectUsers();
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
}

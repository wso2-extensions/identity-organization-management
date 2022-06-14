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

package org.wso2.carbon.identity.organization.management.role.management.endpoint.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.OrganizationsApiService;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.service.RoleManagementService;

import javax.ws.rs.core.Response;

/**
 * Implementation of OrganizationApiService.
 */
public class OrganizationsApiServiceImpl implements OrganizationsApiService {

    @Autowired
    private RoleManagementService roleManagementService;

    @Override
    public Response createRole(String organizationId, RolePostRequest rolePostRequest) {

        return roleManagementService.createRole(organizationId, rolePostRequest);
    }

    @Override
    public Response organizationsOrganizationIdRolesGet(String organizationId, String filter, Integer limit) {

        return roleManagementService.getRolesOfOrganization(organizationId, filter, limit);
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdDelete(String roleId, String organizationId) {

        return roleManagementService.deleteRole(organizationId, roleId);
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdGet(String roleId, String organizationId) {

        return roleManagementService.getRoleUsingOrganizationIdAndRoleId(organizationId, roleId);
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdPatch(String roleId, String organizationId,
                                                                RolePatchRequest rolePatchRequest) {

        return roleManagementService.patchRole(organizationId, roleId, rolePatchRequest);
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdPut(String roleId, String organizationId,
                                                              RolePutRequest rolePutRequest) {

        return roleManagementService.putRole(organizationId, roleId, rolePutRequest);
    }
}

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

package org.wso2.carbon.identity.organization.management.role.management.endpoint.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.*;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.*;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.service.OrganizationManagementService;

import java.util.List;
import javax.ws.rs.core.Response;

public class OrganizationsApiServiceImpl implements OrganizationsApiService {

    @Autowired
    private OrganizationManagementService organizationManagementService;

    @Override
    public Response createRole(String organizationId, RoleRequestObject roleRequestObject) {

        return organizationManagementService.createRole();
    }

    @Override
    public Response organizationsOrganizationIdRolesGet(String organizationId, String filter, Integer startIndex, Integer count) {

        // do some magic!
        return Response.ok().entity("magic!").build();
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdDelete(String organizationId, String roleId) {

        // do some magic!
        return Response.ok().entity("magic!").build();
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdGet(String organizationId, String roleId) {

        // do some magic!
        return Response.ok().entity("magic!").build();
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdPatch(String organizationId, String roleId, RolePatchRequestObject rolePatchRequestObject) {

        // do some magic!
        return Response.ok().entity("magic!").build();
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdPut(String organizationId, String roleId, RolePutRequestObject rolePutRequestObject) {

        // do some magic!
        return Response.ok().entity("magic!").build();
    }
}

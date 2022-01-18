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

package org.wso2.carbon.identity.organization.management.endpoint.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.wso2.carbon.identity.organization.management.endpoint.OrganizationsApiService;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPOSTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPUTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPatchRequestItem;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleMappingDTO;
import org.wso2.carbon.identity.organization.management.endpoint.model.UserRoleOperationDTO;
import org.wso2.carbon.identity.organization.management.endpoint.service.OrganizationManagementService;

import java.util.List;

import javax.ws.rs.core.Response;

/**
 * Implementation of OrganizationsApiService
 */
public class OrganizationsApiServiceImpl implements OrganizationsApiService {
    @Autowired
    private OrganizationManagementService orgMgtService;

    @Override
    public Response organizationsGet(String filter, Integer limit, Long next, Long before) {

        return orgMgtService.getOrganizations();
    }

    @Override
    public Response organizationsOrganizationIdDelete(String organizationId, Boolean force) {

        return orgMgtService.deleteOrganization(organizationId, force);
    }

    @Override
    public Response organizationsOrganizationIdGet(String organizationId, Boolean showChildren) {

        return orgMgtService.getOrganization(organizationId, showChildren);
    }

    @Override
    public Response organizationsOrganizationIdPatch(String organizationId, List<OrganizationPatchRequestItem>
            organizationPatchRequestItem) {

        return orgMgtService.patchOrganization(organizationId, organizationPatchRequestItem);
    }

    @Override
    public Response organizationsOrganizationIdPut(String organizationId, OrganizationPUTRequest
            organizationPUTRequest) {

        return orgMgtService.updateOrganization(organizationId, organizationPUTRequest);
    }

    @Override
    public Response organizationsPost(OrganizationPOSTRequest organizationPOSTRequest) {

        return orgMgtService.addOrganization(organizationPOSTRequest);
    }

    @Override
    public Response organizationsOrganizationIdRolesPost(String organizationId, UserRoleMappingDTO userRoleMappingDTO) {

        return orgMgtService.addOrganizationUserRoleMappings(organizationId, userRoleMappingDTO);
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdUsersGet(String organizationId, String roleId,
                                                                   Integer offset, Integer limit,
                                                                   String attributes, String filter) {

        return orgMgtService
                .getUsersFromOrganizationAndRole(organizationId, roleId, offset, limit, attributes, filter);
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdUsersUserIdDelete(String organizationId, String roleId,
                                                                            String userId, Boolean includeSubOrgs) {
        return orgMgtService
                .deleteOrganizationUserRoleMapping(organizationId, roleId, userId, includeSubOrgs);
    }

    @Override
    public Response organizationsOrganizationIdRolesRoleIdUsersUserIdPatch(String organizationId, String roleId,
                                                                           String userId,
                                                                           List<UserRoleOperationDTO>
                                                                                       userRoleOperationDTO) {

        return orgMgtService
                .patchOrganizationUserRoleMapping(organizationId, roleId, userId, userRoleOperationDTO);
    }

    @Override
    public Response organizationsOrganizationIdUsersUserIdRolesGet(String organizationId, String userId) {

        return orgMgtService.getRolesFromOrganizationAndUser(organizationId, userId);
    }

}

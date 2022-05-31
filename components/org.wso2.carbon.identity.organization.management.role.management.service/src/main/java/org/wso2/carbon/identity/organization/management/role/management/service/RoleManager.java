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

package org.wso2.carbon.identity.organization.management.role.management.service;

import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementException;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;

import java.util.List;

/**
 * Interface for Role Management.
 */
public interface RoleManager {

    /**
     * Add a role.
     *
     * @param organizationId The ID of the organization where we add the role.
     * @param role           Role that is going to be added.
     * @return Basic role info for the response object.
     * @throws RoleManagementException This exception is thrown when an error occurs when adding a role.
     */
    Role addRole(String organizationId, Role role) throws RoleManagementException;

    /**
     * Get a role from role ID.
     *
     * @param organizationId The ID of the organization where role is in.
     * @param roleId         The ID of the role we want.
     * @return A role.
     * @throws RoleManagementException This exception is thrown when an error happens when getting a role
     *                                 from role ID.
     */
    Role getRoleById(String organizationId, String roleId) throws RoleManagementException;

    /**
     * Get roles of a particular organization.
     *
     * @param limit          The maximum number of records to be returned.
     * @param filter         The filter string.
     * @param organizationId The ID of the organization.
     * @return The list containing roles of the organization where organization ID has been passed.
     * @throws RoleManagementException This exception is thrown when an error happens when getting roles from
     *                                 organization ID.
     */
    List<Role> getOrganizationRoles(int limit, String filter, String organizationId) throws RoleManagementException;

    /**
     * Patch a particular role inside an organization.
     *
     * @param organizationId  The ID of the organization.
     * @param roleId          The ID of the role.
     * @param patchOperations The patch operation list.
     * @return The role after being patched.
     * @throws RoleManagementException This exception is thrown when an error happens when patching the role.
     */
    Role patchRole(String organizationId, String roleId, List<PatchOperation> patchOperations)
            throws RoleManagementException;

    /**
     * Patch a particular role inside an organization.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of the role,
     * @param role           The details of role to be changed.
     * @return The updated role.
     * @throws RoleManagementException This exception is thrown when an error occurs when patching the role.
     */
    Role putRole(String organizationId, String roleId, Role role) throws RoleManagementException;

    /**
     * Delete a particular role inside an organization.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of the role.
     * @throws RoleManagementException This exception is thrown when an error occurs when deleting the role.
     */
    void deleteRole(String organizationId, String roleId) throws RoleManagementException;
}

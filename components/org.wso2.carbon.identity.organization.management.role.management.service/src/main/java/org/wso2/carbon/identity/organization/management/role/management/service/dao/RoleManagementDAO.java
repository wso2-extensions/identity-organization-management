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

package org.wso2.carbon.identity.organization.management.role.management.service.dao;

import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.List;

/**
 * Interface for role management data access object.
 */
public interface RoleManagementDAO {

    /**
     * Creates a new {@link Role} in the database.
     *
     * @param organizationId The organization ID of the organization where the role has been created.
     * @param tenantId       The tenant ID of the corresponding organization.
     * @param role           The role to be added.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs
     *                                               during adding a role.
     */
    void createRole(String organizationId, int tenantId, Role role) throws OrganizationManagementServerException;

    /**
     * Get a {@link Role} from the database.
     *
     * @param organizationId ID of the organization where role exists.
     * @param roleId         ID of the role.
     * @param tenantId       The tenant ID.
     * @return The corresponding role.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs
     *                                               during getting a role.
     */
    Role getRoleById(String organizationId, String roleId, int tenantId) throws OrganizationManagementServerException;

    /**
     * Get all the {@link Role}s of an organization.
     *
     * @param organizationId  The ID of the organization.
     * @param tenantId        The tenant ID.
     * @param limit           Specifies the desired number of query results per page.
     * @param expressionNodes The list of filters.
     * @param operators       The list containing the and, or operators.
     * @return A list of Roles.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs during
     *                                               getting a role.
     */
    List<Role> getOrganizationRoles(String organizationId, int tenantId, int limit,
                                    List<ExpressionNode> expressionNodes, List<String> operators)
            throws OrganizationManagementServerException;

    /**
     * Patch a {@link Role} inside an organization.
     *
     * @param organizationId  The ID of the organization.
     * @param roleId          The ID of role.
     * @param tenantId        The tenant ID.
     * @param patchOperations A list containing the patch patchOperations.
     * @return The updated role.
     * @throws OrganizationManagementException The exception is thrown when an error occurs during patching
     *                                         the role.
     */
    Role patchRole(String organizationId, String roleId, int tenantId, List<PatchOperation> patchOperations)
            throws OrganizationManagementException;

    /**
     * Delete a {@link Role}.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of role.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs during
     *                                               deleting a role.
     */
    void deleteRole(String organizationId, String roleId) throws OrganizationManagementServerException;

    /**
     * Replacing a {@link Role} completely using PUT request.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of Role to be updated.
     * @param role           The values for the role to be updated.
     * @param tenantId       The tenant ID.
     * @return The updated role.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs during pathing
     *                                               the role.
     */
    Role putRole(String organizationId, String roleId, Role role, int tenantId)
            throws OrganizationManagementServerException;

    /**
     * Check whether a role exists inside an organization.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of the role.
     * @param roleName       The displayName of the role.
     * @return Whether there exists a role or not.
     * @throws OrganizationManagementException The exception is thrown when an error occurs during checking
     *                                         whether the role exists.
     */
    boolean checkRoleExists(String organizationId, String roleId, String roleName)
            throws OrganizationManagementException;

    /**
     * Check whether a user exists inside a tenant.
     *
     * @param userId   The ID of the user.
     * @param tenantId The ID of the tenant.
     * @return If there is a user then returns true, else false.
     * @throws OrganizationManagementServerException The exception is thrown when an error occurs during checking the
     *                                               user existence.
     */
    boolean checkUserExists(String userId, int tenantId) throws OrganizationManagementServerException;


    /**
     * Check whether a group exists inside a tenant.
     *
     * @param groupId  The ID of the group.
     * @param tenantId The ID of the tenant.
     * @return If there is group then returns true, else false.
     * @throws OrganizationManagementServerException The exception is thrown when an error occurs during checking
     *                                               the group existence.
     */
    boolean checkGroupExists(String groupId, int tenantId) throws OrganizationManagementServerException;
}

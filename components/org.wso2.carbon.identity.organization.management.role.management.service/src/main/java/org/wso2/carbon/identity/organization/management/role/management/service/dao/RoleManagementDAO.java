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

package org.wso2.carbon.identity.organization.management.role.management.service.dao;

import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementServerException;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;

import java.util.List;

public interface RoleManagementDAO {

    /**
     * Creates a new {@link Role} in the database.
     *
     * @param organizationId The organization ID of the organization where the role has been created.
     * @param tenantId       The tenant ID of the corresponding organization.
     * @param role           The role to be added.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs
     *                                       during adding a role.
     */
    void addRole(String organizationId, int tenantId, Role role) throws RoleManagementServerException;

    /**
     * Get a {@link Role} in the database.
     *
     * @param roleId         ID of the role.
     * @param organizationId ID of the organization where role exists.
     * @param tenantId       The tenant ID.
     * @return The corresponding role.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs
     *                                       during getting a role.
     */
    Role getRoleById(String roleId, String organizationId, int tenantId) throws RoleManagementServerException;

    /**
     * Get all the roles of an organization.
     *
     * @param organizationId  The ID of the organization.
     * @param sortOrder       The sorting order.
     * @param tenantId        The tenant ID.
     * @param limit           Specifies the desired number of query results per page.
     * @param expressionNodes The list of filters.
     * @return A list of Roles.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during getting a role.
     */
    List<Role> getOrganizationRoles(String organizationId, String sortOrder, int tenantId, int limit,
                                    List<ExpressionNode> expressionNodes) throws RoleManagementServerException;

    /**
     * Patch a role of an organization.
     *
     * @param organizationId  The ID of the organization.
     * @param roleId          The ID of role.
     * @param tenantId        The tenant ID.
     * @param patchOperations A list containing the patch patchOperations.
     * @return The updated role.
     * @throws RoleManagementServerException The sever exception is thrown when an error occurs during patching
     *                                       the role.
     */
    Role patchRole(String organizationId, String roleId, int tenantId, List<PatchOperation> patchOperations)
            throws RoleManagementServerException;

    /**
     * Delete a role.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of role.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during deleting a role.
     */
    void deleteRole(String organizationId, String roleId) throws RoleManagementServerException;

    /**
     * Replacing a role completely using PUT request.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of Role to be updated.
     * @param role           The values for the role to be updated.
     * @param tenantId       The tenant ID.
     * @return The updated role.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during pathing
     *                                       the role.
     */
    Role putRole(String organizationId, String roleId, Role role, int tenantId) throws RoleManagementServerException;
}

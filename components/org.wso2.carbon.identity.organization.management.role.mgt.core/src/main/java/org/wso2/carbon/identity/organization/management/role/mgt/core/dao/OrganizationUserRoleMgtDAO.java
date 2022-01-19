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

package org.wso2.carbon.identity.organization.management.role.mgt.core.dao;

import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtServerException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.ChildParentAssociation;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.OrganizationUserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.Role;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.RoleMember;

import java.util.List;

/**
 * Organization Management-Role Management DAO.
 */
public interface OrganizationUserRoleMgtDAO {

    /**
     * Add organization-user-role mappings.
     * @param organizationUserRoleMappings List of Organization-User-Role mappings.
     * @param tenantID The tenant ID.
     * @throws OrganizationUserRoleMgtException Organization-User-Role management exception.
     */
    void addOrganizationUserRoleMappings(List<OrganizationUserRoleMapping> organizationUserRoleMappings, int tenantID)
            throws OrganizationUserRoleMgtException;

    /**
     * Get user-ids by organization and role.
     * @param organizationId ID of the organization.
     * @param roleId ID of the role.
     * @param offset The offset of the results.
     * @param limit The limit of the results.
     * @param requestedAttributes The list of attributes.
     * @param tenantID The tenant ID.
     * @param filter The filter to filter the results.
     * @return The List of RoleMembers.
     * @throws OrganizationUserRoleMgtServerException Organization-User-Role Management exception.
     */
    List<RoleMember> getUserIdsByOrganizationAndRole(String organizationId, String roleId, int offset, int limit,
                                                     List<String> requestedAttributes, int tenantID, String filter)
            throws OrganizationUserRoleMgtServerException;

    /**
     * Delete organization-user-role mappings.
     * @param deletionList List of organizations to be deleted for user role mappings.
     * @param userId ID of the user.
     * @param roleId ID of role.
     * @param tenantId The tenant ID.
     * @throws OrganizationUserRoleMgtException Organization-User-Role Management exception.
     */
    void deleteOrganizationsUserRoleMapping(List<OrganizationUserRoleMapping> deletionList, String userId,
                                            String roleId, int tenantId)
            throws OrganizationUserRoleMgtException;

    /**
     * Delete all organization-user-role mappings of a user.
     * @param userId ID of the user.
     * @param tenantId Tenant ID.
     * @throws OrganizationUserRoleMgtException Organization-User-Role Management exception.
     */
    void deleteOrganizationsUserRoleMappings(String userId, int tenantId) throws OrganizationUserRoleMgtException;

    /**
     * Get roleids by organization and user ids.
     * @param organizationId ID of the organization.
     * @param userId ID of the user.
     * @param tenantId The tenant ID.
     * @return List of Roles.
     * @throws OrganizationUserRoleMgtServerException Organization-User-Role Management Server exception.
     */
    List<Role> getRolesByOrganizationAndUser(String organizationId, String userId, int tenantId)
            throws OrganizationUserRoleMgtServerException;

    /**
     * Updating the organization-user-role mappings on mandatory property.
     * @param organizationUserRoleMappingsToAdd List of Organization-User-Role mappings to add.
     * @param organizationUserRoleMappingsToDelete List of Organization-User-Role mappings to delete.
     * @param tenantId The tenant ID.
     * @throws OrganizationUserRoleMgtServerException Organization-User-Role Management Server exception.
     */
    void updateMandatoryProperty(List<OrganizationUserRoleMapping> organizationUserRoleMappingsToAdd,
                                 List<OrganizationUserRoleMapping> organizationUserRoleMappingsToDelete, int tenantId)
            throws OrganizationUserRoleMgtServerException;

    /**
     * Check whether there is an organization-user-role mapping.
     * @param organizationId ID of the organization.
     * @param userId ID of the user.
     * @param roleId ID of the role.
     * @param assignedLevel The assigned level of the role.
     * @param mandatory Mandatory or not.
     * @param tenantId The tenant ID.
     * @return The boolean value of whether the user exists or not.
     * @throws OrganizationUserRoleMgtException Organization-User-Role Management exception.
     */
    boolean isOrganizationUserRoleMappingExists(String organizationId, String userId, String roleId,
                                                String assignedLevel, boolean mandatory,
                                                int tenantId)
            throws OrganizationUserRoleMgtException;

    /**
     * Get the mandatory value of a directly assigned organization-user-role mapping.
     * @param organizationId ID of the organization.
     * @param userId ID of the user.
     * @param roleId ID of the role.
     * @param tenantId The tenant ID.
     * @return The mandatory value of the organization-user-role mapping.
     * @throws OrganizationUserRoleMgtException Organization-User-Role Management exception.
     */
    int getDirectlyAssignedOrganizationUserRoleMappingInheritance(String organizationId, String userId, String roleId,
                                                                  int tenantId)
            throws OrganizationUserRoleMgtException;

    /**
     * Get role id by SCIM group name.
     * @param roleName Name of the role.
     * @param tenantId Tenant ID.
     * @return The roleId
     * @throws OrganizationUserRoleMgtServerException Organization-User-Role Management exception.
     */
    Integer getRoleIdBySCIMGroupName(String roleName, int tenantId) throws OrganizationUserRoleMgtServerException;

    /**
     * Get all the sub organizations and their immediate parents.
     * @param organizationId ID of the organization.
     * @return The child-parent association of all the sub-organizations.
     * @throws OrganizationUserRoleMgtException Organization-User-Role Management exception.
     */
    List<ChildParentAssociation> getAllSubOrganizations(String organizationId) throws OrganizationUserRoleMgtException;

    /**
     * Get assignedAt value of any organization-user-role mapping.
     * @param organizationId ID of the organization.
     * @param userId ID of the user.
     * @param roleId ID of the role.
     * @param tenantId The tenant ID.
     * @return The assignedAt value of an organization-user-role mapping.
     * @throws OrganizationUserRoleMgtException Organization-User-Role Management exception.
     */
    String getAssignedAtOfAnyOrganizationUserRoleMapping(String organizationId, String userId, String roleId,
                                                         int tenantId) throws OrganizationUserRoleMgtException;
}

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.FilterTreeBuilder;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.organization.management.role.management.service.dao.RoleManagementDAO;
import org.wso2.carbon.identity.organization.management.role.management.service.dao.RoleManagementDAOImpl;
import org.wso2.carbon.identity.organization.management.role.management.service.internal.RoleManagementDataHolder;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Group;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.models.User;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.DISPLAY_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.GROUPS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.PERMISSIONS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.USERS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ATTRIBUTE_PATCHING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_GROUP_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_USER_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_REMOVING_REQUIRED_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_DISPLAY_NAME_ALREADY_EXISTS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_DISPLAY_NAME_MULTIPLE_VALUES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_DISPLAY_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.generateUniqueID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;

/**
 * Implementation of Role Manager Interface.
 */
public class RoleManagerImpl implements RoleManager {

    private static final RoleManagementDAO roleManagementDAO = new RoleManagementDAOImpl();

    @Override
    public Role createRole(String organizationId, Role role) throws OrganizationManagementException {

        role.setId(generateUniqueID());
        validateOrganizationId(organizationId);
        validateRoleNameNotExist(organizationId, role.getDisplayName());
        // todo: skip user existence check atm, this user can be from any org.
//        if (CollectionUtils.isNotEmpty(role.getUsers())) {
//            List<String> userIdList = role.getUsers().stream().map(User::getId).collect(Collectors.toList());
//            if (CollectionUtils.isNotEmpty(userIdList)) {
//                validateUsers(userIdList, getTenantId());
//            }
//        }
        if (CollectionUtils.isNotEmpty(role.getGroups())) {
            List<String> groupIdList = role.getGroups().stream().map(Group::getGroupId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(groupIdList)) {
                validateGroups(groupIdList, getTenantId());
            }
        }
        roleManagementDAO.createRole(organizationId, role);
        return new Role(role.getId(), role.getDisplayName());
    }

    @Override
    public Role getRoleById(String organizationId, String roleId) throws OrganizationManagementException {

        validateOrganizationId(organizationId);
        validateRoleId(organizationId, roleId);
        return roleManagementDAO.getRoleById(organizationId, roleId);
    }

    @Override
    public List<Role> getOrganizationRoles(int limit, String filter, String organizationId)
            throws OrganizationManagementException {

        validateOrganizationId(organizationId);
        List<ExpressionNode> expressionNodes = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        getExpressionNodes(filter, expressionNodes, operators);
        return roleManagementDAO.getOrganizationRoles(organizationId, limit, expressionNodes, operators);
    }

    @Override
    public Role patchRole(String organizationId, String roleId, List<PatchOperation> patchOperations)
            throws OrganizationManagementException {

        validateOrganizationId(organizationId);
        validateRoleId(organizationId, roleId);
        for (PatchOperation patchOperation : patchOperations) {
            String patchPath = patchOperation.getPath();
            String patchOp = patchOperation.getOp();
            if (StringUtils.contains(patchPath, "[")) {
                patchPath = StringUtils.strip(patchPath.split("\\[")[0]);
            }
            if (StringUtils.equalsIgnoreCase(patchPath, DISPLAY_NAME) &&
                    StringUtils.equalsIgnoreCase(patchOp, PATCH_OP_REMOVE)) {
                throw handleClientException(ERROR_CODE_REMOVING_REQUIRED_ATTRIBUTE, DISPLAY_NAME,
                        PATCH_OP_REMOVE.toLowerCase());
            }
            if (!(StringUtils.equalsIgnoreCase(patchPath, DISPLAY_NAME) ||
                    StringUtils.equalsIgnoreCase(patchPath, USERS) ||
                    StringUtils.equalsIgnoreCase(patchPath, GROUPS) ||
                    StringUtils.equalsIgnoreCase(patchPath, PERMISSIONS))) {
                throw handleClientException(ERROR_CODE_INVALID_ATTRIBUTE_PATCHING, patchPath,
                        patchOperation.getOp());
            }
            if (CollectionUtils.isNotEmpty(patchOperation.getValues())) {
                if (StringUtils.equalsIgnoreCase(patchPath, USERS)) {
                    validateUsers(patchOperation.getValues(), getTenantId());
                } else if (StringUtils.equalsIgnoreCase(patchPath, GROUPS)) {
                    validateGroups(patchOperation.getValues(), getTenantId());
                } else if (StringUtils.equalsIgnoreCase(patchPath, DISPLAY_NAME)) {
                    validatePatchOpDisplayName(patchOperation.getValues(), organizationId);
                }
            }
        }
        return roleManagementDAO.patchRole(organizationId, roleId, patchOperations);
    }

    @Override
    public Role putRole(String organizationId, String roleId, Role role) throws OrganizationManagementException {

        validateOrganizationId(organizationId);
        validateRoleId(organizationId, roleId);
        if (StringUtils.isBlank(role.getDisplayName())) {
            throw handleClientException(ERROR_CODE_ROLE_DISPLAY_NAME_NULL);
        }
        if (CollectionUtils.isNotEmpty(role.getUsers())) {
            List<String> userIdList = role.getUsers().stream().map(User::getId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(userIdList)) {
                validateUsers(userIdList, getTenantId());
            }
        }
        if (CollectionUtils.isNotEmpty(role.getGroups())) {
            List<String> groupIdList = role.getGroups().stream().map(Group::getGroupId)
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(groupIdList)) {
                validateGroups(groupIdList, getTenantId());
            }
        }
        return roleManagementDAO.putRole(organizationId, roleId, role);
    }

    @Override
    public void deleteRole(String organizationId, String roleId) throws OrganizationManagementException {

        validateOrganizationId(organizationId);
        validateRoleId(organizationId, roleId);
        roleManagementDAO.deleteRole(organizationId, roleId);
    }

    /**
     * Get an instance of OrganizationManager
     */
    private OrganizationManager getOrganizationManager() {

        return RoleManagementDataHolder.getInstance().getOrganizationManager();
    }

    /**
     * Getting the expression nodes for cursor-based pagination.
     *
     * @param filter          The filter.
     * @param expressionNodes The array list to contain nodes.
     * @param operators       The array list to contain operators.
     * @throws OrganizationManagementException Throw an exception if an erroneous value is passed.
     */
    private void getExpressionNodes(String filter, List<ExpressionNode> expressionNodes,
                                    List<String> operators) throws OrganizationManagementException {

        try {
            if (StringUtils.isNotBlank(filter)) {
                FilterTreeBuilder filterTreeBuilder = new FilterTreeBuilder(filter);
                Node rootNode = filterTreeBuilder.buildTree();
                Utils.setExpressionNodeAndOperatorLists(rootNode, expressionNodes, operators, true);
            }
        } catch (IOException | IdentityException e) {
            throw handleClientException(ERROR_CODE_INVALID_FILTER_FORMAT);
        }
    }

    /**
     * Check whether the organization ID exists.
     *
     * @param organizationId The ID of the organization.
     * @throws OrganizationManagementException This exception is thrown if any error occurs while validating the
     *                                         organization ID.
     */
    private void validateOrganizationId(String organizationId) throws OrganizationManagementException {

        boolean checkOrganizationExists = getOrganizationManager().isOrganizationExistById(organizationId);
        if (!checkOrganizationExists) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId,
                    Integer.toString(getTenantId()));
        }
    }

    /**
     * Check whether the roleId exists.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of the role.
     * @throws OrganizationManagementException This exception is thrown if an error occurs while validating the role ID.
     */
    private void validateRoleId(String organizationId, String roleId) throws OrganizationManagementException {

        boolean checkRoleIdExists = roleManagementDAO.checkRoleExists(organizationId, StringUtils.strip(roleId),
                null);
        if (!checkRoleIdExists) {
            throw handleClientException(ERROR_CODE_INVALID_ROLE, roleId);
        }
    }

    /**
     * Validate the role name by checking its empty or already existing.
     *
     * @param organizationId ID of the organization.
     * @param roleName       Name of the role.
     * @throws OrganizationManagementException This exception is thrown if an error occurs while validating
     *                                         the role name.
     */
    private void validateRoleNameNotExist(String organizationId, String roleName)
            throws OrganizationManagementException {

        if (StringUtils.isBlank(roleName)) {
            throw handleClientException(ERROR_CODE_ROLE_DISPLAY_NAME_NULL);
        }
        boolean checkRoleNameExists = roleManagementDAO.checkRoleExists(organizationId, null,
                StringUtils.strip(roleName));
        if (checkRoleNameExists) {
            throw handleClientException(ERROR_CODE_ROLE_DISPLAY_NAME_ALREADY_EXISTS, roleName, organizationId);
        }
    }

    /**
     * Check the passed user ID list is valid.
     *
     * @param userIdList The user ID list.
     * @param tenantId   The tenant ID.
     * @throws OrganizationManagementException Throws an exception if a user ID is not valid.
     */
    private void validateUsers(List<String> userIdList, int tenantId) throws OrganizationManagementException {

        for (String userId : userIdList) {
            if (!roleManagementDAO.checkUserExists(userId, tenantId)) {
                throw handleClientException(ERROR_CODE_INVALID_USER_ID, userId);
            }
        }
    }

    /**
     * Check the passed group ID list is valid.
     *
     * @param groupIdList The group ID list.
     * @param tenantId    The tenant ID.
     * @throws OrganizationManagementException Throws an exception if a group ID is not valid.
     */
    private void validateGroups(List<String> groupIdList, int tenantId) throws OrganizationManagementException {

        for (String groupId : groupIdList) {
            if (!roleManagementDAO.checkGroupExists(groupId, tenantId)) {
                throw handleClientException(ERROR_CODE_INVALID_GROUP_ID, groupId);
            }
        }
    }

    private void validatePatchOpDisplayName(List<String> values, String organizationId)
            throws OrganizationManagementException {

        if (values.size() > 1) {
            throw handleClientException(ERROR_CODE_ROLE_DISPLAY_NAME_MULTIPLE_VALUES);
        }
        validateRoleNameNotExist(organizationId, values.get(0));
    }
}

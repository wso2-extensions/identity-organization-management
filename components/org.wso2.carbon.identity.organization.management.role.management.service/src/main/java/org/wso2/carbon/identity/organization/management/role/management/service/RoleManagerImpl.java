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
import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementClientException;
import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementException;
import org.wso2.carbon.identity.organization.management.role.management.service.internal.RoleManagementDataHolder;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicGroup;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicUser;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_GROUP_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_USER_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_ROLE_NAME_ALREADY_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_ROLE_NAME_NOT_NULL;

/**
 * Implementation of Role Manager Interface.
 */
public class RoleManagerImpl implements RoleManager {

    private static final RoleManagementDAO roleManagementDAO = new RoleManagementDAOImpl();

    @Override
    public Role addRole(String organizationId, Role role) throws RoleManagementException {

        try {
            boolean checkOrganizationExists = getOrganizationManager().isOrganizationExistById(organizationId);
            if (!checkOrganizationExists) {
                throw Utils.handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId);
            }
            if (StringUtils.isBlank(role.getDisplayName())) {
                throw Utils.handleClientException(ERROR_CODE_ROLE_NAME_NOT_NULL);
            }
            boolean checkRoleNameExists = roleManagementDAO.checkRoleExists(organizationId, null,
                    StringUtils.strip(role.getDisplayName()));
            if (checkRoleNameExists) {
                throw Utils.handleClientException(ERROR_CODE_ROLE_NAME_ALREADY_EXISTS, role.getDisplayName(),
                        organizationId);
            }
            checkGroupUserListsValidity(role);
            roleManagementDAO.addRole(organizationId, Utils.getTenantId(), role);
            return new Role(role.getId(), role.getDisplayName());
        } catch (OrganizationManagementException e) {
            throw new RoleManagementException(e.getMessage(), e.getDescription(), e.getErrorCode(), e);
        }
    }

    @Override
    public Role getRoleById(String organizationId, String roleId) throws RoleManagementException {

        try {
            validateOrganizationAndRoleId(organizationId, roleId);
            return roleManagementDAO.getRoleById(organizationId, roleId, Utils.getTenantId());
        } catch (OrganizationManagementException e) {
            throw new RoleManagementException(e.getMessage(), e.getDescription(), e.getErrorCode(), e);
        }
    }

    @Override
    public List<Role> getOrganizationRoles(int limit, String after, String before, String sortOrder, String filter,
                                           String organizationId) throws RoleManagementException {

        try {
            boolean checkOrganizationExists = getOrganizationManager().isOrganizationExistById(organizationId);
            if (!checkOrganizationExists) {
                throw Utils.handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId);
            }
            List<ExpressionNode> expressionNodes = new ArrayList<>();
            List<String> operators = new ArrayList<>();
            getExpressionNodes(filter, after, before, expressionNodes, operators);
            return roleManagementDAO.getOrganizationRoles(organizationId, sortOrder, Utils.getTenantId(), limit,
                    expressionNodes, operators);
        } catch (OrganizationManagementException e) {
            throw new RoleManagementException(e.getMessage(), e.getDescription(), e.getErrorCode(), e);
        }
    }

    @Override
    public Role patchRole(String organizationId, String roleId, List<PatchOperation> patchOperations)
            throws RoleManagementException {

        try {
            validateOrganizationAndRoleId(organizationId, roleId);
            return roleManagementDAO.patchRole(organizationId, roleId, Utils.getTenantId(), patchOperations);
        } catch (OrganizationManagementException e) {
            throw new RoleManagementException(e.getMessage(), e.getDescription(), e.getErrorCode(), e);
        }
    }

    @Override
    public Role putRole(String organizationId, String roleId, Role role) throws RoleManagementException {

        try {
            validateOrganizationAndRoleId(organizationId, roleId);
            if (StringUtils.isBlank(role.getDisplayName())) {
                throw Utils.handleClientException(ERROR_CODE_ROLE_NAME_NOT_NULL);
            }
            checkGroupUserListsValidity(role);
            return roleManagementDAO.putRole(organizationId, roleId, role, Utils.getTenantId());
        } catch (OrganizationManagementException e) {
            throw new RoleManagementException(e.getMessage(), e.getDescription(), e.getErrorCode(), e);
        }
    }

    @Override
    public void deleteRole(String organizationId, String roleId) throws RoleManagementException {

        try {
            validateOrganizationAndRoleId(organizationId, roleId);
            roleManagementDAO.deleteRole(organizationId, roleId);
        } catch (OrganizationManagementException e) {
            throw new RoleManagementException(e.getMessage(), e.getDescription(), e.getErrorCode(), e);
        }
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
     * @param after           The next pointer to the page.
     * @param before          The previous pointer to the page.
     * @param expressionNodes The array list to contain nodes.
     * @param operators       The array list to contain operators.
     * @throws RoleManagementClientException Throw an exception if an erroneous value is passed.
     */
    private void getExpressionNodes(String filter, String after, String before, List<ExpressionNode> expressionNodes,
                                    List<String> operators) throws RoleManagementClientException {

        if (StringUtils.isBlank(filter)) {
            filter = StringUtils.EMPTY;
        }
        String paginatedFilter = getPaginatedFilter(filter, after, before);
        try {
            if (StringUtils.isNotBlank(paginatedFilter)) {
                FilterTreeBuilder filterTreeBuilder = new FilterTreeBuilder(paginatedFilter);
                Node rootNode = filterTreeBuilder.buildTree();
                Utils.setExpressionNodeAndOperatorLists(rootNode, expressionNodes, operators, true);
            }
        } catch (IOException | IdentityException e) {
            throw Utils.handleClientException(ERROR_CODE_INVALID_FILTER_FORMAT);
        }
    }

    /**
     * Getting the paginated filter.
     *
     * @param paginatedFilter The paginated filter.
     * @param after           The next pointer to page.
     * @param before          The previous pointer to page.
     * @return The paginated filter.
     * @throws RoleManagementClientException Throw an exception if an erroneous value is passed.
     */
    private String getPaginatedFilter(String paginatedFilter, String after, String before) throws
            RoleManagementClientException {

        try {
            //pagination is done with the uuid(role id) comparison
            if (StringUtils.isNotBlank(before)) {
                String decodedString = new String(Base64.getDecoder().decode(before), StandardCharsets.UTF_8);
                paginatedFilter += StringUtils.isNotBlank(paginatedFilter) ? " and before gt " + decodedString :
                        "before gt " + decodedString;
            } else if (StringUtils.isNotBlank(after)) {
                String decodedString = new String(Base64.getDecoder().decode(after), StandardCharsets.UTF_8);
                paginatedFilter += StringUtils.isNotBlank(paginatedFilter) ? " and after lt " + decodedString :
                        "after lt " + decodedString;
            }
        } catch (IllegalArgumentException e) {
            throw Utils.handleClientException(ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION);
        }
        return paginatedFilter;
    }

    /**
     * Check whether the organization ID and role ID exists.
     *
     * @param organizationId The ID of the organization.
     * @param roleId         The ID of the role.
     * @throws RoleManagementException         This exception is thrown if any error occurs while checking the
     *                                         validity of the role id.
     * @throws OrganizationManagementException This exception is thrown if any error occurs while checking the
     *                                         validity of the organization id.
     */
    private void validateOrganizationAndRoleId(String organizationId, String roleId) throws
            RoleManagementException, OrganizationManagementException {

        boolean checkOrganizationExists = getOrganizationManager().isOrganizationExistById(organizationId);
        if (!checkOrganizationExists) {
            throw Utils.handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId);
        }
        boolean checkRoleIdExists = roleManagementDAO.checkRoleExists(organizationId, StringUtils.strip(roleId),
                null);
        if (!checkRoleIdExists) {
            throw Utils.handleClientException(ERROR_CODE_INVALID_ROLE, roleId);
        }
    }

    /**
     * Check the passed user ID list is valid.
     *
     * @param userIdList The user ID list.
     * @param tenantId   The tenant ID.
     * @throws RoleManagementException Throws an exception if a user ID is not valid.
     */
    private void checkUserValidity(List<String> userIdList, int tenantId) throws RoleManagementException {

        for (String userId : userIdList) {
            if (!roleManagementDAO.checkUserExists(userId, tenantId)) {
                throw Utils.handleClientException(ERROR_CODE_INVALID_USER_ID, userId);
            }
        }
    }

    /**
     * Check the passed group ID list is valid.
     *
     * @param groupIdList The group ID list.
     * @param tenantId    The tenant ID.
     * @throws RoleManagementException Throws an exception if a group ID is not valid.
     */
    public void checkGroupValidity(List<String> groupIdList, int tenantId) throws RoleManagementException {

        for (String groupId : groupIdList) {
            if (!roleManagementDAO.checkGroupExists(groupId, tenantId)) {
                throw Utils.handleClientException(ERROR_CODE_INVALID_GROUP_ID, groupId);
            }
        }
    }

    /**
     * Check group and user lists validity.
     * @param role The Role Object.
     * @throws RoleManagementException Throws an exception if the user or group list contains invalid ID.
     */
    private void checkGroupUserListsValidity(Role role) throws RoleManagementException {

        List<String> userIdList = role.getUsers().stream().map(BasicUser::getId).collect(Collectors.toList());
        List<String> groupIdList = role.getGroups().stream().map(BasicGroup::getGroupId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(userIdList)) {
            checkUserValidity(userIdList, Utils.getTenantId());
        }
        if (CollectionUtils.isNotEmpty(groupIdList)) {
            checkGroupValidity(groupIdList, Utils.getTenantId());
        }
    }
}

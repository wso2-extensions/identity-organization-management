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

import com.google.gson.JsonSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.FilterTreeBuilder;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.organization.management.role.management.service.dao.RoleManagementDAO;
import org.wso2.carbon.identity.organization.management.role.management.service.dao.RoleManagementDAOImpl;
import org.wso2.carbon.identity.organization.management.role.management.service.internal.RoleManagementDataHolder;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Cursor;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Group;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.models.RolesResponse;
import org.wso2.carbon.identity.organization.management.role.management.service.models.User;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getAllowedPermissions;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.CursorDirection.BACKWARD;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.CursorDirection.FORWARD;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.DISPLAY_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.GROUPS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ORG_CREATOR_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ORG_SWITCHER_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.PERMISSIONS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.USERS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ALL_ORGANIZATION_PERMISSIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.BASE_ORGANIZATION_PERMISSION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_GROUP_VALIDITY;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ATTRIBUTE_PATCHING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_GROUP_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_USER_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_REMOVING_REQUIRED_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_RESIDENT_ORGANIZATION_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_DISPLAY_NAME_ALREADY_EXISTS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_DISPLAY_NAME_MULTIPLE_VALUES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_DISPLAY_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_IS_UNMODIFIABLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_LIST_INVALID_CURSOR;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_ROLE_PATCH;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.generateUniqueID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

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
        // skip user existence check atm, this user can be from any org. Fix this through
        // https://github.com/wso2-extensions/identity-organization-management/issues/50

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
        switchRoleAssignmentInAncestorOrganizations(organizationId, role);
        return new Role(role.getId(), role.getDisplayName());
    }

    @Override
    public Role getRoleById(String organizationId, String roleId) throws OrganizationManagementException {

        validateOrganizationId(organizationId);
        validateRoleId(organizationId, roleId);
        return roleManagementDAO.getRoleById(organizationId, roleId);
    }

    @Override
    public RolesResponse getOrganizationRoles(int count, String filter, String organizationId, String cursor)
            throws OrganizationManagementException {

        String direction = FORWARD.toString();
        String cursorValue = " ";
        String nextCursor = null;
        String previousCursor = null;
        validateOrganizationId(organizationId);
        List<ExpressionNode> expressionNodes = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        getExpressionNodes(filter, expressionNodes, operators);

        if (StringUtils.isNotBlank(cursor)) {
            Cursor cursorObj = decodeCursor(cursor);
            cursorValue = cursorObj.getCursorValue();
            direction = cursorObj.getDirection();
        }

        // Count + 1 number of records fetched in order to check the necessity of next page or previous page.
        List<Role> roles = roleManagementDAO.getOrganizationRoles(organizationId, count + 1, expressionNodes,
                operators, cursorValue, direction);
        if (StringUtils.equals(FORWARD.toString(), direction)) {
            if (StringUtils.isNotBlank(cursorValue)) {
                previousCursor = encodeCursor(cursorValue, BACKWARD.toString());
            }
            if (roles.size() == count + 1) {
                nextCursor = encodeCursor(roles.get(count - 1).getDisplayName(), direction);
                roles.remove(count);
            }
        } else {
            nextCursor = encodeCursor(cursorValue, FORWARD.toString());
            if (roles.size() == count + 1) {
                previousCursor = encodeCursor(roles.get(0).getDisplayName(), direction);
                roles.remove(0);
            }
        }

        int totalResults = roleManagementDAO.getTotalOrganizationRoles(organizationId, expressionNodes, operators);
        return new RolesResponse(nextCursor, totalResults, previousCursor, count, roles);
    }

    @Override
    public List<Role> getUserOrganizationRoles(String userId, String organizationId)
            throws OrganizationManagementException {

        return roleManagementDAO.getUserOrganizationRoles(userId, organizationId);
    }

    @Override
    public List<String> getUserOrganizationPermissions(String userId, String organizationId)
            throws OrganizationManagementException {

        return roleManagementDAO.getUserOrganizationPermissions(userId, organizationId);
    }

    @Override
    public Role patchRole(String organizationId, String roleId, List<PatchOperation> patchOperations)
            throws OrganizationManagementException {

        validateOrganizationId(organizationId);
        validateRoleId(organizationId, roleId);
        if (isGivenRole(organizationId, roleId, ORG_CREATOR_ROLE)) {
            throw handleClientException(ERROR_CODE_ROLE_IS_UNMODIFIABLE, roleId);
        }
        boolean organizationSwitcherRole = isGivenRole(organizationId, roleId, ORG_SWITCHER_ROLE);
        for (PatchOperation patchOperation : patchOperations) {
            String patchPath = patchOperation.getPath();
            String patchOp = patchOperation.getOp();
            if (StringUtils.contains(patchPath, "[")) {
                patchPath = StringUtils.strip(patchPath.split("\\[")[0]);
            }
            if (organizationSwitcherRole) {
                validatePatchForOrganizationSwitchRole(patchPath, patchOp, roleId);
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

    private void validatePatchForOrganizationSwitchRole(String patchPath, String patchOp, String roleId)
            throws OrganizationManagementClientException {

        if (StringUtils.equalsIgnoreCase(patchPath, DISPLAY_NAME) ||
                StringUtils.equalsIgnoreCase(patchPath, PERMISSIONS)) {
            throw handleClientException(ERROR_CODE_UNSUPPORTED_ROLE_PATCH, roleId);
        }
        // As of now, only adding users are supported.
        if (StringUtils.equalsIgnoreCase(patchPath, USERS) && (StringUtils.equalsIgnoreCase(patchOp, PATCH_OP_REMOVE) ||
                StringUtils.equalsIgnoreCase(patchOp, PATCH_OP_REPLACE))) {
            throw handleClientException(ERROR_CODE_UNSUPPORTED_ROLE_PATCH, roleId);
        }
    }

    @Override
    public Role putRole(String organizationId, String roleId, Role role) throws OrganizationManagementException {

        validateOrganizationId(organizationId);
        validateRoleId(organizationId, roleId);
        if (isGivenRole(organizationId, roleId, ORG_CREATOR_ROLE) ||
                isGivenRole(organizationId, roleId, ORG_SWITCHER_ROLE)) {
            throw handleClientException(ERROR_CODE_ROLE_IS_UNMODIFIABLE, roleId);
        }
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
        // The org-creator role and org-switcher role assigned during organization creation is not allowed for deletion.
        if (isGivenRole(organizationId, roleId, ORG_SWITCHER_ROLE) ||
                isGivenRole(organizationId, roleId, ORG_CREATOR_ROLE)) {
            throw handleClientException(ERROR_CODE_ROLE_IS_UNMODIFIABLE, roleId);
        }
        roleManagementDAO.deleteRole(organizationId, roleId);
    }

    /**
     * Get an instance of OrganizationManager.
     */
    private OrganizationManager getOrganizationManager() {

        return RoleManagementDataHolder.getInstance().getOrganizationManager();
    }

    /**
     * Get an instance of OrganizationUserResidentResolverService.
     */
    private OrganizationUserResidentResolverService getOrganizationUserResidentResolverService() {

        return RoleManagementDataHolder.getInstance().getOrganizationUserResidentResolverService();
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
     * Check whether it is the given role.
     *
     * @param organizationId The organization ID.
     * @param roleId         The role ID.
     * @param roleName       The role name.
     * @return whether it is the given role.
     * @throws OrganizationManagementException Error while retrieving role.
     */
    private boolean isGivenRole(String organizationId, String roleId, String roleName)
            throws OrganizationManagementException {

        Role role = roleManagementDAO.getRoleById(organizationId, roleId);
        if (role == null) {
            throw handleClientException(ERROR_CODE_INVALID_ROLE, roleId);
        }
        return roleName.equalsIgnoreCase(role.getDisplayName());
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
            try {
                if (!getUserStoreManager(tenantId).isGroupExist(groupId)) {
                    throw handleClientException(ERROR_CODE_INVALID_GROUP_ID, groupId);
                }
            } catch (UserStoreException e) {
                throw handleServerException(ERROR_CODE_GETTING_GROUP_VALIDITY, e, groupId);
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

    /**
     * Get the userstore manager by tenant id.
     *
     * @return The userstore manager.
     * @throws UserStoreException Error while getting the userstore manager.
     */
    private AbstractUserStoreManager getUserStoreManager(int tenantId) throws UserStoreException {

        RealmService realmService = RoleManagementDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);

        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }

    private String encodeCursor(String cursorValue, String direction) {

        Cursor cursorObject = new Cursor(cursorValue, direction);
        return Base64.getEncoder().withoutPadding().encodeToString(Utils.getGson().toJson(cursorObject)
                .getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String cursorString) throws OrganizationManagementException {

        String decodeString = new String(Base64.getDecoder().decode(cursorString), StandardCharsets.UTF_8);
        try {
            return Utils.getGson().fromJson(decodeString, Cursor.class);
        } catch (JsonSyntaxException e) {
            throw handleClientException(ERROR_CODE_ROLE_LIST_INVALID_CURSOR, cursorString);
        }
    }

    private void switchRoleAssignmentInAncestorOrganizations(String organizationId, Role role)
            throws OrganizationManagementException {

        List<String> organizationSwitcherAllowedPermissions = getAllowedPermissions(BASE_ORGANIZATION_PERMISSION);
        organizationSwitcherAllowedPermissions.addAll(ALL_ORGANIZATION_PERMISSIONS);

        boolean switchRoleRequired = false;
        for (String rolePermission : role.getPermissions()) {
            if (organizationSwitcherAllowedPermissions.contains(rolePermission)) {
                switchRoleRequired = true;
                break;
            }
        }

        if (switchRoleRequired) {
            // TODO: support groups.
            List<User> users = role.getUsers();
            if (users != null) {
                List<String> userIds = users.stream().map(User::getId).collect(Collectors.toList());
                for (String userId : userIds) {
                    String residentOrgId = getOrganizationUserResidentResolverService()
                            .resolveResidentOrganization(userId, organizationId)
                            .orElseThrow(() -> handleClientException(ERROR_CODE_RESIDENT_ORGANIZATION_NOT_FOUND));
                    List<String> ancestorOrgIds = getOrganizationManager()
                            .getAncestorOrganizationIdsUpToGivenAncestorOrganization(organizationId, residentOrgId);
                    for (String ancestorOrgId : ancestorOrgIds) {
                        String roleId = roleManagementDAO.getRoleIdByName(ancestorOrgId, ORG_SWITCHER_ROLE);
                        if (StringUtils.isNotBlank(roleId)) {
                            patchRole(ancestorOrgId, roleId, Collections.singletonList(new PatchOperation
                                    (PATCH_OP_ADD, USERS, Collections.singletonList(userId))));
                        }
                    }
                }
            }
        }
    }
}

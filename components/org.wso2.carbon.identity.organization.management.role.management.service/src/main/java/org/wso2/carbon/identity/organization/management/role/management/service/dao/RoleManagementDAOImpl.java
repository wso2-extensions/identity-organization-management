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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages;
import org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.FilterOperator;
import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementException;
import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementServerException;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicGroup;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicPermission;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicUser;
import org.wso2.carbon.identity.organization.management.role.management.service.models.FilterQueryBuilder;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ATTRIBUTE_COLUMN_MAP;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.COMMA_SEPARATOR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.DISPLAY_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_ADDING_GROUP_TO_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_ADDING_INVALID_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_ADDING_PERMISSION_TO_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_ADDING_ROLE_TO_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_ADDING_USER_TO_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_REMOVE_OP_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_CODE_ROLE_NAME_OR_ID_REQUIRED;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_DISPLAY_NAME_MULTIPLE_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_GETTING_GROUPS_USING_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_GETTING_PERMISSIONS_USING_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_GETTING_PERMISSION_IDS_USING_PERMISSION_STRING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_GETTING_ROLES_FROM_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_GETTING_ROLE_FROM_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_GETTING_USERS_USING_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_PATCHING_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_REMOVING_GROUPS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_REMOVING_INVALID_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_REMOVING_PERMISSIONS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_REMOVING_REQUIRED_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_REMOVING_ROLE_FROM_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_REMOVING_USERS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ErrorMessages.ERROR_REPLACING_DISPLAY_NAME_OF_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.GROUPS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.PERMISSIONS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ROLE_ACTION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.USERS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_PERMISSION_IF_NOT_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_GROUP_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_GROUP_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_PERMISSION_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_UM_ORG_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_USER_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_USER_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.AND;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_GROUP_ROLE_MAPPING_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_PERMISSION_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_PERMISSION_ROLE_MAPPING_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_ROLE_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_ROLE_NAME_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_USER_ROLE_MAPPING_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_GROUPS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_GROUPS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_PERMISSIONS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_PERMISSIONS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_ROLE_FROM_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_USERS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_USERS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_GROUPS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSIONS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSIONS_WITH_ID_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSION_ID_FROM_STRING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSION_ID_FROM_STRING_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_ROLES_FROM_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_ROLES_FROM_ORGANIZATION_ID_TAIL;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_ROLE_FROM_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_USERS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.OR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_COUNT;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ACTION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_LIMIT;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.TENANT_ID_APPENDER;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.UM_ACTION_APPENDER;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.UPDATE_ROLE_DISPLAY_NAME;

/**
 * Implementation of RoleManagementDAO Interface.
 */
public class RoleManagementDAOImpl implements RoleManagementDAO {

    @Override
    public void addRole(String organizationId, int tenantId, Role role) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(ADD_ROLE_UM_ORG_ROLE,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, role.getId());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME, role.getName());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                            namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                        }, role, false);
                if (CollectionUtils.isNotEmpty(role.getGroups())) {
                    addGroupsToRole(role.getGroups().stream().map(BasicGroup::getGroupId).collect(Collectors.toList()),
                            role.getId());
                }
                if (CollectionUtils.isNotEmpty(role.getUsers())) {
                    addUsersToRole(role.getUsers().stream().map(BasicUser::getId).collect(Collectors.toList()),
                            role.getId());
                }
                if (CollectionUtils.isNotEmpty(role.getPermissions())) {
                    //check if the permission string exists or not, if not add it.
                    checkPermissionsExist(role.getPermissions(), Utils.getTenantId(), role.getId());
                    //then add permissions to the role.
                    addPermissionsToRole(role.getPermissions(), role.getId());
                }
                return null;
            });
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_ADDING_ROLE_TO_ORGANIZATION, e);
        }
    }

    @Override
    public Role getRoleById(String organizationId, String roleId, int tenantId)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            Role role = namedJdbcTemplate.fetchSingleRecord(GET_ROLE_FROM_ID,
                    (resultSet, rowNumber) -> new Role(
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                    }
            );
            if (Optional.ofNullable(role).isPresent()) {
                List<BasicGroup> basicGroupList = getGroupsFromRoleId(roleId);
                List<BasicUser> usersList = getUsersFromRoleId(roleId);
                List<String> permissionsList = getPermissionsFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(basicGroupList)) {
                    role.setGroups(basicGroupList);
                }
                if (CollectionUtils.isNotEmpty(usersList)) {
                    role.setUsers(usersList);
                }
                if (CollectionUtils.isNotEmpty(permissionsList)) {
                    role.setPermissions(permissionsList);
                }
            }
            return role;
        } catch (DataAccessException e) {
            throw Utils.handleServerException(ERROR_GETTING_ROLE_FROM_ID, e, roleId);
        }
    }

    @Override
    public List<Role> getOrganizationRoles(String organizationId, String sortOrder, int tenantId, int limit,
                                           List<ExpressionNode> expressionNodes)
            throws RoleManagementServerException {

        FilterQueryBuilder filterQueryBuilder = new FilterQueryBuilder();
        appendFilterQuery(expressionNodes, filterQueryBuilder);
        Map<String, String> filterAttributeValue = filterQueryBuilder.getFilterAttributeValue();

        String sqlStm = GET_ROLES_FROM_ORGANIZATION_ID + filterQueryBuilder.getFilterQuery() +
                String.format(GET_ROLES_FROM_ORGANIZATION_ID_TAIL, sortOrder);

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<Role> roleList;
        try {
            roleList = namedJdbcTemplate.executeQuery(
                    sqlStm,
                    (resultSet, rowNumber) -> new Role(resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                        for (Map.Entry<String, String> entry : filterAttributeValue.entrySet()) {
                            namedPreparedStatement.setString(entry.getKey(), entry.getValue());
                        }
                        namedPreparedStatement.setInt(DB_SCHEMA_LIMIT, limit);
                    }
            );
            return roleList;
        } catch (DataAccessException e) {
            throw Utils.handleServerException(ERROR_GETTING_ROLES_FROM_ORGANIZATION, e, organizationId);
        }
    }

    @Override
    public Role patchRole(String organizationId, String roleId, int tenantId, List<PatchOperation> patchOperations)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            return namedJdbcTemplate.withTransaction(template -> {
                for (PatchOperation patchOp : patchOperations) {
                    String op = patchOp.getOp().trim();
                    if (StringUtils.equalsIgnoreCase(op, PATCH_OP_ADD)) {
                        patchOperationAdd(roleId, patchOp.getPath(), patchOp.getValues());
                    } else if (StringUtils.equalsIgnoreCase(op, PATCH_OP_REMOVE)) {
                        /* if values are passed they should be on the path param. Therefore, if values are passed
                        with this, it should give errors. */
                        if (patchOp.getValues() != null) {
                            throw Utils.handleClientException(ERROR_CODE_REMOVE_OP_VALUES, null);
                        }
                        patchOperationRemove(roleId, patchOp.getPath());
                    } else if (StringUtils.equalsIgnoreCase(op, PATCH_OP_REPLACE)) {
                        patchOperationReplace(roleId, patchOp.getPath(), patchOp.getValues());
                    }
                }
                return getRoleById(organizationId, roleId, tenantId);
            });
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_PATCHING_ROLE, e, organizationId);
        }
    }

    @Override
    public void deleteRole(String organizationId, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(DELETE_ROLE_FROM_ORGANIZATION,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                        });
                return null;
            });
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_REMOVING_ROLE_FROM_ORGANIZATION, e, roleId, organizationId);
        }
    }

    @Override
    public Role putRole(String organizationId, String roleId, Role role, int tenantId)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                List<String> users = getUsersFromRoleId(roleId).stream().map(BasicUser::getId)
                        .collect(Collectors.toList());
                List<String> groups = getGroupsFromRoleId(roleId).stream().map(BasicGroup::getGroupId)
                        .collect(Collectors.toList());
                List<String> permissions = getPermissionsWithIdFromRoleId(roleId).stream()
                        .map(BasicPermission::getId).map(i -> i.toString()).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(users)) {
                    removeUsersFromRole(users, roleId);
                }
                if (CollectionUtils.isNotEmpty(groups)) {
                    removeGroupsFromRole(groups, roleId);
                }
                if (CollectionUtils.isNotEmpty(permissions)) {
                    removePermissionsFromRole(permissions, roleId);
                }
                template.executeUpdate(UPDATE_ROLE_DISPLAY_NAME,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME, role.getName());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, role.getId());
                        });
                if (CollectionUtils.isNotEmpty(role.getUsers())) {
                    addUsersToRole(role.getUsers().stream().map(BasicUser::getId).collect(Collectors.toList()),
                            roleId);
                }
                if (CollectionUtils.isNotEmpty(role.getGroups())) {
                    addGroupsToRole(role.getGroups().stream().map(BasicGroup::getGroupId).collect(Collectors.toList()),
                            roleId);
                }
                if (CollectionUtils.isNotEmpty(role.getPermissions())) {
                    checkPermissionsExist(role.getPermissions(), tenantId, role.getId());
                    addPermissionsToRole(role.getPermissions(), roleId);
                }
                return null;
            });
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_PATCHING_ROLE, e, organizationId);
        }
        return role;
    }

    @Override
    public boolean checkRoleExists(String organizationId, String roleId, String roleName)
            throws RoleManagementException {

        if (roleId == null && roleName == null || roleId != null && roleName != null) {
            throw Utils.handleClientException(ERROR_CODE_ROLE_NAME_OR_ID_REQUIRED, organizationId);
        }
        String roleAttribute = roleId == null ? roleName : roleId;
        String roleParameter = roleId == null ? DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME : DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID;
        String stm = roleId == null ? CHECK_ROLE_NAME_EXISTS : CHECK_ROLE_EXISTS;
        ErrorMessages errorMessage = roleId == null ? ERROR_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_NAME :
                ERROR_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_ID;

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            Integer roleCount = namedJdbcTemplate.fetchSingleRecord(stm,
                    (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(roleParameter, roleAttribute);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                    });
            if (roleCount == null) {
                return false;
            }
            return roleCount > 0;
        } catch (DataAccessException e) {
            throw Utils.handleServerException(errorMessage, e, roleId, organizationId);
        }
    }

    /**
     * Check whether the permissions exist in the UM_ORG_PERMISSION TABLE and if not add them.
     *
     * @param permissions The list of permissions.
     * @param tenantId    The tenant ID.
     * @param roleId      The role ID.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs
     *                                       during checking whether the permissions exist or not.
     */
    private void checkPermissionsExist(List<String> permissions, int tenantId, String roleId)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                for (String permission : permissions) {
                    Integer value = template.fetchSingleRecord(CHECK_PERMISSION_EXISTS,
                            (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID, permission);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ACTION, ROLE_ACTION);
                            });
                    if (value == null) {
                        template.executeInsert(ADD_PERMISSION_IF_NOT_EXISTS,
                                namedPreparedStatement -> {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID, permission);
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ACTION, ROLE_ACTION);
                                    namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                                }, permission, false);
                    }
                }
                return null;
            });
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_ADDING_PERMISSION_TO_ROLE, e, roleId);
        }
    }

    /**
     * The replace patch operation.
     *
     * @param roleId The role ID.
     * @param path   The path of the patch operation.
     * @param values The value for the patch operation.
     * @throws RoleManagementException The exception is thrown when an error occurs during patch
     *                                 operation.
     */
    private void patchOperationReplace(String roleId, String path, List<String> values)
            throws RoleManagementException {

        if (CollectionUtils.isNotEmpty(values)) {
            if (StringUtils.equals(path, DISPLAY_NAME)) {
                if (CollectionUtils.size(values) > 1) {
                    throw Utils.handleClientException(ERROR_DISPLAY_NAME_MULTIPLE_VALUES, null);
                }
                replaceDisplayName(values.get(0), roleId);
            } else if (StringUtils.equalsIgnoreCase(path, USERS)) {
                List<BasicUser> users = getUsersFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(users)) {
                    removeUsersFromRole(users.stream().map(BasicUser::getId).collect(Collectors.toList()), roleId);
                }
                addUsersToRole(values, roleId);
            } else if (StringUtils.equalsIgnoreCase(path, GROUPS)) {
                List<BasicGroup> groups = getGroupsFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(groups)) {
                    removeGroupsFromRole(groups.stream().map(BasicGroup::getGroupId)
                            .collect(Collectors.toList()), roleId);
                }
                addGroupsToRole(values, roleId);
            } else if (StringUtils.equalsIgnoreCase(path, PERMISSIONS)) {
                List<BasicPermission> permissions = getPermissionsWithIdFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(permissions)) {
                    removePermissionsFromRole(permissions.stream().map(BasicPermission::getId)
                            .map(i -> Integer.toString(i)).collect(Collectors.toList()), roleId);
                }
                checkPermissionsExist(values, Utils.getTenantId(), roleId);
                addPermissionsToRole(values, roleId);
            }
        }
    }

    /**
     * The remove patch operation.
     *
     * @param roleId The role ID.
     * @param path   The path of the patch operation.
     * @throws RoleManagementException The error is thrown when and error occurs during patch operation.
     */
    private void patchOperationRemove(String roleId, String path)
            throws RoleManagementException {

        String patchPath = path;
        if (patchPath.contains("[")) {
            patchPath = patchPath.split("\\[")[0];
        }
        patchPath = StringUtils.strip(patchPath);
        // if the path is displayName throw an error
        if (StringUtils.equalsIgnoreCase(patchPath, DISPLAY_NAME)) {
            throw Utils.handleClientException(ERROR_REMOVING_REQUIRED_ATTRIBUTE, DISPLAY_NAME,
                    PATCH_OP_REMOVE.toLowerCase());
        }
        // if path name is not users, groups or permissions throw an error.
        if (StringUtils.equalsIgnoreCase(patchPath, USERS) || StringUtils.equalsIgnoreCase(patchPath, GROUPS) ||
                StringUtils.equalsIgnoreCase(patchPath, PERMISSIONS)) {
            throw Utils.handleClientException(ERROR_REMOVING_INVALID_ATTRIBUTE, path,
                    PATCH_OP_REMOVE.toLowerCase());
        }

        //List<String> conditions = new ArrayList<>();
        // get the conditions associated with the path.
        // String pathValues = StringUtils.strip(path.split("\\[")[1].replace("\\]", ""))
        //        .toLowerCase();

        // if patchPath equals the StringUtils.strip(path) remove all values in the path for the particular role.
        if (StringUtils.equals(patchPath, StringUtils.strip(path))) {
            if (StringUtils.equalsIgnoreCase(patchPath, USERS)) {
                List<String> userIdList = getUsersFromRoleId(roleId).stream().map(BasicUser::getId)
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(userIdList)) {
                    removeUsersFromRole(userIdList, roleId);
                }
            } else if (StringUtils.equalsIgnoreCase(patchPath, GROUPS)) {
                List<String> groupIdList = getGroupsFromRoleId(roleId).stream().map(BasicGroup::getGroupId)
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(groupIdList)) {
                    removeGroupsFromRole(groupIdList, roleId);
                }
            } else if (StringUtils.equalsIgnoreCase(patchPath, PERMISSIONS)) {
                List<String> permissionIdList = getPermissionIdsFromString(getPermissionsFromRoleId(roleId),
                        Utils.getTenantId()).stream().map(i -> i.toString()).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(permissionIdList)) {
                    removePermissionsFromRole(permissionIdList, roleId);
                }
            }
        }
    }

    /**
     * The add patch operation.
     *
     * @param roleId The role ID.
     * @param path   The path of the patch operation.
     * @param values The value for the patch operation.
     * @throws RoleManagementException The exception is thrown when an error occurs during patch operation.
     */
    private void patchOperationAdd(String roleId, String path, List<String> values)
            throws RoleManagementException {

        if (CollectionUtils.isNotEmpty(values)) {
            if (StringUtils.equalsIgnoreCase(path, USERS)) {
                List<BasicUser> userList = values.stream().map(BasicUser::new).collect(Collectors.toList());
                //first check if there are users with same id are assigned to this role.
                List<BasicUser> newUserList = new ArrayList<>();
                for (BasicUser basicUser : userList) {
                    if (checkUserInRoleUserTable(basicUser, roleId)) {
                        newUserList.add(basicUser);
                    }
                }
                //if not add them.
                if (CollectionUtils.isNotEmpty(newUserList)) {
                    addUsersToRole(newUserList.stream().map(BasicUser::getId).collect(Collectors.toList()), roleId);
                }
            } else if (StringUtils.equalsIgnoreCase(path, GROUPS)) {
                List<BasicGroup> groupList = values.stream().map(BasicGroup::new).collect(Collectors.toList());
                //first check if there are groups with same id are assigned to this role.
                List<BasicGroup> newGroupList = new ArrayList<>();
                for (BasicGroup basicGroup : groupList) {
                    if (checkGroupInRoleGroupTable(basicGroup, roleId)) {
                        newGroupList.add(basicGroup);
                    }
                }
                //if not add them.
                if (CollectionUtils.isNotEmpty(newGroupList)) {
                    addGroupsToRole(newGroupList.stream().map(BasicGroup::getGroupId)
                            .collect(Collectors.toList()), roleId);
                }
            } else if (StringUtils.equalsIgnoreCase(path, PERMISSIONS)) {
                //first check if there are permissions with same id are assigned to this role.
                List<String> newPermissionList = new ArrayList<>();
                for (String permission : values) {
                    if (checkPermissionInRolePermissionTable(permission, roleId, Utils.getTenantId())) {
                        newPermissionList.add(permission);
                    }
                }
                //if not add them.
                if (CollectionUtils.isNotEmpty(newPermissionList)) {
                    addPermissionsToRole(newPermissionList, roleId);
                }
            } else if (StringUtils.equals(path, DISPLAY_NAME)) {
                // if it is display name just replace the display name.
                // if there are multiple values for display name throw an exception.
                if (CollectionUtils.size(values) > 1) {
                    throw Utils.handleClientException(ERROR_DISPLAY_NAME_MULTIPLE_VALUES, null);
                }
                replaceDisplayName(values.get(0), roleId);
            } else {
                throw Utils.handleClientException(ERROR_ADDING_INVALID_ATTRIBUTE, path, PATCH_OP_ADD.toLowerCase());
            }
        }
    }

    /**
     * Check whether there is a user-role mapping in UM_ORG_ROLE_USER table.
     *
     * @param basicUser The user object.
     * @param roleId    The role ID.
     * @return If there is a mapping then return false if not return true.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs while checking
     *                                       user-role mapping.
     */
    private boolean checkUserInRoleUserTable(BasicUser basicUser, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            Integer value = namedJdbcTemplate.withTransaction(template ->
                    template.fetchSingleRecord(CHECK_USER_ROLE_MAPPING_EXISTS,
                            (resultSet, i) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_USER_ID, basicUser.getId());
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                            }));
            return value == null || value == 0;
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_PATCHING_ROLE, e, roleId);
        }
    }

    /**
     * Check whether there is a group-role mapping in UM_ORG_ROLE_GROUP table.
     *
     * @param basicGroup The group object.
     * @param roleId     The role ID.
     * @return If there is a mapping return false, else return true.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during checking
     *                                       the group-role mappings.
     */
    private boolean checkGroupInRoleGroupTable(BasicGroup basicGroup, String roleId)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            Integer value = namedJdbcTemplate.withTransaction(template ->
                    template.fetchSingleRecord(CHECK_GROUP_ROLE_MAPPING_EXISTS,
                            (resultSet, i) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID,
                                        basicGroup.getGroupId());
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                            }));
            return value == null || value == 0;
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_PATCHING_ROLE, e, roleId);
        }
    }

    /**
     * Check whether there is a permission-role mapping in UM_ORG_ROLE_PERMISSION table.
     *
     * @param permission The permission to be checked.
     * @param roleId     The role ID.
     * @param tenantId   The tenant ID.
     * @return If there is a mapping then return false, if not return true.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs while checking
     *                                       a permission-role map exist.
     */
    private boolean checkPermissionInRolePermissionTable(String permission, String roleId, int tenantId)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            Integer value = namedJdbcTemplate.withTransaction(template ->
                    template.fetchSingleRecord(CHECK_PERMISSION_ROLE_MAPPING_EXISTS,
                            (resultSet, i) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID, permission);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ACTION, ROLE_ACTION);
                            }));
            return value == null || value == 0;
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_PATCHING_ROLE, e, roleId);
        }
    }

    /**
     * Appending the filter query for cursor-based pagination.
     *
     * @param expressionNodes    The list of expression nodes.
     * @param filterQueryBuilder The FilerQueryBuilder object.
     */
    private void appendFilterQuery(List<ExpressionNode> expressionNodes, FilterQueryBuilder filterQueryBuilder) {

        int count = 1;
        StringBuilder filter = new StringBuilder();
        if (CollectionUtils.isEmpty(expressionNodes)) {
            filterQueryBuilder.setFilterQuery(StringUtils.EMPTY);
        } else {
            for (ExpressionNode expressionNode : expressionNodes) {
                String operation = expressionNode.getOperation();
                String value = expressionNode.getValue();
                String attributeName = ATTRIBUTE_COLUMN_MAP.get(expressionNode.getAttributeValue());
                if (StringUtils.isNotBlank(attributeName) && StringUtils.isNotBlank(value) &&
                        StringUtils.isNotBlank(operation)) {
                    FilterOperator operator = FilterOperator.valueOf(operation.trim().toUpperCase());
                    filter.append(attributeName).append(applyFilterOperation(count, operator));
                    filterQueryBuilder.setFilterAttributeValue(operator.getPrefix() + value + operator.getSuffix());
                    count++;
                }
            }
            filterQueryBuilder.setFilterQuery(StringUtils.isBlank(filter.toString()) ? StringUtils.EMPTY :
                    filter.toString());
        }
    }

    /**
     * Get the users assigned for a particular role.
     *
     * @param roleId The role ID.
     * @return The list of users.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs while retrieving the
     *                                       users assigned for a particular role.
     */
    private List<BasicUser> getUsersFromRoleId(String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<BasicUser> userList;
        try {
            userList = namedJdbcTemplate.withTransaction(template -> template.executeQuery(GET_USERS_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> new BasicUser(resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_USER_ID)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    }
            ));
            return userList;
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_GETTING_USERS_USING_ROLE_ID, e, roleId);
        }
    }

    /**
     * Get the groups assigned for a particular role.
     *
     * @param roleId The role ID.
     * @return The list of groups.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during
     *                                       retrieving groups assigned to a particular role.
     */
    private List<BasicGroup> getGroupsFromRoleId(String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<BasicGroup> basicGroupList;
        try {
            basicGroupList = namedJdbcTemplate.withTransaction(
                    template -> template.executeQuery(GET_GROUPS_FROM_ROLE_ID,
                            (resultSet, rowNumber) ->
                                    new BasicGroup(resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID),
                                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_NAME)),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                            }));
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_GETTING_GROUPS_USING_ROLE_ID, e, roleId);
        }
        return basicGroupList;
    }

    /**
     * Get the permissions assigned for a particular role.
     *
     * @param roleId The role ID.
     * @return The list of permissions.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during retrieving
     *                                       permissions from a particular role.
     */
    private List<String> getPermissionsFromRoleId(String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<String> permissions;
        try {
            permissions = namedJdbcTemplate.withTransaction(template -> template.executeQuery(
                    GET_PERMISSIONS_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    }
            ));
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_GETTING_PERMISSIONS_USING_ROLE_ID, e, roleId);
        }
        return permissions;
    }

    /**
     * Get permission strings and their ids assigned to a role.
     *
     * @param roleId The role ID.
     * @return The list of permissions.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during retrieving
     *                                       the permissions.
     */
    private List<BasicPermission> getPermissionsWithIdFromRoleId(String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<BasicPermission> permissions;
        try {
            permissions = namedJdbcTemplate.withTransaction(template -> template.executeQuery(
                    GET_PERMISSIONS_WITH_ID_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> new BasicPermission(resultSet.getInt(DB_SCHEMA_COLUMN_NAME_UM_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    }
            ));
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_GETTING_PERMISSIONS_USING_ROLE_ID, e, roleId);
        }
        return permissions;
    }

    /**
     * Add groups to a role.
     *
     * @param groupIdList The group list.
     * @param roleId      The role ID.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during
     *                                       adding groups to a role.
     */
    private void addGroupsToRole(List<String> groupIdList, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfGroups = groupIdList.size();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(buildQueryForInsertingValues(numberOfGroups, ADD_ROLE_GROUP_MAPPING,
                                ADD_ROLE_GROUP_MAPPING_INSERT_VALUES),
                        namedPreparedStatement -> {
                            for (int i = 0; i < numberOfGroups; i++) {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + i,
                                        groupIdList.get(i));
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i,
                                        roleId);

                            }
                        }, groupIdList, false);
                return null;
            });
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_ADDING_GROUP_TO_ROLE, e, roleId);
        }
    }

    /**
     * Assign users to a role.
     *
     * @param userIdList The list of user IDs.
     * @param roleId     The role ID.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during
     *                                       assigning users to a role.
     */
    private void addUsersToRole(List<String> userIdList, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfUsers = userIdList.size();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(
                        buildQueryForInsertingValues(numberOfUsers, ADD_ROLE_USER_MAPPING,
                                ADD_ROLE_USER_MAPPING_INSERT_VALUES),
                        namedPreparedStatement -> {
                            for (int i = 0; i < numberOfUsers; i++) {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_USER_ID + i,
                                        userIdList.get(i));
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i,
                                        roleId);
                            }
                        }, userIdList, false);
                return null;
            });
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_ADDING_USER_TO_ROLE, e, roleId);
        }
    }

    /**
     * Assign permissions to a role.
     *
     * @param permissions The list of permissions.
     * @param roleId      The role ID.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during
     *                                       assigning permissions to a role.
     */
    private void addPermissionsToRole(List<String> permissions, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                List<Integer> permissionList = getPermissionIdsFromString(permissions, Utils.getTenantId());
                int numberOfPermissions = permissionList.size();
                template.executeInsert(buildQueryForInsertingValues(numberOfPermissions, ADD_ROLE_PERMISSION_MAPPING,
                                ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES),
                        namedPreparedStatement -> {
                            for (int i = 0; i < numberOfPermissions; i++) {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID + i,
                                        permissionList.get(i).toString());
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i, roleId);
                            }
                        }, permissionList, false);
                return null;
            });
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_ADDING_PERMISSION_TO_ROLE, e, roleId);
        }
    }

    /**
     * Retrieve the IDs of the permission strings.
     *
     * @param permissionStrings The list of permissions strings.
     * @param tenantId          The tenant ID.
     * @return The list of permission Ids.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs during
     *                                       getting IDs from permissions.
     */
    private List<Integer> getPermissionIdsFromString(List<String> permissionStrings, int tenantId)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfPermissions = permissionStrings.size();
        try {
            return namedJdbcTemplate.withTransaction(template -> template.executeQuery(
                    buildQueryForGettingPermissionIdsFromString(numberOfPermissions),
                    (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_UM_ID),
                    namedPreparedStatement -> {
                        for (int i = 0; i < numberOfPermissions; i++) {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID + i,
                                    permissionStrings.get(i));
                        }
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ACTION, ROLE_ACTION);
                    }
            ));
        } catch (TransactionException e) {
            throw new RoleManagementServerException(ERROR_GETTING_PERMISSION_IDS_USING_PERMISSION_STRING.getCode(),
                    ERROR_GETTING_PERMISSION_IDS_USING_PERMISSION_STRING.getMessage(),
                    ERROR_GETTING_PERMISSION_IDS_USING_PERMISSION_STRING.getDescription(), e);
        }
    }

    /**
     * Remove the groups assigned to a role.
     *
     * @param groupList The list of group IDs.
     * @param roleId    The role ID.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs while
     *                                       removing the assigned groups of a role.
     */
    private void removeGroupsFromRole(List<String> groupList, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfGroups = groupList.size();
        try {
            if (numberOfGroups > 0) {
                namedJdbcTemplate.withTransaction(template -> {
                    template.executeUpdate(buildQueryForRemovingValues(numberOfGroups, DELETE_GROUPS_FROM_ROLE,
                                    DELETE_GROUPS_FROM_ROLE_MAPPING),
                            namedPreparedStatement -> {
                                for (int i = 0; i < numberOfGroups; i++) {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i,
                                            roleId);
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + i,
                                            groupList.get(i));
                                }
                            });
                    return null;
                });
            }
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_REMOVING_GROUPS_FROM_ROLE, e, roleId);
        }
    }

    /**
     * Remove the assigned permissions from a role.
     *
     * @param permissionList The list of permissions.
     * @param roleId         The role ID.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs while removing
     *                                       the assigned permissions from a role.
     */
    private void removePermissionsFromRole(List<String> permissionList, String roleId)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfPermissions = permissionList.size();
        try {
            if (numberOfPermissions > 0) {
                namedJdbcTemplate.withTransaction(template -> {
                    template.executeUpdate(buildQueryForRemovingValues(numberOfPermissions,
                                    DELETE_PERMISSIONS_FROM_ROLE,
                                    DELETE_PERMISSIONS_FROM_ROLE_MAPPING),
                            namedPreparedStatement -> {
                                for (int i = 0; i < numberOfPermissions; i++) {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i, roleId);
                                    namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID + i,
                                            Integer.parseInt(permissionList.get(i)));
                                }
                            });
                    return null;
                });
            }
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_REMOVING_PERMISSIONS_FROM_ROLE, e);
        }
    }

    /**
     * Remove the assigned users from a role.
     *
     * @param usersList The list of users.
     * @param roleId    The role ID.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs while removing
     *                                       the assigned users from a role.
     */
    private void removeUsersFromRole(List<String> usersList, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfUsers = usersList.size();
        try {
            if (numberOfUsers > 0) {
                namedJdbcTemplate.withTransaction(template -> {
                    template.executeUpdate(buildQueryForRemovingValues(numberOfUsers, DELETE_USERS_FROM_ROLE,
                                    DELETE_USERS_FROM_ROLE_MAPPING),
                            namedPreparedStatement -> {
                                for (int i = 0; i < numberOfUsers; i++) {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i, roleId);
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_USER_ID + i,
                                            usersList.get(i));
                                }
                            });
                    return null;
                });
            }
        } catch (TransactionException e) {
            throw Utils.handleServerException(ERROR_REMOVING_USERS_FROM_ROLE, e, roleId);
        }
    }

    /**
     * A method to build a query for inserting values.
     *
     * @param numberOfElements      Number of elements that should be included in the query.
     * @param mappingQuery          The mapping query.
     * @param mappingInsertionQuery The query containing values for the mapping query.
     * @return The combined query.
     */
    private String buildQueryForInsertingValues(int numberOfElements, String mappingQuery,
                                                String mappingInsertionQuery) {

        StringBuilder sb = new StringBuilder(mappingQuery);
        for (int i = 0; i < numberOfElements; i++) {
            sb.append(String.format(mappingInsertionQuery, i));
            if (i != numberOfElements - 1) {
                sb.append(COMMA_SEPARATOR);
            }
        }
        return sb.toString();
    }

    /**
     * A method to build a query for removing values.
     *
     * @param numberOfElements   Number of elements to be included in the query.
     * @param mappingQuery       The mapping query.
     * @param mappingRemoveQuery The query containing the values for the mapping query.
     * @return The combined query.
     */
    private String buildQueryForRemovingValues(int numberOfElements, String mappingQuery, String mappingRemoveQuery) {

        StringBuilder sb = new StringBuilder(mappingQuery);
        for (int i = 0; i < numberOfElements; i++) {
            sb.append(String.format(mappingRemoveQuery, i));
            if (i != numberOfElements - 1) {
                sb.append(OR);
            }
        }
        return sb.toString();
    }

    /**
     * A query builder to get the permission IDs from permission strings.
     *
     * @param numberOfElements Number of elements to be included in the query.
     * @return The combined query.
     */
    private String buildQueryForGettingPermissionIdsFromString(int numberOfElements) {

        StringBuilder sb = new StringBuilder(GET_PERMISSION_ID_FROM_STRING);
        sb.append("(");
        for (int i = 0; i < numberOfElements; i++) {
            sb.append(String.format(GET_PERMISSION_ID_FROM_STRING_VALUES, i));
            if (i != numberOfElements - 1) {
                sb.append(OR);
            }
        }
        sb.append(")").append(AND).append(TENANT_ID_APPENDER).append(AND).append(UM_ACTION_APPENDER);
        return sb.toString();
    }

    /**
     * Applying the filter operation for cursor-based pagination.
     *
     * @param count    The number of elements.
     * @param operator The filter operator.
     * @return The filter.
     */
    private String applyFilterOperation(int count, FilterOperator operator) {

        return operator.applyFilterBuilder(count);
    }

    /**
     * Patch operation to replace the display name.
     *
     * @param displayName The display name of a role.
     * @param roleId      The role ID.
     * @throws RoleManagementServerException The server exception is thrown when an error occurs while patching the
     *                                       display name.
     */
    private void replaceDisplayName(String displayName, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(UPDATE_ROLE_DISPLAY_NAME,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME, displayName);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    });
        } catch (DataAccessException e) {
            throw new RoleManagementServerException(ERROR_REPLACING_DISPLAY_NAME_OF_ROLE.getCode(),
                    String.format(ERROR_REPLACING_DISPLAY_NAME_OF_ROLE.getMessage(), displayName, roleId));
        }
    }
}

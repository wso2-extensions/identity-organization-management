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

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.FilterOperator;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementClientException;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementException;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementServerException;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicGroup;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicPermission;
import org.wso2.carbon.identity.organization.management.role.management.service.models.BasicUser;
import org.wso2.carbon.identity.organization.management.role.management.service.models.FilterQueryBuilder;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ATTRIBUTE_COLUMN_MAP;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.COMMA_SEPARATOR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.*;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.DISPLAY_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.GROUPS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.PERMISSIONS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.USERS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.ADD_ROLE_GROUP_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.ADD_ROLE_GROUP_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.ADD_ROLE_PERMISSION_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.ADD_ROLE_USER_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.ADD_ROLE_USER_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.ADD_ROLE_UM_RM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.DELETE_GROUPS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.DELETE_GROUPS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.DELETE_PERMISSIONS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.DELETE_PERMISSIONS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.DELETE_ROLE_FROM_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.DELETE_USERS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.DELETE_USERS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.OR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.GET_GROUPS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.GET_ROLE_FROM_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.GET_ROLES_FROM_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.GET_ROLES_FROM_ORGANIZATION_ID_TAIL;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.GET_PERMISSIONS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.GET_PERMISSIONS_WITH_ID_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.GET_USERS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.REPLACE_DISPLAY_NAME_OF_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.SQLConstants.SQLPlaceholders.*;


public class RoleManagementDAOImpl implements RoleManagementDAO {

    @Override
    public void addRole(String organizationId, int tenantId, Role role) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                        template.executeInsert(ADD_ROLE_UM_RM_ROLE,
                                namedPreparedStatement -> {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, role.getId());
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME,
                                            role.getName());
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
                            addPermissionsToRole(role.getPermissions(),
                                    role.getId());
                        }
                        return null;
                    });
        } catch (TransactionException e) {
            throw new RoleManagementServerException(ERROR_ADDING_ROLE_TO_ORGANIZATION.getCode(),
                    String.format(ERROR_ADDING_ROLE_TO_ORGANIZATION.getMessage(), organizationId),
                    ERROR_ADDING_ROLE_TO_ORGANIZATION.getDescription(), e);
        }
    }

    @Override
    public Role getRoleById(String roleId, String organizationId, int tenantId)
            throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        Role role;
        try {
            role = namedJdbcTemplate.fetchSingleRecord(GET_ROLE_FROM_ID,
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
        } catch (DataAccessException e) {
            throw new RoleManagementServerException(ERROR_GETTING_ROLE_FROM_ID.getCode(),
                    String.format(ERROR_GETTING_ROLE_FROM_ID.getMessage(), roleId),
                    ERROR_GETTING_ROLE_FROM_ID.getDescription(), e);
        }
        return role;
    }

    //TODO: test this -> used cursor based pagination
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
        List<Role> roleList = Collections.emptyList();
        try {
            roleList = namedJdbcTemplate.withTransaction(template -> template.executeQuery(
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
            ));
        } catch (TransactionException e) {
            new RoleManagementServerException(ERROR_GETTING_ROLES_FROM_ORGANIZATION.getCode(),
                    String.format(ERROR_GETTING_ROLES_FROM_ORGANIZATION.getMessage(), organizationId),
                    ERROR_GETTING_ROLES_FROM_ORGANIZATION.getDescription(), e);
        }
        return roleList;
    }

    @Override
    public Role patchRole(String organizationId, String roleId, int tenantId, List<PatchOperation> patchOperations) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();

        // add, remove groups, users, permissions
        // when replacing groups, users or permissions, remove them and add
        // cannot add or remove displayName since displayName is required
        try {
            namedJdbcTemplate.withTransaction(template -> {
                for (PatchOperation patchOp : patchOperations) {
                    String op = patchOp.getOp().trim();
                    if (StringUtils.equalsIgnoreCase(op, PATCH_OP_ADD)) {
                        patchOperationAdd(roleId, patchOp.getPath(), patchOp.getValues());
                    } else if (StringUtils.equalsIgnoreCase(op, PATCH_OP_REMOVE)) {
                        patchOperationRemove(roleId, patchOp.getPath(), patchOp.getValues());
                    } else if (StringUtils.equalsIgnoreCase(op, PATCH_OP_REPLACE)) {
                        patchOperationReplace(roleId, patchOp.getPath(), patchOp.getValues());
                    }
                }
                return null;
            });
        } catch (TransactionException e) {
            new RoleManagementServerException(ERROR_PATCHING_ROLE.getCode(),
                    String.format(ERROR_PATCHING_ROLE.getMessage(), organizationId),
                    ERROR_PATCHING_ROLE.getDescription(), e);
        }
        return null;
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
            // After deleting the role, delete the mappings.
            // users, permissions, groups and TODO: if the role exist inside the groups
            // To do that get all the groups, permissions and users with the role Id
            List<BasicUser> users = getUsersFromRoleId(roleId);
            List<BasicGroup> groups = getGroupsFromRoleId(roleId);
            List<BasicPermission> permissions = getPermissionsWithIdFromRoleId(roleId);
            if (CollectionUtils.isNotEmpty(users)) {
                removeUsersFromRole(users.stream().map(BasicUser::getId)
                        .collect(Collectors.toList()), roleId);
            }
            if (CollectionUtils.isNotEmpty(groups)) {
                removeGroupsFromRole(groups.stream().map(BasicGroup::getGroupId)
                        .collect(Collectors.toList()), roleId);
            }
            if (CollectionUtils.isNotEmpty(permissions)) {
                removePermissionsFromRole(permissions.stream().map(BasicPermission::getId).map(v -> Integer.toString(v))
                        .collect(Collectors.toList()), roleId);
            }
        } catch (TransactionException e) {
            new RoleManagementServerException(ERROR_REMOVING_ROLE_FROM_ORGANIZATION.getCode(),
                    String.format(ERROR_REMOVING_ROLE_FROM_ORGANIZATION.getMessage(), roleId, organizationId),
                    ERROR_REMOVING_ROLE_FROM_ORGANIZATION.getDescription(), e);
        }
    }

    @Override
    public Role putRole(String organizationId, String roleId, Role role, int tenantId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            // first remove the users, permissions and groups associated with it and remove them and then add them
            namedJdbcTemplate.withTransaction(template -> {
                List<String> users = getUsersFromRoleId(roleId).stream().map(BasicUser::getId).collect(Collectors.toList());
                List<String> groups = getGroupsFromRoleId(roleId).stream().map(BasicGroup::getGroupId).collect(Collectors.toList());
                List<String> permissions = getPermissionsFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(users)) {
                    removeUsersFromRole(users, roleId);
                }
                if (CollectionUtils.isNotEmpty(groups)) {
                    removeGroupsFromRole(groups, roleId);
                }
                if (CollectionUtils.isNotEmpty(permissions)) {
                    removePermissionsFromRole(groups, roleId);
                }
                template.executeUpdate(REPLACE_DISPLAY_NAME_OF_ROLE,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME, role.getName());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, role.getId());
                        });
                if (CollectionUtils.isNotEmpty(role.getUsers())) {
                    addUsersToRole(role.getUsers().stream().map(BasicUser::getId).collect(Collectors.toList()), roleId);
                }
                if (CollectionUtils.isNotEmpty(role.getGroups())) {
                    addGroupsToRole(role.getGroups().stream().map(BasicGroup::getGroupId).collect(Collectors.toList()),
                            roleId);
                }
                if (CollectionUtils.isNotEmpty(role.getPermissions())) {
                    addPermissionsToRole(role.getPermissions(), roleId);
                }
                return null;
            });

            // second add the new users again to the role id and change values
        } catch (TransactionException e) {
            throw new RoleManagementServerException(ERROR_PATCHING_ROLE.getCode(),
                    String.format(ERROR_PATCHING_ROLE.getMessage(), organizationId),
                    ERROR_PATCHING_ROLE.getDescription(), e);
        }
        return role;
    }

    private void patchOperationReplace(String roleId, String path, List<String> values)
            throws RoleManagementException {

        // Only the displayName can be replaced. users, groups and permissions cannot be replaced with each other.
        // Instead, if the replace operation is going then, we first remove all of them and add them again.
        if (CollectionUtils.isNotEmpty(values)) {
            if (StringUtils.equals(path, DISPLAY_NAME)) {
                if (CollectionUtils.size(values) == 1) {
                    replaceDisplayName(values.get(0), roleId);
                } else {
                    throw new RoleManagementClientException(ERROR_DISPLAY_NAME_MULTIPLE_VALUES.getMessage(),
                            ERROR_DISPLAY_NAME_MULTIPLE_VALUES.getDescription(),
                            ERROR_DISPLAY_NAME_MULTIPLE_VALUES.getCode());
                }
            } else if (StringUtils.equalsIgnoreCase(path, USERS)) {
                List<BasicUser> users = getUsersFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(users)) {
                    removeUsersFromRole(users.stream().map(BasicUser::getId).collect(Collectors.toList()), roleId);
                }
                addUsersToRole(values, roleId);
            } else if (StringUtils.equalsIgnoreCase(path, GROUPS)) {
                List<BasicGroup> groups = getGroupsFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(groups)) {
                    removeGroupsFromRole(groups.stream().map(BasicGroup::getGroupId).collect(Collectors.toList()), roleId);
                }
                addGroupsToRole(values, roleId);
            } else if (StringUtils.equalsIgnoreCase(path, PERMISSIONS)) {
                List<BasicPermission> permissions = getPermissionsWithIdFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(permissions)) {
                    removePermissionsFromRole(permissions.stream().map(BasicPermission::getId).
                            map(i -> Integer.toString(i)).collect(Collectors.toList()), roleId);
                }
                addPermissionsToRole(values, roleId);
            }
        }
    }

    private void patchOperationRemove(String roleId, String path, List<String> values)
            throws RoleManagementException {

        if (CollectionUtils.isNotEmpty(values)) {
            if (StringUtils.equalsIgnoreCase(path, USERS)) {
                removeUsersFromRole(values, roleId);
            } else if (StringUtils.equalsIgnoreCase(path, GROUPS)) {
                removeGroupsFromRole(values, roleId);
            } else if (StringUtils.equalsIgnoreCase(path, PERMISSIONS)) {
                removePermissionsFromRole(values, roleId);
            } else if (StringUtils.equals(path, DISPLAY_NAME)) {
                throw new RoleManagementClientException(
                        String.format(ERROR_REMOVING_REQUIRED_ATTRIBUTE.getMessage(), DISPLAY_NAME),
                        String.format(ERROR_REMOVING_REQUIRED_ATTRIBUTE.getDescription(), DISPLAY_NAME,
                                PATCH_OP_REMOVE.toLowerCase()),
                        ERROR_REMOVING_REQUIRED_ATTRIBUTE.getCode());
            } else {
                throw new RoleManagementClientException(ERROR_REMOVING_INVALID_ATTRIBUTE.getCode(),
                        String.format(ERROR_REMOVING_INVALID_ATTRIBUTE.getMessage(), path),
                        String.format(ERROR_REMOVING_INVALID_ATTRIBUTE.getDescription(), path,
                                PATCH_OP_REMOVE.toLowerCase()));
            }
        }
    }

    private void patchOperationAdd(String roleId, String path, List<String> values)
            throws RoleManagementException {

        if (CollectionUtils.isNotEmpty(values)) {
            if (StringUtils.equalsIgnoreCase(path, USERS)) {
                List<BasicUser> userList = values.stream().map(BasicUser::new).collect(Collectors.toList());
                addUsersToRole(userList.stream().map(BasicUser::getId).collect(Collectors.toList()), roleId);
            } else if (StringUtils.equalsIgnoreCase(path, GROUPS)) {
                List<BasicGroup> groupList = values.stream().map(BasicGroup::new).collect(Collectors.toList());
                addGroupsToRole(groupList.stream().map(BasicGroup::getGroupId).collect(Collectors.toList()), roleId);
            } else if (StringUtils.equalsIgnoreCase(path, PERMISSIONS)) {
                addPermissionsToRole(values, roleId);
            } else if (StringUtils.equals(path, DISPLAY_NAME)) {
                throw new RoleManagementClientException(ERROR_ADDING_REQUIRED_ATTRIBUTE.getCode(),
                        String.format(ERROR_ADDING_REQUIRED_ATTRIBUTE.getMessage(), DISPLAY_NAME),
                        String.format(ERROR_ADDING_REQUIRED_ATTRIBUTE.getDescription(), DISPLAY_NAME,
                                PATCH_OP_ADD.toLowerCase()));
            } else {
                throw new RoleManagementClientException(ERROR_ADDING_INVALID_ATTRIBUTE.getCode(),
                        String.format(ERROR_ADDING_INVALID_ATTRIBUTE.getMessage(), path),
                        String.format(ERROR_ADDING_INVALID_ATTRIBUTE.getDescription(), path, PATCH_OP_ADD.toLowerCase()));
            }
        }
    }

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

    private List<BasicUser> getUsersFromRoleId(String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<BasicUser> userList;
        try {
            userList = namedJdbcTemplate.withTransaction(template -> template.executeQuery(GET_USERS_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> new BasicUser(resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_USER_NAME)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    }
            ));
        } catch (TransactionException e) {
            throw new RoleManagementServerException(ERROR_GETTING_USERS_USING_ROLE_ID.getCode(),
                    String.format(ERROR_GETTING_USERS_USING_ROLE_ID.getMessage(), roleId),
                    ERROR_GETTING_USERS_USING_ROLE_ID.getDescription(), e);
        }
        return userList;
    }

    private List<BasicGroup> getGroupsFromRoleId(String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<BasicGroup> basicGroupList;
        try {
            basicGroupList = namedJdbcTemplate.withTransaction(template -> template.executeQuery(GET_GROUPS_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> new BasicGroup(resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_NAME)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    }));
        } catch (TransactionException e) {
            throw new RoleManagementServerException(ERROR_GETTING_GROUPS_USING_ROLE_ID.getCode(),
                    String.format(ERROR_GETTING_GROUPS_USING_ROLE_ID.getMessage(), roleId),
                    ERROR_GETTING_GROUPS_USING_ROLE_ID.getDescription(), e);
        }
        return basicGroupList;
    }

    private List<String> getPermissionsFromRoleId(String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<String> permissions = null;
        try {
            permissions = namedJdbcTemplate.withTransaction(template -> template.executeQuery(
                    GET_PERMISSIONS_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    }
            ));
        } catch (TransactionException e) {
            new RoleManagementServerException(ERROR_GETTING_PERMISSIONS_USING_ROLE_ID.getCode(),
                    String.format(ERROR_GETTING_PERMISSIONS_USING_ROLE_ID.getMessage(), roleId),
                    ERROR_GETTING_PERMISSIONS_USING_ROLE_ID.getDescription(), e);
        }
        return permissions;
    }

    private List<BasicPermission> getPermissionsWithIdFromRoleId(String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<BasicPermission> permissions = null;
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
            new RoleManagementServerException(ERROR_GETTING_PERMISSIONS_USING_ROLE_ID.getCode(),
                    String.format(ERROR_GETTING_PERMISSIONS_USING_ROLE_ID.getMessage(), roleId),
                    ERROR_GETTING_PERMISSIONS_USING_ROLE_ID.getDescription(), e);
        }
        return permissions;
    }

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
            throw new RoleManagementServerException(ERROR_ADDING_GROUP_TO_ROLE.getCode(),
                    String.format(ERROR_ADDING_GROUP_TO_ROLE.getMessage(), roleId),
                    ERROR_ADDING_GROUP_TO_ROLE.getDescription(), e);
        }
    }

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
            throw new RoleManagementServerException(ERROR_ADDING_USER_TO_ROLE.getCode(),
                    String.format(ERROR_ADDING_USER_TO_ROLE.getMessage(), roleId),
                    ERROR_ADDING_USER_TO_ROLE.getDescription(), e);
        }
    }

    private void addPermissionsToRole(List<String> permissionList, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfPermissions = permissionList.size();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(buildQueryForInsertingValues(numberOfPermissions, ADD_ROLE_PERMISSION_MAPPING,
                                ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES),
                        namedPreparedStatement -> {
                            for (int i = 0; i < numberOfPermissions; i++) {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID + i,
                                        permissionList.get(i));
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i, roleId);
                            }
                        }, permissionList, false);
                return null;
            });
        } catch (TransactionException e) {
            throw new RoleManagementServerException(ERROR_ADDING_PERMISSION_TO_ROLE.getCode(),
                    String.format(ERROR_ADDING_PERMISSION_TO_ROLE.getMessage(), roleId),
                    ERROR_ADDING_PERMISSION_TO_ROLE.getDescription(), e);
        }
    }

    private void removeGroupsFromRole(List<String> groupList, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfGroups = groupList.size();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(buildQueryForRemovingValues(numberOfGroups, DELETE_GROUPS_FROM_ROLE,
                                DELETE_GROUPS_FROM_ROLE_MAPPING),
                        namedPreparedStatement -> {
                            for (int i = 0; i < numberOfGroups; i++) {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i, roleId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + i,
                                        groupList.get(i));
                            }
                        });
                return null;
            });
        } catch (TransactionException e) {
            new RoleManagementServerException(ERROR_REMOVING_GROUPS_FROM_ROLE.getCode(),
                    String.format(ERROR_REMOVING_GROUPS_FROM_ROLE.getMessage(), roleId),
                    ERROR_REMOVING_GROUPS_FROM_ROLE.getDescription(), e);
        }
    }

    private void removePermissionsFromRole(List<String> permissionList, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfPermissions = permissionList.size();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(buildQueryForRemovingValues(numberOfPermissions, DELETE_PERMISSIONS_FROM_ROLE,
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
        } catch (TransactionException e) {
            new RoleManagementServerException(ERROR_REMOVING_PERMISSIONS_FROM_ROLE.getCode(),
                    String.format(ERROR_REMOVING_PERMISSIONS_FROM_ROLE.getMessage(), e),
                    ERROR_REMOVING_PERMISSIONS_FROM_ROLE.getDescription(), e);
        }
    }

    private void removeUsersFromRole(List<String> usersList, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int numberOfUsers = usersList.size();
        try {
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
        } catch (TransactionException e) {
            new RoleManagementServerException(ERROR_REMOVING_USERS_FROM_ROLE.getCode(),
                    String.format(ERROR_REMOVING_USERS_FROM_ROLE.getMessage(), roleId),
                    ERROR_REMOVING_USERS_FROM_ROLE.getDescription(), e);
        }
    }

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

    private String applyFilterOperation(int count, FilterOperator operator) {

        return operator.applyFilterBuilder(count);
    }

    private void replaceDisplayName(String displayName, String roleId) throws RoleManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(REPLACE_DISPLAY_NAME_OF_ROLE,
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

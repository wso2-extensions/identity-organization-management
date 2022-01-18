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

package org.wso2.carbon.identity.organization.management.role.mgt.core.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtServerException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.ChildParentAssociation;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.OrganizationUserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.Role;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.RoleAssignedLevel;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.RoleAssignment;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.RoleMember;
import org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils;
import org.wso2.carbon.identity.scim2.common.impl.IdentitySCIMManager;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.SCIMResponse;
import org.wso2.charon3.core.protocol.endpoints.UserResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.AND;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.ASSIGNED_AT_ADDING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.COUNT_COLUMN_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.DELETE_ALL_ORGANIZATION_USER_ROLE_MAPPINGS_BY_USERID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.DELETE_ORGANIZATION_USER_ROLE_MAPPINGS_ASSIGNED_AT_ORG_LEVEL;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.DELETE_ORGANIZATION_USER_ROLE_MAPPING_VALUES;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.FIND_ALL_CHILD_ORG_IDS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.GET_ASSIGNED_AT_VALUE_OF_ORGANIZATION_USER_ROLE_MAPPING_LINK;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.GET_DIRECTLY_ASSIGNED_ORGANIZATION_USER_ROLE_MAPPING_LINK;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.GET_ORGANIZATION_USER_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.GET_ROLES_BY_ORG_AND_USER;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.GET_ROLE_ID_BY_SCIM_GROUP_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.GET_USERS_BY_ORG_AND_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING_VALUES;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.MANDATORY_ADDING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.OR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.VIEW_ASSIGNED_AT_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.VIEW_ASSIGNED_AT_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.VIEW_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.VIEW_MANDATORY_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.VIEW_PARENT_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.VIEW_ROLE_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.VIEW_ROLE_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.H2Constants.VIEW_USER_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_HYBRID_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MANDATORY;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_HYBRID_ROLE_ID_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_GET_CHILDREN_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_PER_USER_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.handleServerException;

/**
 * Implementation of OrganizationUserRoleMgtDAO.
 */
public class OrganizationUserRoleMgtDAOImpl implements OrganizationUserRoleMgtDAO {
    private static final Log LOG = LogFactory.getLog(OrganizationUserRoleMgtDAOImpl.class);

    @Override
    public void addOrganizationUserRoleMappings(List<OrganizationUserRoleMapping> organizationUserRoleMappings,
                                                int tenantID)
            throws OrganizationUserRoleMgtException {
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(queryForMultipleInserts(organizationUserRoleMappings.size()),
                        namedPreparedStatement -> {
                            int n = organizationUserRoleMappings.size();
                            for (int i = 0; i < n; i++) {
                                OrganizationUserRoleMapping organizationUserRoleMapping =
                                        organizationUserRoleMappings.get(i);
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ID + "%d", i),
                                        Utils.generateUniqueID());
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_USER_ID + "%d", i),
                                        organizationUserRoleMapping.getUserId());
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%d", i),
                                        organizationUserRoleMapping.getRoleId());
                                namedPreparedStatement
                                        .setInt(String.format(DB_SCHEMA_COLUMN_NAME_HYBRID_ROLE_ID + "%d", i),
                                        organizationUserRoleMapping.getHybridRoleId());
                                namedPreparedStatement.setInt(String.format(DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%d", i),
                                        tenantID);
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ORG_ID + "%d", i),
                                        organizationUserRoleMapping.getOrganizationId());
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%d", i),
                                        organizationUserRoleMapping.getAssignedLevelOrganizationId());
                                namedPreparedStatement.setInt(String.format(DB_SCHEMA_COLUMN_NAME_MANDATORY + "%d", i),
                                        organizationUserRoleMapping.isMandatory() ? 1 : 0);
                            }
                        }, organizationUserRoleMappings, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR, "", e);
        }
    }

    @Override
    public List<RoleMember> getUserIdsByOrganizationAndRole(String organizationId, String roleId, int offset, int limit,
                                                            List<String> requestedAttributes, int tenantID,
                                                            String filter)
            throws OrganizationUserRoleMgtServerException {
        boolean paginationReq = offset > -1 || limit > 0;

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<OrganizationUserRoleMapping> organizationUserRoleMappings;
        Map<String, List<RoleAssignment>> userRoleAssignments = new HashMap<>();
        List<RoleMember> roleMembers = new ArrayList<>();

        try {
            organizationUserRoleMappings = namedJdbcTemplate.executeQuery(GET_USERS_BY_ORG_AND_ROLE,
                    (resultSet, rowNumber) ->
                            new OrganizationUserRoleMapping(organizationId,
                                    resultSet.getString(VIEW_USER_ID_COLUMN), roleId,
                                    resultSet.getString(VIEW_ASSIGNED_AT_COLUMN),
                                    resultSet.getString(VIEW_ASSIGNED_AT_NAME_COLUMN),
                                    resultSet.getInt(VIEW_MANDATORY_COLUMN) == 1),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_ID, roleId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantID);
                    });

            organizationUserRoleMappings.stream().map(organizationUserRoleMapping -> userRoleAssignments
                            .computeIfAbsent(organizationUserRoleMapping.getUserId(), k -> new ArrayList<>())
                            .add(new RoleAssignment(organizationUserRoleMapping.isMandatory(),
                                    new RoleAssignedLevel(organizationUserRoleMapping.getAssignedLevelOrganizationId(),
                                            organizationUserRoleMapping.getAssignedLevelOrganizationName()))))
                    .collect(Collectors.toList());

            for (Map.Entry<String, List<RoleAssignment>> entry : userRoleAssignments.entrySet()) {

                String userId = entry.getKey();
                // Obtain the user store manager.
                UserManager userManager = IdentitySCIMManager.getInstance().getUserManager();
                // Create an endpoint and hand-over the request.
                UserResourceManager userResourceManager = new UserResourceManager();
                // Modify the given filter by adding the user ID.
                String modifiedFilter;
                if (StringUtils.isNotEmpty(filter)) {
                    modifiedFilter = filter + " and id eq " + userId;
                } else {
                    modifiedFilter = "id eq " + userId;
                }

                SCIMResponse scimResponse = userResourceManager.listWithGET(userManager, modifiedFilter,
                        1, 1, null, null, null,
                        requestedAttributes.stream().collect(Collectors.joining(",")), null);

                // Decode the received response.
                Map<String, Object> attributes;
                ObjectMapper mapper = new ObjectMapper();
                attributes = mapper.readValue(scimResponse.getResponseMessage(),
                        new TypeReference<Map<String, Object>>() {
                        });
                if (attributes.containsKey("totalResults") && ((Integer) attributes.get("totalResults")) > 0 &&
                        attributes.containsKey("Resources") && ((ArrayList) attributes.get("Resources")).size() > 0) {
                    Map<String, Object> userAttributes =
                            (Map<String, Object>) ((ArrayList) attributes.get("Resources")).get(0);
                    userAttributes.put("assignedMeta", entry.getValue());
                    RoleMember roleMember = new RoleMember(userAttributes);
                    roleMembers.add(roleMember);
                }
            }
            // Sort role member list.
            Collections.sort(roleMembers, (m1, m2) -> ((String) m1.getUserAttributes().get("userName")).compareTo(
                    String.valueOf(m2.getUserAttributes().get("userName"))));

            if (paginationReq && CollectionUtils.isNotEmpty(roleMembers)) {
                return roleMembers.subList(offset < 0 ? 0 : offset, Math.min(offset + limit, roleMembers.size()));
            }
        } catch (CharonException | IOException | DataAccessException e) {
            String message = String.format(String.valueOf(ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR), roleId,
                    organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR.getCode(), e);
        }
        return roleMembers;
    }

    @Override
    public void deleteOrganizationsUserRoleMapping(List<OrganizationUserRoleMapping> deletionList,
                                                   String userId, String roleId, int tenantId)
            throws OrganizationUserRoleMgtException {
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(queryForMultipleRoleMappingDeletion(deletionList.size()),
                        namedPreparedStatement -> {
                            int n = deletionList.size();
                            for (int i = 0; i < n; i++) {
                                OrganizationUserRoleMapping organizationUserRoleMapping = deletionList.get(i);
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ORG_ID + "%d", i),
                                        organizationUserRoleMapping.getOrganizationId());
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_USER_ID + "%d", i),
                                        organizationUserRoleMapping.getUserId());
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%d", i),
                                        organizationUserRoleMapping.getRoleId());
                                namedPreparedStatement.setInt(String.format(DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%d", i),
                                        tenantId);
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%d", i),
                                        organizationUserRoleMapping.getAssignedLevelOrganizationId());
                                namedPreparedStatement.setInt(String.format(DB_SCHEMA_COLUMN_NAME_MANDATORY + "%d", i),
                                        organizationUserRoleMapping.isMandatory() ? 1 : 0);
                            }
                        });
                return null;
            });
        } catch (TransactionException e) {
            throw new OrganizationUserRoleMgtServerException(
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_ERROR.getMessage(),
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_ERROR.getCode(), e);
        }
    }

    @Override
    public void deleteOrganizationsUserRoleMappings(String userId, int tenantId)
            throws OrganizationUserRoleMgtException {
        NamedJdbcTemplate namedjdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            namedjdbcTemplate.withTransaction(template -> {
                template.executeUpdate(DELETE_ALL_ORGANIZATION_USER_ROLE_MAPPINGS_BY_USERID,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                            namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                        });
                return null;
            });
        } catch (TransactionException e) {
            throw new OrganizationUserRoleMgtServerException(
                    String.format(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_PER_USER_ERROR.getMessage(),
                            userId),
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_PER_USER_ERROR.getCode(), e);
        }
    }

    @Override
    public List<Role> getRolesByOrganizationAndUser(String organizationId, String userId, int tenantID)
            throws OrganizationUserRoleMgtServerException {
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        List<Role> roles;
        try {
            roles = namedJdbcTemplate.executeQuery(GET_ROLES_BY_ORG_AND_USER,
                    (resultSet, rowNumber) -> new Role(resultSet.getString(VIEW_ROLE_ID_COLUMN),
                            "Internal/" + resultSet.getString(VIEW_ROLE_NAME_COLUMN)), //internal roles.
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantID);
                    });
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR.getMessage()), userId,
                            organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR.getCode(), e);
        }
        return roles;
    }

    @Override
    public void updateMandatoryProperty(List<OrganizationUserRoleMapping> organizationUserRoleMappingsToAdd,
                                        List<OrganizationUserRoleMapping> organizationUserRoleMappingsToDelete,
                                        int tenantId)
            throws OrganizationUserRoleMgtServerException {
        JdbcTemplate jdbcTemplate = Utils.getNewJdbcTemplate();
        try {
            jdbcTemplate.withTransaction(template -> {
                /*
                 We are getting the organization-user-role mappings accordingly for all the scenarios mentioned in
                 @{OrganizationUserRoleManagerImpl} Therefore, we only need to add the user role mappings,
                 delete the user role mappings and update the user role mappings accordingly.
                 */
                // add organization-user-role mappings
                if (CollectionUtils.isNotEmpty(organizationUserRoleMappingsToAdd)) {
                    template.executeInsert(queryForMultipleInserts(organizationUserRoleMappingsToAdd.size()),
                            preparedStatement -> {
                                int parameterIndex = 0;
                                for (OrganizationUserRoleMapping organizationUserRoleMapping :
                                        organizationUserRoleMappingsToAdd) {
                                    preparedStatement.setString(++parameterIndex, Utils.generateUniqueID());
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getUserId());
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getRoleId());
                                    preparedStatement.setInt(++parameterIndex,
                                            organizationUserRoleMapping.getHybridRoleId());
                                    preparedStatement.setInt(++parameterIndex, tenantId);
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getOrganizationId());
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getAssignedLevelOrganizationId());
                                    preparedStatement.setInt(++parameterIndex,
                                            organizationUserRoleMapping.isMandatory() ? 1 : 0);
                                }
                            }, organizationUserRoleMappingsToAdd, false);
                }
                //delete organization-user-role mappings
                if (CollectionUtils.isNotEmpty(organizationUserRoleMappingsToDelete)) {
                    template.executeUpdate(
                            queryForMultipleRoleMappingDeletion(organizationUserRoleMappingsToDelete.size()),
                            preparedStatement -> {
                                int parameterIndex = 0;
                                for (OrganizationUserRoleMapping organizationUserRoleMapping :
                                        organizationUserRoleMappingsToDelete) {
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getOrganizationId());
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getUserId());
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getRoleId());
                                    preparedStatement.setInt(++parameterIndex, tenantId);
                                    preparedStatement.setString(++parameterIndex,
                                            organizationUserRoleMapping.getAssignedLevelOrganizationId());
                                    preparedStatement.setInt(++parameterIndex,
                                            organizationUserRoleMapping.isMandatory() ? 1 : 0);
                                }
                            });
                }
                return null;
            });
        } catch (TransactionException e) {
            throw new OrganizationUserRoleMgtServerException(
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR.getMessage(),
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR.getCode(), e);
        }
    }

    @Override
    public boolean isOrganizationUserRoleMappingExists(String organizationId, String userId, String roleId, String
            assignedLevel, boolean mandatory, int tenantId) throws OrganizationUserRoleMgtException {
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int mappingsCount;
        try {
            mappingsCount = namedJdbcTemplate
                    .fetchSingleRecord(buildIsRoleMappingExistsQuery(assignedLevel, true),
                            (resultSet, rowNumber) ->
                                    resultSet.getInt(COUNT_COLUMN_NAME),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_ID, roleId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT, assignedLevel);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_MANDATORY, mandatory ? 1 : 0);
                            });
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR), roleId,
                            userId, organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR.getCode(), e);
        }
        return mappingsCount > 0;
    }

    @Override
    public int getDirectlyAssignedOrganizationUserRoleMappingInheritance(String organizationId, String
            userId, String roleId, int tenantId) throws OrganizationUserRoleMgtException {
        /*
        Since this method is to get directly assigned organization-user-role mapping, assignedLevel(an org. id) =
        @param{organizationId}
        */
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        int directlyAssignedRoleMappingInheritance = -1;
        try {
            boolean mappingsExists = namedJdbcTemplate
                    .fetchSingleRecord(buildIsRoleMappingExistsQuery(organizationId, false),
                            /*
                             We are not checking whether the role is mandatory or not. We want to get a user role
                             mapping on params organizationId, userId, roleId, tenantId and assignedLevel
                             */
                            (resultSet, rowNumber) ->
                                    resultSet.getInt(COUNT_COLUMN_NAME) > 0,
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_ID, roleId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT, organizationId);
                            });
            if (!mappingsExists) {
                return directlyAssignedRoleMappingInheritance;
            }
            List<Integer> results =
                    namedJdbcTemplate.executeQuery(GET_DIRECTLY_ASSIGNED_ORGANIZATION_USER_ROLE_MAPPING_LINK,
                            (resultSet, rowNumber) -> resultSet.getInt(VIEW_MANDATORY_COLUMN),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_ID, roleId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT, organizationId);
                            });
            /* Here, we get the mandatory and non-mandatory values of the organization-user-role mappings according to
             * the following parameters, userId, roleId, tenantId, organizationId, assignedAt value. There is a
             * possibility of having both mandatory and non-mandatory values due to this. But since we need to
             * update the values according to priority we select the max value from those values.*/
            directlyAssignedRoleMappingInheritance = results.stream().max(Integer::compare).get();
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR), roleId,
                            userId, organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR.getCode(), e);
        }
        return directlyAssignedRoleMappingInheritance;
    }

    @Override
    public Integer getRoleIdBySCIMGroupName(String roleName, int tenantId) throws
            OrganizationUserRoleMgtServerException {
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ROLE_ID_BY_SCIM_GROUP_NAME,
                    (resultSet, rowNumber) ->
                            resultSet.getInt(DatabaseConstants.H2Constants.VIEW_ID_COLUMN),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_NAME, roleName);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_HYBRID_ROLE_ID_RETRIEVING_ERROR, roleName);
        }
    }

    @Override
    public List<ChildParentAssociation> getAllSubOrganizations(String organizationId) throws
            OrganizationUserRoleMgtException {
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        try {
            return namedJdbcTemplate.executeQuery(FIND_ALL_CHILD_ORG_IDS,
                    (resultSet, rowNumber) ->
                            new ChildParentAssociation(resultSet.getString(VIEW_ID_COLUMN),
                                    resultSet.getString(VIEW_PARENT_ID_COLUMN)),
                    namedPreparedStatement -> namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID,
                            organizationId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ORGANIZATION_GET_CHILDREN_ERROR, "Organization Id " + organizationId,
                    e);
        }
    }

    @Override
    public String getAssignedAtOfAnyOrganizationUserRoleMapping(String organizationId, String userId, String roleId,
                                                                int tenantId) throws OrganizationUserRoleMgtException {
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewNamedJdbcTemplate();
        String assignedAt = null;
        try {
            boolean mappingExists = namedJdbcTemplate
                    .fetchSingleRecord(buildIsRoleMappingExistsQuery(null, false),
                            (resultSet, rowNumber) ->
                                    resultSet.getInt(COUNT_COLUMN_NAME) == 1,
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_ID, roleId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                            });
            if (!mappingExists) {
                return assignedAt;
            }
            assignedAt =
                    namedJdbcTemplate.fetchSingleRecord(GET_ASSIGNED_AT_VALUE_OF_ORGANIZATION_USER_ROLE_MAPPING_LINK,
                            (resultSet, rowNumber) -> resultSet.getString(VIEW_ASSIGNED_AT_COLUMN),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_ID, roleId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                            });
        } catch (DataAccessException e) {
            String message =
                    String.format(String.valueOf(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR), roleId,
                            userId, organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR.getCode(), e);
        }
        return assignedAt;
    }

    private String queryForMultipleInserts(Integer numberOfMapings) {
        StringBuilder sb = new StringBuilder();
        sb.append(INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING);

        for (int i = 0; i < numberOfMapings; i++) {
            sb.append(String.format(INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING_VALUES, i));
            if (i != numberOfMapings - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    private String queryForMultipleRoleMappingDeletion(int numberOfOrganizations) {
        StringBuilder sb = new StringBuilder();
        sb.append(DELETE_ORGANIZATION_USER_ROLE_MAPPINGS_ASSIGNED_AT_ORG_LEVEL);
        sb.append("(");
        for (int i = 0; i < numberOfOrganizations; i++) {
            sb.append("(").append(String.format(DELETE_ORGANIZATION_USER_ROLE_MAPPING_VALUES, i)).append(")");
            if (i != numberOfOrganizations - 1) {
                sb.append(OR);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildIsRoleMappingExistsQuery(String assignedLevel, boolean checkMandatory) {

        StringBuilder sb = new StringBuilder();
        sb.append(GET_ORGANIZATION_USER_ROLE_MAPPING);
        if (StringUtils.isNotEmpty(assignedLevel)) {
            sb.append(AND).append(ASSIGNED_AT_ADDING);
        }
        if (checkMandatory) {
            sb.append(AND).append(MANDATORY_ADDING);
        }
        return sb.toString();
    }
}

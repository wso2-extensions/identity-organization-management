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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtServerException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.OrganizationUserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.Role;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.RoleAssignment;
import org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils;
import org.wso2.carbon.identity.scim2.common.impl.IdentitySCIMManager;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.SCIMResponse;
import org.wso2.charon3.core.protocol.endpoints.UserResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.AND;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.ASSIGNED_AT_ADDING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.COUNT_COLUMN_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.DELETE_ALL_ORGANIZATION_USER_ROLE_MAPPINGS_BY_USERID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.DELETE_ORGANIZATION_USER_ROLE_MAPPINGS_ASSIGNED_AT_ORG_LEVEL;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.DELETE_ORGANIZATION_USER_ROLE_MAPPING_VALUES;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.FIND_ALL_CHILD_ORG_IDS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.FORCED_ADDING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.GET_DIRECTLY_ASSIGNED_ORGANIZATION_USER_ROLE_MAPPING_LINK;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.GET_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.GET_ORGANIZATION_USER_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.GET_ROLES_BY_ORG_AND_USER;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.GET_ROLE_ID_AND_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.GET_USERS_BY_ORG_AND_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING_VALUES;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.OR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.SCIM_GROUP_ATTR_VALUE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.VIEW_ASSIGNED_AT_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.VIEW_ASSIGNED_AT_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.VIEW_ATTR_VALUE_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.VIEW_FORCED_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.VIEW_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.VIEW_ROLE_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.VIEW_ROLE_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLConstants.VIEW_USER_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ATTR_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ATTR_VALUE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_FORCED;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.DatabaseConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_GET_CHILDREN_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_GET_ORGANIZATION_ID_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_PER_USER_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_RETRIEVING_DATA_FROM_IDENTITY_DB_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ORGANIZATION_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.SCIM_ROLE_ID_ATTR_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getNewNamedJdbcTemplate;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getNewTemplateForIdentityDatabase;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.handleServerException;

/**
 * Implementation of OrganizationUserRoleMgtDAO.
 */
public class OrganizationUserRoleMgtDAOImpl implements OrganizationUserRoleMgtDAO {

    private static final Log LOG = LogFactory.getLog(OrganizationUserRoleMgtDAOImpl.class);

    @Override
    public void addOrganizationUserRoleMappings(List<OrganizationUserRoleMapping> organizationUserRoleMappings,
                                                int tenantId)
            throws OrganizationUserRoleMgtException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                addOrganizationUserRoleMappingsFromList(organizationUserRoleMappings, tenantId);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR, "", e);
        }
    }

    @Override
    public List<Map<String, Object>> getUserIdsByOrganizationAndRole(String organizationId, String roleId, int offset,
                                                                     int limit, List<String> requestedAttributes,
                                                                     int tenantId, String filter)
            throws OrganizationUserRoleMgtServerException {

        boolean paginationReq = offset > -1 || limit > 0;

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        List<OrganizationUserRoleMapping> organizationUserRoleMappings;
        Map<String, List<RoleAssignment>> userRoleAssignments = new HashMap<>();
        List<Map<String, Object>> usersWithUserAttributesList = new ArrayList<>();

        try {
            organizationUserRoleMappings = namedJdbcTemplate.executeQuery(GET_USERS_BY_ORG_AND_ROLE,
                    (resultSet, rowNumber) ->
                            new OrganizationUserRoleMapping(organizationId,
                                    resultSet.getString(VIEW_USER_ID_COLUMN), roleId,
                                    resultSet.getString(VIEW_ASSIGNED_AT_COLUMN),
                                    resultSet.getString(VIEW_ASSIGNED_AT_NAME_COLUMN),
                                    resultSet.getInt(VIEW_FORCED_COLUMN) == 1),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_ID, roleId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                    });

            organizationUserRoleMappings.stream().map(organizationUserRoleMapping -> userRoleAssignments
                            .computeIfAbsent(organizationUserRoleMapping.getUserId(), k -> new ArrayList<>())
                            .add(new RoleAssignment(organizationUserRoleMapping.isForced(),
                                    new HashMap<String, String>() {{
                                        put(ORGANIZATION_ID,
                                                organizationUserRoleMapping.getAssignedLevelOrganizationId());
                                        put(ORGANIZATION_NAME,
                                                organizationUserRoleMapping.getAssignedLevelOrganizationName());
                                    }})))
                    .collect(Collectors.toList());

            for (Map.Entry<String, List<RoleAssignment>> entry : userRoleAssignments.entrySet()) {

                String userId = entry.getKey();
                // Obtain the user store manager.
                UserManager userManager = IdentitySCIMManager.getInstance().getUserManager();
                // Create an endpoint and hand-over the request.
                UserResourceManager userResourceManager = new UserResourceManager();
                // Modify the given filter by adding the user ID.
                String modifiedFilter;
                if (StringUtils.isNotBlank(filter)) {
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
                    usersWithUserAttributesList.add(userAttributes);
                }
            }
            // Sort role member list.
            usersWithUserAttributesList.sort((m1, m2) -> ((String) m1.get("userName")).compareTo(
                    String.valueOf(m2.get("userName"))));

            if (paginationReq && CollectionUtils.isNotEmpty(usersWithUserAttributesList)) {
                return usersWithUserAttributesList.subList(offset < 0 ? 0 : offset, Math.min(offset + limit,
                        usersWithUserAttributesList.size()));
            }
        } catch (CharonException | IOException | DataAccessException e) {
            String message = String.format(String.valueOf(ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR), roleId,
                    organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR.getCode(), e);
        }
        return usersWithUserAttributesList;
    }

    @Override
    public void deleteOrganizationsUserRoleMapping(List<OrganizationUserRoleMapping> deletionList,
                                                   String userId, String roleId, int tenantId)
            throws OrganizationUserRoleMgtException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                deleteOrganizationUserRoleMappingsFromList(deletionList, tenantId);
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

        NamedJdbcTemplate namedjdbcTemplate = getNewNamedJdbcTemplate();
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
    public List<Role> getRolesByOrganizationAndUser(String organizationId, String userId, int tenantId)
            throws OrganizationUserRoleMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        List<String> roleIdList;
        List<Role> roles;
        try {
            roleIdList = namedJdbcTemplate.withTransaction(template ->
                    template.executeQuery(GET_ROLES_BY_ORG_AND_USER,
                            (resultSet, rowNumber) -> resultSet.getString(VIEW_ROLE_ID_COLUMN),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                            }));
            roles = getRoleIdAndRoleNameUsingSCIM(roleIdList, tenantId);
        } catch (TransactionException e) {
            String message =
                    String.format(ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR.getMessage(), userId,
                            organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR.getCode(), e);
        }
        return roles;
    }

    @Override
    public void updateForcedProperty(List<OrganizationUserRoleMapping> organizationUserRoleMappingsToAdd,
                                     List<OrganizationUserRoleMapping> organizationUserRoleMappingsToDelete,
                                     int tenantId)
            throws OrganizationUserRoleMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                /*
                 * We are getting the organization-user-role mappings accordingly for all the scenarios mentioned in
                 * @{OrganizationUserRoleManagerImpl} Therefore, we only need to add the user role mappings,
                 * delete the user role mappings and update the user role mappings accordingly.
                 * */
                // add organization-user-role mappings
                if (CollectionUtils.isNotEmpty(organizationUserRoleMappingsToAdd)) {
                    addOrganizationUserRoleMappingsFromList(organizationUserRoleMappingsToAdd, tenantId);
                }
                //delete organization-user-role mappings
                if (CollectionUtils.isNotEmpty(organizationUserRoleMappingsToDelete)) {
                    deleteOrganizationUserRoleMappingsFromList(organizationUserRoleMappingsToDelete, tenantId);
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
            assignedLevel, boolean forced, int tenantId) throws OrganizationUserRoleMgtException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
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
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_FORCED, forced ? 1 : 0);
                            });
        } catch (DataAccessException e) {
            String message =
                    String.format(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR.getMessage(), roleId,
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
         * Since this method is to get directly assigned organization-user-role mapping, assignedLevel(an org. id) =
         * @param{organizationId}
         * */
        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        int directlyAssignedRoleMappingExist = -1;
        try {
            boolean mappingsExists = namedJdbcTemplate
                    .fetchSingleRecord(buildIsRoleMappingExistsQuery(organizationId, false),
                            /*
                             * We are not checking whether the role is forced or not. We want to get a user role
                             * mapping on params organizationId, userId, roleId, tenantId and assignedLevel
                             * */
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
                return directlyAssignedRoleMappingExist;
            }
            List<Integer> results =
                    namedJdbcTemplate.executeQuery(GET_DIRECTLY_ASSIGNED_ORGANIZATION_USER_ROLE_MAPPING_LINK,
                            (resultSet, rowNumber) -> resultSet.getInt(VIEW_FORCED_COLUMN),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, userId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_ID, roleId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID, organizationId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT, organizationId);
                            });
            /*
             * Here, we get the ed and non-forced values of the organization-user-role mappings according to
             * the following parameters, userId, roleId, tenantId, organizationId, assignedAt value. There is a
             * possibility of having both forced and non-forced values due to this. But since we need to
             * update the values according to priority we select the max value from those values.
             * */
            directlyAssignedRoleMappingExist = results.stream().max(Integer::compare).get();
        } catch (DataAccessException e) {
            String message =
                    String.format(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR.getMessage(), roleId,
                            userId, organizationId);
            throw new OrganizationUserRoleMgtServerException(message,
                    ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR.getCode(), e);
        }
        return directlyAssignedRoleMappingExist;
    }

    @Override
    public List<String> getAllSubOrganizations(String organizationId) throws
            OrganizationUserRoleMgtException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        try {
            return namedJdbcTemplate.executeQuery(FIND_ALL_CHILD_ORG_IDS,
                    (resultSet, rowNumber) ->
                            resultSet.getString(VIEW_ID_COLUMN),
                    namedPreparedStatement -> namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID,
                            organizationId));
        } catch (DataAccessException e) {
            throw new OrganizationUserRoleMgtServerException(String.format(
                    ERROR_CODE_ORGANIZATION_GET_CHILDREN_ERROR.getMessage(), organizationId),
                    ERROR_CODE_ORGANIZATION_GET_CHILDREN_ERROR.getCode(), e);
        }
    }

    @Override
    public boolean checkOrganizationIdAvailability(String organizationId) throws OrganizationUserRoleMgtException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        try {
            return StringUtils.isNotBlank(namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_ID,
                    (resultSet, rowNumber) -> resultSet.getString(VIEW_ID_COLUMN),
                    namedPreparedStatement -> namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ORG_ID,
                            organizationId)));
        } catch (DataAccessException e) {
            throw new OrganizationUserRoleMgtServerException(String.format(
                    ERROR_CODE_ORGANIZATION_GET_ORGANIZATION_ID_ERROR.getMessage(), organizationId),
                    ERROR_CODE_ORGANIZATION_GET_ORGANIZATION_ID_ERROR.getCode(), e);
        }
    }

    //get roleId and roleName using SCIM
    private List<Role> getRoleIdAndRoleNameUsingSCIM(List<String> roleIdList, int tenantId) throws
            OrganizationUserRoleMgtServerException {

        List<Role> roleList;
        if (CollectionUtils.isEmpty(roleIdList)) {
            return null;
        }
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplateForIdentityDatabase();
        try {
            roleList = namedJdbcTemplate.withTransaction(template ->
                    template.executeQuery(buildQueryForGettingRoleDetails(roleIdList.size()),
                            (resultSet, rowNumber) -> new Role(resultSet.getString(VIEW_ATTR_VALUE_COLUMN),
                                    resultSet.getString(VIEW_ROLE_NAME_COLUMN)),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ATTR_NAME,
                                        SCIM_ROLE_ID_ATTR_NAME);
                                int index = 0;
                                for (String roleId : roleIdList) {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ATTR_VALUE +
                                            (index++), roleId);
                                }
                            }));
        } catch (TransactionException e) {
            throw new OrganizationUserRoleMgtServerException(
                    ERROR_CODE_RETRIEVING_DATA_FROM_IDENTITY_DB_ERROR.getMessage(),
                    ERROR_CODE_RETRIEVING_DATA_FROM_IDENTITY_DB_ERROR.getCode(), e);
        }
        return roleList;
    }

    private String queryForMultipleInserts(Integer numberOfMappings) {

        StringBuilder sb = new StringBuilder();
        sb.append(INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING);

        for (int i = 0; i < numberOfMappings; i++) {
            sb.append(String.format(INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING_VALUES, i));
            if (i != numberOfMappings - 1) {
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

    private String buildIsRoleMappingExistsQuery(String assignedLevel, boolean checkForced) {

        StringBuilder sb = new StringBuilder();
        sb.append(GET_ORGANIZATION_USER_ROLE_MAPPING);
        if (StringUtils.isNotBlank(assignedLevel)) {
            sb.append(AND).append(ASSIGNED_AT_ADDING);
        }
        if (checkForced) {
            sb.append(AND).append(FORCED_ADDING);
        }
        return sb.toString();
    }

    private String buildQueryForGettingRoleDetails(int numberOfRoles) {
        StringBuilder sb = new StringBuilder();
        sb.append(GET_ROLE_ID_AND_NAME).append("(");
        for (int i = 0; i < numberOfRoles; i++) {
            sb.append(String.format(SCIM_GROUP_ATTR_VALUE, i));
            if (i != numberOfRoles - 1) {
                sb.append(OR);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private void addOrganizationUserRoleMappingsFromList(List<OrganizationUserRoleMapping>
                                                                 organizationUserRoleMappingList,
                                                         int tenantId)
            throws OrganizationUserRoleMgtException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(queryForMultipleInserts(organizationUserRoleMappingList.size()),
                        namedPreparedStatement -> {
                            int numberOfOrganizationUserRoleMappings = organizationUserRoleMappingList.size();
                            for (int i = 0; i < numberOfOrganizationUserRoleMappings; i++) {
                                OrganizationUserRoleMapping organizationUserRoleMapping =
                                        organizationUserRoleMappingList.get(i);
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ID + "%d", i),
                                                Utils.generateUniqueID());
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_USER_ID + "%d", i),
                                                organizationUserRoleMapping.getUserId());
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%d", i),
                                                organizationUserRoleMapping.getRoleId());
                                namedPreparedStatement
                                        .setInt(String.format(DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%d", i),
                                                tenantId);
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ORG_ID + "%d", i),
                                                organizationUserRoleMapping.getOrganizationId());
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%d", i),
                                                organizationUserRoleMapping.getAssignedLevelOrganizationId());
                                namedPreparedStatement
                                        .setInt(String.format(DB_SCHEMA_COLUMN_NAME_FORCED + "%d", i),
                                                organizationUserRoleMapping.isForced() ? 1 : 0);
                            }
                        }, organizationUserRoleMappingList, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR, "", e);
        }
    }

    private void deleteOrganizationUserRoleMappingsFromList(List<OrganizationUserRoleMapping>
                                                                    organizationUserRoleMappingList, int tenantId)
            throws OrganizationUserRoleMgtException {

        NamedJdbcTemplate namedJdbcTemplate = getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(
                        queryForMultipleRoleMappingDeletion(organizationUserRoleMappingList.size()),
                        namedPreparedStatement -> {
                            int numberOfOrganizationUserRoleMappings = organizationUserRoleMappingList.size();
                            for (int i = 0; i < numberOfOrganizationUserRoleMappings; i++) {
                                OrganizationUserRoleMapping organizationUserRoleMapping =
                                        organizationUserRoleMappingList.get(i);
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ORG_ID + "%d", i),
                                                organizationUserRoleMapping.getOrganizationId());
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_USER_ID + "%d", i),
                                                organizationUserRoleMapping.getUserId());
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%d", i),
                                                organizationUserRoleMapping.getRoleId());
                                namedPreparedStatement
                                        .setInt(String.format(DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%d", i),
                                                tenantId);
                                namedPreparedStatement
                                        .setString(String.format(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%d", i),
                                                organizationUserRoleMapping.getAssignedLevelOrganizationId());
                                namedPreparedStatement
                                        .setInt(String.format(DB_SCHEMA_COLUMN_NAME_FORCED + "%d", i),
                                                organizationUserRoleMapping.isForced() ? 1 : 0);
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
}

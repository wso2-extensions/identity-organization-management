/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.dao;

import org.apache.commons.collections.CollectionUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CREATE_ORGANIZATION_USER_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_ROOT_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER_BY_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATION_FOR_ROOT_USER_IN_ORG;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG_HEAD;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG_TAIL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_SHARED_USER_ROLES_HEAD;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_SHARED_USER_ROLES_TAIL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_USER_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_UUID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_USER_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATE_ORGANIZATION_USER_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_OF_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getNewTemplate;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;
import static org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants.Error.UNEXPECTED_SERVER_ERROR;

/**
 * DAO implementation for managing organization user associations.
 */
public class OrganizationUserSharingDAOImpl implements OrganizationUserSharingDAO {

    @Override
    public void createOrganizationUserAssociation(String userId, String orgId, String associatedUserId,
                                                  String associatedOrgId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(CREATE_ORGANIZATION_USER_ASSOCIATION, namedPreparedStatement -> {
                    namedPreparedStatement.setString(1, userId);
                    namedPreparedStatement.setString(2, orgId);
                    namedPreparedStatement.setString(3, associatedUserId);
                    namedPreparedStatement.setString(4, associatedOrgId);
                }, null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_ORGANIZATION_USER_ASSOCIATION, e, associatedUserId);
        }
    }

    public boolean deleteUserAssociationOfUserByAssociatedOrg(String userId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, userId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
            return true;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER, e,
                    userId);
        }
    }

    @Override
    public boolean deleteUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_ROOT_USER,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedUserId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
            return true;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public List<UserAssociation> getUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(COLUMN_NAME_UM_SHARED_TYPE);
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedUserId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public List<UserAssociation> getUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId,
                                                                     SharedType sharedType)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER_BY_SHARED_TYPE,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE));
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedUserId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                        namedPreparedStatement.setString(3, sharedType.name());
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public UserAssociation getUserAssociationOfAssociatedUserByOrgId(String associatedUserId, String orgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_USER_ASSOCIATION_FOR_ROOT_USER_IN_ORG,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE));
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedUserId);
                        namedPreparedStatement.setString(2, orgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG, e,
                    orgId);
        }
    }

    @Override
    public UserAssociation getUserAssociation(String userId, String organizationId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_SHARED_USER,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE));
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, userId);
                        namedPreparedStatement.setString(2, organizationId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_OF_SHARED_USER, e,
                    userId, organizationId);
        }
    }

    @Override
    public List<String> getEligibleUsernamesForUserRemovalFromRole(String roleId, List<String> deletedUserNamesList,
                                                                   String tenantDomain, String permittedOrgId)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(deletedUserNamesList)) {
            return Collections.emptyList();
        }

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        String placeholders = deletedUserNamesList.stream()
                .map(username -> ":" + COLUMN_NAME_UM_USER_NAME + deletedUserNamesList.indexOf(username))
                .collect(Collectors.joining(","));

        String fetchRestrictedUsernamesWithPermittedAccessQuery =
                GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG_HEAD + placeholders +
                        GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG_TAIL;

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(
                    fetchRestrictedUsernamesWithPermittedAccessQuery,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_USER_NAME),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_UM_ROLE_ID, roleId);
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_PERMITTED_ORG_ID, permittedOrgId);

                        // Dynamically set usernames
                        for (int i = 0; i < deletedUserNamesList.size(); i++) {
                            namedPreparedStatement
                                    .setString(COLUMN_NAME_UM_USER_NAME + i, deletedUserNamesList.get(i));
                        }
                    }
                                                 );
        } catch (DataAccessException e) {
            String errorMessage =
                    String.format("Error while retrieving permitted usernames for role ID: %s in tenant domain: %s.",
                            roleId, tenantDomain);
            throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
        }
    }

    /*@Override
    public List<String> getSharedUserRolesOfSharedUser(List<String> roleIds, String tenantDomain)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        // Dynamically generate placeholders for the role IDs
        String placeholders = roleIds.stream()
                .map(roleId -> ":" + COLUMN_NAME_UM_ROLE_ID + roleIds.indexOf(roleId))
                .collect(Collectors.joining(","));

        // Construct the query
        String fetchEligibleUUIDsQuery = GET_ELIGIBLE_UUIDS_BY_ROLE_AND_TENANT_HEAD + placeholders +
                GET_ELIGIBLE_UUIDS_BY_ROLE_AND_TENANT_TAIL;

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(
                    fetchEligibleUUIDsQuery,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_UUID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);

                        // Dynamically set role IDs
                        for (int i = 0; i < roleIds.size(); i++) {
                            namedPreparedStatement.setString(COLUMN_NAME_UM_ROLE_ID + i, roleIds.get(i));
                        }
                    }
                                                 );
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error while retrieving UM_UUIDs for role IDs in tenant domain: %s.",
                    tenantDomain);
            throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
        }
    }*/

    @Override
    public List<String> getSharedUserRolesOfSharedUser(List<String> roleIds, String tenantDomain)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        // Generate placeholders for the role IDs dynamically
        String placeholders = roleIds.stream()
                .map(roleId -> ":" + COLUMN_NAME_UM_UUID + roleIds.indexOf(roleId))
                .collect(Collectors.joining(","));

        String fetchSharedRolesQuery = GET_SHARED_USER_ROLES_HEAD + placeholders + GET_SHARED_USER_ROLES_TAIL;

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(
                    fetchSharedRolesQuery,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_UUID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);

                        // Dynamically set role IDs
                        for (int i = 0; i < roleIds.size(); i++) {
                            namedPreparedStatement
                                    .setString(COLUMN_NAME_UM_UUID + i, roleIds.get(i));
                        }
                    }
                                                 );
        } catch (DataAccessException e) {
            String errorMessage =
                    String.format("Error while retrieving shared user roles for tenant domain: %s.", tenantDomain);
            throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
        }
    }

    @Override
    public void addEditRestrictionsForSharedUserRoles(String username, String tenantDomain, String domainName,
                                                      String name, String permittedOrgId) {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        //error4 = code

    }

}

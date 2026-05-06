/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.dao;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.util.AgentDbUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementServerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ERROR_RETRIEVING_AGENT_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ROLE_IN_TENANT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_RESTRICTED_AGENT_NAMES_BY_ROLE_AND_ORG;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_SHARED_AGENT_ROLES;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_SHARED_ROLES_OF_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.INSERT_RESTRICTED_EDIT_PERMISSION_FOR_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_AGENT_NAME;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_DOMAIN_NAME;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_EDIT_OPERATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_UUID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_NAME_AGENT_NAMES;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getNewTemplate;
import static org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants.Error.UNEXPECTED_SERVER_ERROR;

/**
 * DAO implementation for managing organization agent associations.
 * Association CRUD operations are delegated to {@link OrganizationUserSharingDAOImpl} backed by the agent
 * identity datasource ({@code jdbc/AgentIdentity}), which holds a {@code UM_ORG_USER_ASSOCIATION} table
 * with the same schema as the user sharing table. Role-related queries always target the UM database.
 */
public class OrganizationAgentSharingDAOImpl implements OrganizationAgentSharingDAO {

    private static final Log LOG = LogFactory.getLog(OrganizationAgentSharingDAOImpl.class);

    private final OrganizationUserSharingDAOImpl userSharingDao =
            new OrganizationUserSharingDAOImpl(AgentDbUtil::getAgentNewTemplate);

    @Override
    public void createOrganizationAgentAssociation(String agentId, String orgId, String associatedAgentId,
                                                   String associatedOrgId, SharedType sharedType)
            throws OrganizationManagementServerException {

        userSharingDao.createOrganizationUserAssociation(agentId, orgId, associatedAgentId, associatedOrgId,
                toUserSharedType(sharedType));
    }

    @Override
    public boolean deleteAgentAssociationOfAgentByAssociatedOrg(String agentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        return userSharingDao.deleteUserAssociationOfUserByAssociatedOrg(agentId, associatedOrgId);
    }

    @Override
    public boolean deleteAgentAssociationsOfAssociatedAgent(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        return userSharingDao.deleteUserAssociationsOfAssociatedUser(associatedAgentId, associatedOrgId);
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId)
            throws OrganizationManagementServerException {

        return userSharingDao.getUserAssociationsOfAssociatedUser(associatedAgentId, associatedOrgId)
                .stream().map(this::toAgentAssociation).collect(Collectors.toList());
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId,
                                                                         List<String> orgIdsScope,
                                                                         List<ExpressionNode> expressionNodes,
                                                                         String sortOrder, int limit)
            throws OrganizationManagementException {

        return userSharingDao.getUserAssociationsOfAssociatedUser(associatedAgentId, associatedOrgId,
                        orgIdsScope, expressionNodes, sortOrder, limit)
                .stream().map(this::toAgentAssociation).collect(Collectors.toList());
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId,
                                                                         SharedType sharedType)
            throws OrganizationManagementServerException {

        return userSharingDao.getUserAssociationsOfAssociatedUser(associatedAgentId, associatedOrgId,
                        toUserSharedType(sharedType))
                .stream().map(this::toAgentAssociation).collect(Collectors.toList());
    }

    @Override
    public boolean hasAgentAssociations(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        return userSharingDao.hasUserAssociations(associatedAgentId, associatedOrgId);
    }

    @Override
    public boolean hasAgentAssociationsInOrganizations(String associatedAgentId, String associatedOrgId,
                                                       List<String> orgIds)
            throws OrganizationManagementServerException {

        return userSharingDao.hasUserAssociationsInOrganizations(associatedAgentId, associatedOrgId, orgIds);
    }

    @Override
    public AgentAssociation getAgentAssociationOfAssociatedAgentByOrgId(String associatedAgentId, String orgId)
            throws OrganizationManagementServerException {

        return toAgentAssociation(
                userSharingDao.getUserAssociationOfAssociatedUserByOrgId(associatedAgentId, orgId));
    }

    @Override
    public AgentAssociation getAgentAssociation(String agentId, String organizationId)
            throws OrganizationManagementServerException {

        return toAgentAssociation(userSharingDao.getUserAssociation(agentId, organizationId));
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfGivenAgentOnGivenOrgs(String associatedAgentId,
                                                                               List<String> orgIds)
            throws OrganizationManagementServerException {

        return userSharingDao.getUserAssociationsOfGivenUserOnGivenOrgs(associatedAgentId, orgIds)
                .stream().map(this::toAgentAssociation).collect(Collectors.toList());
    }

    @Override
    public void updateSharedTypeOfAgentAssociation(int id, SharedType sharedType)
            throws OrganizationManagementServerException {

        userSharingDao.updateSharedTypeOfUserAssociation(id, toUserSharedType(sharedType));
    }

    @Override
    public List<String> getNonDeletableAgentRoleAssignments(String roleId,
                                                            List<String> deletedDomainQualifiedAgentNames,
                                                            String tenantDomain, String permittedOrgId)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(deletedDomainQualifiedAgentNames)) {
            return Collections.emptyList();
        }

        Map<String, List<String>> domainToAgentNamesMap = groupAgentNamesByDomain(deletedDomainQualifiedAgentNames);
        List<String> nonDeletableAgentNames = new ArrayList<>();
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        for (Map.Entry<String, List<String>> entry : domainToAgentNamesMap.entrySet()) {
            String domainName = entry.getKey();
            List<String> agentNamesInDomain = entry.getValue();

            String agentNamePlaceholder = "AGENT_NAME_";
            List<String> agentNamePlaceholders = new ArrayList<>();
            for (int i = 1; i <= agentNamesInDomain.size(); i++) {
                agentNamePlaceholders.add(":" + agentNamePlaceholder + i + ";");
            }
            String sqlStatement = GET_RESTRICTED_AGENT_NAMES_BY_ROLE_AND_ORG.replace(
                    PLACEHOLDER_NAME_AGENT_NAMES, String.join(", ", agentNamePlaceholders));

            try {
                nonDeletableAgentNames.addAll(namedJdbcTemplate.executeQuery(sqlStatement,
                        (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_AGENT_NAME),
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(COLUMN_NAME_UM_UUID, roleId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_PERMITTED_ORG_ID, permittedOrgId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_EDIT_OPERATION,
                                    EditOperation.DELETE.name());
                            for (int index = 1; index <= agentNamesInDomain.size(); index++) {
                                namedPreparedStatement.setString(agentNamePlaceholder + index,
                                        agentNamesInDomain.get(index - 1));
                            }
                        }));
            } catch (DataAccessException e) {
                String errorMessage = String.format(
                        "Error while retrieving permitted agent names for role ID: %s in tenant domain: %s.", roleId,
                        tenantDomain);
                throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
            }
        }
        return nonDeletableAgentNames;
    }

    @Override
    public List<String> getSharedAgentRolesFromAgentRoles(List<String> roleIds, String tenantDomain)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }

        String roleIdPlaceholder = "ROLE_ID_";
        List<String> roleIdPlaceholders = new ArrayList<>();
        for (int i = 1; i <= roleIds.size(); i++) {
            roleIdPlaceholders.add(":" + roleIdPlaceholder + i + ";");
        }

        String fetchSharedRolesQuery =
                GET_SHARED_AGENT_ROLES.replace(PLACEHOLDER_ROLE_IDS, String.join(", ", roleIdPlaceholders));

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        try {
            return namedJdbcTemplate.executeQuery(
                    fetchSharedRolesQuery,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_UUID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        int index = 1;
                        for (String roleId : roleIds) {
                            namedPreparedStatement.setString(roleIdPlaceholder + index, roleId);
                            index++;
                        }
                    });
        } catch (DataAccessException e) {
            String errorMessage =
                    String.format("Error while retrieving shared agent roles for tenant domain: %s.", tenantDomain);
            throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
        }
    }

    @Override
    public void addEditRestrictionsForSharedAgentRole(String roleId, String agentName, String tenantDomain,
                                                      String domainName, EditOperation editOperation,
                                                      String permittedOrgId)
            throws AgentSharingMgtServerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        try {
            // Query to retrieve UM_ID of the agent role assignment.
            Integer agentRoleId = namedJdbcTemplate.fetchSingleRecord(GET_AGENT_ROLE_IN_TENANT,
                    (resultSet, rowNumber) -> resultSet.getInt(COLUMN_NAME_UM_ID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_UM_AGENT_NAME, agentName);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_UUID, roleId);
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                    });

            if (agentRoleId != null) {
                // Insert the record into UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS.
                namedJdbcTemplate.executeUpdate(INSERT_RESTRICTED_EDIT_PERMISSION_FOR_AGENT,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_HYBRID_USER_ROLE_ID, agentRoleId);
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID, tenantId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_EDIT_OPERATION, editOperation.name());
                            namedPreparedStatement.setString(COLUMN_NAME_UM_PERMITTED_ORG_ID, permittedOrgId);
                        });
            } else {
                throw new AgentSharingMgtServerException(ERROR_CODE_ERROR_RETRIEVING_AGENT_ROLE_ID);
            }
        } catch (DataAccessException e) {
            throw new AgentSharingMgtServerException(ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION);
        }
    }

    @Override
    public List<String> getRolesSharedWithAgentInOrganization(String agentName, int tenantId, String domainName)
            throws AgentSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_SHARED_ROLES_OF_SHARED_AGENT,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_UUID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_UM_AGENT_NAME, agentName);
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                    });
        } catch (DataAccessException e) {
            throw new AgentSharingMgtServerException(ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT);
        }
    }

    private AgentAssociation toAgentAssociation(UserAssociation ua) {

        if (ua == null) {
            return null;
        }
        AgentAssociation agentAssociation = new AgentAssociation();
        agentAssociation.setId(ua.getId());
        agentAssociation.setAgentId(ua.getUserId());
        agentAssociation.setOrganizationId(ua.getOrganizationId());
        agentAssociation.setAssociatedAgentId(ua.getAssociatedUserId());
        agentAssociation.setAgentResidentOrganizationId(ua.getUserResidentOrganizationId());
        if (ua.getSharedType() != null) {
            agentAssociation.setSharedType(SharedType.valueOf(ua.getSharedType().name()));
        }
        return agentAssociation;
    }

    private org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType
            toUserSharedType(SharedType agentSharedType) {

        return org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType
                .valueOf(agentSharedType.name());
    }

    private Map<String, List<String>> groupAgentNamesByDomain(List<String> deletedDomainQualifiedAgentNames) {

        Map<String, List<String>> domainToAgentNamesMap = new HashMap<>();
        for (String deletedDomainQualifiedAgentName : deletedDomainQualifiedAgentNames) {
            String domainName =
                    org.wso2.carbon.user.core.util.UserCoreUtil.extractDomainFromName(deletedDomainQualifiedAgentName);
            String agentName =
                    org.wso2.carbon.user.core.util.UserCoreUtil.removeDomainFromName(deletedDomainQualifiedAgentName);
            domainToAgentNamesMap.computeIfAbsent(domainName, k -> new ArrayList<>()).add(agentName);
        }
        return domainToAgentNamesMap;
    }
}

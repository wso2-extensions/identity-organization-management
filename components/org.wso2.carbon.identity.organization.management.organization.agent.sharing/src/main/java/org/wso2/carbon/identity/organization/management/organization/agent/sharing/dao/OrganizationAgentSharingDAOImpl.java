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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.util.AgentDbUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.List;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ERROR_RETRIEVING_AGENT_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ROLE_IN_TENANT;
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
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getNewTemplate;

/**
 * DAO implementation for managing organization agent associations.
 * Association CRUD operations are delegated to {@link OrganizationUserSharingDAOImpl} backed by the agent
 * identity datasource ({@code jdbc/AgentIdentity}), which holds a {@code UM_ORG_USER_ASSOCIATION} table
 * with the same schema as the user sharing table. Role-related queries always target the UM database.
 */
public class OrganizationAgentSharingDAOImpl implements OrganizationAgentSharingDAO {

    private static final Log LOG = LogFactory.getLog(OrganizationAgentSharingDAOImpl.class);

    private final OrganizationUserSharingDAOImpl userSharingDao =
            new OrganizationUserSharingDAOImpl(AgentDbUtil::getAgentNewTemplate, AgentDbUtil::getAgentDbProductType);

    @Override
    public void createOrganizationAgentAssociation(String agentId, String orgId, String associatedAgentId,
                                                   String associatedOrgId, SharedType sharedType)
            throws OrganizationManagementServerException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating agent association for associatedAgentId: " + associatedAgentId + " in orgId: "
                    + orgId);
        }
        userSharingDao.createOrganizationUserAssociation(agentId, orgId, associatedAgentId, associatedOrgId,
                toUserSharedType(sharedType));
    }

    @Override
    public boolean deleteAgentAssociationOfAgentByAssociatedOrg(String agentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting agent association for agentId: " + agentId + " in associatedOrgId: "
                    + associatedOrgId);
        }
        return userSharingDao.deleteUserAssociationOfUserByAssociatedOrg(agentId, associatedOrgId);
    }

    @Override
    public boolean deleteAgentAssociationsOfAssociatedAgent(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        return userSharingDao.deleteUserAssociationsOfAssociatedUser(associatedAgentId, associatedOrgId);
    }

    @Override
    public boolean deleteAgentAssociationsByOrganizationId(String orgId)
            throws OrganizationManagementServerException {

        return userSharingDao.deleteUserAssociationsByOrganizationId(orgId);
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId)
            throws OrganizationManagementServerException {

        return userSharingDao.getUserAssociationsOfAssociatedUser(associatedAgentId, associatedOrgId)
                .stream().map(this::toAgentAssociation).collect(Collectors.toList());
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

    @Override
    public AgentAssociation getAgentAssociation(String sharedAgentId, String sharedOrgId)
            throws OrganizationManagementServerException {

        return toAgentAssociation(userSharingDao.getUserAssociation(sharedAgentId, sharedOrgId));
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

}

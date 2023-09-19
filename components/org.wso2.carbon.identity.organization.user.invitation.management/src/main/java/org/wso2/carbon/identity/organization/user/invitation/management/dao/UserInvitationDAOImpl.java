/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.user.invitation.management.dao;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtClientException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtServerException;
import org.wso2.carbon.identity.organization.user.invitation.management.models.Invitation;
import org.wso2.carbon.identity.organization.user.invitation.management.models.RoleAssignments;
import org.wso2.carbon.identity.organization.user.invitation.management.models.SharedUserAssociation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static java.time.ZoneOffset.UTC;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_APP_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_CONFIRMATION_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_CREATED_AT;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_DOMAIN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_EMAIL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_EXPIRED_AT;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_INVITED_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_REDIRECT_URL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ROLE_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_SHARED_USER_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_STATUS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_SUB_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_USER_NAME;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_USER_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.CREATE_USER_ASSOCIATION_TO_ORG;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.DELETE_ALL_ORG_ASSOCIATIONS_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.DELETE_INVITATION_BY_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.DELETE_ORG_ASSOCIATION_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.DELETE_ROLE_ASSIGNMENTS_BY_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_ACTIVE_INVITATION_BY_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_ALL_ORG_ASSOCIATIONS_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_INVITATIONS_BY_INVITED_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_INVITATIONS_BY_INVITED_ORG_ID_WITH_STATUS_FILTER_EXPIRED;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_INVITATIONS_BY_INVITED_ORG_ID_WITH_STATUS_FILTER_PENDING;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_INVITATION_BY_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_INVITATION_FROM_CONFIRMATION_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_INVITATION_ID_FROM_CONFIRMATION_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_ORG_ASSOCIATIONS_FOR_ACTUAL_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.GET_ROLE_ASSIGNMENTS_BY_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.STORE_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.SQLConstants.SQLQueries.STORE_ROLE_ASSIGNMENTS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_COMMIT_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_CREATE_ORG_ASSOC;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_DELETE_INVITATION_BY_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_DELETE_INVITATION_DETAILS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_DELETE_ROLE_ASSIGNMENTS_BY_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_INVITATION_BY_CONF_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_INVITATION_BY_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_ORG_ASSOCIATIONS_FOR_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_ORG_ASSOCIATION_FOR_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_MULTIPLE_INVITATIONS_FOR_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_RETRIEVE_INVITATIONS_FOR_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_RETRIEVE_INVITATION_BY_CONFIRMATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_RETRIEVE_INVITATION_BY_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_RETRIEVE_INVITATION_DETAILS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_RETRIEVE_ROLE_ASSIGNMENTS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_RETRIEVE_ROLE_ASSIGNMENTS_FOR_INVITATION_BY_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_STORE_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_STORE_ROLES_APP_ID_INVALID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_STORE_ROLE_ASSIGNMENTS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.FILTER_STATUS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.OPERATION_EQ;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.SQL_FK_CONSTRAINT_VIOLATION_ERROR_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.STATUS_PENDING;

/**
 * DAO implementation for invitations
 */
public class UserInvitationDAOImpl implements UserInvitationDAO {

    private static String getInvitationsByOrganizationQuery(String filterParam, String filterOp, String filterValue) {

        String getInvitationsByOrganizationIdQuery = GET_INVITATIONS_BY_INVITED_ORG_ID;
        if (FILTER_STATUS.equals(filterParam) && OPERATION_EQ.equals(filterOp)) {
            getInvitationsByOrganizationIdQuery = GET_INVITATIONS_BY_INVITED_ORG_ID_WITH_STATUS_FILTER_EXPIRED;
            if (STATUS_PENDING.equals(filterValue)) {
                getInvitationsByOrganizationIdQuery = GET_INVITATIONS_BY_INVITED_ORG_ID_WITH_STATUS_FILTER_PENDING;
            }
        }
        return getInvitationsByOrganizationIdQuery;
    }

    @Override
    public void createInvitation(Invitation invitation) throws UserInvitationMgtException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(true)) {
            try (PreparedStatement invitationCreatePrepStat = connection.prepareStatement(STORE_INVITATION)) {
                invitationCreatePrepStat.setString(1, invitation.getInvitationId());
                invitationCreatePrepStat.setString(2, invitation.getConfirmationCode());
                invitationCreatePrepStat.setString(3, invitation.getUsername());
                invitationCreatePrepStat.setString(4, invitation.getEmail());
                invitationCreatePrepStat.setString(5, invitation.getUserDomain());
                invitationCreatePrepStat.setString(6, invitation.getUserOrganizationId());
                invitationCreatePrepStat.setString(7, invitation.getInvitedOrganizationId());
                invitationCreatePrepStat.setString(8, invitation.getStatus());
                Timestamp currentTimestamp = new Timestamp(new Date().getTime());
                invitationCreatePrepStat.setTimestamp(9, currentTimestamp,
                        Calendar.getInstance(TimeZone.getTimeZone(UTC)));
                int expiryTime = Integer.parseInt(IdentityUtil.getProperty(
                        "OrganizationUserInvitation.DefaultExpiryTime"));
                Timestamp expiryTimestamp = new Timestamp(
                        new Timestamp(currentTimestamp.getTime() + TimeUnit.MINUTES.toMillis(expiryTime)).getTime());
                invitationCreatePrepStat.setTimestamp(10, expiryTimestamp,
                        Calendar.getInstance(TimeZone.getTimeZone(UTC)));
                invitationCreatePrepStat.setString(11, invitation.getUserRedirectUrl());
                invitationCreatePrepStat.executeUpdate();
            } catch (SQLException e) {
                throw handleServerException(ERROR_CODE_STORE_INVITATION, invitation.getUsername(), e);
            }
            if (invitation.getRoleAssignments() != null) {
                try (PreparedStatement invitationRoleAssignmentPrepStat =
                             connection.prepareStatement(STORE_ROLE_ASSIGNMENTS)) {
                    for (RoleAssignments roleAssignment : invitation.getRoleAssignments()) {
                        String applicationId = roleAssignment.getApplicationId();
                        if (roleAssignment.getRoles() != null) {
                            for (String role : roleAssignment.getRoles()) {
                                invitationRoleAssignmentPrepStat.setString(1, invitation.getInvitationId());
                                invitationRoleAssignmentPrepStat.setString(2, applicationId);
                                invitationRoleAssignmentPrepStat.setString(3, role);
                                invitationRoleAssignmentPrepStat.addBatch();
                            }
                        }
                    }
                    invitationRoleAssignmentPrepStat.executeBatch();
                } catch (SQLException e) {
                    IdentityDatabaseUtil.rollbackTransaction(connection);
                    if (SQL_FK_CONSTRAINT_VIOLATION_ERROR_CODE == e.getErrorCode() &&
                            StringUtils.containsIgnoreCase(e.getMessage(), "FK_ORG_USER_ROLE_SP_APP")) {
                        throw handleClientException(ERROR_CODE_STORE_ROLES_APP_ID_INVALID, StringUtils.EMPTY, e);
                    }
                    throw handleServerException(ERROR_CODE_STORE_ROLE_ASSIGNMENTS, invitation.getUsername(), e);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_COMMIT_INVITATION, null, e);
        }
    }

    @Override
    public Invitation getInvitationByInvitationId(String invitationId)
            throws UserInvitationMgtServerException {

        Invitation invitation = new Invitation();
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement invitationGetPrepStat =
                         connection.prepareStatement(GET_INVITATION_BY_INVITATION_ID)) {
                invitationGetPrepStat.setString(1, invitationId);
                try (ResultSet invitationsResultSet = invitationGetPrepStat.executeQuery()) {
                    if (!invitationsResultSet.next()) {
                        return null;
                    }
                    do {
                        invitation.setInvitationId(invitationsResultSet.getString(COLUMN_NAME_INVITATION_ID));
                        invitation.setConfirmationCode(invitationsResultSet.
                                getString(COLUMN_NAME_CONFIRMATION_CODE));
                        invitation.setUsername(invitationsResultSet.getString(COLUMN_NAME_USER_NAME));
                        invitation.setUserDomain(invitationsResultSet.getString(COLUMN_NAME_DOMAIN));
                        invitation.setEmail(invitationsResultSet.getString(COLUMN_NAME_EMAIL));
                        invitation.setUserOrganizationId(invitationsResultSet.getString(COLUMN_NAME_USER_ORG_ID));
                        invitation.setInvitedOrganizationId(invitationsResultSet.
                                getString(COLUMN_NAME_INVITED_ORG_ID));
                        invitation.setStatus(invitationsResultSet.getString(COLUMN_NAME_STATUS));
                        invitation.setCreatedAt(invitationsResultSet.getTimestamp(COLUMN_NAME_CREATED_AT));
                        invitation.setExpiredAt(invitationsResultSet.getTimestamp(COLUMN_NAME_EXPIRED_AT));
                        invitation.setUserRedirectUrl(invitationsResultSet.getString(COLUMN_NAME_REDIRECT_URL));
                    } while (invitationsResultSet.next());
                }
            } catch (SQLException e) {
                throw handleServerException(ERROR_CODE_RETRIEVE_INVITATION_DETAILS, invitationId, e);
            }
            List<RoleAssignments> roleAssignmentsResultList = new ArrayList<>();
            try (PreparedStatement roleAssignmentsPrepStat =
                         connection.prepareStatement(GET_ROLE_ASSIGNMENTS_BY_INVITATION_ID)) {
                roleAssignmentsPrepStat.setString(1, invitation.getInvitationId());
                try (ResultSet roleAssignmentsResultSet = roleAssignmentsPrepStat.executeQuery()) {
                    while (roleAssignmentsResultSet.next()) {
                        RoleAssignments roleAssignment = new RoleAssignments();
                        roleAssignment.setApplicationId(roleAssignmentsResultSet.getString(COLUMN_NAME_APP_ID));
                        roleAssignment.setRole(roleAssignmentsResultSet.getString(COLUMN_NAME_ROLE_ID));
                        roleAssignmentsResultList.add(roleAssignment);
                    }
                }
            } catch (SQLException e) {
                throw handleServerException(ERROR_CODE_RETRIEVE_ROLE_ASSIGNMENTS, invitationId, e);
            }
            List<RoleAssignments> roleAssignmentsList = processRoleAssignments(roleAssignmentsResultList);
            invitation.setRoleAssignments(roleAssignmentsList.toArray(new RoleAssignments[0]));
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_GET_INVITATION, invitationId, e);
        }
        return invitation;
    }

    @Override
    public Invitation getInvitationByConfirmationCode(String confirmationCode)
            throws UserInvitationMgtServerException {

        Invitation invitation = null;
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement invitationGetPrepStat =
                     connection.prepareStatement(GET_INVITATION_FROM_CONFIRMATION_CODE)) {
            invitationGetPrepStat.setString(1, confirmationCode);
            try (ResultSet invitationsResultSet = invitationGetPrepStat.executeQuery()) {
                if (invitationsResultSet.next()) {
                    invitation = new Invitation();
                    invitation.setInvitationId(invitationsResultSet.getString(COLUMN_NAME_INVITATION_ID));
                    invitation.setConfirmationCode(invitationsResultSet.getString(COLUMN_NAME_CONFIRMATION_CODE));
                    invitation.setUsername(invitationsResultSet.getString(COLUMN_NAME_USER_NAME));
                    invitation.setEmail(invitationsResultSet.getString(COLUMN_NAME_EMAIL));
                    invitation.setUserOrganizationId(invitationsResultSet.getString(COLUMN_NAME_USER_ORG_ID));
                    invitation.setInvitedOrganizationId(invitationsResultSet.getString(COLUMN_NAME_INVITED_ORG_ID));
                    invitation.setStatus(invitationsResultSet.getString(COLUMN_NAME_STATUS));
                    invitation.setExpiredAt(invitationsResultSet.getTimestamp(COLUMN_NAME_EXPIRED_AT));
                }
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_RETRIEVE_INVITATION_BY_CONFIRMATION_ID, confirmationCode, e);
        }
        return invitation;
    }

    @Override
    public Invitation getInvitationWithRolesByConfirmationCode(String confirmationCode)
            throws UserInvitationMgtServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
            PreparedStatement getInvitationIdFromCodePrepStat =
                    connection.prepareStatement(GET_INVITATION_ID_FROM_CONFIRMATION_CODE)) {
            getInvitationIdFromCodePrepStat.setString(1, confirmationCode);
            try (ResultSet resultSet = getInvitationIdFromCodePrepStat.executeQuery()) {
                if (resultSet.next()) {
                    String invitationId = resultSet.getString(COLUMN_NAME_INVITATION_ID);
                    return getInvitationByInvitationId(invitationId);
                }
                return null;
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_GET_INVITATION_BY_CONF_CODE, confirmationCode, e);
        }
    }

    @Override
    public List<Invitation> getInvitationsByOrganization(String organizationId, String filterParam, String filterOp,
                                                         String filterValue) throws UserInvitationMgtServerException {

        String getInvitationsByOrganizationIdQuery = getInvitationsByOrganizationQuery(filterParam, filterOp,
                filterValue);
        List<Invitation> invitationsList = new ArrayList<>();
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement invitationGetPrepStat =
                     connection.prepareStatement(getInvitationsByOrganizationIdQuery)) {
            invitationGetPrepStat.setString(1, organizationId);
            try (ResultSet invitationsResultSet = invitationGetPrepStat.executeQuery()) {
                while (invitationsResultSet.next()) {
                    Invitation invitation = new Invitation();
                    invitation.setInvitationId(invitationsResultSet.getString(COLUMN_NAME_INVITATION_ID));
                    invitation.setConfirmationCode(invitationsResultSet.getString(COLUMN_NAME_CONFIRMATION_CODE));
                    invitation.setUsername(invitationsResultSet.getString(COLUMN_NAME_USER_NAME));
                    invitation.setEmail(invitationsResultSet.getString(COLUMN_NAME_EMAIL));
                    invitation.setUserOrganizationId(invitationsResultSet.getString(COLUMN_NAME_USER_ORG_ID));
                    invitation.setInvitedOrganizationId(invitationsResultSet.getString(COLUMN_NAME_INVITED_ORG_ID));
                    invitation.setStatus(invitationsResultSet.getString(COLUMN_NAME_STATUS));
                    invitation.setCreatedAt(invitationsResultSet.getTimestamp(COLUMN_NAME_CREATED_AT));
                    invitation.setExpiredAt(invitationsResultSet.getTimestamp(COLUMN_NAME_EXPIRED_AT));
                    invitation.setUserRedirectUrl(invitationsResultSet.getString(COLUMN_NAME_REDIRECT_URL));
                    invitationsList.add(invitation);
                }
            } catch (SQLException e) {
                throw handleServerException(ERROR_CODE_RETRIEVE_INVITATION_BY_ORG_ID, organizationId, e);
            }
            try (PreparedStatement roleAssignmentsPrepStat =
                         connection.prepareStatement(GET_ROLE_ASSIGNMENTS_BY_INVITATION_ID)) {
                for (Invitation invitation : invitationsList) {
                    List<RoleAssignments> roleAssignmentsResultList = new ArrayList<>();
                    roleAssignmentsPrepStat.setString(1, invitation.getInvitationId());
                    try (ResultSet roleAssignmentsResultSet = roleAssignmentsPrepStat.executeQuery()) {
                        while (roleAssignmentsResultSet.next()) {
                            RoleAssignments roleAssignment = new RoleAssignments();
                            roleAssignment.setInvitationId(roleAssignmentsResultSet.
                                    getString(COLUMN_NAME_INVITATION_ID));
                            roleAssignment.setApplicationId(roleAssignmentsResultSet.getString(COLUMN_NAME_APP_ID));
                            roleAssignment.setRole(roleAssignmentsResultSet.getString(COLUMN_NAME_ROLE_ID));
                            roleAssignmentsResultList.add(roleAssignment);
                        }
                    }
                    List<RoleAssignments> roleAssignmentsList = processRoleAssignments(roleAssignmentsResultList);
                    invitation.setRoleAssignments(roleAssignmentsList.toArray(new RoleAssignments[0]));
                }
            } catch (SQLException e) {
                throw handleServerException(ERROR_CODE_RETRIEVE_ROLE_ASSIGNMENTS_FOR_INVITATION_BY_ORG_ID,
                        organizationId, e);
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_RETRIEVE_INVITATIONS_FOR_ORG_ID, organizationId, e);
        }
        return invitationsList;
    }

    @Override
    public boolean deleteInvitation(String invitationId) throws UserInvitationMgtServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(true)) {
            try (PreparedStatement invitationDeletePrepStat =
                         connection.prepareStatement(DELETE_ROLE_ASSIGNMENTS_BY_INVITATION_ID)) {
                invitationDeletePrepStat.setString(1, invitationId);
                invitationDeletePrepStat.executeUpdate();
            } catch (SQLException e) {
                throw handleServerException(ERROR_CODE_DELETE_ROLE_ASSIGNMENTS_BY_INVITATION, invitationId, e);
            }
            try (PreparedStatement invitationDeletePrepStat =
                         connection.prepareStatement(DELETE_INVITATION_BY_INVITATION_ID)) {
                invitationDeletePrepStat.setString(1, invitationId);
                invitationDeletePrepStat.executeUpdate();
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                throw handleServerException(ERROR_CODE_DELETE_INVITATION_DETAILS, invitationId, e);
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_DELETE_INVITATION_BY_ID, null, e);
        }
    }

    @Override
    public Invitation getActiveInvitationByUser(String username, String domain, String userOrganizationId,
                                                String invitedOrganizationId) throws UserInvitationMgtException {

        List<Invitation> invitationList = new ArrayList<>();
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement invitationGetByUserPrepStat =
                     connection.prepareStatement(GET_ACTIVE_INVITATION_BY_USER)) {
            invitationGetByUserPrepStat.setString(1, username);
            invitationGetByUserPrepStat.setString(2, domain);
            invitationGetByUserPrepStat.setString(3, userOrganizationId);
            invitationGetByUserPrepStat.setString(4, invitedOrganizationId);
            try (ResultSet invitationsResultSet = invitationGetByUserPrepStat.executeQuery()) {
                while (invitationsResultSet.next()) {
                    Invitation invitation = new Invitation();
                    invitation.setInvitationId(invitationsResultSet.getString(COLUMN_NAME_INVITATION_ID));
                    invitation.setConfirmationCode(invitationsResultSet.getString(COLUMN_NAME_CONFIRMATION_CODE));
                    invitation.setUsername(invitationsResultSet.getString(COLUMN_NAME_USER_NAME));
                    invitation.setEmail(invitationsResultSet.getString(COLUMN_NAME_EMAIL));
                    invitation.setUserOrganizationId(invitationsResultSet.getString(COLUMN_NAME_USER_ORG_ID));
                    invitation.setInvitedOrganizationId(invitationsResultSet.getString(COLUMN_NAME_INVITED_ORG_ID));
                    invitation.setStatus(invitationsResultSet.getString(COLUMN_NAME_STATUS));
                    invitation.setCreatedAt(invitationsResultSet.getTimestamp(COLUMN_NAME_CREATED_AT));
                    invitation.setExpiredAt(invitationsResultSet.getTimestamp(COLUMN_NAME_EXPIRED_AT));
                    invitation.setUserRedirectUrl(invitationsResultSet.getString(COLUMN_NAME_REDIRECT_URL));
                    invitationList.add(invitation);
                }
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_GET_INVITATION_BY_USER, username, e);
        }
        if (invitationList.size() > 1) {
            throw handleClientException(ERROR_CODE_MULTIPLE_INVITATIONS_FOR_USER, username, null);
        } else if (invitationList.isEmpty()) {
            return null;
        }
        return invitationList.get(0);
    }

    @Override
    public void createOrganizationAssociation(String realUserId, String residentOrgId, String sharedUserId,
                                              String sharedOrgId) throws UserInvitationMgtException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement createOrgAssocPrepStat = connection.prepareStatement(CREATE_USER_ASSOCIATION_TO_ORG)) {
            createOrgAssocPrepStat.setString(1, sharedUserId);
            createOrgAssocPrepStat.setString(2, sharedOrgId);
            createOrgAssocPrepStat.setString(3, realUserId);
            createOrgAssocPrepStat.setString(4, residentOrgId);
            createOrgAssocPrepStat.executeUpdate();
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_CREATE_ORG_ASSOC, sharedUserId, e);
        }
    }

    @Override
    public boolean deleteOrgUserAssociationToSharedOrg(String userId, String organizationId)
            throws UserInvitationMgtException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement userOrgDeletePrepStat =
                     connection.prepareStatement(DELETE_ORG_ASSOCIATION_FOR_SHARED_USER)) {
            userOrgDeletePrepStat.setString(1, userId);
            userOrgDeletePrepStat.setString(2, organizationId);
            userOrgDeletePrepStat.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_GET_INVITATION_BY_USER, userId, e);
        }
    }

    @Override
    public boolean deleteAllAssociationsOfOrgUserToSharedOrgs(String userId, String organizationId)
            throws UserInvitationMgtException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement userOrgDeletePrepStat =
                     connection.prepareStatement(DELETE_ALL_ORG_ASSOCIATIONS_FOR_SHARED_USER)) {
            userOrgDeletePrepStat.setString(1, userId);
            userOrgDeletePrepStat.setString(2, organizationId);
            userOrgDeletePrepStat.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_GET_INVITATION_BY_USER, userId, e);
        }
    }

    @Override
    public List<SharedUserAssociation> getAllAssociationsOfOrgUserToSharedOrgs(String userId, String organizationId)
            throws UserInvitationMgtException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement getUserSharedOrgsPrepStat =
                     connection.prepareStatement(GET_ALL_ORG_ASSOCIATIONS_FOR_SHARED_USER)) {
            getUserSharedOrgsPrepStat.setString(1, userId);
            getUserSharedOrgsPrepStat.setString(2, organizationId);
            List<SharedUserAssociation> sharedUserAssociationList = new ArrayList<>();
            try (ResultSet resultSet = getUserSharedOrgsPrepStat.executeQuery()) {
                if (resultSet.next()) {
                    SharedUserAssociation sharedUserAssociation = new SharedUserAssociation();
                    sharedUserAssociation.setSharedUserId(resultSet.getString(COLUMN_NAME_SHARED_USER_ID));
                    sharedUserAssociation.setSharedOrganizationId(resultSet.getString(COLUMN_NAME_SUB_ORG_ID));
                    sharedUserAssociationList.add(sharedUserAssociation);
                }
            }
            return sharedUserAssociationList;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_GET_ORG_ASSOCIATIONS_FOR_USER, userId, e);
        }
    }

    @Override
    public SharedUserAssociation getAssociationOfOrgUserToSharedOrg(String userId, String sharedOrgId)
            throws UserInvitationMgtException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement getUserSharedOrgPrepStat =
                     connection.prepareStatement(GET_ORG_ASSOCIATIONS_FOR_ACTUAL_USER)) {
            getUserSharedOrgPrepStat.setString(1, userId);
            getUserSharedOrgPrepStat.setString(2, sharedOrgId);
            try (ResultSet resultSet = getUserSharedOrgPrepStat.executeQuery()) {
                if (resultSet.next()) {
                    SharedUserAssociation sharedUserAssociation = new SharedUserAssociation();
                    sharedUserAssociation.setSharedUserId(resultSet.getString(COLUMN_NAME_SHARED_USER_ID));
                    sharedUserAssociation.setSharedOrganizationId(resultSet.getString(COLUMN_NAME_SUB_ORG_ID));
                    return sharedUserAssociation;
                }
                return null;
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_GET_ORG_ASSOCIATION_FOR_USER, userId, e);
        }
    }

    private List<RoleAssignments> processRoleAssignments(List<RoleAssignments> roleAssignmentsResultList) {

        // Processing the role assignments
        List<RoleAssignments> roleAssignmentsList = new ArrayList<>();
        // To keep the processing remarks of the application ids
        List<String> processedAppIds = new ArrayList<>();
        if (roleAssignmentsResultList.isEmpty()) {
            return roleAssignmentsResultList;
        }
        for (int i = 0; i < roleAssignmentsResultList.size(); i++) {
            String appId = roleAssignmentsResultList.get(i).getApplicationId();
            if (processedAppIds.contains(appId)) {
                continue;
            }
            RoleAssignments roleAssignment = new RoleAssignments();
            List<String> rolesList = new ArrayList<>();
            for (RoleAssignments roleAssignments : roleAssignmentsResultList) {
                if (appId.equals(roleAssignments.getApplicationId())) {
                    rolesList.add(roleAssignments.getRole());
                }
            }
            roleAssignment.setRoles(rolesList.toArray(new String[0]));
            roleAssignment.setApplicationId(appId);
            processedAppIds.add(appId);
            roleAssignmentsList.add(roleAssignment);
        }
        return roleAssignmentsList;
    }

    private UserInvitationMgtServerException handleServerException(
            UserInvitationMgtConstants.ErrorMessage error, String data, Throwable e) {

        String description = processDescription(error, data);
        return new UserInvitationMgtServerException(error.getCode(), error.getMessage(), description, e);
    }

    private UserInvitationMgtClientException handleClientException(
            UserInvitationMgtConstants.ErrorMessage error, String data, Throwable e) {

        String description = processDescription(error, data);
        return new UserInvitationMgtClientException(error.getCode(), error.getMessage(), description, e);
    }

    private String processDescription(UserInvitationMgtConstants.ErrorMessage error, String data) {

        String description = error.getDescription();
        if (StringUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return description;
    }
}

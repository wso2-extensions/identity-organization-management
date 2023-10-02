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

package org.wso2.carbon.identity.organization.management.organization.user.association.dao;

import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.organization.user.association.models.SharedUserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.SQLConstants.CREATE_ORGANIZATION_USER_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.SQLConstants.DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.SQLConstants.DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG;
import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_SHARED_USER_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_SUB_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATE_ORGANIZATION_USER_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * DAO implementation for organization user association management.
 */
public class OrganizationUserAssociationDAOImpl implements OrganizationUserAssociationDAO {

    @Override
    public void createOrganizationUserAssociation(String userId, String residentOrgId, String sharedUserId,
                                                  String sharedOrgId) throws OrganizationManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement createOrgAssocPrepStat = connection.prepareStatement(
                     CREATE_ORGANIZATION_USER_ASSOCIATION)) {
            createOrgAssocPrepStat.setString(1, sharedUserId);
            createOrgAssocPrepStat.setString(2, sharedOrgId);
            createOrgAssocPrepStat.setString(3, userId);
            createOrgAssocPrepStat.setString(4, residentOrgId);
            createOrgAssocPrepStat.executeUpdate();
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_ORGANIZATION_USER_ASSOCIATION, e, sharedUserId);
        }
    }

    @Override
    public boolean deleteOrganizationUserAssociationOfSharedUser(String sharedUserId, String userOrganizationId)
            throws OrganizationManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement userOrgDeletePrepStat =
                     connection.prepareStatement(DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER)) {
            userOrgDeletePrepStat.setString(1, sharedUserId);
            userOrgDeletePrepStat.setString(2, userOrganizationId);
            userOrgDeletePrepStat.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER, e,
                    sharedUserId);
        }
    }

    @Override
    public boolean deleteOrganizationUserAssociations(String userId, String organizationId)
            throws OrganizationManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement userOrgDeletePrepStat =
                     connection.prepareStatement(DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER)) {
            userOrgDeletePrepStat.setString(1, userId);
            userOrgDeletePrepStat.setString(2, organizationId);
            userOrgDeletePrepStat.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATIONS, e, userId);
        }
    }

    @Override
    public List<SharedUserAssociation> getOrganizationUserAssociationsOfUser(String userId, String userOrganizationId)
            throws OrganizationManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement getUserSharedOrgsPrepStat =
                     connection.prepareStatement(GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER)) {
            getUserSharedOrgsPrepStat.setString(1, userId);
            getUserSharedOrgsPrepStat.setString(2, userOrganizationId);
            List<SharedUserAssociation> sharedUserAssociationList = new ArrayList<>();
            try (ResultSet resultSet = getUserSharedOrgsPrepStat.executeQuery()) {
                while (resultSet.next()) {
                    SharedUserAssociation sharedUserAssociation = new SharedUserAssociation();
                    sharedUserAssociation.setSharedUserId(resultSet.getString(COLUMN_NAME_SHARED_USER_ID));
                    sharedUserAssociation.setSharedOrganizationId(resultSet.getString(COLUMN_NAME_SUB_ORG_ID));
                    sharedUserAssociationList.add(sharedUserAssociation);
                }
            }
            return sharedUserAssociationList;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e, userId);
        }
    }

    @Override
    public SharedUserAssociation getOrganizationUserAssociationOfUserAtSharedOrg(String userId, String sharedOrgId)
            throws OrganizationManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement getUserSharedOrgPrepStat =
                     connection.prepareStatement(GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG)) {
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
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG, e,
                    userId, sharedOrgId);
        }
    }
}

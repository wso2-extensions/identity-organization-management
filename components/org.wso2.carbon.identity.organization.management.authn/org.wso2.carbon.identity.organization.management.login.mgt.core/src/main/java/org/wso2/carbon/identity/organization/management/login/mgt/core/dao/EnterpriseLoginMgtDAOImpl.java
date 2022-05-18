/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.login.mgt.core.dao;

import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.login.mgt.core.common.Constants;
import org.wso2.carbon.identity.organization.management.login.mgt.core.exceptions.EnterpriseLoginManagementException;
import org.wso2.carbon.identity.organization.management.login.mgt.core.exceptions.EnterpriseLoginManagementServerException;
import org.wso2.carbon.identity.organization.management.login.mgt.core.model.EmailDomainMapping;
import org.wso2.carbon.identity.organization.management.login.mgt.core.model.ServiceMappingConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of abstract DAO layer.
 */
public class EnterpriseLoginMgtDAOImpl implements EnterpriseLoginMgtDAO {

    @Override
    public void addEnterpriseLoginConfiguration(List<ServiceMappingConfiguration> configurationMappings,
                                                EmailDomainMapping emailDomainMapping)
            throws EnterpriseLoginManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement ps = connection.prepareStatement(SQLQueries.ADD_ENTERPRISE_LGN_MAPPING)) {
                for (ServiceMappingConfiguration configuration : configurationMappings) {
                    ps.setString(1, configuration.getInboundSpResourceId());
                    ps.setInt(2, configuration.getInboundSpTenantId());
                    ps.setString(3, configuration.getOutboundSpResourceId());
                    ps.setInt(4, configuration.getOutboundSpTenantId());
                    ps.addBatch();
                }
                ps.executeBatch();

                addEmailDomains(emailDomainMapping.getEmailDomains(), emailDomainMapping.getOutboundSpTenantId(),
                        emailDomainMapping.getInboundSpTenantId(), connection);
                IdentityDatabaseUtil.commitTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                throw new EnterpriseLoginManagementServerException(
                        Constants.ErrorMessage.ERROR_PERSISTING_CONFIG_MAPPINGS.getCode(),
                        Constants.ErrorMessage.ERROR_PERSISTING_CONFIG_MAPPINGS.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_PERSISTING_CONFIG_MAPPINGS.getCode(),
                    Constants.ErrorMessage.ERROR_PERSISTING_CONFIG_MAPPINGS.getMessage(), e);
        }
    }

    @Override
    public void addServices(List<ServiceMappingConfiguration> serviceMappingConfigurations)
            throws EnterpriseLoginManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement ps = connection.prepareStatement(SQLQueries.ADD_ENTERPRISE_LGN_MAPPING)) {
                for (ServiceMappingConfiguration configuration : serviceMappingConfigurations) {
                    ps.setString(1, configuration.getInboundSpResourceId());
                    ps.setInt(2, configuration.getInboundSpTenantId());
                    ps.setString(3, configuration.getOutboundSpResourceId());
                    ps.setInt(4, configuration.getOutboundSpTenantId());
                    ps.addBatch();
                }
                ps.executeBatch();
                IdentityDatabaseUtil.commitTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                throw new EnterpriseLoginManagementServerException(
                        Constants.ErrorMessage.ERROR_PERSISTING_CONFIG_MAPPINGS.getCode(),
                        Constants.ErrorMessage.ERROR_PERSISTING_CONFIG_MAPPINGS.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_PERSISTING_CONFIG_MAPPINGS.getCode(),
                    Constants.ErrorMessage.ERROR_PERSISTING_CONFIG_MAPPINGS.getMessage(), e);
        }
    }

    /**
     * Adds enterprise IDP email domains for the organization.
     *
     * @param outboundSpTenantId Tenant identifier of the organization.
     * @param emailDomains       Email domains of the organization users.
     * @param inboundSpTenantId  Tenant identifier of the registering service.
     * @param connection         Connection object.
     * @throws EnterpriseLoginManagementServerException If error occurred while persisting configurations.
     */
    private void addEmailDomains(List<String> emailDomains, int outboundSpTenantId, int inboundSpTenantId,
                                 Connection connection)
            throws EnterpriseLoginManagementServerException {

        try (PreparedStatement ps = connection.prepareStatement(SQLQueries.ADD_EMAIL_DOMAINS)) {
            for (String emailDomain : emailDomains) {
                ps.setInt(1, outboundSpTenantId);
                ps.setInt(2, inboundSpTenantId);
                ps.setString(3, emailDomain);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_ADDING_EMAIL_DOMAIN.getCode(),
                    Constants.ErrorMessage.ERROR_ADDING_EMAIL_DOMAIN.getMessage(), e);
        }
    }


    /**
     * Adds enterprise IDP email domains for the organization.
     *
     * @param outboundSpTenantId Tenant identifier of the organization.
     * @param emailDomains       Email domains of the organization users.
     * @param inboundSpTenantId  Tenant identifier of the registering service.
     * @throws EnterpriseLoginManagementServerException If error occurred while persisting configurations.
     */
    public void addEmailDomains(List<String> emailDomains, int outboundSpTenantId, int inboundSpTenantId)
            throws EnterpriseLoginManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement ps = connection.prepareStatement(SQLQueries.ADD_EMAIL_DOMAINS)) {
                for (String emailDomain : emailDomains) {
                    ps.setInt(1, outboundSpTenantId);
                    ps.setInt(2, inboundSpTenantId);
                    ps.setString(3, emailDomain);
                    ps.addBatch();
                }
                ps.executeBatch();
                IdentityDatabaseUtil.commitTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                throw new EnterpriseLoginManagementServerException(
                        Constants.ErrorMessage.ERROR_ADDING_EMAIL_DOMAIN.getCode(),
                        Constants.ErrorMessage.ERROR_ADDING_EMAIL_DOMAIN.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_ADDING_EMAIL_DOMAIN.getCode(),
                    Constants.ErrorMessage.ERROR_ADDING_EMAIL_DOMAIN.getMessage(), e);
        }
    }

    @Override
    public List<String> getEmailDomains(int inboundSpTenantId, int outboundSpTenantId)
            throws EnterpriseLoginManagementServerException {

        List<String> emailDomains = new ArrayList<>();
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement ps = connection.prepareStatement(SQLQueries.GET_EMAIL_DOMAINS)) {
            ps.setInt(1, outboundSpTenantId);
            ps.setInt(2, inboundSpTenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String emailDomain = rs.getString(Constants.EMAIL_DOMAIN);
                emailDomains.add(emailDomain);
            }
            rs.close();
            return emailDomains;
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_RETRIEVE_EMAIL_DOMAIN.getCode(),
                    Constants.ErrorMessage.ERROR_RETRIEVE_EMAIL_DOMAIN.getMessage(), e);
        }
    }

    @Override
    public void deleteSpecificEmailDomain(int inboundSpTenantId, int outboundSpTenantId, String emailDomain)
            throws EnterpriseLoginManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement ps = connection.prepareStatement(SQLQueries.DELETE_SPECIFIC_EMAIL_DOMAIN)) {
                ps.setInt(1, inboundSpTenantId);
                ps.setInt(2, outboundSpTenantId);
                ps.setString(3, emailDomain);
                ps.executeUpdate();
                IdentityDatabaseUtil.commitTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                throw new EnterpriseLoginManagementServerException(
                        Constants.ErrorMessage.ERROR_DELETING_EMAIL_DOMAIN.getCode(),
                        Constants.ErrorMessage.ERROR_DELETING_EMAIL_DOMAIN.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_DELETING_EMAIL_DOMAIN.getCode(),
                    Constants.ErrorMessage.ERROR_DELETING_EMAIL_DOMAIN.getMessage(), e);
        }
    }

    @Override
    public List<ServiceMappingConfiguration> getConfiguration(int inboundSpTenantId, int outboundSpTenantId)
            throws EnterpriseLoginManagementException {

        List<ServiceMappingConfiguration> serviceConfigMappings = new ArrayList<>();
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement ps = connection.prepareStatement(SQLQueries.GET_ENTERPRISE_LGN_MAPPING)) {
            ps.setInt(1, outboundSpTenantId);
            ps.setInt(2, inboundSpTenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String inboundSpResourceId = rs.getString(Constants.INBOUND_SP_RESOURCE_ID);
                int inboundSpTenantIdRetrieved = rs.getInt(Constants.INBOUND_SP_TENANT_ID);
                String outboundSpResourceId = rs.getString(Constants.OUTBOUND_SP_RESOURCE_ID);
                serviceConfigMappings.add(new ServiceMappingConfiguration(inboundSpResourceId,
                        inboundSpTenantIdRetrieved, outboundSpResourceId, outboundSpTenantId));
            }
            rs.close();
            return serviceConfigMappings;
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_RETRIEVE_CONFIG_MAPPINGS.getCode(),
                    Constants.ErrorMessage.ERROR_RETRIEVE_CONFIG_MAPPINGS.getMessage(), e);
        }
    }

    @Override
    public void updateOutboundSpId(ServiceMappingConfiguration configuration)
            throws EnterpriseLoginManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement ps =
                         connection.prepareStatement(SQLQueries.UPDATE_ENTERPRISE_LGN_MAPPING_OF_SERVICE)) {
                ps.setString(1, configuration.getOutboundSpResourceId());
                ps.setInt(2, configuration.getInboundSpTenantId());
                ps.setString(3, configuration.getInboundSpResourceId());
                ps.setInt(4, configuration.getOutboundSpTenantId());
                ps.executeUpdate();
                IdentityDatabaseUtil.commitTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                throw new EnterpriseLoginManagementServerException(
                        Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getCode(),
                        Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getCode(),
                    Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getMessage(), e);
        }
    }

    @Override
    public void deleteConfiguration(int inboundSpTenantId, int outboundSpTenantId)
            throws EnterpriseLoginManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement ps = connection.prepareStatement(SQLQueries.REMOVE_ENTERPRISE_LGN_MAPPING)) {
                ps.setInt(1, inboundSpTenantId);
                ps.setInt(2, outboundSpTenantId);
                ps.executeUpdate();
                deleteEmailDomains(inboundSpTenantId, outboundSpTenantId, connection);
                IdentityDatabaseUtil.commitTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                throw new EnterpriseLoginManagementServerException(
                        Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getCode(),
                        Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getCode(),
                    Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getMessage(), e);
        }
    }

    private void deleteEmailDomains(int inboundSpTenantId, int outboundSpTenantId, Connection connection)
            throws EnterpriseLoginManagementServerException {

        try (PreparedStatement ps = connection.prepareStatement(SQLQueries.DELETE_EMAIL_DOMAINS)) {
            ps.setInt(1, inboundSpTenantId);
            ps.setInt(2, outboundSpTenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_DELETING_EMAIL_DOMAIN.getCode(),
                    Constants.ErrorMessage.ERROR_DELETING_EMAIL_DOMAIN.getMessage(), e);
        }
    }

    @Override
    public void deleteServiceSpecificConfiguration(int inboundSpTenantId, int outboundSpTenantId,
                                                   String inboundSpResourceId)
            throws EnterpriseLoginManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement ps =
                         connection.prepareStatement(SQLQueries.REMOVE_ENTERPRISE_LGN_MAPPING_OF_SERVICE)) {
                ps.setInt(1, inboundSpTenantId);
                ps.setInt(2, outboundSpTenantId);
                ps.setString(3, inboundSpResourceId);
                ps.executeUpdate();
                IdentityDatabaseUtil.commitTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                throw new EnterpriseLoginManagementServerException(
                        Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getCode(),
                        Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getCode(),
                    Constants.ErrorMessage.ERROR_DELETING_CONFIG_MAPPING.getMessage(), e);
        }
    }

    @Override
    public List<Integer> getOrganizationsForEmailDomain(String emailDomain, int inboundSpTenantId)
            throws EnterpriseLoginManagementServerException {

        List<Integer> orgRegisteredWithEmailDomains = new ArrayList<>();
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement ps = connection.prepareStatement(SQLQueries.GET_ORG_FOR_EMAIL)) {
            ps.setString(1, emailDomain);
            ps.setInt(2, inboundSpTenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int outboundSpTenantId = rs.getInt(Constants.OUTBOUND_SP_TENANT_ID);
                orgRegisteredWithEmailDomains.add(outboundSpTenantId);
            }
            rs.close();
            return orgRegisteredWithEmailDomains;
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_RETRIEVING_ORG.getCode(),
                    Constants.ErrorMessage.ERROR_RETRIEVING_ORG.getMessage(), e);
        }
    }

    @Override
    public String getOutboundSpResourceId(String inboundSpResourceId, int inboundSpTenantId, int outboundSpTenantId)
            throws EnterpriseLoginManagementServerException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement ps = connection.prepareStatement(SQLQueries.GET_OUTBOUND_SP_RESOURCE_ID)) {
            ps.setInt(1, outboundSpTenantId);
            ps.setInt(2, inboundSpTenantId);
            ps.setString(3, inboundSpResourceId);
            ResultSet rs = ps.executeQuery();
            String outboundSpId = "";
            while (rs.next()) {
                outboundSpId = rs.getString(Constants.OUTBOUND_SP_RESOURCE_ID);
            }
            rs.close();
            return outboundSpId;
        } catch (SQLException e) {
            throw new EnterpriseLoginManagementServerException(
                    Constants.ErrorMessage.ERROR_RETRIEVE_OUTBOUND_SP.getCode(),
                    Constants.ErrorMessage.ERROR_RETRIEVE_OUTBOUND_SP.getMessage(), e);
        }
    }
}

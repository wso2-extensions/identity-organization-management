/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.login.mgt.core.dao;

import org.wso2.carbon.identity.organization.management.login.mgt.core.exceptions.EnterpriseLoginManagementException;
import org.wso2.carbon.identity.organization.management.login.mgt.core.exceptions.EnterpriseLoginManagementServerException;
import org.wso2.carbon.identity.organization.management.login.mgt.core.model.EmailDomainMapping;
import org.wso2.carbon.identity.organization.management.login.mgt.core.model.ServiceMappingConfiguration;

import java.util.List;

/**
 * DAO layer for Enterprise Login Mgt.
 */
public interface EnterpriseLoginMgtDAO {

    /**
     * Adds enterprise login management configurations.
     *
     * @param serviceMappingConfigurations List of Configuration DTO.
     * @param emailDomainDTO               Email domains DTO object.
     * @throws EnterpriseLoginManagementServerException If error occurred while persisting configurations.
     */
    void addEnterpriseLoginConfiguration(List<ServiceMappingConfiguration> serviceMappingConfigurations,
                                         EmailDomainMapping emailDomainDTO)
            throws EnterpriseLoginManagementServerException;

    /**
     * Adds enterprise login management configurations.
     *
     * @param serviceMappingConfigurations List of Configuration DTO.
     * @throws EnterpriseLoginManagementServerException If error occurred while persisting configurations.
     */
    void addServices(List<ServiceMappingConfiguration> serviceMappingConfigurations)
            throws EnterpriseLoginManagementServerException;

    /**
     * Retrieves enterprise login management configurations.
     *
     * @param outboundSpTenantId Organization identifier for which the services should be added.
     * @param inboundSpTenantId  Tenant identifier for which the service is registered for.
     * @return List<ServiceMappingConfiguration>  List of Configurations.
     * @throws EnterpriseLoginManagementException If an error occurred while retrieving the configurations.
     */
    List<ServiceMappingConfiguration> getConfiguration(int inboundSpTenantId, int outboundSpTenantId)
            throws EnterpriseLoginManagementException;

    /**
     * Deletes enterprise login management configurations.
     *
     * @param outboundSpTenantId Organization identifier for which the services should be added.
     * @param inboundSpTenantId  Tenant identifier for which the service is registered for.
     * @throws EnterpriseLoginManagementServerException If an error occurred while deleting configuration.
     */
    void deleteConfiguration(int inboundSpTenantId, int outboundSpTenantId)
            throws EnterpriseLoginManagementServerException;

    /**
     * Deletes enterprise login management configuration of specific service.
     *
     * @param inboundSpTenantId   Tenant identifier for which the service is registered for.
     * @param outboundSpTenantId  Organization identifier for which the services should be added.
     * @param inboundSpResourceId Service provider identifier with which an organization was registered with.
     */
    void deleteServiceSpecificConfiguration(int inboundSpTenantId, int outboundSpTenantId, String inboundSpResourceId)
            throws EnterpriseLoginManagementServerException;

    /**
     * Adds enterprise IDP email domains for the organization.
     *
     * @param outboundSpTenantId Tenant identifier of the organization.
     * @param emailDomains       Email domains of the organization users.
     * @param inboundSpTenantId  Tenant identifier of the registering service.
     * @throws EnterpriseLoginManagementServerException If error occurred while persisting configurations.
     */
    void addEmailDomains(List<String> emailDomains, int outboundSpTenantId, int inboundSpTenantId)
            throws EnterpriseLoginManagementServerException;

    /**
     * Gets email domains of the organization for enterprise login management.
     *
     * @param outboundSpTenantId Organization identifier for which the services should be added.
     * @param inboundSpTenantId  Tenant identifier for which the service is registered for.
     * @return ArrayList<String> List of email domains.
     * @throws EnterpriseLoginManagementServerException If an error occurred while deleting specific email domain.
     */
    List<String> getEmailDomains(int inboundSpTenantId, int outboundSpTenantId)
            throws EnterpriseLoginManagementServerException;

    /**
     * Deletes an email domain of the organization for enterprise login management.
     *
     * @param outboundSpTenantId Organization identifier for which the services should be added.
     * @param emailDomain        Email domain which has to be deleted.
     * @param inboundSpTenantId  Tenant identifier for which the service is
     *                           registered for.
     * @throws EnterpriseLoginManagementServerException If an error occurred while deleting specific email domain.
     */
    void deleteSpecificEmailDomain(int inboundSpTenantId, int outboundSpTenantId, String emailDomain)
            throws EnterpriseLoginManagementServerException;

    /**
     * Retrieves list of organizations registered with provided email domain and registered for provided service.
     *
     * @param emailDomain       Email domains registered.
     * @param inboundSpTenantId Tenant identifier for which the service is registered for.
     * @return List<Integer>     List of organizations.
     * @throws EnterpriseLoginManagementServerException If an error occurred while retrieving organizations.
     */
    List<Integer> getOrganizationsForEmailDomain(String emailDomain, int inboundSpTenantId)
            throws EnterpriseLoginManagementServerException;

    /**
     * Retrieves identifier of application created in organization for service.
     *
     * @param inboundSpTenantId   Tenant identifier for which the service is registered for.
     * @param outboundSpTenantId  Tenant identifier for which the service is registered in.
     * @param inboundSpResourceId SP identifier for which the service is registered for.
     * @throws EnterpriseLoginManagementServerException If an error occurred while retrieving outbound sp identifier.
     */
    String getOutboundSpResourceId(String inboundSpResourceId, int inboundSpTenantId, int outboundSpTenantId)
            throws EnterpriseLoginManagementServerException;

    /**
     * Updates enterprise login management application sp identifier.
     *
     * @param configuration Configuration that to be updated.
     * @throws EnterpriseLoginManagementServerException If error occurred while deleting email domains.
     */
    void updateOutboundSpId(ServiceMappingConfiguration configuration) throws EnterpriseLoginManagementServerException;
}

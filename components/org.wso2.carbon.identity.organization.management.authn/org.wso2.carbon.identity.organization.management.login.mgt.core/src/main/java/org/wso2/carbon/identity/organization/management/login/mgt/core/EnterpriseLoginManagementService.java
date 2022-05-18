/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.login.mgt.core;

import org.wso2.carbon.identity.organization.management.login.mgt.core.exceptions.EnterpriseLoginManagementException;
import org.wso2.carbon.identity.organization.management.login.mgt.core.model.Configuration;

import java.util.List;
import java.util.Optional;

/**
 * Enterprise login management service interface.
 */
public interface EnterpriseLoginManagementService {

    /**
     * Add enterprise login management configuration.
     *
     * @param configuration   Enterprise login management configuration.
     * @param inboundSpTenant Tenant the inbound service provider belongs to.
     * @throws EnterpriseLoginManagementException If an error while adding configurations.
     */
    void addEnterpriseLoginConfiguration(Configuration configuration, String inboundSpTenant)
            throws EnterpriseLoginManagementException;

    /**
     * Delete configurations of the organization.
     *
     * @param organization    Organization of whose configurations to be deleted.
     * @param inboundSpTenant Tenant the inbound service provider belongs to.
     * @throws EnterpriseLoginManagementException If an error while deleting configurations.
     */
    void deleteEnterpriseLoginConfiguration(String organization, String inboundSpTenant)
            throws EnterpriseLoginManagementException;

    /**
     * Get configurations of the organization.
     *
     * @param organization    Organization.
     * @param inboundSpTenant Tenant the inbound service provider belongs to.
     * @return Configuration  Enterprise login management configuration.
     * @throws EnterpriseLoginManagementException If an error while retrieving configurations.
     */
    Optional<Configuration> getEnterpriseLoginConfiguration(String organization, String inboundSpTenant)
            throws EnterpriseLoginManagementException;

    /**
     * Update configurations of the organization.
     *
     * @param configurationRequest Configurations that to be updated.
     * @param inboundSpTenant      Tenant the inbound service provider belongs to.
     * @throws EnterpriseLoginManagementException If an error while updating configurations.
     */
    void updateEnterpriseLoginConfiguration(Configuration configurationRequest, String inboundSpTenant)
            throws EnterpriseLoginManagementException;

    /**
     * Retrieve organizations that are registered with provided email domain.
     *
     * @param emailDomain     Email domain of the user who is logging in.
     * @param inboundSpTenant Organization of the service
     * @return List<String>     List of organizations registered with provided email domain.
     * @throws EnterpriseLoginManagementException If an error occurred while retrieving organizations registered with
     *                                            provided email domain.
     */
    List<Integer> getOrganizationsForEmailDomain(String emailDomain, String inboundSpTenant)
            throws EnterpriseLoginManagementException;

    /**
     * Resolve identifier of the service provider of a particular organization registered with inbound sp service.
     *
     * @param organization    Organization.
     * @param inboundSpTenant Tenant the inbound service provider belongs to.
     * @param inboundSp       Service provider through which enterprise login is triggered.
     * @return String           Resource identifier of the outbound service provider.
     * @throws EnterpriseLoginManagementException If an error occurred while retrieving organizations registered with
     *                                            provided email domain.
     */
    String resolveOrganizationSpResourceId(String organization, String inboundSp, String inboundSpTenant)
            throws EnterpriseLoginManagementException;

}

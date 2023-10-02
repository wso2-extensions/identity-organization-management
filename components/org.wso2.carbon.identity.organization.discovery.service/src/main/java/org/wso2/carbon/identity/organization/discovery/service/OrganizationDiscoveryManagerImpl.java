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

package org.wso2.carbon.identity.organization.discovery.service;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigClientException;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigException;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.discovery.service.dao.OrganizationDiscoveryDAO;
import org.wso2.carbon.identity.organization.discovery.service.dao.OrganizationDiscoveryDAOImpl;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.discovery.service.model.OrgDiscoveryAttribute;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DISCOVERY_ATTRIBUTE_ALREADY_ADDED_FOR_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DISCOVERY_ATTRIBUTE_TAKEN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_DISABLED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DUPLICATE_DISCOVERY_ATTRIBUTE_TYPES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_DISCOVERY_CONFIGURATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNAUTHORIZED_ORG_FOR_DISCOVERY_ATTRIBUTE_MANAGEMENT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_DISCOVERY_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Implementation of Organization Discovery Manager Interface.
 */
public class OrganizationDiscoveryManagerImpl implements OrganizationDiscoveryManager {

    private static final OrganizationDiscoveryDAO organizationDiscoveryDAO = new OrganizationDiscoveryDAOImpl();
    private static final String EMAIL_DOMAIN = "emailDomain";
    public static final String EMAIL_DOMAIN_DISCOVERY_ENABLE_CONFIG = "emailDomain.enable";

    @Override
    public List<OrgDiscoveryAttribute> addOrganizationDiscoveryAttributes(String organizationId,
                                                                          List<OrgDiscoveryAttribute>
                                                                                  discoveryAttributes)
            throws OrganizationManagementException {

        String rootOrgId = getOrganizationManager().getPrimaryOrganizationId(organizationId);
        validateRootOrganization(rootOrgId, organizationId);

        if (!isEmailDomainDiscoveryConfigurationEnabled(rootOrgId)) {
            throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_DISABLED, getOrganizationId());
        }

        if (organizationDiscoveryDAO.isDiscoveryAttributeAddedToOrganization(organizationId)) {
            throw handleClientException(ERROR_CODE_DISCOVERY_ATTRIBUTE_ALREADY_ADDED_FOR_ORGANIZATION, organizationId);
        }

        validateOrganizationDiscoveryAttributes(false, rootOrgId, null, discoveryAttributes);

        organizationDiscoveryDAO.addOrganizationDiscoveryAttributes(organizationId, discoveryAttributes);
        return discoveryAttributes;
    }

    @Override
    public List<OrgDiscoveryAttribute> getOrganizationDiscoveryAttributes(String organizationId)
            throws OrganizationManagementException {

        String rootOrgId = getOrganizationManager().getPrimaryOrganizationId(organizationId);
        validateRootOrganization(rootOrgId, organizationId);
        if (!isEmailDomainDiscoveryConfigurationEnabled(rootOrgId)) {
            throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_DISABLED, getOrganizationId());
        }
        return organizationDiscoveryDAO.getOrganizationDiscoveryAttributes(organizationId);
    }

    @Override
    public void deleteOrganizationDiscoveryAttributes(String organizationId) throws OrganizationManagementException {

        String rootOrgId = getOrganizationManager().getPrimaryOrganizationId(organizationId);
        validateRootOrganization(rootOrgId, organizationId);
        if (!isEmailDomainDiscoveryConfigurationEnabled(rootOrgId)) {
            throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_DISABLED, getOrganizationId());
        }
        organizationDiscoveryDAO.deleteOrganizationDiscoveryAttributes(organizationId);
    }

    @Override
    public List<OrgDiscoveryAttribute> updateOrganizationDiscoveryAttributes(String organizationId,
                                                                             List<OrgDiscoveryAttribute>
                                                                                     discoveryAttributes)
            throws OrganizationManagementException {

        String rootOrgId = getOrganizationManager().getPrimaryOrganizationId(organizationId);
        validateRootOrganization(rootOrgId, organizationId);
        if (!isEmailDomainDiscoveryConfigurationEnabled(rootOrgId)) {
            throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_DISABLED, getOrganizationId());
        }
        validateOrganizationDiscoveryAttributes(true, rootOrgId, organizationId, discoveryAttributes);
        organizationDiscoveryDAO.updateOrganizationDiscoveryAttributes(organizationId, discoveryAttributes);
        return discoveryAttributes;
    }

    @Override
    public boolean isDiscoveryAttributeAvailable(String type, String value)
            throws OrganizationManagementException {

        return !organizationDiscoveryDAO.isDiscoveryAttributeExistInHierarchy
                (false, getOrganizationId(), null, type,
                        Collections.singletonList(value));
    }

    @Override
    public Map<String, List<OrgDiscoveryAttribute>> getOrganizationsDiscoveryAttributes()
            throws OrganizationManagementException {

        return organizationDiscoveryDAO.getOrganizationsDiscoveryAttributes(getOrganizationId());
    }

    private boolean isEmailDomainDiscoveryConfigurationEnabled(String rootOrgId)
            throws OrganizationManagementClientException, OrganizationManagementServerException {

        try {
            List<ConfigProperty> configProperties = getOrganizationConfigManager().getDiscoveryConfiguration()
                    .getConfigProperties();
            for (ConfigProperty configProperty : configProperties) {
                // Currently only email domain discovery is supported.
                if (EMAIL_DOMAIN_DISCOVERY_ENABLE_CONFIG.equals(configProperty.getKey()) &&
                        StringUtils.equalsIgnoreCase(configProperty.getValue(), "true")) {
                    return true;
                }
            }
            return false;
        } catch (OrganizationConfigException e) {
            if (e instanceof OrganizationConfigClientException) {
                if (StringUtils.equals(e.getErrorCode(),
                        OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST.getCode())) {
                    throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_DISABLED, getOrganizationId());
                }
            }
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_DISCOVERY_CONFIGURATION, e, rootOrgId);
        }
    }

    private void validateRootOrganization(String rootOrgId, String organizationId)
            throws OrganizationManagementClientException {

        // Not having a root organization implies that the organization is not a valid organization.
        if (StringUtils.isBlank(rootOrgId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId);
        }
        if (!StringUtils.equals(getOrganizationId(), rootOrgId)) {
            throw handleClientException(ERROR_CODE_UNAUTHORIZED_ORG_FOR_DISCOVERY_ATTRIBUTE_MANAGEMENT, organizationId);
        }
    }

    private void validateOrganizationDiscoveryAttributes(boolean excludeCurrentOrganization, String rootOrgId,
                                                         String organizationId,
                                                         List<OrgDiscoveryAttribute> discoveryAttributes)
            throws OrganizationManagementException {

        Set<String> uniqueDiscoveryAttributeTypes = new HashSet<>();
        for (OrgDiscoveryAttribute attribute : discoveryAttributes) {
            if (!uniqueDiscoveryAttributeTypes.add(attribute.getType())) {
                throw handleClientException(ERROR_CODE_DUPLICATE_DISCOVERY_ATTRIBUTE_TYPES, attribute.getType());
            }

            // Currently only email domain discovery is supported.
            if (!StringUtils.equals(EMAIL_DOMAIN, attribute.getType())) {
                throw handleClientException(ERROR_CODE_UNSUPPORTED_DISCOVERY_ATTRIBUTE, attribute.getType());
            }

            attribute.setValues(attribute.getValues().stream().distinct().collect(Collectors.toList()));
            boolean discoveryAttributeTaken = organizationDiscoveryDAO.isDiscoveryAttributeExistInHierarchy
                    (excludeCurrentOrganization, rootOrgId, organizationId, attribute.getType(), attribute.getValues());
            if (discoveryAttributeTaken) {
                throw handleClientException(ERROR_CODE_DISCOVERY_ATTRIBUTE_TAKEN, attribute.getType());
            }
        }
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationDiscoveryServiceHolder.getInstance().getOrganizationManager();
    }

    private OrganizationConfigManager getOrganizationConfigManager() {

        return OrganizationDiscoveryServiceHolder.getInstance().getOrganizationConfigManager();
    }
}

/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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
import org.osgi.annotation.bundle.Capability;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.orgdiscovery.OrganizationDiscoveryHandler;
import org.wso2.carbon.identity.application.authentication.framework.model.OrganizationDiscoveryInput;
import org.wso2.carbon.identity.application.authentication.framework.model.OrganizationDiscoveryResult;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigException;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST;
import static org.wso2.carbon.identity.organization.discovery.service.constant.DiscoveryConstants.EMAIL_DOMAIN_DISCOVERY_TYPE;
import static org.wso2.carbon.identity.organization.discovery.service.constant.DiscoveryConstants.ENABLE_CONFIG;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNAUTHORIZED_ORG_ACCESS;

/**
 * Implementation of the Organization Discovery Handler.
 */
@Capability(
        namespace = "osgi.service",
        attribute = {
                "objectClass=org.wso2.carbon.identity.application.authentication.framework.handler.orgdiscovery" +
                        ".OrganizationDiscoveryHandler",
                "service.scope=singleton"
        }
)
public class OrganizationDiscoveryHandlerImpl implements OrganizationDiscoveryHandler {

    @Override
    public OrganizationDiscoveryResult discoverOrganization(OrganizationDiscoveryInput orgDiscoveryInput,
                                                            AuthenticationContext context)
            throws FrameworkException {

        boolean validDiscoveryParams = validateDiscoveryParameters(orgDiscoveryInput);
        if (!validDiscoveryParams) {
            return OrganizationDiscoveryResult.failure(
                    FrameworkConstants.OrgDiscoveryFailureDetails.VALID_DISCOVERY_PARAMETERS_NOT_FOUND.getCode(),
                    FrameworkConstants.OrgDiscoveryFailureDetails.VALID_DISCOVERY_PARAMETERS_NOT_FOUND.getMessage());
        }

        String appId = context.getServiceProviderResourceId();
        String mainAppResideTenantDomain = context.getTenantDomain();
        String mainAppOrgId;
        try {
            mainAppOrgId = OrganizationDiscoveryServiceHolder.getInstance().getOrganizationManager()
                    .resolveOrganizationId(mainAppResideTenantDomain);
        } catch (OrganizationManagementException e) {
            throw new FrameworkException("Error while getting organization ID for tenant domain: "
                    + mainAppResideTenantDomain, e);
        }

        if (StringUtils.isNotBlank(orgDiscoveryInput.getOrgId())) {
            return handleOrgDiscoveryByOrgId(orgDiscoveryInput.getOrgId(), appId, mainAppOrgId);
        } else if (StringUtils.isNotBlank(orgDiscoveryInput.getLoginHint())) {
            String loginHint = orgDiscoveryInput.getLoginHint();
            String orgDiscoveryType = StringUtils.isNotBlank(orgDiscoveryInput.getOrgDiscoveryType()) ?
                    orgDiscoveryInput.getOrgDiscoveryType() : EMAIL_DOMAIN_DISCOVERY_TYPE;
            return handleOrgDiscoveryByLoginHint(loginHint, appId, mainAppOrgId, orgDiscoveryType, context);
        } else {
            //TODO: Need to handle the default this based on the default parameter.
            if (StringUtils.isNotBlank(orgDiscoveryInput.getOrgHandle())) {
                return handleOrgDiscoveryByOrgHandle(orgDiscoveryInput.getOrgHandle(), appId, mainAppOrgId);
            } else if (StringUtils.isNotBlank(orgDiscoveryInput.getOrgName())) {
                return handleOrgDiscoveryByOrgName(orgDiscoveryInput.getOrgName(), appId, mainAppOrgId);
            }
        }
        // If any of the above conditions are not met, it means valid discovery parameters are not found.
        return OrganizationDiscoveryResult.failure(
                FrameworkConstants.OrgDiscoveryFailureDetails.VALID_DISCOVERY_PARAMETERS_NOT_FOUND.getCode(),
                FrameworkConstants.OrgDiscoveryFailureDetails.VALID_DISCOVERY_PARAMETERS_NOT_FOUND.getMessage());
    }

    private OrganizationDiscoveryResult handleOrgDiscoveryByOrgId(String orgId, String appId, String mainAppOrgId)
            throws FrameworkException {

        Optional<Organization> organization = getOrganization(orgId);
        if (!organization.isPresent()) {
            return OrganizationDiscoveryResult.failure(
                    FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getCode(),
                    FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getMessage());
        }
        Optional<String> sharedApplicationId = getSharedApplicationId(appId, mainAppOrgId, orgId);
        if (!sharedApplicationId.isPresent()) {
            return OrganizationDiscoveryResult.failure(
                    FrameworkConstants.OrgDiscoveryFailureDetails.APPLICATION_NOT_SHARED.getCode(),
                    FrameworkConstants.OrgDiscoveryFailureDetails.APPLICATION_NOT_SHARED.getMessage());
        }
        return OrganizationDiscoveryResult.success(organization.get(), sharedApplicationId.get());
    }

    private OrganizationDiscoveryResult handleOrgDiscoveryByOrgHandle(String orgHandle, String appId,
                                                                      String mainAppOrgId)
            throws FrameworkException {

        String orgId;
        try {
            orgId = OrganizationDiscoveryServiceHolder.getInstance().getOrganizationManager()
                    .resolveOrganizationId(orgHandle);
        } catch (OrganizationManagementException e) {
            if (e instanceof OrganizationManagementClientException &&
                    ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                return OrganizationDiscoveryResult.failure(
                        FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getCode(),
                        FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getMessage());
            } else {
                throw new FrameworkException("Error while resolving organization ID for organization handle: "
                        + orgHandle, e);
            }
        }
        return handleOrgDiscoveryByOrgId(orgId, appId, mainAppOrgId);
    }

    private OrganizationDiscoveryResult handleOrgDiscoveryByOrgName(String orgName, String appId,
                                                                    String mainAppOrgId)
            throws FrameworkException {

        String orgId;
        try {
            orgId = OrganizationDiscoveryServiceHolder.getInstance().getOrganizationManager()
                    .getOrganizationIdByName(orgName);
            if (StringUtils.isBlank(orgId)) {
                return OrganizationDiscoveryResult.failure(
                        FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getCode(),
                        FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getMessage());
            }
        } catch (OrganizationManagementException e) {
            throw new FrameworkException("Error while resolving organization ID for organization name: "
                    + orgName, e);
        }
        return handleOrgDiscoveryByOrgId(orgId, appId, mainAppOrgId);
    }

    private OrganizationDiscoveryResult handleOrgDiscoveryByLoginHint(String loginHint, String appId,
                                                                      String mainAppOrgId, String orgDiscoveryType,
                                                                      AuthenticationContext context)
            throws FrameworkException {

        if (!isOrganizationDiscoveryTypeEnabled(orgDiscoveryType)) {
            return OrganizationDiscoveryResult.failure(
                    FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_DISCOVERY_TYPE_NOT_ENABLED_OR_SUPPORTED
                            .getCode(),
                    FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_DISCOVERY_TYPE_NOT_ENABLED_OR_SUPPORTED
                            .getMessage());
        }
        try {
            String orgId = OrganizationDiscoveryServiceHolder.getInstance()
                    .getOrganizationDiscoveryManager()
                    .getOrganizationIdByDiscoveryAttribute(orgDiscoveryType, loginHint, mainAppOrgId, context);
            if (StringUtils.isBlank(orgId)) {
                return OrganizationDiscoveryResult.failure(
                        FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getCode(),
                        FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getMessage());
            }
            Optional<Organization> organization = getOrganization(orgId);
            if (!organization.isPresent()) {
                return OrganizationDiscoveryResult.failure(
                        FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getCode(),
                        FrameworkConstants.OrgDiscoveryFailureDetails.ORGANIZATION_NOT_FOUND.getMessage());
            }
            Optional<String> sharedApplicationId = getSharedApplicationId(appId, mainAppOrgId, orgId);
            if (!sharedApplicationId.isPresent()) {
                return OrganizationDiscoveryResult.failure(
                        FrameworkConstants.OrgDiscoveryFailureDetails.APPLICATION_NOT_SHARED.getCode(),
                        FrameworkConstants.OrgDiscoveryFailureDetails.APPLICATION_NOT_SHARED.getMessage());
            }
            return OrganizationDiscoveryResult.success(organization.get(), sharedApplicationId.get());
        } catch (OrganizationManagementException e) {
            throw new FrameworkException("Error while discovering organization by login hint for application: "
                    + appId + " in organization ID: " + mainAppOrgId, e);
        }
    }

    private boolean validateDiscoveryParameters(OrganizationDiscoveryInput orgDiscoveryInput) {

        return StringUtils.isNotBlank(orgDiscoveryInput.getOrgId()) ||
                StringUtils.isNotBlank(orgDiscoveryInput.getLoginHint()) ||
                StringUtils.isNotBlank(orgDiscoveryInput.getOrgHandle()) ||
                StringUtils.isNotBlank(orgDiscoveryInput.getOrgName());
    }

    private Optional<String> getSharedApplicationId(String appId, String appResideOrgId, String sharedOrgId)
            throws FrameworkException {

        String sharedAppId;
        try {
            sharedAppId = OrganizationDiscoveryServiceHolder.getInstance().getApplicationManagementService()
                    .getSharedAppId(appId, appResideOrgId, sharedOrgId);
        } catch (IdentityApplicationManagementException e) {
            throw new FrameworkException("Error while retrieving shared application id: " + appId
                    + " for organization ID: " + sharedOrgId, e);
        }
        return Optional.ofNullable(sharedAppId);
    }

    private Optional<Organization> getOrganization(String orgId) throws FrameworkException {

        try {
            Organization organization = OrganizationDiscoveryServiceHolder.getInstance()
                    .getOrganizationManager().getOrganization(orgId, false, false);
            return Optional.of(organization);
        } catch (OrganizationManagementException e) {
            if (e instanceof OrganizationManagementClientException &&
                    (ERROR_CODE_INVALID_ORGANIZATION.getCode().equals(e.getErrorCode()) ||
                            ERROR_CODE_UNAUTHORIZED_ORG_ACCESS.getCode().equals(e.getErrorCode()))) {
                return Optional.empty();
            } else {
                throw new FrameworkException("Error while retrieving organization details for organization ID: "
                        + orgId, e);
            }
        }
    }

    private boolean isOrganizationDiscoveryTypeEnabled(String discoveryType) throws FrameworkException {

        try {
            DiscoveryConfig discoveryConfig = OrganizationDiscoveryServiceHolder.getInstance()
                    .getOrganizationConfigManager().getDiscoveryConfiguration();
            Map<String, AttributeBasedOrganizationDiscoveryHandler> discoveryHandlers =
                    OrganizationDiscoveryServiceHolder.getInstance().getAttributeBasedOrganizationDiscoveryHandlers();

            List<ConfigProperty> configProperties = discoveryConfig.getConfigProperties();
            for (ConfigProperty configProperty : configProperties) {
                String type = configProperty.getKey().split(ENABLE_CONFIG)[0];
                if (discoveryType.equals(type) && discoveryHandlers.get(type) != null &&
                        Boolean.parseBoolean(configProperty.getValue())) {
                    return true;
                }
            }
        } catch (OrganizationConfigException e) {
            if (ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST.getCode().equals(e.getErrorCode())) {
                return false;
            }
            throw new FrameworkException("Error while retrieving organization discovery configuration.", e);
        }
        return false;
    }
}

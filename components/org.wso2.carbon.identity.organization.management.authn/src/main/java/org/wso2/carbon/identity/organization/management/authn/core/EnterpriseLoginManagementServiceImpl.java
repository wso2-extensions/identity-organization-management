/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.authn.core;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.cors.mgt.core.exception.CORSManagementServiceException;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.authn.core.common.Constants;
import org.wso2.carbon.identity.organization.management.authn.core.dao.DAOFactory;
import org.wso2.carbon.identity.organization.management.authn.core.dao.EnterpriseLoginMgtDAO;
import org.wso2.carbon.identity.organization.management.authn.core.exceptions.EnterpriseLoginManagementClientException;
import org.wso2.carbon.identity.organization.management.authn.core.exceptions.EnterpriseLoginManagementException;
import org.wso2.carbon.identity.organization.management.authn.core.exceptions.EnterpriseLoginManagementServerException;
import org.wso2.carbon.identity.organization.management.authn.core.internal.EnterpriseLoginManagerDataHolder;
import org.wso2.carbon.identity.organization.management.authn.core.model.Configuration;
import org.wso2.carbon.identity.organization.management.authn.core.model.EmailDomainMapping;
import org.wso2.carbon.identity.organization.management.authn.core.model.ServiceMappingConfiguration;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;

/**
 * Enterprise login management service implementation.
 */
public class EnterpriseLoginManagementServiceImpl implements EnterpriseLoginManagementService {


    private static final Log LOG = LogFactory.getLog(EnterpriseLoginManagementServiceImpl.class);

    private static EnterpriseLoginMgtDAO enterpriseLoginMgtDAO = DAOFactory.getInstance().getEnterpriseLoginMgtDAO();

    @Override
    public void addEnterpriseLoginConfiguration(Configuration configurationRequest, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        validateConfiguration(configurationRequest, inboundSpTenant);

        checkIfConfigsAlreadyExist(configurationRequest, inboundSpTenant);

        addConfiguration(configurationRequest, inboundSpTenant);
    }

    @Override
    public Optional<Configuration> getEnterpriseLoginConfiguration(String organization, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        validateRequestParams(organization, inboundSpTenant);
        int outboundSpTenantId = resolveTenantId(organization);
        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        List<ServiceMappingConfiguration> existingConfigurations = enterpriseLoginMgtDAO.
                getConfiguration(inboundSpTenantId, outboundSpTenantId);
        List<String> emailDomains = enterpriseLoginMgtDAO.getEmailDomains(inboundSpTenantId, outboundSpTenantId);
        List<String> servicesRegistered = new ArrayList<>();

        if (existingConfigurations.isEmpty()) {
            return Optional.empty();
        }
        for (ServiceMappingConfiguration configurationDTO : existingConfigurations) {
            servicesRegistered.add(configurationDTO.getInboundSpResourceId());
        }
        return Optional.of(new Configuration(servicesRegistered, emailDomains));
    }

    @Override
    public void deleteEnterpriseLoginConfiguration(String organization, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        validateRequestParams(organization, inboundSpTenant);
        int outboundSpTenantId = resolveTenantId(organization);
        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        List<ServiceMappingConfiguration> existingConfiguration =
                enterpriseLoginMgtDAO.getConfiguration(inboundSpTenantId, outboundSpTenantId);
        // Keep track of deleted configs  and services to revert in case.
        Map<String, ServiceProvider> deletedOutboundSps = new HashMap<>();
        List<ServiceMappingConfiguration> deletedConfigs = new ArrayList<>();
        if (!existingConfiguration.isEmpty()) {
            try {
                for (ServiceMappingConfiguration configuration : existingConfiguration) {
                    // Confirms that application exists before deleting.
                    ServiceProvider outboundSp =
                            getEnterpriseLoginMgtApplication(configuration.getOutboundSpResourceId(), organization);
                    deleteEnterpriseLoginMgtApplication(outboundSp.getApplicationResourceId(), organization);
                    deletedOutboundSps.put(configuration.getOutboundSpResourceId(), outboundSp);
                    deletedConfigs.add(configuration);
                }
                // Deletes configuration (and emailDomains).
                enterpriseLoginMgtDAO.deleteConfiguration(inboundSpTenantId, outboundSpTenantId);
            } catch (EnterpriseLoginManagementException e) {
                rollbackDelete(organization, deletedOutboundSps, deletedConfigs);
                throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void updateEnterpriseLoginConfiguration(Configuration configurationRequest,
                                                   String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        validateConfiguration(configurationRequest, inboundSpTenant);
        String organization = configurationRequest.getOrganization();
        int outboundSpTenantId = resolveTenantId(organization);
        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        List<ServiceMappingConfiguration> existingConfigurations = enterpriseLoginMgtDAO.
                getConfiguration(inboundSpTenantId, outboundSpTenantId);
        List<String> configuredEmailDomains = enterpriseLoginMgtDAO.getEmailDomains(inboundSpTenantId,
                outboundSpTenantId);
        List<String> servicesRegistered = new ArrayList<>();

        if (CollectionUtils.isEmpty(existingConfigurations)) {
            throw new EnterpriseLoginManagementClientException(
                    Constants.ErrorMessage.ERROR_CODE_NO_CONFIGS.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_NO_CONFIGS.getMessage());
        }

        for (ServiceMappingConfiguration configurationDTO : existingConfigurations) {
            servicesRegistered.add(configurationDTO.getInboundSpResourceId());
        }

        List<String> servicesToAdd = new ArrayList<>();
        for (String serviceInRequest : configurationRequest.getServices()) {
            if (!servicesRegistered.contains(serviceInRequest)) {
                // Need to add new services.
                servicesToAdd.add(serviceInRequest);
            }
        }

        List<String> servicesToRemove = new ArrayList<>();
        for (String existingService : servicesRegistered) {
            if (!configurationRequest.getServices().contains(existingService)) {
                // Need to remove omitted  services.
                servicesToRemove.add(existingService);
            }
        }

        List<String> emailDomainsToAdd = new ArrayList<>();
        for (String emailDomainInRequest : configurationRequest.getEmailDomains()) {
            if (!configuredEmailDomains.contains(emailDomainInRequest)) {
                // Need to add new services.
                emailDomainsToAdd.add(emailDomainInRequest);
            }
        }

        List<String> emailDomainsToRemove = new ArrayList<>();
        for (String existingEmailDomain : configuredEmailDomains) {
            if (!configurationRequest.getEmailDomains().contains(existingEmailDomain)) {
                // Need to remove omitted email domains.
                emailDomainsToRemove.add(existingEmailDomain);
            }
        }

        // Adding new services to be registered.
        if (!CollectionUtils.isEmpty(servicesToAdd)) {
            addServices(servicesToAdd, organization, inboundSpTenant);
        }

        // Adding new email domains.
        if (!CollectionUtils.isEmpty(emailDomainsToAdd)) {
            addEmailDomains(emailDomainsToAdd, organization, inboundSpTenant);
        }

        // Deleting omitted services.
        if (!CollectionUtils.isEmpty(servicesToRemove)) {
            deleteEnterpriseLoginConfiguredServices(inboundSpTenant, organization, servicesToRemove);
        }

        // Deleting omitted services.
        if (!CollectionUtils.isEmpty(emailDomainsToRemove)) {
            deleteEnterpriseLoginConfiguredEmailDomains(inboundSpTenant, organization, emailDomainsToRemove);
        }

    }

    @Override
    public List<Integer> getOrganizationsForEmailDomain(String emailDomain, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        return enterpriseLoginMgtDAO.getOrganizationsForEmailDomain(emailDomain, inboundSpTenantId);
    }

    @Override
    public String resolveOrganizationSpResourceId(String organization, String inboundSp, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        try {
            int inboundSpTenantId = resolveTenantId(inboundSpTenant);
            int outboundSpTenantId = resolveTenantId(organization);
            String inboundSpResourceId = EnterpriseLoginManagerDataHolder.getApplicationManagementService().
                    getServiceProvider(inboundSp, inboundSpTenant).getApplicationResourceId();
            String outboundSpResourceId = enterpriseLoginMgtDAO.getOutboundSpResourceId(inboundSpResourceId,
                    inboundSpTenantId, outboundSpTenantId);
            if (outboundSpResourceId.isEmpty()) {
                throw new EnterpriseLoginManagementClientException(
                        Constants.ErrorMessage.ERROR_CODE_NO_VALID_OUTBOUND_SP.getCode(),
                        Constants.ErrorMessage.ERROR_CODE_NO_VALID_OUTBOUND_SP.getMessage());
            }
            return outboundSpResourceId;
        } catch (IdentityApplicationManagementException e) {
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    /**
     * Validate the request parameters.
     *
     * @param organization    Organization in which the services are registered.
     * @param inboundSpTenant Tenant from which the request initiated.
     * @throws EnterpriseLoginManagementClientException If the validations fails.
     */
    private void validateRequestParams(String organization, String inboundSpTenant)
            throws EnterpriseLoginManagementClientException {

        if (StringUtils.isBlank(organization)) {
            throw new EnterpriseLoginManagementClientException(
                    Constants.ErrorMessage.ERROR_CODE_EMPTY_ORG.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_EMPTY_ORG.getMessage());
        }

        if (StringUtils.isBlank(inboundSpTenant)) {
            throw new EnterpriseLoginManagementClientException(
                    Constants.ErrorMessage.ERROR_CODE_EMPTY_INBOUND_SP_TENANT.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_EMPTY_INBOUND_SP_TENANT.getMessage());
        }
    }

    /**
     * Validate the configuration.
     *
     * @param configurationRequest Configuration with services and emailDomains.
     * @param inboundSpTenant      Tenant from which the request initiated.
     * @throws EnterpriseLoginManagementClientException If validation fails for configuration.
     */
    private void validateConfiguration(Configuration configurationRequest, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        if (StringUtils.isBlank(inboundSpTenant)) {
            throw new EnterpriseLoginManagementClientException(
                    Constants.ErrorMessage.ERROR_CODE_EMPTY_INBOUND_SP_TENANT.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_EMPTY_INBOUND_SP_TENANT.getMessage());
        }

        String organization = configurationRequest.getOrganization();
        if (StringUtils.isBlank(organization)) {
            throw new EnterpriseLoginManagementClientException(Constants.ErrorMessage.ERROR_CODE_EMPTY_ORG.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_EMPTY_ORG.getMessage());
        }

        if (CollectionUtils.isEmpty(configurationRequest.getServices())) {
            throw new EnterpriseLoginManagementClientException(
                    Constants.ErrorMessage.ERROR_CODE_SERVICE_NOT_PRESENT.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_SERVICE_NOT_PRESENT.getMessage());
        }

        if (CollectionUtils.isEmpty(configurationRequest.getEmailDomains())) {
            throw new EnterpriseLoginManagementClientException(
                    Constants.ErrorMessage.ERROR_CODE_EMAIL_NOT_PRESENT.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_EMAIL_NOT_PRESENT.getMessage());
        }

        for (String emailDomainInRequest : configurationRequest.getEmailDomains()) {
            if (StringUtils.isBlank(emailDomainInRequest) || !isValidEmailDomain(emailDomainInRequest)) {
                throw new EnterpriseLoginManagementClientException(
                        Constants.ErrorMessage.ERROR_CODE_INVALID_EMAIL_DOMAIN.getCode(),
                        Constants.ErrorMessage.ERROR_CODE_INVALID_EMAIL_DOMAIN.getMessage());
            }
        }

        for (String serviceInRequest : configurationRequest.getServices()) {
            if (StringUtils.isBlank(serviceInRequest)) {
                throw new EnterpriseLoginManagementClientException(
                        Constants.ErrorMessage.ERROR_CODE_EMPTY_SERVICE.getCode(),
                        Constants.ErrorMessage.ERROR_CODE_EMPTY_SERVICE.getMessage());
            }
            // Check whether services to be added are valid ones.
            checkIfValidService(inboundSpTenant, serviceInRequest);
        }
    }

    /**
     * Check if the email domain to be added is valid.
     *
     * @param emailDomainInRequest Email domain that is to be added.
     */
    private boolean isValidEmailDomain(String emailDomainInRequest) {

        Matcher matcher = (Constants.EMAIL_DOMAIN_REGEX).matcher(emailDomainInRequest);
        return matcher.matches();
    }

    /**
     * Check if the enterprise login configurations exist for the organization.
     *
     * @param configuration   Configuration with services and emailDomains.
     * @param inboundSpTenant Tenant the inbound service provider belongs to.
     * @throws EnterpriseLoginManagementException If an error occurred while validating for conflicting services.
     */
    private void checkIfConfigsAlreadyExist(Configuration configuration, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        String organization = configuration.getOrganization();
        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        int outboundSpTenantId = resolveTenantId(organization);
        List<ServiceMappingConfiguration> configurations = enterpriseLoginMgtDAO.getConfiguration(inboundSpTenantId,
                outboundSpTenantId);
        if (!CollectionUtils.isEmpty(configurations)) {
            // Conflict as the configurations already exist.
            throw new EnterpriseLoginManagementClientException(
                    Constants.ErrorMessage.ERROR_CODE_SERVICE_ALREADY_REGISTERED.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_SERVICE_ALREADY_REGISTERED.getMessage());
        }
    }

    /**
     * Check if the services to be registered are valid.
     *
     * @param serviceInRequest Service requested to be added.
     * @param inboundSpTenant  Tenant from which the request initiated.
     */
    private void checkIfValidService(String inboundSpTenant, String serviceInRequest)
            throws EnterpriseLoginManagementException {

        ApplicationManagementService applicationManagementService =
                EnterpriseLoginManagerDataHolder.getApplicationManagementService();
        try {
            // Check if the services are valid.
            if (applicationManagementService.
                    getApplicationByResourceId(serviceInRequest, inboundSpTenant) == null) {
                throw new EnterpriseLoginManagementClientException(
                        Constants.ErrorMessage.ERROR_CODE_SERVICE_NOT_FOUND.getCode(),
                        Constants.ErrorMessage.ERROR_CODE_SERVICE_NOT_FOUND.getMessage());
            }
        } catch (IdentityApplicationManagementException e) {
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    /**
     * Add configurations.
     *
     * @param configurationRequest Configuration.
     * @param inboundSpTenant      Tenant in which service resides in.
     * @throws EnterpriseLoginManagementException If error occurred while adding configuration.
     */
    private void addConfiguration(Configuration configurationRequest, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        String organization = configurationRequest.getOrganization();
        int outboundSpTenantId = resolveTenantId(organization);
        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        Map<String, String> createdSpResourceIds = new HashMap<>();
        for (String service : configurationRequest.getServices()) {
            // Creating enterprise login application for each service, in the request.
            createdSpResourceIds.put(service, createEnterpriseLoginApplication(organization, service, inboundSpTenant));
        }

        List<ServiceMappingConfiguration> serviceMappingConfigurations =
                getServiceMappingConfigurations(configurationRequest, outboundSpTenantId, createdSpResourceIds,
                        inboundSpTenantId);

        EmailDomainMapping emailDomainMapping = new EmailDomainMapping(configurationRequest.getEmailDomains(),
                outboundSpTenantId, inboundSpTenantId);

        try {
            // Adding enterprise login mgt configuration for the organization after validation.
            enterpriseLoginMgtDAO.addEnterpriseLoginConfiguration(serviceMappingConfigurations, emailDomainMapping);
        } catch (EnterpriseLoginManagementServerException e) {
            // Revert actions of adding application. Deleting all for which SpIds are added.
            for (String createdSpResourceId : createdSpResourceIds.values()) {
                try {
                    deleteEnterpriseLoginMgtApplication(getEnterpriseLoginMgtApplication(createdSpResourceId,
                            organization).getApplicationResourceId(), organization);
                } catch (EnterpriseLoginManagementException rollbackException) {
                    LOG.error("Error occurred while roll back adding enterprise login configurations for " +
                            "organization: " + organization, rollbackException);
                }
            }
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    /**
     * Add new services.
     *
     * @param servicesToAdd   List of services to be added.
     * @param inboundSpTenant Tenant in which service resides in.
     * @throws EnterpriseLoginManagementException If error occurred while adding configuration.
     */
    private void addServices(List<String> servicesToAdd, String organization, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        int outboundSpTenantId = resolveTenantId(organization);
        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        Map<String, String> createdSpResourceIds = new HashMap<>();
        for (String service : servicesToAdd) {
            // Creating enterprise login application for each service, in the request.
            createdSpResourceIds.put(service, createEnterpriseLoginApplication(organization, service, inboundSpTenant));
        }

        List<ServiceMappingConfiguration> serviceMappingConfigurations =
                getServiceMappingConfigurations(servicesToAdd, outboundSpTenantId, createdSpResourceIds,
                        inboundSpTenantId);

        try {
            // Adding enterprise login mgt configuration for the organization after validation.
            enterpriseLoginMgtDAO.addServices(serviceMappingConfigurations);
        } catch (EnterpriseLoginManagementServerException e) {
            // Revert actions of adding application. Deleting all for which SpIds are added.
            for (String createdSpResourceId : createdSpResourceIds.values()) {
                try {
                    deleteEnterpriseLoginMgtApplication(getEnterpriseLoginMgtApplication(createdSpResourceId,
                            organization).getApplicationResourceId(), organization);
                } catch (EnterpriseLoginManagementException rollbackException) {
                    LOG.error("Error occurred while roll back adding enterprise login configurations for " +
                            "organization: " + organization, rollbackException);
                }
            }
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    /**
     * Add new email domains.
     *
     * @param emailDomainsToAdd List of email domains to be added.
     * @param inboundSpTenant   Tenant in which service resides in.
     * @param organization      Organization to which these email domains are added to.
     * @throws EnterpriseLoginManagementException If error occurred while adding configuration.
     */
    private void addEmailDomains(List<String> emailDomainsToAdd, String organization, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        int outboundSpTenantId = resolveTenantId(organization);
        enterpriseLoginMgtDAO.addEmailDomains(emailDomainsToAdd, outboundSpTenantId, inboundSpTenantId);
    }

    /**
     * Creates enterprise login management application for the organization.
     *
     * @param organization    Organization for which the configurations have to be registered.
     * @param service         Service for which the application to be created.
     * @param inboundSpTenant Tenant in which service resides in.
     * @return String         Unique Identifier of the created application.
     * @throws EnterpriseLoginManagementException If error occurred while creating the application.
     */
    private String createEnterpriseLoginApplication(String organization, String service, String inboundSpTenant)
            throws EnterpriseLoginManagementException {

        ApplicationManagementService applicationManagementService =
                EnterpriseLoginManagerDataHolder.getApplicationManagementService();
        int outboundSpTenantId;
        String modifiedServiceName;
        String serviceName;
        try {
            outboundSpTenantId = resolveTenantId(organization);
            // Obtain service name and update. It will be used in application name and description.
            serviceName = applicationManagementService.
                    getApplicationByResourceId(service, inboundSpTenant).getApplicationName();
            modifiedServiceName = serviceName.replace(" ", "_").toUpperCase();
        } catch (IdentityApplicationManagementException e) {
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        }

        ServiceProvider serviceProvider = null;
        try {
            // Creating new tenant flow for the organization.
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(organization, true);
            RealmService realmService = EnterpriseLoginManagerDataHolder.getRealmService();
            String adminUsername = realmService.getTenantUserRealm(outboundSpTenantId).getRealmConfiguration().
                    getAdminUserName();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(adminUsername);

            ServiceURL commonAuthServiceUrl = ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).build();
            String callbackUrl = commonAuthServiceUrl.getAbsolutePublicURL();
            String allowedOrigin = commonAuthServiceUrl.getAbsolutePublicUrlWithoutPath();

            // Prepare consumer oauth application.
            OAuthConsumerAppDTO consumerApp = new OAuthConsumerAppDTO();
            String clientId = UUID.randomUUID().toString();
            consumerApp.setOauthConsumerKey(clientId);
            consumerApp.setOAuthVersion(Constants.OAUTH2_VERSION);
            consumerApp.setGrantTypes(Constants.AUTHORIZATION_CODE_GRANT_TYPE);
            consumerApp.setCallbackUrl(callbackUrl);

            // Create Oauth consumer app.
            OAuthConsumerAppDTO createdOAuthApp = EnterpriseLoginManagerDataHolder.getOAuthAdminService()
                    .registerAndRetrieveOAuthApplicationData(consumerApp);

            // Obtain oauth consumer app configs.
            InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig =
                    createInboundAuthRequestConfig(createdOAuthApp.getOauthConsumerKey());
            List<InboundAuthenticationRequestConfig> inbounds = new ArrayList<>();
            inbounds.add(inboundAuthenticationRequestConfig);
            InboundAuthenticationConfig inboundAuthConfig = new InboundAuthenticationConfig();
            inboundAuthConfig.setInboundAuthenticationRequestConfigs(
                    inbounds.toArray(new InboundAuthenticationRequestConfig[0]));

            // Prepare Service provide.
            serviceProvider = new ServiceProvider();
            serviceProvider.setApplicationName(Constants.ENTERPRISE_LOGIN_MGT_SP_NAME + modifiedServiceName);
            serviceProvider.setDescription(Constants.ENTERPRISE_LOGIN_MGT_SP_DESC + serviceName);
            serviceProvider.setTemplateId(Constants.ENTERPRISE_LOGIN_MGT_SP_TEMPLATE_ID);
            serviceProvider.setInboundAuthenticationConfig(inboundAuthConfig);

            String applicationId = applicationManagementService.createApplication(serviceProvider, organization,
                    adminUsername);

            // Update CORS Origins.
            List<String> corsOrigins = new ArrayList<>();
            corsOrigins.add(allowedOrigin);
            EnterpriseLoginManagerDataHolder.getCorsManagementService()
                    .setCORSOrigins(applicationId, corsOrigins, organization);

            return applicationId;
        } catch (IdentityApplicationManagementException e) {
            LOG.error("Error occurred while creating enterprise login management application for organization: "
                    + organization, e);
            rollbackInbounds(getConfiguredInbounds(serviceProvider));
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        } catch (IdentityOAuthAdminException | UserStoreException | CORSManagementServiceException |
                URLBuilderException e) {
            throw new EnterpriseLoginManagementServerException(e.getMessage(), e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Rollback Delete.
     *
     * @param organization       Organization in which deletion happened.
     * @param deletedOutboundSps Outbound service providers that were deleted.
     * @param configsToRollback  Configurations that has to be deleted.
     */
    private void rollbackDelete(String organization, Map<String, ServiceProvider> deletedOutboundSps,
                                List<ServiceMappingConfiguration> configsToRollback) {

        if (!configsToRollback.isEmpty()) {
            for (ServiceMappingConfiguration configToRollback : configsToRollback) {
                try {
                    String deletedOutboundSpResourceId = configToRollback.getOutboundSpResourceId();
                    ServiceProvider outboundSp = deletedOutboundSps.get(deletedOutboundSpResourceId);
                    String updatedOutboundSpResourceId = createEnterpriseLoginApplicationWithSp(outboundSp,
                            organization);
                    configToRollback.setOutboundSpResourceId(updatedOutboundSpResourceId);
                    enterpriseLoginMgtDAO.updateOutboundSpId(configToRollback);
                } catch (EnterpriseLoginManagementException e) {
                    LOG.error("Error occurred while roll back deleting enterprise login configurations for " +
                            "organization: " + organization, e);
                }
            }
        }
    }

    /**
     * Roll back adding oauth consumer application.
     *
     * @param currentlyAddedInbounds List<InboundAuthenticationRequestConfig>.
     * @throws EnterpriseLoginManagementException If error occurred while rollback.
     */
    private void rollbackInbounds(List<InboundAuthenticationRequestConfig> currentlyAddedInbounds)
            throws EnterpriseLoginManagementException {

        for (InboundAuthenticationRequestConfig inbound : currentlyAddedInbounds) {
            deleteOAuthInbound(inbound);
        }
    }

    /**
     * Retrieve list of added inbound oauth configurations.
     *
     * @param app Oauth consumer application.
     */
    private List<InboundAuthenticationRequestConfig> getConfiguredInbounds(ServiceProvider app) {

        if (app.getInboundAuthenticationConfig() != null &&
                app.getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs() != null) {
            return Arrays.asList(app.getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs());
        }
        return Collections.emptyList();
    }

    /**
     * Delete added oauth consumer application.
     *
     * @param inbound List<InboundAuthenticationRequestConfig>.
     * @throws EnterpriseLoginManagementServerException If error occurred while deleting consumer application.
     */
    private void deleteOAuthInbound(InboundAuthenticationRequestConfig inbound)
            throws EnterpriseLoginManagementServerException {

        try {
            String consumerKey = inbound.getInboundAuthKey();
            EnterpriseLoginManagerDataHolder.getOAuthAdminService().removeOAuthApplicationData(consumerKey);
        } catch (IdentityOAuthAdminException e) {
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), "Error while trying to rollback " +
                    "OAuth2/OpenIDConnect configuration.", e);
        }
    }

    /**
     * Creates enterprise login management application for the organization with existing sp.
     *
     * @param organization Organization for which the configurations have to be registered.
     * @return String      Identifier of the created application.
     * @throws EnterpriseLoginManagementException If error occurred while creating the application.
     */
    private String createEnterpriseLoginApplicationWithSp(ServiceProvider serviceProvider, String organization)
            throws EnterpriseLoginManagementException {

        ApplicationManagementService applicationManagementService =
                EnterpriseLoginManagerDataHolder.getApplicationManagementService();

        // Check if the organization is valid and obtain the tenant Identifier.
        int outboundSpTenantId = IdentityTenantUtil.getTenantId(organization);

        try {
            // Creating new tenant flow for the organization.
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(organization, true);
            RealmService realmService = EnterpriseLoginManagerDataHolder.getRealmService();
            String adminUsername = realmService.getTenantUserRealm(outboundSpTenantId).getRealmConfiguration().
                    getAdminUserName();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(adminUsername);

            // Create service provider.
            return applicationManagementService.createApplication(serviceProvider, organization,
                    adminUsername);
        } catch (IdentityApplicationManagementException e) {
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        } catch (UserStoreException e) {
            throw new EnterpriseLoginManagementServerException(e.getMessage(), e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Create inbound authentication request configuration.
     *
     * @param clientId                              Client id of the oauth consumer app created.
     * @return InboundAuthenticationRequestConfig   Inbound authentication request configuration.
     */
    private InboundAuthenticationRequestConfig createInboundAuthRequestConfig(String clientId) {

        InboundAuthenticationRequestConfig oidcInbound = new InboundAuthenticationRequestConfig();
        oidcInbound.setInboundAuthType("oauth2");
        oidcInbound.setInboundAuthKey(clientId);
        return oidcInbound;
    }

    /**
     * Validate and return valid outbound tenant id for the organization.
     *
     * @param organization Organization/Tenant for which the configurations have to be registered.
     * @return int         Tenant id of the organization.
     * @throws EnterpriseLoginManagementException If an error occurred while retrieving
     */
    private int resolveTenantId(String organization) throws EnterpriseLoginManagementException {

        try {
            // Check if the organization is valid and obtain the tenant Identifier.
            return IdentityTenantUtil.getTenantId(organization);
        } catch (IdentityRuntimeException e) {
            throw new EnterpriseLoginManagementClientException(Constants.ErrorMessage.ERROR_CODE_INVALID_ORG.getCode(),
                    Constants.ErrorMessage.ERROR_CODE_INVALID_ORG.getMessage(), e);
        }
    }

    /**
     * Return enterprise login mgt application registered with .
     *
     * @param outboundSpResourceId Identifier of the enterprise login mgt application.
     * @param organization         Organization in which the application is registered.
     * @return ServiceProvider     Enterprise login mgt application.
     */
    private ServiceProvider getEnterpriseLoginMgtApplication(String outboundSpResourceId, String organization)
            throws EnterpriseLoginManagementException {

        try {
            ApplicationManagementService applicationManagementService =
                    EnterpriseLoginManagerDataHolder.getApplicationManagementService();

            ServiceProvider serviceProvider = applicationManagementService.getApplicationByResourceId(
                    outboundSpResourceId, organization);

            if (serviceProvider == null) {
                throw new EnterpriseLoginManagementClientException(
                        Constants.ErrorMessage.ERROR_CODE_ENTERPRISE_APP_NOT_EXIST.getCode(),
                        Constants.ErrorMessage.ERROR_CODE_ENTERPRISE_APP_NOT_EXIST.getMessage());
            }
            return serviceProvider;
        } catch (IdentityApplicationManagementException e) {
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    /**
     * Delete enterprise login mgt application registered with .
     *
     * @param resourceId   Identifier of the enterprise login mgt application.
     * @param organization Organization to which the application is added to.
     * @throws EnterpriseLoginManagementException If an error occurred while deleting enterprise login mgt app.
     */
    private void deleteEnterpriseLoginMgtApplication(String resourceId, String organization)
            throws EnterpriseLoginManagementException {

        ApplicationManagementService applicationManagementService =
                EnterpriseLoginManagerDataHolder.getApplicationManagementService();

        // Check if the organization is valid and obtain the tenant Identifier.
        int outboundSpTenantId = IdentityTenantUtil.getTenantId(organization);

        try {
            // Creating new tenant flow for the organization.
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(organization, true);
            RealmService realmService = EnterpriseLoginManagerDataHolder.getRealmService();
            String adminUsername = realmService.getTenantUserRealm(outboundSpTenantId).getRealmConfiguration().
                    getAdminUserName();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(adminUsername);
            applicationManagementService.deleteApplicationByResourceId(resourceId, organization, adminUsername);

        } catch (IdentityApplicationManagementException e) {
            throw new EnterpriseLoginManagementException(e.getErrorCode(), e.getMessage(), e);
        } catch (UserStoreException e) {
            throw new EnterpriseLoginManagementException(e.getMessage(), e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Returns enterprise login mgt configuration DTO.
     *
     * @param configuration      Configuration with services and email domains.
     * @param outboundSpTenantId Tenant identifier of the org to which configurations to be added.
     * @param outboundSpIds      Identifier of the enterprise login mgt application created for each service.
     * @param inboundSpTenantId  Tenant identifier of the service provider that requests the configs to be added.
     * @return List<ServiceMappingConfiguration> List of service mapping configurations.
     */
    private List<ServiceMappingConfiguration> getServiceMappingConfigurations(Configuration configuration,
                                                                              int outboundSpTenantId,
                                                                              Map<String, String> outboundSpIds,
                                                                              int inboundSpTenantId) {

        List<ServiceMappingConfiguration> serviceMappingConfigurations = new ArrayList<>();
        String inboundSpResourceId;
        String outboundSpResourceId;
        for (String serviceInRequest : configuration.getServices()) {
            inboundSpResourceId = serviceInRequest;
            outboundSpResourceId = outboundSpIds.get(serviceInRequest);
            serviceMappingConfigurations.add(new ServiceMappingConfiguration(inboundSpResourceId, inboundSpTenantId,
                    outboundSpResourceId, outboundSpTenantId));
        }
        return serviceMappingConfigurations;
    }

    /**
     * Returns enterprise login mgt service mappings.
     *
     * @param services           List of services to be added.
     * @param outboundSpTenantId Tenant identifier of the org to which configurations to be added.
     * @param outboundSpIds      Identifier of the enterprise login mgt application created for each service.
     * @param inboundSpTenantId  Tenant identifier of the service provider that requests the configs to be added.
     * @return List<ServiceMappingConfiguration> List of service mapping configurations.
     */
    private List<ServiceMappingConfiguration> getServiceMappingConfigurations(List<String> services,
                                                                              int outboundSpTenantId,
                                                                              Map<String, String> outboundSpIds,
                                                                              int inboundSpTenantId) {

        List<ServiceMappingConfiguration> serviceMappingConfigurations = new ArrayList<>();
        String inboundSpResourceId;
        String outboundSpResourceId;
        for (String serviceInRequest : services) {
            inboundSpResourceId = serviceInRequest;
            outboundSpResourceId = outboundSpIds.get(serviceInRequest);
            serviceMappingConfigurations.add(new ServiceMappingConfiguration(inboundSpResourceId, inboundSpTenantId,
                    outboundSpResourceId, outboundSpTenantId));
        }
        return serviceMappingConfigurations;
    }

    /**
     * Delete configurations of the organization.
     *
     * @param inboundSpTenant  Tenant which registers the service for the organization.
     * @param organization     Organization of whose configurations to be deleted.
     * @param servicesToRemove List of services to be removed.
     * @throws EnterpriseLoginManagementException If an error while deleting configurations.
     */
    private void deleteEnterpriseLoginConfiguredServices(String inboundSpTenant, String organization,
                                                         List<String> servicesToRemove)
            throws EnterpriseLoginManagementException {

        int outboundSpTenantId = resolveTenantId(organization);
        int inboundSpTenantId = resolveTenantId(inboundSpTenant);
        List<ServiceMappingConfiguration> existingConfiguration =
                enterpriseLoginMgtDAO.getConfiguration(inboundSpTenantId, outboundSpTenantId);
        // Keep track of deleted configs  and services to revert in case.
        Map<String, ServiceProvider> deletedOutboundSps = new HashMap<>();
        List<ServiceMappingConfiguration> configsDeleted = new ArrayList<>();

        try {
            for (String serviceToRemove : servicesToRemove) {
                ServiceMappingConfiguration serviceMappingConfiguration = null;
                for (ServiceMappingConfiguration existingConfigMapping : existingConfiguration) {
                    if (existingConfigMapping.getInboundSpResourceId().equals(serviceToRemove)) {
                        // Obtained configuration mapping for the service.
                        serviceMappingConfiguration = existingConfigMapping;
                    }
                }

                if (serviceMappingConfiguration != null) {
                    ServiceProvider outboundSp =
                            getEnterpriseLoginMgtApplication(serviceMappingConfiguration.getOutboundSpResourceId(),
                                    organization);
                    deleteEnterpriseLoginMgtApplication(outboundSp.getApplicationResourceId(), organization);
                    deletedOutboundSps.put(serviceMappingConfiguration.getOutboundSpResourceId(), outboundSp);
                    configsDeleted.add(serviceMappingConfiguration);
                }
            }

            for (ServiceMappingConfiguration configDeleted : configsDeleted) {
                enterpriseLoginMgtDAO.deleteServiceSpecificConfiguration(configDeleted.getInboundSpTenantId(),
                        configDeleted.getOutboundSpTenantId(), configDeleted.getInboundSpResourceId());
            }
        } catch (EnterpriseLoginManagementException e) {
            rollbackDelete(organization, deletedOutboundSps, configsDeleted);
            throw new EnterpriseLoginManagementServerException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    /**
     * Delete specific email domains of the organization.
     *
     * @param inboundSpTenant      Tenant which has the service registered for the organization.
     * @param organization         Organization of whose configurations to be deleted.
     * @param emailDomainsToRemove List of email domains to remove.
     * @throws EnterpriseLoginManagementException If an error while deleting configurations.
     */
    private void deleteEnterpriseLoginConfiguredEmailDomains(String inboundSpTenant, String organization,
                                                             List<String> emailDomainsToRemove)
            throws EnterpriseLoginManagementException {

        int outboundSpTenantId = IdentityTenantUtil.getTenantId(organization);
        int inboundSpTenantId = IdentityTenantUtil.getTenantId(inboundSpTenant);
        for (String emailDomain : emailDomainsToRemove) {
            // Deletes email domain.
            enterpriseLoginMgtDAO.deleteSpecificEmailDomain(inboundSpTenantId, outboundSpTenantId, emailDomain);
        }
    }
}

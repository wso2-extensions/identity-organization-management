/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import java.util.Map;

import static org.wso2.carbon.identity.organization.management.service.util.Utils.getUserStoreManager;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isSubOrganization;

/**
 * Event handler for waiting until user stores are initialized after organization creation.
 * This handler ensures that user stores (like DEFAULT and AGENT) are fully initialized
 * before the organization creation API returns, preventing intermittent failures during
 * user creation and role assignment operations.
 */
public class UserStoreInitializationHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(UserStoreInitializationHandler.class);
    private static final String ENABLED_CONFIG_KEY = "OrganizationUserStoreInitialization.Enable";
    private static final String USER_STORES_CONFIG_KEY = "OrganizationUserStoreInitialization.UserStores";
    private static final String WAIT_TIME_CONFIG_KEY = "OrganizationUserStoreInitialization.WaitTime";
    private static final String WAIT_INTERVAL_CONFIG_KEY = "OrganizationUserStoreInitialization.WaitInterval";
    private static final int DEFAULT_WAIT_TIME_MS = 120000; // 60 seconds.
    private static final int DEFAULT_WAIT_INTERVAL_MS = 500; // 500 milliseconds.
    private static final String DEFAULT_USER_STORES = "DEFAULT";

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
            Map<String, Object> eventProperties = event.getEventProperties();
            Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            
            int organizationDepthInHierarchy;
            try {
                organizationDepthInHierarchy =
                        getOrganizationManager().getOrganizationDepthInHierarchy(organization.getId());
            } catch (OrganizationManagementServerException e) {
                throw new IdentityEventException(
                        String.format("An error occurred while getting depth of the organization with id: %s",
                                organization.getId()), e);
            }
            
            // Only process for sub-organizations.
            if (isSubOrganization(organizationDepthInHierarchy)) {
                waitForUserStoreInitialization(organization);
            }
        }
    }

    /**
     * Wait for configured user stores to be initialized.
     *
     * @param organization The organization that was created.
     * @throws IdentityEventException If an error occurs while waiting for user stores.
     */
    private void waitForUserStoreInitialization(Organization organization) throws IdentityEventException {

        // Check if the handler is enabled.
        String enabledConfig = IdentityUtil.getProperty(ENABLED_CONFIG_KEY);
        if (StringUtils.isNotEmpty(enabledConfig) && !Boolean.parseBoolean(enabledConfig)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("UserStoreInitializationHandler is disabled. Skipping user store initialization wait.");
            }
            return;
        }

        // Get the list of user stores to wait for.
        String userStoresConfig = IdentityUtil.getProperty(USER_STORES_CONFIG_KEY);
        if (StringUtils.isEmpty(userStoresConfig)) {
            userStoresConfig = DEFAULT_USER_STORES;
        }
        String[] userStoresToWaitFor = userStoresConfig.split(",");

        // Get wait time and interval configuration.
        int waitTime = getConfigValue(WAIT_TIME_CONFIG_KEY, DEFAULT_WAIT_TIME_MS);
        int waitInterval = getConfigValue(WAIT_INTERVAL_CONFIG_KEY, DEFAULT_WAIT_INTERVAL_MS);

        try {
            // Start tenant flow with the created sub-organization's tenant context.
            PrivilegedCarbonContext.startTenantFlow();

            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            String tenantDomain = getOrganizationManager().resolveTenantDomain(organization.getId());
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            carbonContext.setTenantId(tenantId);
            carbonContext.setTenantDomain(tenantDomain);

            AbstractUserStoreManager userStoreManager = getUserStoreManager(tenantId);

            for (String userStoreName : userStoresToWaitFor) {
                String trimmedUserStoreName = userStoreName.trim();
                if (StringUtils.isEmpty(trimmedUserStoreName)) {
                    continue;
                }
                // Wait for each user store sequentially. This ensures each user store is fully initialized
                // before proceeding to the next one. Total wait time = sum of individual wait times.
                waitForSpecificUserStore(userStoreManager, trimmedUserStoreName, waitTime, waitInterval, 
                        organization.getId());
            }

        } catch (UserStoreException e) {
            throw new IdentityEventException(
                    String.format("Error while waiting for user store initialization for organization: %s",
                            organization.getId()), e);
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException(
                String.format("Error while resolving tenant domain for organization: %s",
                    organization.getId()), e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Wait for a specific user store to be initialized.
     *
     * @param userStoreManager The user store manager.
     * @param userStoreName The name of the user store to wait for.
     * @param maxWaitTime Maximum time to wait in milliseconds.
     * @param waitInterval Wait interval between checks in milliseconds.
     * @param organizationId Organization ID for logging.
     * @throws IdentityEventException If the user store is not initialized within the wait time.
     */
    private void waitForSpecificUserStore(AbstractUserStoreManager userStoreManager, String userStoreName,
                                          int maxWaitTime, int waitInterval, String organizationId)
            throws IdentityEventException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Waiting for user store '%s' to be initialized for organization: %s",
                    userStoreName, organizationId));
        }

        UserStoreManager targetUserStore = null;
        long startTime = System.currentTimeMillis();
        
        try {
            while (targetUserStore == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= maxWaitTime) {
                    break;
                }
                
                targetUserStore = userStoreManager.getSecondaryUserStoreManager(userStoreName);
                if (targetUserStore != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("User store '%s' initialized successfully for organization: %s " +
                                "(waited: %d ms)", userStoreName, organizationId, elapsed));
                    }
                    break;
                }
                Thread.sleep(waitInterval);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdentityEventException(
                    String.format("Thread interrupted while waiting for user store '%s' initialization " +
                            "for organization: %s", userStoreName, organizationId), e);
        }

        if (targetUserStore == null) {
            String errorMessage = String.format(
                    "User store '%s' was not initialized within the configured wait time (%d ms) " +
                    "for organization: %s", userStoreName, maxWaitTime, organizationId);
            LOG.error(errorMessage);
            throw new IdentityEventException(errorMessage);
        }
    }

    /**
     * Get integer configuration value.
     *
     * @param configKey Configuration key.
     * @param defaultValue Default value if configuration is not found or invalid.
     * @return Configuration value.
     */
    private int getConfigValue(String configKey, int defaultValue) {

        String configValue = IdentityUtil.getProperty(configKey);
        if (StringUtils.isNotEmpty(configValue)) {
            try {
                return Integer.parseInt(configValue);
            } catch (NumberFormatException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Invalid value for configuration key '%s': %s. Using default value: %d",
                            configKey, configValue, defaultValue), e);
                }
            }
        }
        return defaultValue;
    }

    /**
     * Get organization manager instance.
     *
     * @return OrganizationManager instance.
     */
    private OrganizationManager getOrganizationManager() {

        return OrganizationManagementHandlerDataHolder.getInstance().getOrganizationManager();
    }
}

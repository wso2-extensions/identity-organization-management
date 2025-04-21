/*
 * Copyright (c) 2023-2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.discovery.service.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.model.UserIdentityClaim;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigException;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;
import org.wso2.carbon.identity.organization.discovery.service.AttributeBasedOrganizationDiscoveryHandler;
import org.wso2.carbon.identity.organization.discovery.service.OrganizationDiscoveryManager;
import org.wso2.carbon.identity.organization.discovery.service.OrganizationDiscoveryManagerImpl;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.discovery.service.model.OrgDiscoveryAttribute;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST;
import static org.wso2.carbon.identity.organization.discovery.service.constant.DiscoveryConstants.ENABLE_CONFIG;
import static org.wso2.carbon.identity.organization.discovery.service.constant.DiscoveryConstants.PRE_ADD_USER_EMAIL_DOMAIN_VALIDATE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_EMAIL_DOMAIN_ASSOCIATED_WITH_DIFFERENT_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_EMAIL_DOMAIN_NOT_MAPPED_TO_ORGANIZATION;

/**
 * This is to perform organization discovery related validations upon user operations.
 */
public class OrganizationDiscoveryUserOperationListener extends AbstractIdentityUserOperationEventListener {

    private static final Log LOG = LogFactory.getLog(OrganizationDiscoveryUserOperationListener.class);
    private final OrganizationDiscoveryManager organizationDiscoveryManager = new OrganizationDiscoveryManagerImpl();

    @Override
    public int getExecutionOrderId() {

        int orderId = getOrderId();
        if (orderId != IdentityCoreConstants.EVENT_LISTENER_ORDER_ID) {
            return orderId;
        }
        return 114;
    }

    @Override
    public boolean doPreAddUserWithID(String userName, Object credential, String[] roleList, Map<String, String> claims,
                                      String profile, UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable()) {
            return true;
        }

        try {
            String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                return true;
            }

            if (isSharedUser(claims)) {
                return true;
            }

            String organizationId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
            if (StringUtils.isBlank(organizationId)) {
                organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            }
            String primaryOrganizationId = getOrganizationManager().getPrimaryOrganizationId(organizationId);
            int tenantId = IdentityTenantUtil.getTenantId(getOrganizationManager()
                    .resolveTenantDomain(primaryOrganizationId));
            DiscoveryConfig discoveryConfig = getOrganizationConfigManager()
                    .getDiscoveryConfigurationByTenantId(tenantId);
            List<ConfigProperty> configProperties = discoveryConfig.getConfigProperties();
            Map<String, AttributeBasedOrganizationDiscoveryHandler> discoveryHandlers =
                    organizationDiscoveryManager.getAttributeBasedOrganizationDiscoveryHandlers();
            for (ConfigProperty configProperty : configProperties) {
                String type = configProperty.getKey().split(ENABLE_CONFIG)[0];
                AttributeBasedOrganizationDiscoveryHandler handler = discoveryHandlers.get(type);
                if (handler == null || !Boolean.parseBoolean(configProperty.getValue())) {
                    return true;
                }

                // Currently only email domain based organization discovery is supported.
                if (!handler.requiredEventValidations().contains(PRE_ADD_USER_EMAIL_DOMAIN_VALIDATE)) {
                    return true;
                }
                return isValidEmailDomainForPreAddUser(userName, organizationId, primaryOrganizationId, handler);
            }
        } catch (OrganizationManagementException e) {
            LOG.error("Error while creating user", e);
            return false;
        } catch (OrganizationConfigException e) {
            if (ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST.getCode().equals(e.getErrorCode())) {
                return true;
            }
            LOG.error("Error while creating user", e);
            return false;
        }
        return true;
    }

    private boolean isValidEmailDomainForPreAddUser(String userName, String organizationId,
                                                    String primaryOrganizationId,
                                                    AttributeBasedOrganizationDiscoveryHandler handler)
            throws UserStoreException, OrganizationManagementException {

        // Username should be in the email address format.
        String emailDomain = handler.extractAttributeValue(userName);
        List<OrgDiscoveryAttribute> organizationDiscoveryAttributes = organizationDiscoveryManager
                .getOrganizationDiscoveryAttributes(organizationId, false);

        // If the organization doesn't have any email domains mapped, then we need to check if the
        // email domain in the username is not mapped to any other organization .
        if (organizationDiscoveryAttributes.isEmpty()) {
            boolean domainAvailable = organizationDiscoveryManager.isDiscoveryAttributeValueAvailable
                    (primaryOrganizationId, handler.getType(), emailDomain);
            if (domainAvailable) {
                return true;
            }
            throw new UserStoreException(
                    ERROR_CODE_EMAIL_DOMAIN_ASSOCIATED_WITH_DIFFERENT_ORGANIZATION.getDescription(),
                    ERROR_CODE_EMAIL_DOMAIN_ASSOCIATED_WITH_DIFFERENT_ORGANIZATION.getCode());
        }
        for (OrgDiscoveryAttribute attribute : organizationDiscoveryAttributes) {
            List<String> organizationMappedEmailDomains = attribute.getValues();
            if (organizationMappedEmailDomains != null && organizationMappedEmailDomains.contains(emailDomain)) {
                return true;
            }
            throw new UserStoreException(
                    ERROR_CODE_EMAIL_DOMAIN_NOT_MAPPED_TO_ORGANIZATION.getDescription(),
                    ERROR_CODE_EMAIL_DOMAIN_NOT_MAPPED_TO_ORGANIZATION.getCode());
        }
        return true;
    }

    private OrganizationConfigManager getOrganizationConfigManager() {

        return OrganizationDiscoveryServiceHolder.getInstance().getOrganizationConfigManager();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationDiscoveryServiceHolder.getInstance().getOrganizationManager();
    }

    /**
     * Check whether the user is a shared user or not.
     *
     * @return true if the user is a shared user, false otherwise.
     */
    private boolean isSharedUser(Map<String, String> claims) {

        if (claims != null && StringUtils.isNotBlank(claims.get(CLAIM_MANAGED_ORGANIZATION))) {
            return true;
        }

        Map<String, Object> threadLocalProperties = IdentityUtil.threadLocalProperties.get();
        if (threadLocalProperties == null) {
            return false;
        }
        Object claimProp = threadLocalProperties.get(IdentityMgtConstants.USER_IDENTITY_CLAIMS);
        if (!(claimProp instanceof UserIdentityClaim)) {
            return false;
        }
        UserIdentityClaim userIdentityClaims = (UserIdentityClaim) claimProp;
        String orgClaim = userIdentityClaims.getUserIdentityDataMap().get(CLAIM_MANAGED_ORGANIZATION);
        return StringUtils.isNotBlank(orgClaim);
    }
}

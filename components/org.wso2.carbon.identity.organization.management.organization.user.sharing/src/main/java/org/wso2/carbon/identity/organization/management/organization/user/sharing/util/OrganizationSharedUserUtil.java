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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.listener.SharedUserProfileUpdateGovernanceEventListener;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;

import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;

/**
 * Utility class for organization shared user management.
 */
public class OrganizationSharedUserUtil {

    private static final Log LOG = LogFactory.getLog(OrganizationSharedUserUtil.class);

    public static String getUserManagedOrganizationClaim(AbstractUserStoreManager userStoreManager, String userId)
            throws UserStoreException {

        Map<String, String> claimsMap;
        try {
            claimsMap = userStoreManager
                    .getUserClaimValuesWithID(userId, new String[]{CLAIM_MANAGED_ORGANIZATION}, null);
        } catch (UserStoreException e) {
            if (LOG.isDebugEnabled()) {
                String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                LOG.debug("ManagedOrg claim is not available in the tenant domain: " + tenantDomain);
            }
            return null;
        }
        return claimsMap.get(CLAIM_MANAGED_ORGANIZATION);
    }


    /**
     * Get the user ID of the associated user by the organization ID.
     */
    public static Optional<String> getUserIdOfAssociatedUserByOrgId(String associatedUserId, String orgId)
            throws OrganizationManagementException {

        UserAssociation userAssociation = OrganizationUserSharingDataHolder.getInstance()
                .getOrganizationUserSharingService()
                .getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId);
        if (userAssociation == null) {
            return Optional.empty();
        }
        return Optional.of(userAssociation.getUserId());
    }

    /**
     * Check whether the shared user profile resolver is enabled.
     *
     * @return True if the shared user profile resolver is enabled.
     */
    public static boolean isSharedUserProfileResolverEnabled() {

        IdentityEventListenerConfig identityEventListenerConfig = IdentityUtil.readEventListenerProperty(
                UserOperationEventListener.class.getName(),
                SharedUserProfileUpdateGovernanceEventListener.class.getName());
        if (identityEventListenerConfig == null) {
            return true;
        }
        return StringUtils.isBlank(identityEventListenerConfig.getEnable()) ||
                Boolean.parseBoolean(identityEventListenerConfig.getEnable());
    }
}

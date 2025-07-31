/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.listener.AbstractIdentityProviderMgtListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Identity provider management listener to handle role Ids of inherited password expiry rules.
 */
public class ResidentIdPMgtListener extends AbstractIdentityProviderMgtListener {

    private static final Log LOG = LogFactory.getLog(ResidentIdPMgtListener.class);
    private static final String PASSWORD_EXPIRY_RULES_KEY_PREFIX = "passwordExpiry.rule";
    public static final int ROLE_ID_INDEX = 4;
    public static final int EXPECTED_RULE_TOKEN_COUNT = 5;
    public static final int RULE_TYPE_INDEX = 2;

    /**
     * Returns the priority of this listener.
     *
     * @return The order of this listener.
     */
    @Override
    public int getDefaultOrderId() {

        return 300;
    }

    /**
     * This method is called after the resident identity provider is retrieved from the database.
     *
     * @param identityProvider Resident identity provider to be processed.
     * @param tenantDomain     Tenant domain of the organization.
     * @return true if the operation is successful, false otherwise.
     * @throws IdentityProviderManagementException If an error occurs while resolving shared roles.
     */
    @Override
    public boolean doPostGetResidentIdP(IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {

        resolveSharedRoles(identityProvider, tenantDomain);
        return true;
    }

    /**
     * Resolves shared roles for password expiry rules by converting main role IDs to shared role IDs
     * for organization contexts. Removes rules where the main role doesn't have a corresponding shared role and the
     * main role does not exist in the sub-organization.
     *
     * @param identityProvider Identity provider containing password expiry rules.
     * @param tenantDomain     Tenant domain of the organization.
     * @throws IdentityProviderManagementException If an error occurs while resolving shared roles.
     */
    private void resolveSharedRoles(IdentityProvider identityProvider, String tenantDomain)
            throws IdentityProviderManagementException {

        List<IdentityProviderProperty> propertyList = new ArrayList<>(
                Arrays.asList(identityProvider.getIdpProperties()));
        List<IdentityProviderProperty> passwordExpiryRules = propertyList.stream()
                .filter(property -> property.getName().startsWith(PASSWORD_EXPIRY_RULES_KEY_PREFIX))
                .collect(Collectors.toList());

        // Return if no password expiry rules exist.
        if (passwordExpiryRules.isEmpty()) {
            return;
        }

        List<String> mainRoleIds = extractMainRoleIds(passwordExpiryRules);
        if (mainRoleIds.isEmpty()) {
            return;
        }

        try {
            Map<String, String> mainRoleToSharedRoleMap = OrganizationManagementHandlerDataHolder.getInstance()
                    .getRoleManagementServiceV2()
                    .getMainRoleToSharedRoleMappingsBySubOrg(mainRoleIds, tenantDomain);

            propertyList.removeIf(property -> {
                if (!property.getName().startsWith(PASSWORD_EXPIRY_RULES_KEY_PREFIX)) {
                    // Keep non-password expiry properties.
                    return false;
                }

                String[] ruleTokens = property.getValue().split(",");
                if (ruleTokens.length != EXPECTED_RULE_TOKEN_COUNT || !"roles".equals(ruleTokens[RULE_TYPE_INDEX])) {
                    // Keep non-role rules or malformed rules.
                    return false;
                }

                String mainRoleId = ruleTokens[ROLE_ID_INDEX];
                String sharedRoleId = mainRoleToSharedRoleMap.get(mainRoleId);
                if (StringUtils.isNotEmpty(sharedRoleId)) {
                    // Update the rule with the shared role ID.
                    ruleTokens[ROLE_ID_INDEX] = sharedRoleId;
                    property.setValue(String.join(",", ruleTokens));
                    // Keep the updated rule.
                    return false;
                } else {
                    try {
                        if (!OrganizationManagementHandlerDataHolder.getInstance().getRoleManagementServiceV2()
                                .isExistingRole(mainRoleId, tenantDomain)) {
                            return true;
                        }
                    } catch (IdentityRoleManagementException e) {
                        LOG.error("Error while checking if the main role ID: " + mainRoleId + " exists in tenant: "
                                + tenantDomain, e);
                        return false;
                    }
                    return false;
                }
            });
        } catch (IdentityRoleManagementException e) {
            throw new IdentityProviderManagementException(
                    "Error while retrieving shared role mappings for main role IDs: " + mainRoleIds, e);
        }

        identityProvider.setIdpProperties(propertyList.toArray(new IdentityProviderProperty[0]));
    }

    private static List<String> extractMainRoleIds(List<IdentityProviderProperty> passwordExpiryRules) {

        // Extract main role IDs from password expiry rules related to roles.
        return passwordExpiryRules.stream()
                .map(property -> property.getValue().split(","))
                .filter(ruleTokens -> ruleTokens.length == EXPECTED_RULE_TOKEN_COUNT &&
                        "roles".equals(ruleTokens[RULE_TYPE_INDEX]))
                .map(ruleTokens -> ruleTokens[ROLE_ID_INDEX])
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }
}

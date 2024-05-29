/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.application.util;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ORGANIZATION_LOGIN_AUTHENTICATOR;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ORGANIZATION_SSO_IDP_IMAGE_URL;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.IS_APP_SHARED;
import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.IS_SYSTEM_RESERVED_IDP_DISPLAY_NAME;
import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.IS_SYSTEM_RESERVED_IDP_FLAG;

/**
 * This class provides utility functions for the Organization Application Management.
 */
public class OrgApplicationManagerUtil {

    private static final ThreadLocal<List<String>> b2bApplicationIds = new ThreadLocal<>();

    /**
     * Get a new Jdbc Template.
     *
     * @return a new Jdbc Template.
     */
    public static NamedJdbcTemplate getNewTemplate() {

        return new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
    }

    /**
     * Set property value to service provider indicating if it should be shared with all child organizations.
     *
     * @param serviceProvider The main application.
     * @param value           Property value.
     */
    public static void setShareWithAllChildrenProperty(ServiceProvider serviceProvider, boolean value) {

        Optional<ServiceProviderProperty> shareWithAllChildren = Arrays.stream(serviceProvider.getSpProperties())
                .filter(p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()))
                .findFirst();
        if (shareWithAllChildren.isPresent()) {
            shareWithAllChildren.get().setValue(Boolean.toString(value));
        } else {
            ServiceProviderProperty[] spProperties = serviceProvider.getSpProperties();
            ServiceProviderProperty[] newSpProperties = new ServiceProviderProperty[spProperties.length + 1];
            System.arraycopy(spProperties, 0, newSpProperties, 0, spProperties.length);

            ServiceProviderProperty shareWithAllChildrenProperty = new ServiceProviderProperty();
            shareWithAllChildrenProperty.setName(SHARE_WITH_ALL_CHILDREN);
            shareWithAllChildrenProperty.setValue(Boolean.TRUE.toString());
            newSpProperties[spProperties.length] = shareWithAllChildrenProperty;

            serviceProvider.setSpProperties(newSpProperties);
        }
    }

    /**
     * Set property value to service provider indicating if the app is shared with any child organizations.
     *
     * @param serviceProvider The main application.
     * @param value           The property value.
     */
    public static void setIsAppSharedProperty(ServiceProvider serviceProvider, boolean value) {

        Optional<ServiceProviderProperty> appShared = Arrays.stream(serviceProvider.getSpProperties())
                .filter(p -> IS_APP_SHARED.equals(p.getName()))
                .findFirst();
        if (appShared.isPresent()) {
            appShared.get().setValue(Boolean.toString(value));
            return;
        }
        ServiceProviderProperty[] spProperties = serviceProvider.getSpProperties();
        ServiceProviderProperty[] newSpProperties = new ServiceProviderProperty[spProperties.length + 1];
        System.arraycopy(spProperties, 0, newSpProperties, 0, spProperties.length);

        ServiceProviderProperty isAppSharedProperty = new ServiceProviderProperty();
        isAppSharedProperty.setName(IS_APP_SHARED);
        isAppSharedProperty.setValue(Boolean.toString(value));
        newSpProperties[spProperties.length] = isAppSharedProperty;

        serviceProvider.setSpProperties(newSpProperties);
    }

    /**
     * Check whether the application is a system application.
     *
     * @param applicationName The name of the application
     * @return True if the provided application is a system application.
     */
    public static boolean isSystemApplication(String applicationName) {

        Set<String> systemApplications = OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService()
                .getSystemApplications();
        return systemApplications != null && systemApplications.stream().anyMatch(applicationName::equalsIgnoreCase);
    }

    /**
     * Retrieve the B2B application IDs.
     *
     * @return B2B application IDs.
     */
    public static List<String> getB2BApplicationIds() {

        return b2bApplicationIds.get();
    }

    /**
     * Sets the thread local value to persist the B2B application IDs.
     *
     * @param b2BApplicationIds The email verification state to be skipped.
     */
    public static void setB2BApplicationIds(List<String> b2BApplicationIds) {

        OrgApplicationManagerUtil.b2bApplicationIds.set(b2BApplicationIds);
    }

    /**
     * Clear the thread local used to persist the B2B application IDs.
     */
    public static void clearB2BApplicationIds() {

        b2bApplicationIds.remove();
    }

    /**
     * Create a new identity provider for the organization SSO.
     *
     * @return The created identity provider.
     */
    public static IdentityProvider createOrganizationSSOIDP() {

        FederatedAuthenticatorConfig authConfig = new FederatedAuthenticatorConfig();
        authConfig.setName(ORGANIZATION_LOGIN_AUTHENTICATOR);
        authConfig.setDisplayName(ORGANIZATION_LOGIN_AUTHENTICATOR);
        authConfig.setEnabled(true);

        IdentityProvider idp = new IdentityProvider();
        idp.setIdentityProviderName(FrameworkConstants.ORGANIZATION_LOGIN_IDP_NAME);
        idp.setImageUrl(ORGANIZATION_SSO_IDP_IMAGE_URL);
        idp.setPrimary(false);
        idp.setFederationHub(false);
        idp.setIdentityProviderDescription("Identity provider for Organization SSO.");
        idp.setHomeRealmId(FrameworkConstants.ORGANIZATION_LOGIN_HOME_REALM_IDENTIFIER);
        idp.setDefaultAuthenticatorConfig(authConfig);
        idp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authConfig});
        ClaimConfig claimConfig = new ClaimConfig();
        claimConfig.setLocalClaimDialect(true);
        idp.setClaimConfig(claimConfig);

        // Add system reserved properties.
        IdentityProviderProperty[] idpProperties = new IdentityProviderProperty[1];
        IdentityProviderProperty property = new IdentityProviderProperty();
        property.setName(IS_SYSTEM_RESERVED_IDP_FLAG);
        property.setDisplayName(IS_SYSTEM_RESERVED_IDP_DISPLAY_NAME);
        property.setValue("true");
        idpProperties[0] = property;
        idp.setIdpProperties(idpProperties);

        return idp;
    }

    /**
     * Get the default authentication configuration of service provider.
     *
     * @return The default authentication configuration.
     * @throws OrganizationManagementServerException Exception thrown when retrieving default service provider.
     */
    public static LocalAndOutboundAuthenticationConfig getDefaultAuthenticationConfig() throws
            OrganizationManagementServerException {

        ServiceProvider defaultSP = getDefaultServiceProvider();
        return defaultSP != null ? defaultSP.getLocalAndOutBoundAuthenticationConfig() : null;
    }

    private static ServiceProvider getDefaultServiceProvider() throws OrganizationManagementServerException {

        try {
            return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService()
                    .getServiceProvider(IdentityApplicationConstants.DEFAULT_SP_CONFIG,
                            MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        } catch (IdentityApplicationManagementException e) {
            throw new OrganizationManagementServerException("Error while retrieving default service provider", null, e);
        }
    }
}

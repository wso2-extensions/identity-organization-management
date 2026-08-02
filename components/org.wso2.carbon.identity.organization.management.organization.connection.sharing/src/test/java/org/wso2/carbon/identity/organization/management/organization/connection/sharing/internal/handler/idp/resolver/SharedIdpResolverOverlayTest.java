/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler.idp.resolver;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.application.common.model.JustInTimeProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.component.ConnectionSharingDataHolder;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SharedIdpResolver}'s per-resource overlay (federated authenticators, provisioning
 * connectors) and the JIT provisioning user store fallback, which resolve each attribute against the parent via the
 * registered (default) resolvers.
 */
public class SharedIdpResolverOverlayTest {

    private MockedStatic<ConnectionSharingDataHolder> mockedDataHolder;
    private MockedStatic<IdentityUtil> mockedIdentityUtil;
    private SharedIdpResolver resolver;

    @BeforeMethod
    public void setUp() {

        mockedDataHolder = mockStatic(ConnectionSharingDataHolder.class);
        ConnectionSharingDataHolder dataHolder = mock(ConnectionSharingDataHolder.class);
        mockedDataHolder.when(ConnectionSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getSharedFederatedAuthenticatorResolver(anyString()))
                .thenReturn(new DefaultSharedFederatedAuthenticatorResolver());
        when(dataHolder.getSharedProvisioningConnectorResolver(anyString()))
                .thenReturn(new DefaultSharedProvisioningConnectorResolver());

        mockedIdentityUtil = mockStatic(IdentityUtil.class);
        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(anyString())).thenReturn("CUSTOM_STORE");

        resolver = SharedIdpResolver.getInstance();
    }

    @AfterMethod
    public void tearDown() {

        mockedDataHolder.close();
        mockedIdentityUtil.close();
    }

    @Test
    public void testOverlayResolvesFederatedAuthenticatorsFromParent() {

        IdentityProvider parent = idp("connection");
        FederatedAuthenticatorConfig authenticator = new FederatedAuthenticatorConfig();
        authenticator.setName("SAMLSSOAuthenticator");
        authenticator.setEnabled(true);
        parent.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authenticator});
        IdentityProvider shadow = idp("connection");

        resolver.overlayParentConfiguration(parent, shadow);

        Assert.assertNotNull(shadow.getFederatedAuthenticatorConfigs());
        Assert.assertEquals(shadow.getFederatedAuthenticatorConfigs().length, 1);
        Assert.assertEquals(shadow.getFederatedAuthenticatorConfigs()[0].getName(), "SAMLSSOAuthenticator");
    }

    @Test
    public void testOverlayResolvesProvisioningConnectorsFromParent() {

        IdentityProvider parent = idp("connection");
        ProvisioningConnectorConfig connector = new ProvisioningConnectorConfig();
        connector.setName("scim");
        connector.setEnabled(true);
        parent.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{connector});
        IdentityProvider shadow = idp("connection");

        resolver.overlayParentConfiguration(parent, shadow);

        Assert.assertNotNull(shadow.getProvisioningConnectorConfigs());
        Assert.assertEquals(shadow.getProvisioningConnectorConfigs().length, 1);
        Assert.assertEquals(shadow.getProvisioningConnectorConfigs()[0].getName(), "scim");
    }

    @Test
    public void testOverlayBasicResolvesFederatedAuthenticators() {

        IdentityProvider parent = idp("connection");
        FederatedAuthenticatorConfig authenticator = new FederatedAuthenticatorConfig();
        authenticator.setName("OIDCAuthenticator");
        parent.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authenticator});
        IdentityProvider shadow = idp("connection");

        resolver.overlayBasicParentAttributes(parent, shadow);

        Assert.assertEquals(shadow.getFederatedAuthenticatorConfigs()[0].getName(), "OIDCAuthenticator");
    }

    @Test
    public void testInheritedJitProvisioningUserStoreFallsBackToConfiguredDefault() {

        IdentityProvider parent = idp("connection");
        JustInTimeProvisioningConfig parentJit = new JustInTimeProvisioningConfig();
        parentJit.setProvisioningEnabled(true);
        parentJit.setProvisioningUserStore("PARENT_STORE");
        parent.setJustInTimeProvisioningConfig(parentJit);
        // The shadow does not override the JIT configuration, so it is inherited from the parent.
        IdentityProvider shadow = idp("connection");

        resolver.overlayParentConfiguration(parent, shadow);

        Assert.assertEquals(shadow.getJustInTimeProvisioningConfig().getProvisioningUserStore(), "CUSTOM_STORE");
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreUpdateRejectsAddingAuthenticatorNotInParent() throws Exception {

        IdentityProvider parent = idp("connection");
        IdentityProvider existing = sharedShadow();
        IdentityProvider updating = sharedShadow();
        FederatedAuthenticatorConfig newAuthenticator = new FederatedAuthenticatorConfig();
        newAuthenticator.setName("SAMLSSOAuthenticator");
        updating.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{newAuthenticator});

        resolver.doPreUpdateValidations(updating, existing, parent);
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreUpdateRejectsInheritedAuthenticatorAttributeChange() throws Exception {

        FederatedAuthenticatorConfig parentAuthenticator = new FederatedAuthenticatorConfig();
        parentAuthenticator.setName("SAMLSSOAuthenticator");
        parentAuthenticator.setEnabled(true);
        IdentityProvider parent = idp("connection");
        parent.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{parentAuthenticator});

        IdentityProvider existing = sharedShadow();
        FederatedAuthenticatorConfig existingAuthenticator = new FederatedAuthenticatorConfig();
        existingAuthenticator.setName("SAMLSSOAuthenticator");
        existingAuthenticator.setEnabled(true);
        existing.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{existingAuthenticator});

        IdentityProvider updating = sharedShadow();
        FederatedAuthenticatorConfig updatingAuthenticator = new FederatedAuthenticatorConfig();
        updatingAuthenticator.setName("SAMLSSOAuthenticator");
        // "enabled" is an inherited attribute; a sub-organization may not toggle it on the shadow.
        updatingAuthenticator.setEnabled(false);
        updating.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{updatingAuthenticator});

        resolver.doPreUpdateValidations(updating, existing, parent);
    }

    @Test
    public void testDoPreUpdateValidationsPassesForIdenticalShadowMatchingParent() throws Exception {

        // When the incoming update leaves every inherited authenticator/connector identical to the stored shadow (and
        // present in the parent), the full validation chain runs without raising a restriction error.
        IdentityProvider parent = configuredIdp(false);
        IdentityProvider existing = configuredIdp(true);
        IdentityProvider updating = configuredIdp(true);

        resolver.doPreUpdateValidations(updating, existing, parent);
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreUpdateRejectsConnectorNotInParent() throws Exception {

        IdentityProvider parent = idp("connection");
        IdentityProvider existing = sharedShadow();
        IdentityProvider updating = sharedShadow();
        ProvisioningConnectorConfig connector = new ProvisioningConnectorConfig();
        connector.setName("scim");
        updating.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{connector});

        resolver.doPreUpdateValidations(updating, existing, parent);
    }

    @Test
    public void testOverlayResolvesDefaultAuthenticatorAndConnectorFromParent() {

        IdentityProvider parent = idp("connection");
        FederatedAuthenticatorConfig authenticator = new FederatedAuthenticatorConfig();
        authenticator.setName("SAMLSSOAuthenticator");
        authenticator.setEnabled(true);
        parent.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authenticator});
        parent.setDefaultAuthenticatorConfig(authenticator);
        ProvisioningConnectorConfig connector = new ProvisioningConnectorConfig();
        connector.setName("scim");
        connector.setEnabled(true);
        parent.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{connector});
        parent.setDefaultProvisioningConnectorConfig(connector);
        IdentityProvider shadow = idp("connection");

        resolver.overlayParentConfiguration(parent, shadow);

        Assert.assertNotNull(shadow.getDefaultAuthenticatorConfig());
        Assert.assertEquals(shadow.getDefaultAuthenticatorConfig().getName(), "SAMLSSOAuthenticator");
        Assert.assertNotNull(shadow.getDefaultProvisioningConnectorConfig());
        Assert.assertEquals(shadow.getDefaultProvisioningConnectorConfig().getName(), "scim");
    }

    private IdentityProvider configuredIdp(boolean shared) {

        IdentityProvider identityProvider = shared ? sharedShadow() : idp("connection");
        FederatedAuthenticatorConfig authenticator = new FederatedAuthenticatorConfig();
        authenticator.setName("SAMLSSOAuthenticator");
        authenticator.setEnabled(true);
        identityProvider.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authenticator});
        identityProvider.setDefaultAuthenticatorConfig(authenticator);
        ProvisioningConnectorConfig connector = new ProvisioningConnectorConfig();
        connector.setName("scim");
        connector.setEnabled(true);
        identityProvider.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{connector});
        identityProvider.setDefaultProvisioningConnectorConfig(connector);
        return identityProvider;
    }

    private IdentityProvider idp(String name) {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName(name);
        identityProvider.setEnable(true);
        return identityProvider;
    }

    private IdentityProvider sharedShadow() {

        IdentityProvider identityProvider = idp("connection");
        IdentityProviderProperty isShared = new IdentityProviderProperty();
        isShared.setName(IdPManagementConstants.IS_SHARED_IDP_PROPERTY);
        isShared.setValue(Boolean.TRUE.toString());
        identityProvider.setIdpProperties(new IdentityProviderProperty[]{isShared});
        return identityProvider;
    }
}

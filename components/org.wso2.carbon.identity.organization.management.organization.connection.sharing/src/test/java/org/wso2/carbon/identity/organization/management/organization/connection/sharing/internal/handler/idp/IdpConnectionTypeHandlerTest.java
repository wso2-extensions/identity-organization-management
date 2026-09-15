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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler.idp;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdPGroup;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.exception.ConnectionSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.component.ConnectionSharingDataHolder;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;

import java.nio.file.Paths;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.constant.ConnectionSharingConstants.DISABLED_AUTHENTICATORS_PROPERTY;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.constant.ConnectionSharingConstants.ENABLE_IDP_SHARING_PROPERTY;

/**
 * Unit tests for {@link IdpConnectionTypeHandler#validateConnectionShareEligibility(String, String)} — the guard
 * that rejects connections which must not be shared: when identity-provider sharing is disabled, when the connection
 * is itself a shared (shadow) connection, when it is a trusted token issuer, or when it carries a share-disabled
 * federated authenticator.
 */
public class IdpConnectionTypeHandlerTest {

    private static final String CONNECTION_ID = "idp-resource-id";
    private static final String INITIATING_ORG_ID = "initiating-org-id";
    private static final String TENANT_DOMAIN = "carbon.super";
    private static final String DISABLED_AUTHENTICATOR = "GoogleOIDCAuthenticator";

    private MockedStatic<IdentityUtil> mockedIdentityUtil;
    private MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil;
    private MockedStatic<ConnectionSharingDataHolder> mockedDataHolder;
    private MockedStatic<PrivilegedCarbonContext> mockedCarbonContext;
    private IdpManager idpManager;

    private IdpConnectionTypeHandler handler;

    @BeforeClass
    public void setUpClass() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH,
                Paths.get(carbonHome, "repository", "conf").toString());
    }

    @BeforeMethod
    public void setUp() {

        mockedIdentityUtil = mockStatic(IdentityUtil.class);
        mockedIdentityTenantUtil = mockStatic(IdentityTenantUtil.class);
        mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(-1234);
        mockedDataHolder = mockStatic(ConnectionSharingDataHolder.class);
        mockedCarbonContext = mockStatic(PrivilegedCarbonContext.class);

        idpManager = mock(IdpManager.class);
        ConnectionSharingDataHolder dataHolder = mock(ConnectionSharingDataHolder.class);
        mockedDataHolder.when(ConnectionSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getIdpManager()).thenReturn(idpManager);

        PrivilegedCarbonContext carbonContext = mock(PrivilegedCarbonContext.class);
        when(carbonContext.getTenantDomain()).thenReturn(TENANT_DOMAIN);
        mockedCarbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);

        // Defaults: sharing enabled (property unset) and no share-disabled authenticators.
        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(ENABLE_IDP_SHARING_PROPERTY)).thenReturn(null);
        mockedIdentityUtil.when(() -> IdentityUtil.getPropertyAsList(DISABLED_AUTHENTICATORS_PROPERTY))
                .thenReturn(Collections.emptyList());

        handler = new IdpConnectionTypeHandler();
    }

    @AfterMethod
    public void tearDown() {

        mockedIdentityUtil.close();
        mockedIdentityTenantUtil.close();
        mockedDataHolder.close();
        mockedCarbonContext.close();
    }

    @Test
    public void testIsSharingEnabledWhenConfigurationIsUnset() {

        // Identity provider sharing is enabled unless the configuration is explicitly disabled.
        Assert.assertTrue(handler.isSharingEnabled());
    }

    @Test
    public void testIsSharingEnabledWhenConfigurationIsDisabled() {

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(ENABLE_IDP_SHARING_PROPERTY)).thenReturn("false");

        Assert.assertFalse(handler.isSharingEnabled());
    }

    @Test
    public void testIsSharingEnabledWhenConfigurationIsEnabled() {

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(ENABLE_IDP_SHARING_PROPERTY)).thenReturn("true");

        Assert.assertTrue(handler.isSharingEnabled());
    }

    @Test
    public void testValidatePassesForShareableConnection() throws Exception {

        stubResolvedIdp(shareableIdp());
        // No exception expected for a plain, shareable identity provider.
        handler.validateConnectionShareEligibility(CONNECTION_ID, INITIATING_ORG_ID);
    }

    @Test(expectedExceptions = ConnectionSharingMgtClientException.class)
    public void testValidateThrowsWhenIdpSharingDisabled() throws Exception {

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(ENABLE_IDP_SHARING_PROPERTY)).thenReturn("false");
        handler.validateConnectionShareEligibility(CONNECTION_ID, INITIATING_ORG_ID);
    }

    @Test(expectedExceptions = ConnectionSharingMgtClientException.class)
    public void testValidateThrowsForSharedConnection() throws Exception {

        IdentityProvider idp = shareableIdp();
        IdentityProviderProperty isShared = new IdentityProviderProperty();
        isShared.setName(IdPManagementConstants.IS_SHARED_IDP_PROPERTY);
        isShared.setValue(Boolean.TRUE.toString());
        idp.setIdpProperties(new IdentityProviderProperty[]{isShared});
        stubResolvedIdp(idp);

        handler.validateConnectionShareEligibility(CONNECTION_ID, INITIATING_ORG_ID);
    }

    @Test(expectedExceptions = ConnectionSharingMgtClientException.class)
    public void testValidateThrowsForTrustedTokenIssuer() throws Exception {

        IdentityProvider idp = shareableIdp();
        idp.setTrustedTokenIssuer(true);
        stubResolvedIdp(idp);

        handler.validateConnectionShareEligibility(CONNECTION_ID, INITIATING_ORG_ID);
    }

    @Test(expectedExceptions = ConnectionSharingMgtClientException.class)
    public void testValidateThrowsForShareDisabledAuthenticator() throws Exception {

        mockedIdentityUtil.when(() -> IdentityUtil.getPropertyAsList(DISABLED_AUTHENTICATORS_PROPERTY))
                .thenReturn(Collections.singletonList(DISABLED_AUTHENTICATOR));

        IdentityProvider idp = shareableIdp();
        FederatedAuthenticatorConfig authenticator = new FederatedAuthenticatorConfig();
        authenticator.setName(DISABLED_AUTHENTICATOR);
        idp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authenticator});
        stubResolvedIdp(idp);

        handler.validateConnectionShareEligibility(CONNECTION_ID, INITIATING_ORG_ID);
    }

    @Test
    public void testCreateSharedResourceBuildsAndPersistsShadow() throws Exception {

        IdentityProvider parent = shareableIdp();
        ProvisioningConnectorConfig connector = new ProvisioningConnectorConfig();
        connector.setName("scim");
        parent.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{connector});
        parent.setDefaultProvisioningConnectorConfig(connector);
        parent.setDefaultAuthenticatorConfig(parent.getFederatedAuthenticatorConfigs()[0]);
        IdPGroup group = new IdPGroup();
        group.setIdpGroupName("group-1");
        parent.setIdPGroupConfig(new IdPGroup[]{group});

        when(idpManager.getIdPByResourceId(CONNECTION_ID, "resident-tenant", false)).thenReturn(parent);
        IdentityProvider added = new IdentityProvider();
        added.setResourceId("shadow-resource-id");
        when(idpManager.addIdPWithResourceId(any(), eq("target-tenant"))).thenReturn(added);

        String result = handler.createSharedResource(CONNECTION_ID, INITIATING_ORG_ID, "resident-tenant",
                "target-tenant");

        Assert.assertEquals(result, "shadow-resource-id");
    }

    @Test(expectedExceptions = ConnectionSharingMgtException.class)
    public void testCreateSharedResourceThrowsWhenParentNotFound() throws Exception {

        when(idpManager.getIdPByResourceId(CONNECTION_ID, "resident-tenant", false)).thenReturn(null);

        handler.createSharedResource(CONNECTION_ID, INITIATING_ORG_ID, "resident-tenant", "target-tenant");
    }

    @Test
    public void testDeleteSharedResource() throws Exception {

        handler.deleteSharedResource("shadow-resource-id", "target-tenant");

        verify(idpManager).forceDeleteIdpByResourceId("shadow-resource-id", "target-tenant");
    }

    private void stubResolvedIdp(IdentityProvider identityProvider) throws Exception {

        when(idpManager.getIdPByResourceId(anyString(), anyString(), anyBoolean())).thenReturn(identityProvider);
    }

    private IdentityProvider shareableIdp() {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setResourceId(CONNECTION_ID);
        identityProvider.setIdentityProviderName("google-connection");
        FederatedAuthenticatorConfig authenticator = new FederatedAuthenticatorConfig();
        authenticator.setName("SAMLSSOAuthenticator");
        identityProvider.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authenticator});
        return identityProvider;
    }
}

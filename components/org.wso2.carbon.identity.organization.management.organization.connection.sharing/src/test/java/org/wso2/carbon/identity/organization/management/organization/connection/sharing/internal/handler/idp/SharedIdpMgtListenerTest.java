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
import org.wso2.carbon.identity.application.common.model.UserDefinedFederatedAuthenticatorConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.ConnectionAssociationManager;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.component.ConnectionSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.util.ConnectionSharingUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.idp.mgt.model.SharedIdPResolveType;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.constant.ConnectionSharingConstants.ENABLE_IDP_SHARING_PROPERTY;

/**
 * Unit tests for {@link SharedIdpMgtListener}: the guard rails that prevent direct creation / deletion of a shared
 * (shadow) identity provider and block deletion of a parent connection with live shadows, the shadow-update
 * deny-guard's short-circuit branches, and the defensive clone that preserves the concrete authenticator subtype.
 */
public class SharedIdpMgtListenerTest {

    private static final String TENANT_DOMAIN = "carbon.super";
    private static final String RESOURCE_ID = "shared-idp-resource-id";
    private static final String RESIDENT_ORG_ID = "resident-org-id";

    private MockedStatic<ConnectionSharingDataHolder> mockedDataHolder;
    private ConnectionAssociationManager associationManager;
    private OrganizationManager organizationManager;
    private IdpManager idpManager;
    private SharedIdpMgtListener listener;

    @BeforeClass
    public void setUpClass() {

        // Set CARBON_HOME before PrivilegedCarbonContext is loaded so its static initialization succeeds (required
        // for the shadow-sync tests that mock the carbon context).
        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH,
                Paths.get(carbonHome, "repository", "conf").toString());
    }

    @BeforeMethod
    public void setUp() {

        mockedDataHolder = mockStatic(ConnectionSharingDataHolder.class);
        associationManager = mock(ConnectionAssociationManager.class);
        organizationManager = mock(OrganizationManager.class);
        idpManager = mock(IdpManager.class);
        ConnectionSharingDataHolder dataHolder = mock(ConnectionSharingDataHolder.class);
        mockedDataHolder.when(ConnectionSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getConnectionAssociationManager()).thenReturn(associationManager);
        when(dataHolder.getOrganizationManager()).thenReturn(organizationManager);
        when(dataHolder.getIdpManager()).thenReturn(idpManager);

        // A direct executor runs the shadow-sync asynchronous body inline, keeping it on the test thread where the
        // static mocks apply.
        listener = new SharedIdpMgtListener(Runnable::run);
    }

    @AfterMethod
    public void tearDown() {

        mockedDataHolder.close();
        // Clear the thread-local flow / change markers so they cannot leak across tests.
        ConnectionSharingUtil.endConnectionShareFlow();
        ConnectionSharingUtil.endConnectionUnshareFlow();
        ConnectionSharingUtil.endSharedConnectionSyncFlow();
        ConnectionSharingUtil.consumeConnectionNameUpdated();
        ConnectionSharingUtil.consumeConnectionGroupsUpdated();
        ConnectionSharingUtil.consumeConnectionAuthenticatorsUpdated();
        ConnectionSharingUtil.consumeConnectionProvisioningConnectorsUpdated();
    }

    @Test
    public void testGetDefaultOrderId() {

        Assert.assertEquals(listener.getDefaultOrderId(), 301);
    }

    // ----- isEnable -----

    @Test
    public void testIsEnableWhenIdpSharingConfigurationIsUnset() throws Exception {

        try (MockedStatic<IdentityUtil> mockedIdentityUtil = mockStatic(IdentityUtil.class)) {
            // Neither the listener configuration nor the identity provider sharing configuration is set.
            mockedIdentityUtil.when(() -> IdentityUtil.readEventListenerProperty(anyString(), anyString()))
                    .thenReturn(null);
            mockedIdentityUtil.when(() -> IdentityUtil.getProperty(ENABLE_IDP_SHARING_PROPERTY)).thenReturn(null);

            Assert.assertTrue(listener.isEnable());
        }
    }

    @Test
    public void testIsEnableFalseWhenIdpSharingDisabled() throws Exception {

        try (MockedStatic<IdentityUtil> mockedIdentityUtil = mockStatic(IdentityUtil.class)) {
            mockedIdentityUtil.when(() -> IdentityUtil.readEventListenerProperty(anyString(), anyString()))
                    .thenReturn(null);
            mockedIdentityUtil.when(() -> IdentityUtil.getProperty(ENABLE_IDP_SHARING_PROPERTY)).thenReturn("false");

            Assert.assertFalse(listener.isEnable());
        }
    }

    // ----- doPreAddIdP -----

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreAddIdpRejectsSharedConnectionOutsideShareFlow() throws Exception {

        listener.doPreAddIdP(sharedIdp(), TENANT_DOMAIN);
    }

    @Test
    public void testDoPreAddIdpAllowsSharedConnectionWithinShareFlow() throws Exception {

        ConnectionSharingUtil.startConnectionShareFlow();
        try {
            Assert.assertTrue(listener.doPreAddIdP(sharedIdp(), TENANT_DOMAIN));
        } finally {
            ConnectionSharingUtil.endConnectionShareFlow();
        }
    }

    @Test
    public void testDoPreAddIdpAllowsNonSharedConnection() throws Exception {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName("regular-connection");
        Assert.assertTrue(listener.doPreAddIdP(identityProvider, TENANT_DOMAIN));
    }

    // ----- doPreUpdateIdPByResourceId -----

    @Test
    public void testDoPreUpdateAllowsNullUpdatingIdp() throws Exception {

        Assert.assertTrue(listener.doPreUpdateIdPByResourceId(RESOURCE_ID, null, TENANT_DOMAIN));
    }

    @Test
    public void testDoPreUpdateAllowsSharedConnectionSyncFlow() throws Exception {

        // An internal sync propagation bypasses the deny-guard.
        ConnectionSharingUtil.startSharedConnectionSyncFlow();
        try {
            Assert.assertTrue(listener.doPreUpdateIdPByResourceId(RESOURCE_ID, new IdentityProvider(), TENANT_DOMAIN));
        } finally {
            ConnectionSharingUtil.endSharedConnectionSyncFlow();
        }
    }

    @Test
    public void testDoPreUpdateAllowsWhenExistingIdpNotFound() throws Exception {

        when(idpManager.getIdPByResourceId(anyString(), anyString(), anyBoolean(), any())).thenReturn(null);
        Assert.assertTrue(listener.doPreUpdateIdPByResourceId(RESOURCE_ID, new IdentityProvider(), TENANT_DOMAIN));
    }

    // ----- doPreDeleteIdPByResourceId -----

    @Test
    public void testDoPreDeleteAllowsWithinUnshareFlow() throws Exception {

        ConnectionSharingUtil.startConnectionUnshareFlow();
        try {
            Assert.assertTrue(listener.doPreDeleteIdPByResourceId(RESOURCE_ID, TENANT_DOMAIN));
        } finally {
            ConnectionSharingUtil.endConnectionUnshareFlow();
        }
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreDeleteRejectsDirectShadowDeletion() throws Exception {

        // The resource is a shadow (it has a connection association), so it cannot be deleted directly.
        ConnectionAssociation association = association();
        when(associationManager.getConnectionAssociationBySharedConnectionId(anyString(), anyString()))
                .thenReturn(Optional.of(association));

        listener.doPreDeleteIdPByResourceId(RESOURCE_ID, TENANT_DOMAIN);
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreDeleteRejectsParentIdpWithSharedConnections() throws Exception {

        when(associationManager.getConnectionAssociationBySharedConnectionId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(organizationManager.resolveOrganizationId(TENANT_DOMAIN)).thenReturn(RESIDENT_ORG_ID);
        // The parent connection still has shared connections; it must be unshared first.
        ConnectionAssociation association = association();
        when(associationManager.getConnectionAssociations(anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(association));

        listener.doPreDeleteIdPByResourceId(RESOURCE_ID, TENANT_DOMAIN);
    }

    @Test
    public void testDoPreDeleteAllowsCleanParentIdp() throws Exception {

        when(associationManager.getConnectionAssociationBySharedConnectionId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(organizationManager.resolveOrganizationId(TENANT_DOMAIN)).thenReturn(RESIDENT_ORG_ID);
        when(associationManager.getConnectionAssociations(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        Assert.assertTrue(listener.doPreDeleteIdPByResourceId(RESOURCE_ID, TENANT_DOMAIN));
    }

    // ----- cloneIdentityProvider -----

    @Test
    public void testCloneIdentityProviderPreservesUserDefinedAuthenticatorSubtype() throws Exception {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName("custom-connection");
        UserDefinedFederatedAuthenticatorConfig authenticator = new UserDefinedFederatedAuthenticatorConfig();
        authenticator.setName("custom-authenticator");
        identityProvider.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authenticator});

        IdentityProvider clone = invokeClone(identityProvider);

        Assert.assertNotSame(clone, identityProvider);
        Assert.assertEquals(clone.getIdentityProviderName(), "custom-connection");
        FederatedAuthenticatorConfig clonedAuthenticator = clone.getFederatedAuthenticatorConfigs()[0];
        Assert.assertTrue(clonedAuthenticator instanceof UserDefinedFederatedAuthenticatorConfig,
                "The cloned authenticator must retain its user-defined subtype.");
        Assert.assertEquals(clonedAuthenticator.getName(), "custom-authenticator");
    }

    @Test
    public void testCloneIdentityProviderCopiesBasicFields() throws Exception {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName("connection");
        identityProvider.setEnable(true);

        IdentityProvider clone = invokeClone(identityProvider);

        Assert.assertNotSame(clone, identityProvider);
        Assert.assertEquals(clone.getIdentityProviderName(), "connection");
        Assert.assertTrue(clone.isEnable());
    }

    private IdentityProvider invokeClone(IdentityProvider identityProvider) throws Exception {

        Method method = SharedIdpMgtListener.class.getDeclaredMethod("cloneIdentityProvider", IdentityProvider.class);
        method.setAccessible(true);
        return (IdentityProvider) method.invoke(listener, identityProvider);
    }

    // ----- doPostUpdateIdPByResourceId / doPreUpdateIdP -----

    @Test
    public void testDoPostUpdateIsNoOpWhenNoChangeDetected() throws Exception {

        // No pre-update markers were set, so no shadow sync is triggered.
        Assert.assertTrue(listener.doPostUpdateIdPByResourceId(RESOURCE_ID, new IdentityProvider(),
                new IdentityProvider(), TENANT_DOMAIN));
    }

    @Test
    public void testDoPostUpdateTriggersSyncWhenNameChanged() throws Exception {

        ConnectionSharingUtil.setIsConnectionNameUpdating(true);
        Assert.assertTrue(listener.doPostUpdateIdPByResourceId(RESOURCE_ID, new IdentityProvider(),
                new IdentityProvider(), TENANT_DOMAIN));
    }

    @Test
    public void testDoPreUpdateRecordsChangesForNonSharedConnection() throws Exception {

        IdentityProvider existing = new IdentityProvider();
        existing.setIdentityProviderName("original-name");
        existing.setResourceId(RESOURCE_ID);
        when(idpManager.getIdPByResourceId(anyString(), anyString(), anyBoolean(), any())).thenReturn(existing);

        IdentityProvider updating = new IdentityProvider();
        updating.setIdentityProviderName("renamed");

        // A (non-shared) parent update: the name change is recorded for the post-update propagation.
        Assert.assertTrue(listener.doPreUpdateIdPByResourceId(RESOURCE_ID, updating, TENANT_DOMAIN));
        Assert.assertTrue(ConnectionSharingUtil.consumeConnectionNameUpdated());
    }

    @Test
    public void testDoPreUpdateIdPResolvesExistingByName() throws Exception {

        IdentityProvider existing = new IdentityProvider();
        existing.setResourceId(RESOURCE_ID);
        when(idpManager.getIdPByName(anyString(), anyString(), anyBoolean(), any())).thenReturn(existing);

        // A null updating idp short-circuits to allowed after resolving the existing one by name.
        Assert.assertTrue(listener.doPreUpdateIdP("old-name", null, TENANT_DOMAIN));
    }

    @Test
    public void testDoPostUpdatePropagatesAllChangesToSharedIdps() throws Exception {

        ConnectionSharingUtil.setIsConnectionNameUpdating(true);
        ConnectionSharingUtil.setIsConnectionGroupsUpdating(true);
        ConnectionSharingUtil.setIsConnectionAuthenticatorsUpdating(true);
        ConnectionSharingUtil.setIsConnectionProvisioningConnectorsUpdating(true);

        when(organizationManager.resolveOrganizationId(TENANT_DOMAIN)).thenReturn(RESIDENT_ORG_ID);
        when(associationManager.getConnectionAssociations(anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(association()));
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("shared-tenant");

        // The raw shadow currently mirrors the parent's SAML authenticator, scim connector and one group.
        IdentityProvider rawShadow = sharedIdp();
        FederatedAuthenticatorConfig shadowAuth = new FederatedAuthenticatorConfig();
        shadowAuth.setName("SAMLSSOAuthenticator");
        rawShadow.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{shadowAuth});
        ProvisioningConnectorConfig shadowConnector = new ProvisioningConnectorConfig();
        shadowConnector.setName("scim");
        rawShadow.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{shadowConnector});
        IdPGroup shadowGroup = new IdPGroup();
        shadowGroup.setIdpGroupName("group-a");
        shadowGroup.setIdpGroupId("group-a-id");
        rawShadow.setIdPGroupConfig(new IdPGroup[]{shadowGroup});
        when(idpManager.getIdPByResourceId(anyString(), anyString(), anyBoolean(), eq(SharedIdPResolveType.RAW)))
                .thenReturn(rawShadow);

        // The parent adds a new authenticator/connector; its name and groups are propagated to the shadow.
        IdentityProvider parent = new IdentityProvider();
        parent.setIdentityProviderName("renamed-parent");
        FederatedAuthenticatorConfig samlAuth = new FederatedAuthenticatorConfig();
        samlAuth.setName("SAMLSSOAuthenticator");
        FederatedAuthenticatorConfig newAuth = new FederatedAuthenticatorConfig();
        newAuth.setName("OIDCAuthenticator");
        parent.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{samlAuth, newAuth});
        parent.setDefaultAuthenticatorConfig(samlAuth);
        ProvisioningConnectorConfig scim = new ProvisioningConnectorConfig();
        scim.setName("scim");
        ProvisioningConnectorConfig newConnector = new ProvisioningConnectorConfig();
        newConnector.setName("scim2");
        parent.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[]{scim, newConnector});
        parent.setDefaultProvisioningConnectorConfig(scim);
        IdPGroup parentGroup = new IdPGroup();
        parentGroup.setIdpGroupName("group-a");
        parent.setIdPGroupConfig(new IdPGroup[]{parentGroup});

        try (MockedStatic<PrivilegedCarbonContext> mockedCarbonContext = mockStatic(PrivilegedCarbonContext.class)) {
            PrivilegedCarbonContext carbonContext = mock(PrivilegedCarbonContext.class);
            mockedCarbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);

            Assert.assertTrue(listener.doPostUpdateIdPByResourceId(RESOURCE_ID, new IdentityProvider(), parent,
                    TENANT_DOMAIN));
        }

        verify(idpManager).updateIdPByResourceId(eq("shadow-connection-id"), any(), eq("shared-tenant"));
    }

    @Test
    public void testDoPreUpdateValidatesSharedConnectionAgainstParent() throws Exception {

        IdentityProvider existing = sharedIdp();
        existing.setEnable(true);
        when(idpManager.getIdPByResourceId(eq(RESOURCE_ID), anyString(), anyBoolean(),
                eq(SharedIdPResolveType.RAW))).thenReturn(existing);
        when(associationManager.getConnectionAssociationBySharedConnectionId(anyString(), anyString()))
                .thenReturn(Optional.of(association()));
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("parent-tenant");
        when(idpManager.getIdPByResourceId(eq("parent-connection-id"), anyString(), anyBoolean(),
                eq(SharedIdPResolveType.FULL_RESOLVED))).thenReturn(parentIdp("parent-name", "desc"));

        IdentityProvider updating = sharedIdp();
        updating.setEnable(true);

        // The shadow update is validated against its resolved parent and permitted.
        Assert.assertTrue(listener.doPreUpdateIdPByResourceId(RESOURCE_ID, updating, TENANT_DOMAIN));
    }

    @Test(expectedExceptions = IdentityProviderManagementException.class)
    public void testDoPreUpdateThrowsWhenSharedConnectionHasNoAssociation() throws Exception {

        IdentityProvider existing = sharedIdp();
        when(idpManager.getIdPByResourceId(eq(RESOURCE_ID), anyString(), anyBoolean(),
                eq(SharedIdPResolveType.RAW))).thenReturn(existing);
        when(associationManager.getConnectionAssociationBySharedConnectionId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        listener.doPreUpdateIdPByResourceId(RESOURCE_ID, sharedIdp(), TENANT_DOMAIN);
    }

    // ----- resolveSharedIdp (via doPostGetIdP* hooks) -----

    @Test
    public void testResolveReturnsNonSharedIdpUnchanged() throws Exception {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName("regular-connection");

        IdentityProvider result = listener.doPostGetIdPByResourceId(RESOURCE_ID, identityProvider, TENANT_DOMAIN,
                SharedIdPResolveType.FULL_RESOLVED);

        Assert.assertSame(result, identityProvider);
    }

    @Test
    public void testResolveReturnsSharedIdpUnchangedForRawResolveType() throws Exception {

        IdentityProvider shadow = sharedIdp();

        IdentityProvider result = listener.doPostGetIdPByResourceId(RESOURCE_ID, shadow, TENANT_DOMAIN,
                SharedIdPResolveType.RAW);

        // RAW returns the stored shadow untouched (no parent overlay).
        Assert.assertSame(result, shadow);
    }

    @Test
    public void testResolveReturnsNullForNullIdp() throws Exception {

        Assert.assertNull(listener.doPostGetIdPByResourceId(RESOURCE_ID, null, TENANT_DOMAIN,
                SharedIdPResolveType.FULL_RESOLVED));
    }

    @Test
    public void testResolveReturnsSharedIdpUnchangedWhenNoAssociation() throws Exception {

        IdentityProvider shadow = sharedIdp();
        when(associationManager.getConnectionAssociationBySharedConnectionId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        IdentityProvider result = listener.doPostGetIdPByResourceId(RESOURCE_ID, shadow, TENANT_DOMAIN,
                SharedIdPResolveType.BASE_RESOLVED);

        Assert.assertSame(result, shadow);
    }

    @Test
    public void testResolveBaseResolvedOverlaysParentAttributesOntoClone() throws Exception {

        IdentityProvider shadow = sharedIdp();
        stubParentResolution(parentIdp("parent-name", "parent-description"));

        IdentityProvider result = listener.doPostGetIdPByResourceId(RESOURCE_ID, shadow, TENANT_DOMAIN,
                SharedIdPResolveType.BASE_RESOLVED);

        // The overlay is applied to a clone (never the supplied instance).
        Assert.assertNotSame(result, shadow);
        // Base view inherits the parent's description but keeps the shadow's own name.
        Assert.assertEquals(result.getIdentityProviderDescription(), "parent-description");
        Assert.assertEquals(result.getIdentityProviderName(), "shared-connection");
    }

    @Test
    public void testResolveFullResolvedInheritsParentName() throws Exception {

        IdentityProvider shadow = sharedIdp();
        stubParentResolution(parentIdp("parent-name", "parent-description"));

        IdentityProvider result = listener.doPostGetIdPByResourceId(RESOURCE_ID, shadow, TENANT_DOMAIN,
                SharedIdPResolveType.FULL_RESOLVED);

        Assert.assertNotSame(result, shadow);
        // The runtime (full) view inherits the parent-owned name.
        Assert.assertEquals(result.getIdentityProviderName(), "parent-name");
    }

    @Test
    public void testDoPostGetIdPsReturnsNullForNullList() throws Exception {

        Assert.assertNull(listener.doPostGetIdPs(null, TENANT_DOMAIN, null, SharedIdPResolveType.FULL_RESOLVED));
    }

    @Test
    public void testDoPostGetIdPsLeavesNonSharedEntriesUnchanged() throws Exception {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName("regular-connection");
        List<IdentityProvider> identityProviders = new ArrayList<>(Collections.singletonList(identityProvider));

        List<IdentityProvider> result = listener.doPostGetIdPs(identityProviders, TENANT_DOMAIN, null,
                SharedIdPResolveType.FULL_RESOLVED);

        Assert.assertSame(result.get(0), identityProvider);
    }

    private void stubParentResolution(IdentityProvider parentIdp) throws Exception {

        when(associationManager.getConnectionAssociationBySharedConnectionId(anyString(), anyString()))
                .thenReturn(Optional.of(association()));
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("parent-tenant");
        when(idpManager.getIdPByResourceId(anyString(), anyString(), anyBoolean(), any())).thenReturn(parentIdp);
    }

    private IdentityProvider parentIdp(String name, String description) {

        IdentityProvider parent = new IdentityProvider();
        parent.setIdentityProviderName(name);
        parent.setIdentityProviderDescription(description);
        parent.setEnable(true);
        return parent;
    }

    private ConnectionAssociation association() {

        return new ConnectionAssociation.Builder()
                .resourceType(ResourceType.CONNECTION_IDENTITY_PROVIDER)
                .parentConnectionId("parent-connection-id")
                .connectionResidentOrganizationId(RESIDENT_ORG_ID)
                .sharedConnectionId("shadow-connection-id")
                .organizationId("shared-org-id")
                .build();
    }

    private IdentityProvider sharedIdp() {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName("shared-connection");
        identityProvider.setResourceId(RESOURCE_ID);
        IdentityProviderProperty isShared = new IdentityProviderProperty();
        isShared.setName(IdPManagementConstants.IS_SHARED_IDP_PROPERTY);
        isShared.setValue(Boolean.TRUE.toString());
        identityProvider.setIdpProperties(new IdentityProviderProperty[]{isShared});
        return identityProvider;
    }
}

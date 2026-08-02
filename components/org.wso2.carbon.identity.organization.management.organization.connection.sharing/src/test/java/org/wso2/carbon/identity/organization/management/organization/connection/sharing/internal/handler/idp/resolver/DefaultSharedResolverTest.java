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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;

import java.util.List;

/**
 * Unit tests for the default federated-authenticator and provisioning-connector resolvers, which inherit every
 * attribute from the parent (no local storage on the shadow) and drive the deny-guard off that inheritance.
 */
public class DefaultSharedResolverTest {

    private final DefaultSharedFederatedAuthenticatorResolver authenticatorResolver =
            new DefaultSharedFederatedAuthenticatorResolver();
    private final DefaultSharedProvisioningConnectorResolver connectorResolver =
            new DefaultSharedProvisioningConnectorResolver();

    // ----- federated authenticator resolver -----

    @Test
    public void testAuthenticatorResolverName() {

        Assert.assertEquals(authenticatorResolver.getAuthenticatorName(), "DEFAULT_AUTHENTICATOR");
    }

    @Test
    public void testResolveAuthenticatorReturnsNullForNullParent() {

        Assert.assertNull(authenticatorResolver.resolveAuthenticator(null, null, true));
    }

    @Test
    public void testResolveAuthenticatorInheritsParentAttributes() {

        FederatedAuthenticatorConfig parent = new FederatedAuthenticatorConfig();
        parent.setName("SAMLSSOAuthenticator");
        parent.setDisplayName("SAML");
        parent.setEnabled(true);

        FederatedAuthenticatorConfig resolved = authenticatorResolver.resolveAuthenticator(parent, null, true);

        Assert.assertEquals(resolved.getName(), "SAMLSSOAuthenticator");
        Assert.assertEquals(resolved.getDisplayName(), "SAML");
        Assert.assertTrue(resolved.isEnabled());
    }

    @Test
    public void testResolveAuthenticatorSkipsPropertiesWhenNotResolvingWithParent() {

        FederatedAuthenticatorConfig parent = new FederatedAuthenticatorConfig();
        parent.setName("SAMLSSOAuthenticator");
        parent.setProperties(new org.wso2.carbon.identity.application.common.model.Property[]{
                new org.wso2.carbon.identity.application.common.model.Property()});

        FederatedAuthenticatorConfig baseResolved = authenticatorResolver.resolveAuthenticator(parent, null, false);
        Assert.assertTrue(baseResolved.getProperties() == null || baseResolved.getProperties().length == 0);

        FederatedAuthenticatorConfig fullResolved = authenticatorResolver.resolveAuthenticator(parent, null, true);
        Assert.assertEquals(fullResolved.getProperties().length, 1);
    }

    @Test
    public void testAuthenticatorGetRestrictedModificationsFlagsInheritedChange() {

        FederatedAuthenticatorConfig incoming = new FederatedAuthenticatorConfig();
        incoming.setName("changed-name");
        FederatedAuthenticatorConfig stored = new FederatedAuthenticatorConfig();
        stored.setName("original-name");

        List<String> restricted = authenticatorResolver.getRestrictedModifications(incoming, stored);
        Assert.assertTrue(restricted.contains("name"));
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testAuthenticatorDoPreUpdateValidationThrowsOnInheritedChange() throws Exception {

        FederatedAuthenticatorConfig updating = new FederatedAuthenticatorConfig();
        updating.setName("changed-name");
        FederatedAuthenticatorConfig existing = new FederatedAuthenticatorConfig();
        existing.setName("original-name");

        authenticatorResolver.doPreUpdateValidation(updating, existing, null);
    }

    // ----- provisioning connector resolver -----

    @Test
    public void testConnectorResolverNameIsNull() {

        Assert.assertNull(connectorResolver.getConnectorName());
    }

    @Test
    public void testResolveConnectorReturnsNullForNullParent() {

        Assert.assertNull(connectorResolver.resolveConnector(null, null, true));
    }

    @Test
    public void testResolveConnectorInheritsParentAttributes() {

        ProvisioningConnectorConfig parent = new ProvisioningConnectorConfig();
        parent.setName("scim");
        parent.setEnabled(true);
        parent.setBlocking(true);
        parent.setRulesEnabled(false);

        ProvisioningConnectorConfig resolved = connectorResolver.resolveConnector(parent, null, true);

        Assert.assertEquals(resolved.getName(), "scim");
        Assert.assertTrue(resolved.isEnabled());
        Assert.assertTrue(resolved.isBlocking());
        Assert.assertFalse(resolved.isRulesEnabled());
    }

    @Test
    public void testConnectorGetRestrictedModificationsFlagsInheritedChange() {

        ProvisioningConnectorConfig incoming = new ProvisioningConnectorConfig();
        incoming.setBlocking(true);
        ProvisioningConnectorConfig stored = new ProvisioningConnectorConfig();
        stored.setBlocking(false);

        List<String> restricted = connectorResolver.getRestrictedModifications(incoming, stored);
        Assert.assertFalse(restricted.isEmpty());
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testConnectorDoPreUpdateValidationThrowsOnInheritedChange() throws Exception {

        ProvisioningConnectorConfig updating = new ProvisioningConnectorConfig();
        updating.setBlocking(true);
        ProvisioningConnectorConfig stored = new ProvisioningConnectorConfig();
        stored.setBlocking(false);

        connectorResolver.doPreUpdateValidation(updating, stored, null);
    }
}

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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.model.IdPGroup;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link SharedIdpResolver} covering the read-time overlay of parent configuration onto a shadow
 * (inheritance, the enabled AND-combine, local-group preservation and property merge) and the write-time
 * deny-guard ({@code doPreUpdateValidations}). The parent and shadow are kept free of federated authenticators,
 * provisioning connectors and JIT configuration so the per-resource resolvers are no-ops.
 */
public class SharedIdpResolverTest {

    private SharedIdpResolver resolver;

    @BeforeClass
    public void setUp() {

        resolver = SharedIdpResolver.getInstance();
    }

    @Test
    public void testOverlayBasicInheritsDescriptionFromParent() {

        IdentityProvider parent = idp("connection", true);
        parent.setIdentityProviderDescription("parent-description");
        IdentityProvider shadow = idp("connection", true);

        resolver.overlayBasicParentAttributes(parent, shadow);

        Assert.assertEquals(shadow.getIdentityProviderDescription(), "parent-description");
    }

    @Test
    public void testOverlayBasicKeepsLocalIdpGroups() {

        IdentityProvider parent = idp("connection", true);
        parent.setIdPGroupConfig(new IdPGroup[]{group("parent-group")});
        IdentityProvider shadow = idp("connection", true);
        IdPGroup[] shadowGroups = new IdPGroup[]{group("shadow-group")};
        shadow.setIdPGroupConfig(shadowGroups);

        resolver.overlayBasicParentAttributes(parent, shadow);

        // Identity provider groups are LOCAL: the shadow keeps its own groups, never inheriting the parent's.
        Assert.assertSame(shadow.getIdPGroupConfig(), shadowGroups);
    }

    @Test
    public void testOverlayEnabledIsAndOfParentAndShadow() {

        // Parent disabled forces the shadow disabled even if the shadow is locally enabled.
        Assert.assertFalse(overlayBasicEnabled(false, true));
        // Enabled parent lets the sub-organization keep the shadow disabled locally.
        Assert.assertFalse(overlayBasicEnabled(true, false));
        // Both enabled -> effectively enabled.
        Assert.assertTrue(overlayBasicEnabled(true, true));
    }

    @Test
    public void testOverlayFullInheritsNameFromParent() {

        IdentityProvider parent = idp("parent-connection-name", true);
        IdentityProvider shadow = idp("shadow-local-name", true);

        resolver.overlayParentConfiguration(parent, shadow);

        // The connection name is owned by the parent and reflected on read.
        Assert.assertEquals(shadow.getIdentityProviderName(), "parent-connection-name");
    }

    @Test
    public void testOverlayFullMergesProperties() {

        IdentityProvider parent = idp("connection", true);
        parent.setIdpProperties(new IdentityProviderProperty[]{property("parentProperty", "parent-value")});
        IdentityProvider shadow = idp("connection", true);
        shadow.setIdpProperties(new IdentityProviderProperty[]{
                property(IdPManagementConstants.IS_SHARED_IDP_PROPERTY, "true")});

        resolver.overlayParentConfiguration(parent, shadow);

        Set<String> propertyNames = Arrays.stream(shadow.getIdpProperties())
                .map(IdentityProviderProperty::getName).collect(Collectors.toSet());
        Assert.assertTrue(propertyNames.contains("parentProperty"));
        Assert.assertTrue(propertyNames.contains(IdPManagementConstants.IS_SHARED_IDP_PROPERTY));
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreUpdateValidationsRejectsEnablingWhenParentDisabled() throws Exception {

        IdentityProvider existing = idp("connection", false);
        IdentityProvider updating = idp("connection", true);
        IdentityProvider parent = idp("connection", false);

        resolver.doPreUpdateValidations(updating, existing, parent);
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreUpdateValidationsRejectsInheritedAttributeModification() throws Exception {

        IdentityProvider existing = idp("connection", true);
        existing.setAlias("original-alias");
        IdentityProvider updating = idp("connection", true);
        updating.setAlias("changed-alias");
        IdentityProvider parent = idp("connection", true);

        // Alias is an inherited attribute; a sub-organization may not modify it on the shadow.
        resolver.doPreUpdateValidations(updating, existing, parent);
    }

    @Test(expectedExceptions = IdentityProviderManagementClientException.class)
    public void testDoPreUpdateValidationsRejectsDroppingSharedMarker() throws Exception {

        IdentityProvider existing = idp("connection", true);
        existing.setIdpProperties(new IdentityProviderProperty[]{
                property(IdPManagementConstants.IS_SHARED_IDP_PROPERTY, "true")});
        IdentityProvider updating = idp("connection", true);
        updating.setIdpProperties(new IdentityProviderProperty[]{
                property(IdPManagementConstants.IS_SHARED_IDP_PROPERTY, "false")});
        IdentityProvider parent = idp("connection", true);

        resolver.doPreUpdateValidations(updating, existing, parent);
    }

    private boolean overlayBasicEnabled(boolean parentEnabled, boolean shadowEnabled) {

        IdentityProvider parent = idp("connection", parentEnabled);
        IdentityProvider shadow = idp("connection", shadowEnabled);
        resolver.overlayBasicParentAttributes(parent, shadow);
        return shadow.isEnable();
    }

    private IdentityProvider idp(String name, boolean enabled) {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName(name);
        identityProvider.setEnable(enabled);
        return identityProvider;
    }

    private IdPGroup group(String name) {

        IdPGroup idpGroup = new IdPGroup();
        idpGroup.setIdpGroupName(name);
        return idpGroup;
    }

    private IdentityProviderProperty property(String name, String value) {

        IdentityProviderProperty property = new IdentityProviderProperty();
        property.setName(name);
        property.setValue(value);
        return property;
    }
}

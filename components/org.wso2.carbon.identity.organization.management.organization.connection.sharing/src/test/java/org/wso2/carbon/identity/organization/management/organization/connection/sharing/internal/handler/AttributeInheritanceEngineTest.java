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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler;

import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.exception.RestrictedAttributeModificationException;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link AttributeInheritanceEngine} — the generic inheritance engine that drives the read-time
 * overlay ({@code applyOverlay}) and the write-time deny-guard ({@code restrictedModifications} /
 * {@code validateRestrictedModifications}) off a {@link ConfigAttribute} registry.
 */
public class AttributeInheritanceEngineTest {

    @Test
    public void testApplyOverlayInheritedTakesParentValue() {

        List<ConfigAttribute<Bean, ?>> registry =
                Arrays.asList(ConfigAttribute.inherited("name", Bean::getName, Bean::setName));
        Bean parent = bean("parent-name", null);
        Bean target = bean("local-name", null);

        AttributeInheritanceEngine.applyOverlay(parent, target, registry);

        Assert.assertEquals(target.getName(), "parent-name");
    }

    @Test
    public void testApplyOverlayOverridableKeepsLocallyConfiguredValue() {

        List<ConfigAttribute<Bean, ?>> registry = Arrays.asList(
                ConfigAttribute.overridable("value", Bean::getValue, Bean::setValue, StringUtils::isNotBlank));
        Bean parent = bean(null, "parent-value");
        Bean target = bean(null, "local-value");

        AttributeInheritanceEngine.applyOverlay(parent, target, registry);

        // The target configured the value locally, so its value wins over the parent's.
        Assert.assertEquals(target.getValue(), "local-value");
    }

    @Test
    public void testApplyOverlayOverridableTakesParentWhenNotConfiguredLocally() {

        List<ConfigAttribute<Bean, ?>> registry = Arrays.asList(
                ConfigAttribute.overridable("value", Bean::getValue, Bean::setValue, StringUtils::isNotBlank));
        Bean parent = bean(null, "parent-value");
        Bean target = bean(null, null);

        AttributeInheritanceEngine.applyOverlay(parent, target, registry);

        // The target has not configured the value locally, so the parent's is inherited.
        Assert.assertEquals(target.getValue(), "parent-value");
    }

    @Test
    public void testApplyOverlayLocalIsNeverInherited() {

        List<ConfigAttribute<Bean, ?>> registry =
                Arrays.asList(ConfigAttribute.local("value", Bean::getValue, Bean::setValue));
        Bean parent = bean(null, "parent-value");
        Bean target = bean(null, "local-value");

        AttributeInheritanceEngine.applyOverlay(parent, target, registry);

        Assert.assertEquals(target.getValue(), "local-value");
    }

    @Test
    public void testRestrictedModificationsFlagsOnlyInheritedChanges() {

        List<ConfigAttribute<Bean, ?>> registry = Arrays.asList(
                ConfigAttribute.inherited("name", Bean::getName, Bean::setName),
                ConfigAttribute.overridable("value", Bean::getValue, Bean::setValue, StringUtils::isNotBlank));
        Bean incoming = bean("changed-name", "changed-value");
        Bean stored = bean("original-name", "original-value");

        List<String> restricted = AttributeInheritanceEngine.restrictedModifications(incoming, stored, registry);

        // Only the inherited "name" change is restricted; the overridable "value" change is allowed.
        Assert.assertEquals(restricted.size(), 1);
        Assert.assertTrue(restricted.contains("name"));
    }

    @Test(expectedExceptions = RestrictedAttributeModificationException.class)
    public void testValidateRestrictedModificationsThrowsOnInheritedChange() throws Exception {

        List<ConfigAttribute<Bean, ?>> registry =
                Arrays.asList(ConfigAttribute.inherited("name", Bean::getName, Bean::setName));
        Bean incoming = bean("changed-name", null);
        Bean stored = bean("original-name", null);

        AttributeInheritanceEngine.validateRestrictedModifications(incoming, stored, registry);
    }

    @Test
    public void testValidateRestrictedModificationsPassesWhenOnlyOverridableChanged() throws Exception {

        List<ConfigAttribute<Bean, ?>> registry = Arrays.asList(
                ConfigAttribute.inherited("name", Bean::getName, Bean::setName),
                ConfigAttribute.overridable("value", Bean::getValue, Bean::setValue, StringUtils::isNotBlank));
        Bean incoming = bean("same-name", "changed-value");
        Bean stored = bean("same-name", "original-value");

        // No exception expected; the inherited "name" is unchanged.
        AttributeInheritanceEngine.validateRestrictedModifications(incoming, stored, registry);
    }

    @Test
    public void testIsModifiedTreatsUnsetVariantsAsEqual() {

        // null, empty string, blank string and an empty array all normalize to "unset".
        Assert.assertFalse(AttributeInheritanceEngine.isModified(null, ""));
        Assert.assertFalse(AttributeInheritanceEngine.isModified("", "   "));
        Assert.assertFalse(AttributeInheritanceEngine.isModified(null, null));
        Assert.assertFalse(AttributeInheritanceEngine.isModified(new String[0], null));
    }

    @Test
    public void testIsModifiedDetectsRealValueDifferences() {

        Assert.assertTrue(AttributeInheritanceEngine.isModified("value-a", "value-b"));
        Assert.assertFalse(AttributeInheritanceEngine.isModified("same", "same"));
        Assert.assertTrue(AttributeInheritanceEngine.isModified("value", null));
    }

    private Bean bean(String name, String value) {

        Bean bean = new Bean();
        bean.setName(name);
        bean.setValue(value);
        return bean;
    }

    /**
     * Minimal container used as the generic type parameter for the attribute registry under test.
     */
    private static class Bean {

        private String name;
        private String value;

        String getName() {

            return name;
        }

        void setName(String name) {

            this.name = name;
        }

        String getValue() {

            return value;
        }

        void setValue(String value) {

            this.value = value;
        }
    }
}

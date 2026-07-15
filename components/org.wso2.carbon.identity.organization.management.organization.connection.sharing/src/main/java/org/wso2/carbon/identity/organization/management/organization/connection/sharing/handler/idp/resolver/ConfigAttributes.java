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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.idp.resolver;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The generic inheritance engine that drives both the read-time overlay and the write-time deny-guard off a
 * {@link ConfigAttribute} registry, for any shared-connection container type {@code C} (an
 * {@code IdentityProvider}, a {@code FederatedAuthenticatorConfig} or a {@code ProvisioningConnectorConfig}).
 */
final class ConfigAttributes {

    private static final Gson GSON = new Gson();

    private ConfigAttributes() {

    }

    /**
     * Overlays the parent's configuration onto the target:
     * <ul>
     *   <li>{@code INHERITED} attributes are copied from the parent.</li>
     *   <li>{@code OVERRIDABLE} attributes are copied from the parent unless the target has configured them
     *       locally, in which case the target's value is preserved.</li>
     *   <li>{@code LOCAL} attributes are never copied from the parent — the target's own value is kept.</li>
     * </ul>
     * Mutates {@code target} in place.
     *
     * @param parent   The parent container (the source of inherited values).
     * @param target   The target container to overlay in place.
     * @param registry The attribute registry.
     * @param <C>      The container type.
     */
    static <C> void applyOverlay(C parent, C target, List<ConfigAttribute<C, ?>> registry) {

        // Capture the locally-overridden attributes from the target before the parent configuration is overlaid,
        // so that sub-organization edits win over the parent.
        List<Runnable> localOverrides = new ArrayList<>();
        for (ConfigAttribute<C, ?> attribute : registry) {
            if (attribute.isOverridable()) {
                captureLocalOverride(attribute, target, localOverrides);
            }
        }
        for (ConfigAttribute<C, ?> attribute : registry) {
            // LOCAL attributes are never inherited: leave the target's own value untouched.
            if (!attribute.isLocal()) {
                attribute.copyValue(parent, target);
            }
        }
        localOverrides.forEach(Runnable::run);
    }

    /**
     * Returns the inherited (non-overridable) attributes that an incoming update would change relative to the stored
     * shadow — i.e. the modifications a sub-organization is not allowed to make.
     *
     * @param incoming The incoming (to-be-persisted) container.
     * @param stored   The stored shadow container (the baseline).
     * @param registry The inherited/overridable attribute registry.
     * @param <C>      The container type.
     * @return The display names of the restricted attributes that were modified; empty if none.
     */
    static <C> List<String> restrictedModifications(C incoming, C stored, List<ConfigAttribute<C, ?>> registry) {

        List<String> restrictedModifications = new ArrayList<>();
        for (ConfigAttribute<C, ?> attribute : registry) {
            // Only INHERITED attributes are restricted; OVERRIDABLE and LOCAL are the sub-organization's to edit.
            if (attribute.isInherited()
                    && isModified(attribute.getter.apply(incoming), attribute.getter.apply(stored))) {
                restrictedModifications.add(attribute.displayName);
            }
        }
        return restrictedModifications;
    }

    static <C> void validateRestrictedModifications(C incoming, C stored, List<ConfigAttribute<C, ?>> registry)
            throws IllegalArgumentException {

        for (ConfigAttribute<C, ?> attribute : registry) {
            // Only INHERITED attributes are restricted; OVERRIDABLE and LOCAL are the sub-organization's to edit.
            if (!attribute.isInherited()) {
                continue;
            }
            if (isModified(attribute.getter.apply(incoming), attribute.getter.apply(stored))) {
                throw new IllegalArgumentException("Attribute: '" + attribute.displayName +
                        "' is not allowed to be modified as it is inherited from the parent.");
            }
        }
    }

    /**
     * Returns whether an incoming configuration value differs from the stored value. Comparison is value-based and
     * tolerant of the null/empty/default variations introduced by serialization round-trips (a {@code null}, an
     * empty array, an empty object and a blank string are all treated as "unset").
     */
    static boolean isModified(Object incoming, Object storedValue) {

        return !normalize(incoming).equals(normalize(storedValue));
    }

    private static <C, T> void captureLocalOverride(ConfigAttribute<C, T> attribute, C target,
                                                    List<Runnable> localOverrides) {

        T localValue = attribute.getter.apply(target);
        if (attribute.locallyConfigured.test(localValue)) {
            localOverrides.add(() -> attribute.setter.accept(target, localValue));
        }
    }

    /**
     * Produces a canonical comparison key for a configuration value. Different representations of "unset" collapse
     * to the empty string; everything else is compared by its JSON form, which gives a value-based deep comparison
     * without relying on {@code equals()} being implemented on the model classes.
     */
    private static String normalize(Object value) {

        if (value == null) {
            return StringUtils.EMPTY;
        }
        if (value instanceof String) {
            return StringUtils.trimToEmpty((String) value);
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? Boolean.TRUE.toString() : StringUtils.EMPTY;
        }
        if (value instanceof Object[] && ((Object[]) value).length == 0) {
            return StringUtils.EMPTY;
        }
        String json = GSON.toJson(value);
        if ("null".equals(json) || "[]".equals(json) || "{}".equals(json)) {
            return StringUtils.EMPTY;
        }
        return json;
    }
}

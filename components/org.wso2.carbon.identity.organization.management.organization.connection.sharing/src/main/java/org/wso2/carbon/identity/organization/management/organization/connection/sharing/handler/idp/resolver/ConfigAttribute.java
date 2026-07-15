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

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Describes a single configuration attribute of a shared-connection container {@code C} (an
 * {@code IdentityProvider}, a {@code FederatedAuthenticatorConfig} or a {@code ProvisioningConnectorConfig}) and how
 * a shadow connection derives its value from the parent. It bundles how to read and write the attribute, a
 * human-readable name (used in rejection messages), the {@link Inheritance} mode, and — for overridable ones — how
 * to decide whether the shadow has configured it locally.
 *
 * <p>A resolver declares its attributes as a list of these; both the read-time overlay and the write-time
 * deny-guard are driven by the same list via {@link ConfigAttributes}.</p>
 *
 * @param <C> The container type the attribute lives on.
 * @param <T> The type of the attribute value.
 */
final class ConfigAttribute<C, T> {

    /**
     * How a shadow connection derives an attribute's value from its parent.
     */
    enum Inheritance {

        /** Always taken from the parent on read; a sub-organization may NOT modify it on the shadow. */
        INHERITED,

        /**
         * Taken from the parent unless the shadow has configured it locally (see {@code locallyConfigured}); a
         * sub-organization may modify it.
         */
        OVERRIDABLE,

        /** Never taken from the parent — always the shadow's own value; a sub-organization may modify it. */
        LOCAL
    }

    final String displayName;
    final Function<C, T> getter;
    final BiConsumer<C, T> setter;
    final Inheritance inheritance;
    final Predicate<T> locallyConfigured;

    private ConfigAttribute(String displayName, Function<C, T> getter, BiConsumer<C, T> setter,
                            Inheritance inheritance, Predicate<T> locallyConfigured) {

        this.displayName = displayName;
        this.getter = getter;
        this.setter = setter;
        this.inheritance = inheritance;
        this.locallyConfigured = locallyConfigured;
    }

    /**
     * An inherited attribute that is always taken from the parent and that a sub-organization may NOT modify on a
     * shadow connection.
     */
    static <C, T> ConfigAttribute<C, T> inherited(String displayName, Function<C, T> getter,
                                                  BiConsumer<C, T> setter) {

        return new ConfigAttribute<>(displayName, getter, setter, Inheritance.INHERITED, null);
    }

    /**
     * An attribute a sub-organization may override locally; {@code locallyConfigured} decides whether the shadow's
     * value should win over the parent's during read-time resolution.
     */
    static <C, T> ConfigAttribute<C, T> overridable(String displayName, Function<C, T> getter,
                                                    BiConsumer<C, T> setter, Predicate<T> locallyConfigured) {

        return new ConfigAttribute<>(displayName, getter, setter, Inheritance.OVERRIDABLE, locallyConfigured);
    }

    /**
     * A local-only attribute that is never inherited from the parent — the shadow's own value is always used and a
     * sub-organization may modify it freely (e.g. the outbound provisioning role, which is meaningful only within
     * the sub-organization).
     */
    static <C, T> ConfigAttribute<C, T> local(String displayName, Function<C, T> getter, BiConsumer<C, T> setter) {

        return new ConfigAttribute<>(displayName, getter, setter, Inheritance.LOCAL, null);
    }

    /**
     * Whether this attribute is always taken from the parent (and therefore restricted on the shadow's write path).
     */
    boolean isInherited() {

        return inheritance == Inheritance.INHERITED;
    }

    /**
     * Whether this attribute may carry a local override that wins over the parent's value when the shadow has
     * configured it.
     */
    boolean isOverridable() {

        return inheritance == Inheritance.OVERRIDABLE;
    }

    /**
     * Whether this attribute is never inherited from the parent — the shadow's own value is always used.
     */
    boolean isLocal() {

        return inheritance == Inheritance.LOCAL;
    }

    /**
     * Copies this attribute's value from the source container onto the target (used by the read-time overlay to
     * inherit an attribute from the parent).
     */
    void copyValue(C source, C target) {

        setter.accept(target, getter.apply(source));
    }
}

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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.exception;

/**
 * Signals that an update to a shared (shadow) connection attempted to modify an attribute that is inherited from the
 * parent connection and therefore may not be changed by a sub-organization. The offending attribute is carried on
 * {@link #getAttributeName()} so callers can map it to a connection-type-specific client error.
 */
public class RestrictedAttributeModificationException extends Exception {

    private static final long serialVersionUID = -8027834129837465012L;

    private final String attributeName;

    public RestrictedAttributeModificationException(String attributeName) {

        super("Attribute: '" + attributeName + "' is not allowed to be modified as it is inherited from the parent.");
        this.attributeName = attributeName;
    }

    /**
     * Returns the display name of the inherited attribute whose modification was rejected.
     *
     * @return The restricted attribute's display name.
     */
    public String getAttributeName() {

        return attributeName;
    }
}

/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.capability.governance.model;

/**
 * Enum representing the type of a governance policy.
 */
public enum Policy {

    /**
     * Applies to the entire subtree under the governing org.
     */
    ALLOW_ALL,

    /**
     * Applies to direct children of the governing org only.
     */
    ALLOW_IMMEDIATE,

    /**
     * Applies to the orgs explicitly listed in the corresponding selected-orgs table.
     */
    ALLOW_SELECTED,

    /**
     * Explicit deny for all children of the governing org.
     */
    DENY_ALL
}

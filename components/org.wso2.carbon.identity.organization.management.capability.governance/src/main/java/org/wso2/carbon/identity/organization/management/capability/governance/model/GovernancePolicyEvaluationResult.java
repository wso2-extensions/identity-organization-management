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
 * Model representing the result of a governance policy evaluation.
 */
public class GovernancePolicyEvaluationResult {

    private boolean allowed;

    /**
     * Returns whether the capability is allowed for the evaluated organization.
     *
     * @return {@code true} if allowed; {@code false} if denied or no covering policy was found.
     */
    public boolean isAllowed() {

        return allowed;
    }

    /**
     * Sets the access decision for the evaluated organization.
     *
     * @param allowed {@code true} if the capability is allowed; {@code false} otherwise.
     */
    public void setAllowed(boolean allowed) {

        this.allowed = allowed;
    }
}

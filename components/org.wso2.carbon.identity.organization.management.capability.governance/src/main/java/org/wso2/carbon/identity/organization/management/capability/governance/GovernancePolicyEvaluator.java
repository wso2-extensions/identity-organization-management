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

package org.wso2.carbon.identity.organization.management.capability.governance;

import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtException;
import org.wso2.carbon.identity.organization.management.capability.governance.model.GovernancePolicyEvaluationResult;

/**
 * OSGi service interface for evaluating organization capability governance policies.
 */
public interface GovernancePolicyEvaluator {

    /**
     * Evaluates org-level policies to determine whether {@code orgId} has the given capability.
     *
     * @param orgId the organization being evaluated.
     * @param capability the capability being checked.
     * @param resourceType the type of the resource.
     * @return the evaluation result containing the access decision and any additional policy attributes.
     * @throws GovernancePolicyMgtException if the ancestor hierarchy cannot be traversed.
     */
    GovernancePolicyEvaluationResult evaluate(String orgId, String capability, String resourceType)
            throws GovernancePolicyMgtException;
}

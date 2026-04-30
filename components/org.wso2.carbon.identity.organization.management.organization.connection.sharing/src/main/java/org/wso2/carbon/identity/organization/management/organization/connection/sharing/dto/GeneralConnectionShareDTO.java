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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto;

import org.wso2.carbon.identity.organization.management.organization.connection.sharing.models.connectioncriteria.ConnectionCriteriaType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

/**
 * DTO for sharing connections with all organizations under a given policy.
 */
public class GeneralConnectionShareDTO extends BaseConnectionShareDTO<ConnectionCriteriaType> {

    private PolicyEnum policy;

    /**
     * Returns the sharing policy applied when sharing with all organizations.
     *
     * @return the {@link PolicyEnum} value representing the global sharing scope
     */
    public PolicyEnum getPolicy() {

        return policy;
    }

    /**
     * Sets the sharing policy to apply when sharing with all organizations.
     *
     * @param policy the {@link PolicyEnum} value representing the global sharing scope
     */
    public void setPolicy(PolicyEnum policy) {

        this.policy = policy;
    }
}

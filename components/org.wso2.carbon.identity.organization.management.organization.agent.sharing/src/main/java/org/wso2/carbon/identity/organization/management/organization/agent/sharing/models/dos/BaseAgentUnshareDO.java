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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos;

import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.agentcriteria.AgentCriteriaType;

import java.util.Map;

/**
 * Abstract class for common properties and methods for agent unshare data objects.
 *
 * @param <T> The type of agent criteria used in the agent unsharing operations.
 */
public abstract class BaseAgentUnshareDO<T extends AgentCriteriaType> {

    private Map<String, T> agentCriteria;

    public Map<String, T> getAgentCriteria() {

        return agentCriteria;
    }

    public void setAgentCriteria(Map<String, T> agentCriteria) {

        this.agentCriteria = agentCriteria;
    }
}

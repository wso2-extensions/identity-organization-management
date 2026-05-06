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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing;

import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.AgentSharePatchDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.GeneralAgentShareDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.GeneralAgentUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.GetAgentSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.ResponseAgentSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.SelectiveAgentShareDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.SelectiveAgentUnshareDO;

/**
 * Interface for handling agent sharing policy operations.
 */
public interface AgentSharingPolicyHandlerService {

    /**
     * Populates the selective agent share operation.
     *
     * @param selectiveAgentShareDO Selective agent share data object.
     * @throws AgentSharingMgtException If an error occurs during selective agent share population.
     */
    void populateSelectiveAgentShare(SelectiveAgentShareDO selectiveAgentShareDO) throws AgentSharingMgtException;

    /**
     * Populates the general agent share operation.
     *
     * @param generalAgentShareDO General agent share data object.
     * @throws AgentSharingMgtException If an error occurs during general agent share population.
     */
    void populateGeneralAgentShare(GeneralAgentShareDO generalAgentShareDO) throws AgentSharingMgtException;

    /**
     * Populates the selective agent unshare operation.
     *
     * @param selectiveAgentUnshareDO Selective agent unshare data object.
     * @throws AgentSharingMgtException If an error occurs during selective agent unshare population.
     */
    void populateSelectiveAgentUnshare(SelectiveAgentUnshareDO selectiveAgentUnshareDO)
            throws AgentSharingMgtException;

    /**
     * Populates the general agent unshare operation.
     *
     * @param generalAgentUnshareDO General agent unshare data object.
     * @throws AgentSharingMgtException If an error occurs during general agent unshare population.
     */
    void populateGeneralAgentUnshare(GeneralAgentUnshareDO generalAgentUnshareDO) throws AgentSharingMgtException;

    /**
     * Updates the shared agent attributes.
     *
     * @param agentSharePatchDO Agent share patch data object.
     * @throws AgentSharingMgtException If an error occurs during shared agent attribute update.
     */
    void updateSharedAgentAttributes(AgentSharePatchDO agentSharePatchDO) throws AgentSharingMgtException;

    /**
     * Retrieves the organizations that a given agent is shared with.
     *
     * @param getAgentSharedOrgsDO Data object containing agent ID and query parameters.
     * @return Response data object containing shared organization details.
     * @throws AgentSharingMgtException If an error occurs during retrieval.
     */
    ResponseAgentSharedOrgsDO getAgentSharedOrganizations(GetAgentSharedOrgsDO getAgentSharedOrgsDO)
            throws AgentSharingMgtException;
}

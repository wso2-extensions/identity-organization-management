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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.organization.management.capability.governance.dao.GovernancePolicyDAO;
import org.wso2.carbon.identity.organization.management.capability.governance.dao.GovernancePolicyDAOImpl;
import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtException;
import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtServerException;
import org.wso2.carbon.identity.organization.management.capability.governance.internal.GovernancePolicyDataHolder;
import org.wso2.carbon.identity.organization.management.capability.governance.model.GovernancePolicyEvaluationResult;
import org.wso2.carbon.identity.organization.management.capability.governance.model.OrgGovernancePolicy;
import org.wso2.carbon.identity.organization.management.capability.governance.model.Policy;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.List;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED;

/**
 * Implementation of {@link GovernancePolicyEvaluator}.
 *
 * <p>Walks the ancestor chain from orgId's direct parent up to the root. At each ancestor,
 * the org-level policy is checked. The first matching policy determines the result.</p>
 *
 * <p>If no policy is found in any ancestor, the method returns {@code false} (default deny).</p>
 */
public class GovernancePolicyEvaluatorImpl implements GovernancePolicyEvaluator {

    private static final Log LOG = LogFactory.getLog(GovernancePolicyEvaluatorImpl.class);
    private static final GovernancePolicyDAO GOVERNANCE_POLICY_DAO = new GovernancePolicyDAOImpl();

    /**
     * Evaluates whether the given organization may use the specified capability and resource type.
     *
     * @param orgId the ID of the organization being evaluated.
     * @param capability the capability name.
     * @param resourceType the resource type.
     * @return the evaluation result containing the access decision and any additional policy attributes.
     * @throws GovernancePolicyMgtException if the ancestor hierarchy cannot be traversed.
     */
    @Override
    public GovernancePolicyEvaluationResult evaluate(String orgId, String capability, String resourceType)
            throws GovernancePolicyMgtException {

        return evaluateInternal(orgId, capability, resourceType);
    }

    private GovernancePolicyEvaluationResult evaluateInternal(String orgId, String capability, String resourceType)
            throws GovernancePolicyMgtException {

        OrganizationManager organizationManager = GovernancePolicyDataHolder.getInstance().getOrganizationManager();
        List<String> ancestors;
        try {
            ancestors = organizationManager.getAncestorOrganizationIds(orgId);
        } catch (OrganizationManagementException e) {
            throw new GovernancePolicyMgtServerException(ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getCode(),
                    ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getMessage(),
                    ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getDescription(), e);
        }
        // ancestors[0] = orgId, ancestors[1] = direct parent, ..., ancestors[n] = root.
        for (int i = 1; i < ancestors.size(); i++) {
            String ancestorId = ancestors.get(i);
            boolean isDirectChild = (i == 1);

            Optional<OrgGovernancePolicy> op = GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(
                    ancestorId, capability, resourceType);
            if (op.isPresent() && op.get().coversOrg(orgId, isDirectChild)) {
                GovernancePolicyEvaluationResult result = new GovernancePolicyEvaluationResult();
                result.setAllowed(!Policy.DENY_ALL.equals(op.get().getPolicy()));
                return result;
            }
        }
        // Default deny — no policy found covering this org.
        GovernancePolicyEvaluationResult result = new GovernancePolicyEvaluationResult();
        result.setAllowed(false);
        return result;
    }
}

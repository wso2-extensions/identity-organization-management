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

import org.wso2.carbon.identity.core.util.LambdaExceptionUtils;
import org.wso2.carbon.identity.organization.management.capability.governance.dao.GovernancePolicyDAO;
import org.wso2.carbon.identity.organization.management.capability.governance.dao.GovernancePolicyDAOImpl;
import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtException;
import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtServerException;
import org.wso2.carbon.identity.organization.management.capability.governance.internal.GovernancePolicyDataHolder;
import org.wso2.carbon.identity.organization.management.capability.governance.model.GovernancePolicyEvaluationResult;
import org.wso2.carbon.identity.organization.management.capability.governance.model.OrgGovernancePolicy;
import org.wso2.carbon.identity.organization.management.capability.governance.model.Policy;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.FirstMatchAggregationStrategy;

import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED;

/**
 * Implementation of {@link GovernancePolicyEvaluator}.
 *
 * <p>Walks the ancestor chain from orgId's direct parent up to the root via
 * {@link OrgResourceResolverService}. The first ancestor whose policy passes
 * {@code coversOrg} determines the result. If no ancestor has a matching policy,
 * returns {@code false} (default deny).</p>
 */
public class GovernancePolicyEvaluatorImpl implements GovernancePolicyEvaluator {

    private static final GovernancePolicyDAO GOVERNANCE_POLICY_DAO = new GovernancePolicyDAOImpl();

    @Override
    public GovernancePolicyEvaluationResult evaluate(String orgId, String capability, String resourceType)
            throws GovernancePolicyMgtException {

        OrgResourceResolverService resolverService =
                GovernancePolicyDataHolder.getInstance().getOrgResourceResolverService();
        OrgGovernancePolicy policy;
        try {
            policy = resolverService.getResourcesFromOrgHierarchy(
                    orgId,
                    LambdaExceptionUtils.rethrowFunction(
                            ancestorId -> findOrgGovernancePolicy(ancestorId, capability, resourceType)),
                    policyMatchStrategy(orgId));
        } catch (OrgResourceHierarchyTraverseException e) {
            throw new GovernancePolicyMgtServerException(ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getCode(),
                    ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getMessage(),
                    ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getDescription(), e);
        }

        GovernancePolicyEvaluationResult result = new GovernancePolicyEvaluationResult();
        result.setAllowed(policy != null && !Policy.DENY_ALL.equals(policy.getPolicy()));
        return result;
    }

    private Optional<OrgGovernancePolicy> findOrgGovernancePolicy(String orgId, String capability,
                                                                   String resourceType)
            throws GovernancePolicyMgtException {

        return GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(orgId, capability, resourceType);
    }

    private FirstMatchAggregationStrategy<OrgGovernancePolicy> policyMatchStrategy(String orgId) {

        return new FirstMatchAggregationStrategy<>(false,
                (p, isDirectParent) -> p.coversOrg(orgId, isDirectParent));
    }
}

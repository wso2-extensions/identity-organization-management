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
import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtClientException;
import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtException;
import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtServerException;
import org.wso2.carbon.identity.organization.management.capability.governance.internal.GovernancePolicyDataHolder;
import org.wso2.carbon.identity.organization.management.capability.governance.model.GovernanceOrgSelected;
import org.wso2.carbon.identity.organization.management.capability.governance.model.OrgGovernancePolicy;
import org.wso2.carbon.identity.organization.management.capability.governance.model.Policy;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_GET_DESCENDANT_ORGS_FAILED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_GET_ORG_POLICY_FAILED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_INVALID_SELECTED_ORG;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_ORG_CHECK_FAILED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_POLICY_ALREADY_EXISTS;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_POLICY_MANAGEMENT_NOT_PERMITTED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_POLICY_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * Implementation of {@link GovernancePolicyService}.
 */
public class GovernancePolicyServiceImpl implements GovernancePolicyService {

    private static final Log LOG = LogFactory.getLog(GovernancePolicyServiceImpl.class);
    private static final GovernancePolicyDAO GOVERNANCE_POLICY_DAO = new GovernancePolicyDAOImpl();

    @Override
    public OrgGovernancePolicy addOrgGovernancePolicy(OrgGovernancePolicy policy)
            throws GovernancePolicyMgtException {

        validatePrimaryOrg(policy.getGoverningOrgId());
        if (GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(
                policy.getGoverningOrgId(), policy.getCapability(), policy.getResourceType()).isPresent()) {
            throw new GovernancePolicyMgtClientException(ERROR_CODE_POLICY_ALREADY_EXISTS.getCode(),
                    ERROR_CODE_POLICY_ALREADY_EXISTS.getMessage(),
                    ERROR_CODE_POLICY_ALREADY_EXISTS.getDescription());
        }
        if (Policy.ALLOW_SELECTED.equals(policy.getPolicy())) {
            validateSelectedOrgs(policy.getGoverningOrgId(), policy.getSelectedOrgs());
        }
        GOVERNANCE_POLICY_DAO.addOrgGovernancePolicy(policy);
        return GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(
                        policy.getGoverningOrgId(), policy.getCapability(), policy.getResourceType())
                .orElseThrow(() -> new GovernancePolicyMgtServerException(
                        ERROR_CODE_GET_ORG_POLICY_FAILED.getCode(),
                        ERROR_CODE_GET_ORG_POLICY_FAILED.getMessage(),
                        ERROR_CODE_GET_ORG_POLICY_FAILED.getDescription()));
    }

    @Override
    public OrgGovernancePolicy getOrgGovernancePolicyByKey(String governingOrgId, String resourceType,
            String capability) throws GovernancePolicyMgtException {

        validatePrimaryOrg(governingOrgId);
        return GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(governingOrgId, capability, resourceType)
                .orElseThrow(() -> new GovernancePolicyMgtClientException(
                        ERROR_CODE_POLICY_NOT_FOUND.getCode(),
                        ERROR_CODE_POLICY_NOT_FOUND.getMessage(),
                        ERROR_CODE_POLICY_NOT_FOUND.getDescription()));
    }

    @Override
    public List<OrgGovernancePolicy> getOrgGovernancePolicies(String governingOrgId)
            throws GovernancePolicyMgtException {

        validatePrimaryOrg(governingOrgId);
        return GOVERNANCE_POLICY_DAO.getOrgGovernancePoliciesByGoverningOrg(governingOrgId);
    }

    @Override
    public void deleteOrgGovernancePolicyByKey(String governingOrgId, String resourceType, String capability)
            throws GovernancePolicyMgtException {

        validatePrimaryOrg(governingOrgId);
        getOrgGovernancePolicyByKey(governingOrgId, resourceType, capability);
        GOVERNANCE_POLICY_DAO.deleteOrgGovernancePolicyByKey(governingOrgId, resourceType, capability);
    }

    private void validateSelectedOrgs(String governingOrgId, List<GovernanceOrgSelected> selectedOrgs)
            throws GovernancePolicyMgtException {

        if (selectedOrgs == null || selectedOrgs.isEmpty()) {
            return;
        }
        try {
            Set<String> descendants = new HashSet<>(GovernancePolicyDataHolder.getInstance()
                    .getOrganizationManager()
                    .getChildOrganizationsIds(governingOrgId, true));
            for (GovernanceOrgSelected selected : selectedOrgs) {
                if (!descendants.contains(selected.getTargetOrgId())) {
                    throw new GovernancePolicyMgtClientException(ERROR_CODE_INVALID_SELECTED_ORG.getCode(),
                            ERROR_CODE_INVALID_SELECTED_ORG.getMessage(),
                            String.format(ERROR_CODE_INVALID_SELECTED_ORG.getDescription(),
                                    selected.getTargetOrgId()));
                }
            }
        } catch (OrganizationManagementException e) {
            throw new GovernancePolicyMgtServerException(ERROR_CODE_GET_DESCENDANT_ORGS_FAILED.getCode(),
                    ERROR_CODE_GET_DESCENDANT_ORGS_FAILED.getMessage(),
                    ERROR_CODE_GET_DESCENDANT_ORGS_FAILED.getDescription(), e);
        }
    }

    private void validatePrimaryOrg(String governingOrgId) throws GovernancePolicyMgtException {

        String callerOrgId = getOrganizationId();
        if (!governingOrgId.equals(callerOrgId)) {
            throw new GovernancePolicyMgtClientException(ERROR_CODE_POLICY_MANAGEMENT_NOT_PERMITTED.getCode(),
                    ERROR_CODE_POLICY_MANAGEMENT_NOT_PERMITTED.getMessage(),
                    ERROR_CODE_POLICY_MANAGEMENT_NOT_PERMITTED.getDescription());
        }
        try {
            if (OrganizationManagementUtil.isOrganization(governingOrgId)) {
                throw new GovernancePolicyMgtClientException(ERROR_CODE_POLICY_MANAGEMENT_NOT_PERMITTED.getCode(),
                        ERROR_CODE_POLICY_MANAGEMENT_NOT_PERMITTED.getMessage(),
                        ERROR_CODE_POLICY_MANAGEMENT_NOT_PERMITTED.getDescription());
            }
        } catch (OrganizationManagementException e) {
            throw new GovernancePolicyMgtServerException(ERROR_CODE_ORG_CHECK_FAILED.getCode(),
                    ERROR_CODE_ORG_CHECK_FAILED.getMessage(),
                    ERROR_CODE_ORG_CHECK_FAILED.getDescription(), e);
        }
    }
}

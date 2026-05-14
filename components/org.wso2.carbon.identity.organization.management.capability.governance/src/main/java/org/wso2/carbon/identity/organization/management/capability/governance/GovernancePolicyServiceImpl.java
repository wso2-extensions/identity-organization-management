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
import org.wso2.carbon.identity.organization.management.capability.governance.model.PolicyType;
import org.wso2.carbon.identity.organization.management.capability.governance.model.ResourceGovernancePolicy;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.List;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_GET_ORG_POLICY_FAILED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_GET_RESOURCE_POLICY_FAILED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_OVERRIDE_NOT_PERMITTED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_POLICY_ALREADY_EXISTS;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.ErrorMessage.ERROR_CODE_POLICY_NOT_FOUND;

/**
 * Implementation of {@link GovernancePolicyService}.
 */
public class GovernancePolicyServiceImpl implements GovernancePolicyService {

    private static final Log LOG = LogFactory.getLog(GovernancePolicyServiceImpl.class);
    private static final GovernancePolicyDAO GOVERNANCE_POLICY_DAO = new GovernancePolicyDAOImpl();

    @Override
    public OrgGovernancePolicy addOrgGovernancePolicy(OrgGovernancePolicy policy)
            throws GovernancePolicyMgtException {

        validateOrgGovernancePolicy(policy);
        if (GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(
                policy.getGoverningOrgId(), policy.getCapability(), policy.getResourceType()).isPresent()) {
            throw new GovernancePolicyMgtClientException(ERROR_CODE_POLICY_ALREADY_EXISTS.getCode(),
                    ERROR_CODE_POLICY_ALREADY_EXISTS.getMessage(),
                    ERROR_CODE_POLICY_ALREADY_EXISTS.getDescription());
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

        return GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(governingOrgId, capability, resourceType)
                .orElseThrow(() -> new GovernancePolicyMgtClientException(
                        ERROR_CODE_POLICY_NOT_FOUND.getCode(),
                        ERROR_CODE_POLICY_NOT_FOUND.getMessage(),
                        ERROR_CODE_POLICY_NOT_FOUND.getDescription()));
    }

    @Override
    public List<OrgGovernancePolicy> getOrgGovernancePolicies(String governingOrgId)
            throws GovernancePolicyMgtException {

        return GOVERNANCE_POLICY_DAO.getOrgGovernancePoliciesByGoverningOrg(governingOrgId);
    }

    @Override
    public OrgGovernancePolicy updateOrgGovernancePolicyByKey(String governingOrgId, String resourceType,
            String capability, OrgGovernancePolicy updates) throws GovernancePolicyMgtException {

        OrgGovernancePolicy existing = getOrgGovernancePolicyByKey(governingOrgId, resourceType, capability);
        updates.setId(existing.getId());
        updates.setGoverningOrgId(governingOrgId);
        updates.setResourceType(resourceType);
        updates.setCapability(capability);
        GOVERNANCE_POLICY_DAO.updateOrgGovernancePolicy(updates);
        return GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(governingOrgId, capability, resourceType)
                .orElseThrow(() -> new GovernancePolicyMgtClientException(
                        ERROR_CODE_POLICY_NOT_FOUND.getCode(),
                        ERROR_CODE_POLICY_NOT_FOUND.getMessage(),
                        ERROR_CODE_POLICY_NOT_FOUND.getDescription()));
    }

    @Override
    public void deleteOrgGovernancePolicyByKey(String governingOrgId, String resourceType, String capability)
            throws GovernancePolicyMgtException {

        getOrgGovernancePolicyByKey(governingOrgId, resourceType, capability);
        GOVERNANCE_POLICY_DAO.deleteOrgGovernancePolicyByKey(governingOrgId, resourceType, capability);
    }

    @Override
    public ResourceGovernancePolicy addResourceGovernancePolicy(ResourceGovernancePolicy policy)
            throws GovernancePolicyMgtException {

        if (GOVERNANCE_POLICY_DAO.findResourceGovernancePolicy(
                policy.getGoverningOrgId(), policy.getCapability(),
                policy.getResourceType(), policy.getResourceId()).isPresent()) {
            throw new GovernancePolicyMgtClientException(ERROR_CODE_POLICY_ALREADY_EXISTS.getCode(),
                    ERROR_CODE_POLICY_ALREADY_EXISTS.getMessage(),
                    ERROR_CODE_POLICY_ALREADY_EXISTS.getDescription());
        }
        GOVERNANCE_POLICY_DAO.addResourceGovernancePolicy(policy);
        return GOVERNANCE_POLICY_DAO.findResourceGovernancePolicy(
                        policy.getGoverningOrgId(), policy.getCapability(),
                        policy.getResourceType(), policy.getResourceId())
                .orElseThrow(() -> new GovernancePolicyMgtServerException(
                        ERROR_CODE_GET_RESOURCE_POLICY_FAILED.getCode(),
                        ERROR_CODE_GET_RESOURCE_POLICY_FAILED.getMessage(),
                        ERROR_CODE_GET_RESOURCE_POLICY_FAILED.getDescription()));
    }

    @Override
    public ResourceGovernancePolicy getResourceGovernancePolicyByKey(String governingOrgId, String resourceType,
            String resourceId, String capability) throws GovernancePolicyMgtException {

        return GOVERNANCE_POLICY_DAO.findResourceGovernancePolicy(governingOrgId, capability, resourceType, resourceId)
                .orElseThrow(() -> new GovernancePolicyMgtClientException(
                        ERROR_CODE_POLICY_NOT_FOUND.getCode(),
                        ERROR_CODE_POLICY_NOT_FOUND.getMessage(),
                        ERROR_CODE_POLICY_NOT_FOUND.getDescription()));
    }

    @Override
    public List<ResourceGovernancePolicy> getResourceGovernancePolicies(String governingOrgId)
            throws GovernancePolicyMgtException {

        return GOVERNANCE_POLICY_DAO.getResourceGovernancePoliciesByGoverningOrg(governingOrgId);
    }

    @Override
    public ResourceGovernancePolicy updateResourceGovernancePolicyByKey(String governingOrgId, String resourceType,
            String resourceId, String capability, ResourceGovernancePolicy updates)
            throws GovernancePolicyMgtException {

        ResourceGovernancePolicy existing =
                getResourceGovernancePolicyByKey(governingOrgId, resourceType, resourceId, capability);
        updates.setId(existing.getId());
        updates.setGoverningOrgId(governingOrgId);
        updates.setResourceType(resourceType);
        updates.setResourceId(resourceId);
        updates.setCapability(capability);
        updates.setResourceOwnerOrgId(existing.getResourceOwnerOrgId());
        GOVERNANCE_POLICY_DAO.updateResourceGovernancePolicy(updates);
        return GOVERNANCE_POLICY_DAO.findResourceGovernancePolicy(governingOrgId, capability, resourceType, resourceId)
                .orElseThrow(() -> new GovernancePolicyMgtClientException(
                        ERROR_CODE_POLICY_NOT_FOUND.getCode(),
                        ERROR_CODE_POLICY_NOT_FOUND.getMessage(),
                        ERROR_CODE_POLICY_NOT_FOUND.getDescription()));
    }

    @Override
    public void deleteResourceGovernancePolicyByKey(String governingOrgId, String resourceType, String resourceId,
            String capability) throws GovernancePolicyMgtException {

        getResourceGovernancePolicyByKey(governingOrgId, resourceType, resourceId, capability);
        GOVERNANCE_POLICY_DAO.deleteResourceGovernancePolicyByKey(governingOrgId, resourceType, resourceId, capability);
    }

    // -------------------------------------------------------------------------
    // Write-time validation
    // -------------------------------------------------------------------------

    private void validateOrgGovernancePolicy(OrgGovernancePolicy policy) throws GovernancePolicyMgtException {

        OrganizationManager organizationManager = GovernancePolicyDataHolder.getInstance().getOrganizationManager();
        try {
            if (organizationManager.isPrimaryOrganization(policy.getGoverningOrgId())) {
                return;
            }
            List<String> ancestors = organizationManager.getAncestorOrganizationIds(policy.getGoverningOrgId());
            for (int i = 1; i < ancestors.size(); i++) {
                String ancestorId = ancestors.get(i);
                boolean isDirectParent = (i == 1);
                Optional<OrgGovernancePolicy> ancestorPolicy = GOVERNANCE_POLICY_DAO.findOrgGovernancePolicy(
                        ancestorId, policy.getCapability(), policy.getResourceType());
                if (ancestorPolicy.isPresent()) {
                    OrgGovernancePolicy ap = ancestorPolicy.get();
                    if (coversOrg(ap, policy.getGoverningOrgId(), isDirectParent) && ap.isAllowOverride()) {
                        return;
                    }
                }
            }
        } catch (OrganizationManagementException e) {
            throw new GovernancePolicyMgtServerException(ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getCode(),
                    ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getMessage(),
                    ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED.getDescription(), e);
        }
        throw new GovernancePolicyMgtClientException(ERROR_CODE_OVERRIDE_NOT_PERMITTED.getCode(),
                ERROR_CODE_OVERRIDE_NOT_PERMITTED.getMessage(),
                ERROR_CODE_OVERRIDE_NOT_PERMITTED.getDescription());
    }

    static boolean coversOrg(OrgGovernancePolicy policy, String targetOrgId, boolean isDirectChild) {

        switch (policy.getPolicyType()) {
            case ALL:
                return true;
            case DENY:
                return true;
            case IMMEDIATE:
                return isDirectChild;
            case SELECTED:
                return policy.getSelectedOrgs().stream()
                        .anyMatch(s -> targetOrgId.equals(s.getTargetOrgId()));
            default:
                return false;
        }
    }

    static boolean coversOrg(ResourceGovernancePolicy policy, String targetOrgId, boolean isDirectChild) {

        switch (policy.getPolicyType()) {
            case ALL:
                return true;
            case DENY:
                return true;
            case IMMEDIATE:
                return isDirectChild;
            case SELECTED:
                return policy.getSelectedOrgs().stream()
                        .anyMatch(s -> targetOrgId.equals(s.getTargetOrgId()));
            default:
                return false;
        }
    }

    static boolean effectiveAllowOverride(OrgGovernancePolicy policy, String targetOrgId) {

        if (PolicyType.SELECTED.equals(policy.getPolicyType())) {
            return policy.getSelectedOrgs().stream()
                    .filter(s -> targetOrgId.equals(s.getTargetOrgId()))
                    .findFirst()
                    .map(GovernanceOrgSelected::isAllowOverride)
                    .orElse(false);
        }
        return policy.isAllowOverride();
    }
}

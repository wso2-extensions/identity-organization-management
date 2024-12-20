package org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant;

/**
 * Enum representing the organization scope for which a given policy is shared with.
 */
public enum OrganizationScope {
    EXISTING_ORGS_ONLY,
    EXISTING_ORGS_AND_FUTURE_ORGS_ONLY,
    FUTURE_ORGS_ONLY,
    NO_ORG,
}

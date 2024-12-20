package org.wso2.carbon.identity.organization.resource.sharing.policy.management.model;

/**
 * Model representing the Shared Resource Attribute with respective policies and policy holding orgs.
 */
public class ResourceSharingPolicyWithAttributes {

    private final String policyHoldingOrgId;
    private final ResourceSharingPolicy policy;
    private final SharedResourceAttribute attribute;

    public ResourceSharingPolicyWithAttributes(String policyHoldingOrgId, ResourceSharingPolicy policy,
                                               SharedResourceAttribute attribute) {

        this.policyHoldingOrgId = policyHoldingOrgId;
        this.policy = policy;
        this.attribute = attribute;
    }

    public String getPolicyHoldingOrgId() {

        return policyHoldingOrgId;
    }

    public ResourceSharingPolicy getPolicy() {

        return policy;
    }

    public SharedResourceAttribute getAttribute() {

        return attribute;
    }
}

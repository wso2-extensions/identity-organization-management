/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Enum representing user sharing policies with additional fields for code, name, applicable resources, and details.
 */
public enum PolicyEnum {

    ALL_EXISTING_ORGS_ONLY(
            "GEN-EO-0001",
            "AllExistingOrgsOnly",
            "ALL_EXISTING_ORGS_ONLY",
            Collections.singletonList(ResourceType.USER),
            "This policy applies when the resource needs to be shared with all existing organizations at " +
                    "the current time. Newly created organizations after the policy is applied will not be included " +
                    "under this policy."),
    ALL_EXISTING_AND_FUTURE_ORGS(
            "GEN-EF-0002",
            "AllExistingAndFutureOrgs",
            "ALL_EXISTING_AND_FUTURE_ORGS",
            Collections.singletonList(ResourceType.USER),
            "This policy allows sharing the resource with all current and any future organizations. It " +
                    "ensures that any new organizations created after the policy is set are automatically included."),
    IMMEDIATE_EXISTING_ORGS_ONLY(
            "GEN-EO-0003",
            "ImmediateExistingOrgsOnly",
            "IMMEDIATE_EXISTING_ORGS_ONLY",
            Collections.singletonList(ResourceType.USER),
            "This policy is used to share the resource exclusively with all immediate existing child " +
                    "organizations. Newly created immediate child organizations after the policy is applied are " +
                    "not included."),
    IMMEDIATE_EXISTING_AND_FUTURE_ORGS(
            "GEN-EF-0004",
            "ImmediateExistingAndFutureOrgs",
            "IMMEDIATE_EXISTING_AND_FUTURE_ORGS",
            Collections.singletonList(ResourceType.USER),
            "This policy is used to share the resource exclusively with all immediate existing child " +
                    "organizations. Newly created immediate child organizations after the policy is applied are " +
                    "not included."),
    SELECTED_ORG_ONLY(
            "SEL-EO-0001",
            "SelectedOrgOnly",
            "SELECTED_ORG_ONLY",
            Collections.singletonList(ResourceType.USER),
            "This policy applies when the resource is to be shared with a single, specific organization " +
                    "only. Newly created child organizations under this selected organization will not be included."),
    SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY(
            "SEL-EO-0002",
            "SelectedOrgWithAllExistingChildrenOnly",
            "SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY",
            Collections.singletonList(ResourceType.USER),
            "This policy ensures the resource is shared with a selected organization and all of its " +
                    "existing child organizations. New child organizations created under this selected organization " +
                    "after the policy is applied will not be included."),
    SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN(
            "SEL-EF-0003",
            "SelectedOrgWithAllExistingAndFutureChildren",
            "SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN",
            Collections.singletonList(ResourceType.USER),
            "This policy ensures the resource is shared with a selected organization and all of its child " +
                    "organizations, including those created in the future."),
    SELECTED_ORG_WITH_EXISTING_IMMEDIATE_CHILDREN_ONLY(
            "SEL-EO-0004",
            "SelectedOrgWithExistingImmediateChildrenOnly",
            "SELECTED_ORG_WITH_EXISTING_IMMEDIATE_CHILDREN_ONLY",
            Collections.singletonList(ResourceType.USER),
            "This policy shares the resource with a selected organization and all of its existing " +
                    "immediate child organizations. Newly created immediate children will not be included after " +
                    "the policy is applied."),
    SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN(
            "SEL-EF-0005",
            "SelectedOrgWithExistingImmediateAndFutureChildren",
            "SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN",
            Collections.singletonList(ResourceType.USER),
            "This policy allows sharing the resource with a selected organization and all of its " +
                    "immediate child organizations, including those created in the future."),
    NO_SHARING(
            "NS-0000",
            "NoSharing",
            "NO_SHARING",
            Collections.emptyList(),
            "This policy specifies that no sharing will occur. The resource remains restricted to its " +
                    "current context and is not shared with any organization."
    );

    private final String policyCode;
    private final String policyName;
    private final String value;
    private final List<ResourceType> applicableResources;
    private final String description;

    /**
     * Constructor to initialize the user sharing policy enum.
     *
     * @param policyCode          Unique code representing the sharing policy.
     * @param policyName          Name of the sharing policy.
     * @param value               The value of the sharing policy.
     * @param applicableResources Type of resources to which the policy applies.
     * @param description         Short description of the sharing policy.
     */
    PolicyEnum(String policyCode, String policyName, String value,
               List<ResourceType> applicableResources, String description) {

        this.policyCode = policyCode;
        this.policyName = policyName;
        this.value = value;
        this.applicableResources = applicableResources;
        this.description = description;
    }

    /**
     * Get the unique code of the sharing policy.
     *
     * @return Unique code of the sharing policy.
     */
    public String getPolicyCode() {

        return policyCode;
    }

    /**
     * Get the name of the sharing policy.
     *
     * @return Name of the sharing policy.
     */
    public String getPolicyName() {

        return policyName;
    }

    /**
     * Get the value of the sharing policy.
     *
     * @return Value of the sharing policy.
     */
    public String getValue() {

        return value;
    }

    /**
     * Get the applicable resource type for the sharing policy.
     *
     * @return Types of the applicable resources.
     */
    public List<ResourceType> getApplicableResources() {

        return applicableResources;
    }

    /**
     * Get the short description of the sharing policy.
     *
     * @return Description of the sharing policy.
     */
    public String getDescription() {

        return description;
    }

    /**
     * Get the PolicyEnum based on the given policy code.
     *
     * @param policyCode Code of the sharing policy.
     * @return Corresponding PolicyEnum, wrapped in Optional.
     */
    public static Optional<PolicyEnum> getByPolicyCode(String policyCode) {

        for (PolicyEnum policy : PolicyEnum.values()) {
            if (policy.policyCode.equals(policyCode)) {
                return Optional.of(policy);
            }
        }
        return Optional.empty();
    }

    /**
     * Get the PolicyEnum based on the given policy value.
     *
     * @param value Code of the sharing policy.
     * @return Corresponding PolicyEnum, wrapped in Optional.
     */
    public static Optional<PolicyEnum> getByValue(String value) {

        for (PolicyEnum policy : PolicyEnum.values()) {
            if (policy.value.equals(value)) {
                return Optional.of(policy);
            }
        }
        return Optional.empty();
    }

    /**
     * Validate and get the PolicyEnum based on the given requested policy.
     *
     * @param requestedPolicy Requested policy as an Object (should be an instance of String).
     * @return Corresponding PolicyEnum.
     * @throws IllegalArgumentException if the requested policy is invalid or not found.
     */
    public static PolicyEnum validateAndGetPolicyEnum(String requestedPolicy) {

        for (PolicyEnum policy : PolicyEnum.values()) {
            if (policy.value.equalsIgnoreCase(requestedPolicy) ||
                    policy.policyCode.equalsIgnoreCase(requestedPolicy) ||
                    policy.policyName.equalsIgnoreCase(requestedPolicy)) {
                return policy;
            }
        }
        // Handle the case where no matching policy is found
        throw new IllegalArgumentException("Invalid policy: " + requestedPolicy);
    }

    /**
     * Get the PolicyEnum by matching value.
     *
     * @param value Policy value to match.
     * @return Corresponding PolicyEnum.
     * @throws IllegalArgumentException if the policy value is not found.
     */
    public static PolicyEnum getPolicyByValue(String value) {

        for (PolicyEnum policy : PolicyEnum.values()) {
            if (policy.value.equalsIgnoreCase(value)) {
                return policy;
            }
        }
        throw new IllegalArgumentException("Invalid policy value: " + value);
    }

    /**
     * Get the PolicyEnum by matching policy code.
     *
     * @param policyCode Policy code to match.
     * @return Corresponding PolicyEnum.
     * @throws IllegalArgumentException if the policy code is not found.
     */
    public static PolicyEnum getPolicyByPolicyCode(String policyCode) {

        for (PolicyEnum policy : PolicyEnum.values()) {
            if (policy.policyCode.equalsIgnoreCase(policyCode)) {
                return policy;
            }
        }
        throw new IllegalArgumentException("Invalid policy code: " + policyCode);
    }

    /**
     * Get the PolicyEnum by matching policy name.
     *
     * @param policyName Policy name to match.
     * @return Corresponding PolicyEnum.
     * @throws IllegalArgumentException if the policy name is not found.
     */
    public static PolicyEnum getPolicyByPolicyName(String policyName) {

        for (PolicyEnum policy : PolicyEnum.values()) {
            if (policy.policyName.equalsIgnoreCase(policyName)) {
                return policy;
            }
        }
        throw new IllegalArgumentException("Invalid policy name: " + policyName);
    }
}

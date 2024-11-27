/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.util.TestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.MockitoAnnotations.openMocks;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType.APPLICATION;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType.USER;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.RESOURCE_TYPE_RESOURCE_1;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.RESOURCE_TYPE_RESOURCE_2;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_ORGANIZATION_INVALID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_ORGANIZATION_INVALID_FORMAT;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_ORGANIZATION_ORG_ALL;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_ORGANIZATION_ORG_ALL_CHILD1;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_ORGANIZATION_ORG_IMMEDIATE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_ORGANIZATION_ORG_IMMEDIATE_CHILD1;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_ORGANIZATION_SUPER;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_RESOURCE_1;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_RESOURCE_2;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_RESOURCE_3;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_RESOURCE_4;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_RESOURCE_ATTRIBUTE_1;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.UM_ID_RESOURCE_ATTRIBUTE_2;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.util.TestUtils.closeH2Base;

public class ResourceSharingPolicyHandlerServiceImplTest {

    private ResourceSharingPolicyHandlerServiceImpl resourceSharingPolicyHandlerService;

    @BeforeClass
    public void setUp() throws Exception {

        resourceSharingPolicyHandlerService = new ResourceSharingPolicyHandlerServiceImpl();
        openMocks(this);
        TestUtils.initiateH2Base();
        TestUtils.mockDataSource();
    }

    @AfterClass
    public void tearDown() throws Exception {

        closeH2Base();
    }

    // Tests: CREATE Resource Sharing Policy Records.
    @DataProvider(name = "resourceSharingPolicyProvider")
    public Object[][] resourceSharingPolicyProvider() throws ResourceSharingPolicyMgtException {

        return new Object[][]{
                {new ResourceSharingPolicy.Builder()
                        .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                        .withResourceId(UM_ID_RESOURCE_1)
                        .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                        .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_SUPER)
                        .withSharingPolicy(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS).build()},
                {new ResourceSharingPolicy.Builder()
                        .withResourceType(RESOURCE_TYPE_RESOURCE_2)
                        .withResourceId(UM_ID_RESOURCE_3)
                        .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                        .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_SUPER)
                        .withSharingPolicy(PolicyEnum.IMMEDIATE_EXISTING_AND_FUTURE_ORGS).build()},
                {new ResourceSharingPolicy.Builder()
                        .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                        .withResourceId(UM_ID_RESOURCE_2)
                        .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                        .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_ALL)
                        .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN).build()},
                {new ResourceSharingPolicy.Builder()
                        .withResourceType(RESOURCE_TYPE_RESOURCE_2)
                        .withResourceId(UM_ID_RESOURCE_4)
                        .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                        .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_IMMEDIATE)
                        .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN).build()}
        };

    }

    @Test(dataProvider = "resourceSharingPolicyProvider", priority = 1)
    public void testAddResourceSharingPolicy_success(ResourceSharingPolicy testData) throws Exception {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(testData.getResourceId())
                .withResourceType(testData.getResourceType())
                .withInitiatingOrgId(testData.getInitiatingOrgId())
                .withPolicyHoldingOrgId(testData.getPolicyHoldingOrgId())
                .withSharingPolicy(testData.getSharingPolicy())
                .build();

        int addedPolicyId = resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);

        Assert.assertTrue(addedPolicyId > 0,
                "Expected a positive non-zero integer as the result for a clean record insertion to the database.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 2)
    public void testAddResourceSharingPolicy_failure() throws Exception {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_INVALID)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_ALL)
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN)
                .build();

        resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);
    }

    // Tests: GET Resource Sharing Policy Records.
    @DataProvider(name = "organizationIdsProvider")
    public Object[][] organizationIdsProvider() {

        return new Object[][]{
                {Arrays.asList(UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_ORG_ALL)},
                {Arrays.asList(UM_ID_ORGANIZATION_ORG_IMMEDIATE, UM_ID_ORGANIZATION_ORG_IMMEDIATE_CHILD1)},
                {Arrays.asList(UM_ID_ORGANIZATION_ORG_ALL, UM_ID_ORGANIZATION_ORG_ALL_CHILD1)}
        };
    }

    @Test(dataProvider = "organizationIdsProvider", priority = 3)
    public void testGetResourceSharingPolicies_success(List<String> organizationIds) throws Exception {

        addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        List<ResourceSharingPolicy> actualPolicies =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(organizationIds);

        for (ResourceSharingPolicy policy : actualPolicies) {
            Assert.assertTrue(organizationIds.contains(policy.getPolicyHoldingOrgId()));
        }
    }

    @Test(dataProvider = "organizationIdsProvider", priority = 4)
    public void testGetResourceSharingPoliciesGroupedByResourceType_success(List<String> organizationIds)
            throws Exception {

        addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        Map<ResourceType, List<ResourceSharingPolicy>> actualPoliciesGroupedByType =
                resourceSharingPolicyHandlerService.getResourceSharingPoliciesGroupedByResourceType(organizationIds);

        Assert.assertTrue(actualPoliciesGroupedByType.containsKey(RESOURCE_TYPE_RESOURCE_1),
                "Missing resource type 1");
        Assert.assertTrue(actualPoliciesGroupedByType.containsKey(RESOURCE_TYPE_RESOURCE_2),
                "Missing resource type 2");

        for (Map.Entry<ResourceType, List<ResourceSharingPolicy>> entry : actualPoliciesGroupedByType.entrySet()) {
            ResourceType resourceType = entry.getKey();
            List<ResourceSharingPolicy> policies = entry.getValue();

            switch (resourceType) {
                case USER:
                    for (ResourceSharingPolicy policy : policies) {
                        Assert.assertEquals(policy.getResourceType(), USER);
                        Assert.assertTrue(organizationIds.contains(policy.getPolicyHoldingOrgId()));
                    }
                    break;

                case APPLICATION:
                    for (ResourceSharingPolicy policy : policies) {
                        Assert.assertEquals(policy.getResourceType(), APPLICATION);
                        Assert.assertTrue(organizationIds.contains(policy.getPolicyHoldingOrgId()));
                    }
                    break;

                default:
                    Assert.fail("Unexpected resource type found: " + resourceType);
                    break;
            }
        }
    }

    @Test(dataProvider = "organizationIdsProvider", priority = 5)
    public void testGetResourceSharingPoliciesGroupedByPolicyHoldingOrgId_success(List<String> organizationIds)
            throws Exception {

        addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        Map<String, List<ResourceSharingPolicy>> actualPoliciesGroupedByOrgId =
                resourceSharingPolicyHandlerService.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
                        organizationIds);

        for (String policyHoldingOrgId : actualPoliciesGroupedByOrgId.keySet()) {
            Assert.assertTrue(organizationIds.contains(policyHoldingOrgId));
        }

    }

    @Test(priority = 6)
    public void testGetResourceSharingPolicies_null_success() throws Exception {

        List<ResourceSharingPolicy> actualPolicies =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(null);

        Assert.assertTrue(actualPolicies.isEmpty());
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 7)
    public void testGetResourceSharingPolicies_failure() throws Exception {

        resourceSharingPolicyHandlerService.getResourceSharingPolicies(
                Collections.singletonList(UM_ID_ORGANIZATION_INVALID_FORMAT));
    }

    // Tests: DELETE Resource Sharing Policy Records.
    @Test(dataProvider = "organizationIdsProvider", priority = 8)
    public void testDeleteResourceSharingPolicyRecordById_success(List<String> organizationIds) throws Exception {

        List<Integer> addedResourceSharingPolyIds =
                addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        for (int addedResourceSharingPolyId : addedResourceSharingPolyIds) {
            boolean deleteSuccess = resourceSharingPolicyHandlerService.deleteResourceSharingPolicyRecordById(
                    addedResourceSharingPolyId, UM_ID_ORGANIZATION_SUPER);
            Assert.assertTrue(deleteSuccess, "Failed to delete the resource sharing policy by ID.");
        }
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 9)
    public void testDeleteResourceSharingPolicyRecordById_failure() throws Exception {

        resourceSharingPolicyHandlerService.deleteResourceSharingPolicyRecordById(null, UM_ID_ORGANIZATION_SUPER);
    }

    @DataProvider(name = "organizationsAndResourceTypesAndIdsProvider")
    public Object[][] organizationsAndResourceTypesAndIdsProvider() {

        return new Object[][]{
                {Arrays.asList(UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_ORG_ALL),
                        RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_1},
                {Arrays.asList(UM_ID_ORGANIZATION_ORG_IMMEDIATE, UM_ID_ORGANIZATION_ORG_IMMEDIATE_CHILD1),
                        RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_2},
                {Arrays.asList(UM_ID_ORGANIZATION_ORG_ALL, UM_ID_ORGANIZATION_ORG_ALL_CHILD1),
                        RESOURCE_TYPE_RESOURCE_2, UM_ID_RESOURCE_3},
                {Arrays.asList(UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_SUPER),
                        RESOURCE_TYPE_RESOURCE_2, UM_ID_RESOURCE_4},
        };
    }

    @Test(dataProvider = "organizationsAndResourceTypesAndIdsProvider", priority = 10)
    public void testDeleteResourceSharingPolicyByResourceTypeAndId_success(List<String> organizationIds,
                                                                           ResourceType resourceType,
                                                                           String resourceId) throws Exception {

        addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        // Delete the resource sharing policy by resource type and ID.
        boolean deleteSuccess = resourceSharingPolicyHandlerService.deleteResourceSharingPolicyByResourceTypeAndId(
                resourceType, resourceId, UM_ID_ORGANIZATION_SUPER);
        Assert.assertTrue(deleteSuccess, "Failed to delete the resource sharing policy by resource type and ID.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 11)
    public void testDeleteResourceSharingPolicyByResourceTypeAndId_failure() throws Exception {

        // Attempt to delete a policy with a non-existent resource type and ID.
        resourceSharingPolicyHandlerService.deleteResourceSharingPolicyByResourceTypeAndId(null,
                UM_ID_RESOURCE_1, UM_ID_ORGANIZATION_SUPER);
    }

    // Tests: CREATE Shared Resource Attributes Records.
    @Test(priority = 12)
    public void testAddSharedResourceAttributes_success() throws Exception {

        List<SharedResourceAttribute> sharedResourceAttributes = Arrays.asList(
                new SharedResourceAttribute.Builder()
                        .withResourceSharingPolicyId(1)
                        .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                        .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_1)
                        .build(),
                new SharedResourceAttribute.Builder()
                        .withResourceSharingPolicyId(1)
                        .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                        .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_2)
                        .build());

        boolean isAdded = resourceSharingPolicyHandlerService.addSharedResourceAttributes(sharedResourceAttributes);
        Assert.assertTrue(isAdded, "Failed to add shared resource attributes.");

    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 13)
    public void testAddSharedResourceAttributes_failure() throws Exception {

        List<SharedResourceAttribute> sharedResourceAttributes = Collections.singletonList(
                new SharedResourceAttribute.Builder()
                        .withResourceSharingPolicyId(Integer.MIN_VALUE)
                        .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                        .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_1)
                        .build());

        resourceSharingPolicyHandlerService.addSharedResourceAttributes(sharedResourceAttributes);
    }

    // Tests: GET Shared Resource Attributes Records.
    @Test(priority = 14)
    public void testGetSharedResourceAttributesBySharingPolicyId_success() throws Exception {

        addAndAssertSharedResourceAttributes();
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesBySharingPolicyId(1);
        Assert.assertNotNull(sharedResourceAttributes, "Expected non-null list of shared resource attributes.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 15)
    public void testGetSharedResourceAttributesBySharingPolicyId_failure() throws Exception {

        resourceSharingPolicyHandlerService.getSharedResourceAttributesBySharingPolicyId(null);
    }

    @Test(priority = 16)
    public void testGetSharedResourceAttributesByType_success() throws Exception {

        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesByType(
                        SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1);
        Assert.assertNotNull(sharedResourceAttributes, "Expected non-null list of shared resource attributes by type.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes by type.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 17)
    public void testGetSharedResourceAttributesByType_failure() throws Exception {

        resourceSharingPolicyHandlerService.getSharedResourceAttributesByType(null);
    }

    @Test(priority = 18)
    public void testGetSharedResourceAttributesById_success() throws Exception {

        addAndAssertSharedResourceAttributes();
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesById(UM_ID_RESOURCE_ATTRIBUTE_1);
        Assert.assertNotNull(sharedResourceAttributes, "Expected non-null list of shared resource attributes by ID.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes by ID.");
    }

    @Test(priority = 19)
    public void testGetSharedResourceAttributesByTypeAndId_success() throws Exception {

        addAndAssertSharedResourceAttributes();
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesByTypeAndId(
                        SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1);
        Assert.assertNotNull(sharedResourceAttributes,
                "Expected non-null list of shared resource attributes by type and ID.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes by type and ID.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 20)
    public void testGetSharedResourceAttributesByTypeAndId_failure() throws Exception {

        resourceSharingPolicyHandlerService.getSharedResourceAttributesByTypeAndId(null, null);
    }

    // Tests: DELETE Shared Resource Attributes Records.
    @Test(priority = 21)
    public void testDeleteSharedResourceAttributesByResourceSharingPolicyId_success() throws Exception {

        boolean result = resourceSharingPolicyHandlerService.deleteSharedResourceAttributesByResourceSharingPolicyId
                (1, SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_ORGANIZATION_SUPER);
        Assert.assertTrue(result, "Expected successful deletion of shared resource attributes by policy ID and " +
                "attribute type.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 22)
    public void testDeleteSharedResourceAttributesByResourceSharingPolicyId_failure() throws Exception {

        resourceSharingPolicyHandlerService.deleteSharedResourceAttributesByResourceSharingPolicyId(null,
                SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_ORGANIZATION_SUPER);
    }

    @Test(priority = 23)
    public void testDeleteSharedResourceAttributeByAttributeTypeAndId_success() throws Exception {

        boolean result = resourceSharingPolicyHandlerService.deleteSharedResourceAttributeByAttributeTypeAndId(
                SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1, UM_ID_ORGANIZATION_SUPER);
        Assert.assertTrue(result,
                "Expected successful deletion of shared resource attribute by attribute type and ID.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 24)
    public void testDeleteSharedResourceAttributeByAttributeTypeAndId_failure() throws Exception {

        resourceSharingPolicyHandlerService.deleteSharedResourceAttributeByAttributeTypeAndId
                (null, UM_ID_RESOURCE_1, UM_ID_ORGANIZATION_SUPER);
    }

    // Helpers: Private helper methods for tests.
    private List<Integer> addAndAssertResourceSharingPolicy(List<String> policyHoldingOrgIds)
            throws ResourceSharingPolicyMgtException, OrganizationManagementServerException {

        ResourceSharingPolicy policy1 = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(policyHoldingOrgIds.get(0))
                .withSharingPolicy(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS)
                .build();

        ResourceSharingPolicy policy2 = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_2)
                .withResourceType(RESOURCE_TYPE_RESOURCE_2)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(policyHoldingOrgIds.get(1))
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN)
                .build();

        List<Integer> policyIds = new ArrayList<>();
        List<ResourceSharingPolicy> resourceSharingPolicies = Arrays.asList(policy1, policy2);

        for (ResourceSharingPolicy resourceSharingPolicy : resourceSharingPolicies) {
            int resourceSharingPolicyRecord =
                    resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);
            Assert.assertTrue(resourceSharingPolicyRecord > 0,
                    "Expected a positive non-zero integer as the result for a clean record insertion to " +
                            "the database.");
            policyIds.add(resourceSharingPolicyRecord);
        }
        return policyIds;
    }

    private void addAndAssertSharedResourceAttributes()
            throws ResourceSharingPolicyMgtException, OrganizationManagementServerException {

        List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();
        List<Integer> resourceSharingPolicyIds =
                addAndAssertResourceSharingPolicy(Arrays.asList(UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_ORG_ALL));

        for (int resourceSharingPolicyId : resourceSharingPolicyIds) {
            SharedResourceAttribute sharedResourceAttribute = new SharedResourceAttribute.Builder()
                    .withResourceSharingPolicyId(resourceSharingPolicyId)
                    .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                    .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_1)
                    .build();

            sharedResourceAttributes.add(sharedResourceAttribute);
        }

        boolean isAdded = resourceSharingPolicyHandlerService.addSharedResourceAttributes(sharedResourceAttributes);
        Assert.assertTrue(isAdded, "Failed to add shared resource attributes.");

    }

}

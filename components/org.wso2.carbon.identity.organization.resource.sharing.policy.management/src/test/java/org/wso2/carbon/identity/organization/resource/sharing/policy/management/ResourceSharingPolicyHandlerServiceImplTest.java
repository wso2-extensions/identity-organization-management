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

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.TestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtClientException;
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
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType.USER;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.MOCKED_DATA_ACCESS_EXCEPTION;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.MOCKED_TRANSACTION_EXCEPTION;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constants.TestResourceSharingConstants.RESOURCE_TYPE_RESOURCE_1;
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

/**
 * Unit tests for ResourceSharingPolicyHandlerServiceImpl.
 */
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
                        .withResourceType(RESOURCE_TYPE_RESOURCE_1)
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
                        .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                        .withResourceId(UM_ID_RESOURCE_4)
                        .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                        .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_IMMEDIATE)
                        .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN).build()}
        };

    }

    @Test(dataProvider = "resourceSharingPolicyProvider", priority = 1)
    public void testAddResourceSharingPolicySuccess(ResourceSharingPolicy testData) throws Exception {

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
    public void testAddResourceSharingPolicyFailure() throws Exception {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_INVALID)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_ALL)
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN)
                .build();

        resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 3)
    public void testAddResourceSharingPolicyFailureForNotApplicableResource() throws Exception {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_INVALID)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_ALL)
                .withSharingPolicy(PolicyEnum.NO_SHARING)
                .build();

        resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);
    }

    // Tests: GET Resource Sharing Policy Records.
    @Test(priority = 4)
    public void testGetResourceSharingPolicyByIdSuccess() throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_ALL)
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_ONLY)
                .build();

        int recordId = resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);
        Optional<ResourceSharingPolicy> optionalResource =
                resourceSharingPolicyHandlerService.getResourceSharingPolicyById(recordId);

        Assert.assertTrue(optionalResource.isPresent(), "ResourceSharingPolicy should be present.");
        ResourceSharingPolicy resource = optionalResource.get();
        Assert.assertNotNull(resource, "ResourceSharingPolicy object should not be null.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 5)
    public void testGetResourceSharingPolicyByIdFailure() throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .fetchSingleRecord(anyString(), any(), any());

            resourceSharingPolicyHandlerService.getResourceSharingPolicyById(1);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @DataProvider(name = "organizationIdsProvider")
    public Object[][] organizationIdsProvider() {

        return new Object[][]{
                {Arrays.asList(UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_ORG_ALL)},
                {Arrays.asList(UM_ID_ORGANIZATION_ORG_IMMEDIATE, UM_ID_ORGANIZATION_ORG_IMMEDIATE_CHILD1)},
                {Arrays.asList(UM_ID_ORGANIZATION_ORG_ALL, UM_ID_ORGANIZATION_ORG_ALL_CHILD1)}
        };
    }

    @Test(dataProvider = "organizationIdsProvider", priority = 6)
    public void testGetResourceSharingPoliciesSuccess(List<String> organizationIds) throws Exception {

        addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        List<ResourceSharingPolicy> actualPolicies =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(organizationIds);

        for (ResourceSharingPolicy policy : actualPolicies) {
            Assert.assertTrue(organizationIds.contains(policy.getPolicyHoldingOrgId()));
        }
    }

    @Test(dataProvider = "organizationIdsProvider", priority = 7)
    public void testGetResourceSharingPoliciesGroupedByResourceTypeSuccess(List<String> organizationIds)
            throws Exception {

        addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        Map<ResourceType, List<ResourceSharingPolicy>> actualPoliciesGroupedByType =
                resourceSharingPolicyHandlerService.getResourceSharingPoliciesGroupedByResourceType(organizationIds);

        Assert.assertTrue(actualPoliciesGroupedByType.containsKey(RESOURCE_TYPE_RESOURCE_1));
        Assert.assertTrue(actualPoliciesGroupedByType.containsKey(RESOURCE_TYPE_RESOURCE_1));

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

                default:
                    Assert.fail("Unexpected resource type found: " + resourceType);
                    break;
            }
        }
    }

    @Test(dataProvider = "organizationIdsProvider", priority = 8)
    public void testGetResourceSharingPoliciesGroupedByPolicyHoldingOrgIdSuccess(List<String> organizationIds)
            throws Exception {

        addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        Map<String, List<ResourceSharingPolicy>> actualPoliciesGroupedByOrgId =
                resourceSharingPolicyHandlerService.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
                        organizationIds);

        for (String policyHoldingOrgId : actualPoliciesGroupedByOrgId.keySet()) {
            Assert.assertTrue(organizationIds.contains(policyHoldingOrgId));
        }

    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 9)
    public void testGetResourceSharingPoliciesFailureForNullOrgIdInList() throws Exception {

        List<ResourceSharingPolicy> actualPolicies =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(Collections.singletonList(null));

        Assert.assertTrue(actualPolicies.isEmpty());
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 10)
    public void testGetResourceSharingPoliciesFailureForEmptyOrgIdInList() throws Exception {

        List<ResourceSharingPolicy> actualPolicies =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(Collections.singletonList(""));

        Assert.assertTrue(actualPolicies.isEmpty());
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 11)
    public void testGetResourceSharingPoliciesFailureForInvalidOrgId() throws Exception {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeQuery(anyString(), any(), any());

            resourceSharingPolicyHandlerService.getResourceSharingPolicies(
                    Collections.singletonList(UM_ID_ORGANIZATION_INVALID_FORMAT));

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 12)
    public void testGetResourceSharingPoliciesFailureForNullOrgs() throws Exception {

        List<String> policyHoldingOrgIds = new ArrayList<>();
        policyHoldingOrgIds.add(UM_ID_RESOURCE_1);
        policyHoldingOrgIds.add(null);

        List<ResourceSharingPolicy> actualPolicies =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(policyHoldingOrgIds);

        Assert.assertTrue(actualPolicies.isEmpty());
    }

    // Tests: DELETE Resource Sharing Policy Records.
    @Test(dataProvider = "organizationIdsProvider", priority = 13)
    public void testDeleteResourceSharingPolicyRecordByIdSuccess(List<String> organizationIds) throws Exception {

        List<Integer> addedResourceSharingPolyIds =
                addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        for (int addedResourceSharingPolyId : addedResourceSharingPolyIds) {
            resourceSharingPolicyHandlerService.deleteResourceSharingPolicyRecordById(
                    addedResourceSharingPolyId, UM_ID_ORGANIZATION_SUPER);
            Optional<ResourceSharingPolicy> resourceSharingPolicyById =
                    resourceSharingPolicyHandlerService.getResourceSharingPolicyById(addedResourceSharingPolyId);
            Assert.assertFalse(resourceSharingPolicyById.isPresent());
        }
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 14)
    public void testDeleteResourceSharingPolicyRecordByIdFailure()
            throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeUpdate(anyString(), any());

            resourceSharingPolicyHandlerService.deleteResourceSharingPolicyRecordById(1, UM_ID_ORGANIZATION_SUPER);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 15)
    public void testDeleteResourceSharingPolicyRecordByIdAndEmptySharingInitiatedOrgIdFailure()
            throws ResourceSharingPolicyMgtException {

        resourceSharingPolicyHandlerService.deleteResourceSharingPolicyRecordById(1, "");
    }

    @Test(priority = 16)
    public void testDeleteResourceSharingPolicyByResourceTypeAndIdSuccess() throws Exception {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN)
                .build();

        int addedPolicyId = resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);

        // Deleting the added policy
        resourceSharingPolicyHandlerService.deleteResourceSharingPolicyByResourceTypeAndId(
                RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_1, UM_ID_ORGANIZATION_SUPER);

        // Verifying that the policy is deleted
        Optional<ResourceSharingPolicy> deletedPolicy =
                resourceSharingPolicyHandlerService.getResourceSharingPolicyById(addedPolicyId);
        Assert.assertFalse(deletedPolicy.isPresent());
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 17)
    public void testDeleteResourceSharingPolicyByResourceTypeAndIdFailure() throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeUpdate(anyString(), any());

            resourceSharingPolicyHandlerService.deleteResourceSharingPolicyByResourceTypeAndId(RESOURCE_TYPE_RESOURCE_1,
                    UM_ID_RESOURCE_1, UM_ID_ORGANIZATION_SUPER);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(priority = 18)
    public void testDeleteResourceSharingPolicyInOrgByResourceTypeAndIdSuccess() throws Exception {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN)
                .build();

        int addedPolicyId = resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);

        // Deleting the added policy
        resourceSharingPolicyHandlerService.deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                UM_ID_ORGANIZATION_SUPER, RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_1, UM_ID_ORGANIZATION_SUPER);

        // Verifying that the policy is deleted
        Optional<ResourceSharingPolicy> deletedPolicy =
                resourceSharingPolicyHandlerService.getResourceSharingPolicyById(addedPolicyId);
        Assert.assertFalse(deletedPolicy.isPresent());
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 19)
    public void testDeleteResourceSharingPolicyInOrgByResourceTypeAndIdFailure()
            throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeUpdate(anyString(), any());

            resourceSharingPolicyHandlerService.deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                    UM_ID_ORGANIZATION_SUPER, RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_1, UM_ID_ORGANIZATION_SUPER);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    // Tests: CREATE Shared Resource Attributes Records.
    @Test(priority = 20)
    public void testAddSharedResourceAttributesSuccess() throws Exception {

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
        Assert.assertTrue(isAdded, "Added shared resource attributes.");

    }

    @Test(priority = 21)
    public void testAddSharedResourceAttributesSuccessWithIgnoringInvalidAttributes() throws Exception {

        List<SharedResourceAttribute> sharedResourceAttributes = Arrays.asList(
                new SharedResourceAttribute.Builder()
                        .withResourceSharingPolicyId(Integer.MIN_VALUE)
                        .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                        .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_1)
                        .build(),
                new SharedResourceAttribute.Builder()
                        .withResourceSharingPolicyId(1)
                        .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                        .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_2)
                        .build());

        boolean isAdded = resourceSharingPolicyHandlerService.addSharedResourceAttributes(sharedResourceAttributes);
        Assert.assertTrue(isAdded, "Skipped the invalid attributes and added the rest.");

    }

    @Test(priority = 22)
    public void testAddSharedResourceAttributesSuccessWithInapplicableAttribute() throws Exception {

        ResourceSharingPolicy resourceSharingPolicyMock = mock(ResourceSharingPolicy.class);
        ResourceType resourceTypeMock = mock(ResourceType.class);

        when(resourceSharingPolicyMock.getResourceType()).thenReturn(resourceTypeMock);
        when(resourceTypeMock.isApplicableAttributeType(any(SharedAttributeType.class))).thenReturn(false);

        // Valid attribute
        SharedResourceAttribute validSharedResourceAttribute = new SharedResourceAttribute.Builder()
                .withResourceSharingPolicyId(1)
                .withSharedAttributeType(SharedAttributeType.ROLE)
                .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_1)
                .build();

        // Invalid attribute
        SharedResourceAttribute invalidSharedResourceAttribute = new SharedResourceAttribute.Builder()
                .withResourceSharingPolicyId(-1)
                .withSharedAttributeType(SharedAttributeType.ROLE)
                .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_2)
                .build();

        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerServiceMock =
                spy(new ResourceSharingPolicyHandlerServiceImpl());

        doReturn(Optional.of(resourceSharingPolicyMock)).when(resourceSharingPolicyHandlerServiceMock)
                .getResourceSharingPolicyById(validSharedResourceAttribute.getResourceSharingPolicyId());

        doReturn(Optional.empty()).when(resourceSharingPolicyHandlerServiceMock)
                .getResourceSharingPolicyById(invalidSharedResourceAttribute.getResourceSharingPolicyId());

        List<SharedResourceAttribute> sharedResourceAttributes =
                Arrays.asList(validSharedResourceAttribute, invalidSharedResourceAttribute);

        boolean isAdded = resourceSharingPolicyHandlerServiceMock.addSharedResourceAttributes(sharedResourceAttributes);

        Assert.assertTrue(isAdded, "Skipped the inapplicable attributes and added the rest.");
        verify(resourceSharingPolicyHandlerServiceMock, times(1))
                .getResourceSharingPolicyById(validSharedResourceAttribute.getResourceSharingPolicyId());
        verify(resourceSharingPolicyHandlerServiceMock, times(1))
                .getResourceSharingPolicyById(invalidSharedResourceAttribute.getResourceSharingPolicyId());
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 23)
    public void testAddSharedResourceAttributesFailure() throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new TransactionException(MOCKED_TRANSACTION_EXCEPTION)).when(mockJdbcTemplate)
                    .withTransaction(any());

            resourceSharingPolicyHandlerService.addSharedResourceAttributes(
                    Collections.singletonList(new SharedResourceAttribute()));

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (TransactionException e) {
            throw new TestException(e);
        }
    }

    // Tests: GET Shared Resource Attributes Records.
    @Test(priority = 24)
    public void testGetSharedResourceAttributesBySharingPolicyIdSuccess() throws Exception {

        addAndAssertSharedResourceAttributes();
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesBySharingPolicyId(1);
        Assert.assertNotNull(sharedResourceAttributes,
                "Expected non-null list of shared resource attributes.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 25)
    public void testGetSharedResourceAttributesBySharingPolicyIdFailure() throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeQuery(anyString(), any(), any());

            resourceSharingPolicyHandlerService.getSharedResourceAttributesBySharingPolicyId(1);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(priority = 26)
    public void testGetSharedResourceAttributesByTypeSuccess() throws Exception {

        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesByType(
                        SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1);
        Assert.assertNotNull(sharedResourceAttributes,
                "Expected non-null list of shared resource attributes by type.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes by type.");
    }

    @Test(priority = 27)
    public void testGetSharedResourceAttributesByIdSuccess() throws Exception {

        addAndAssertSharedResourceAttributes();
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesById(UM_ID_RESOURCE_ATTRIBUTE_1);
        Assert.assertNotNull(sharedResourceAttributes,
                "Expected non-null list of shared resource attributes by ID.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes by ID.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 28)
    public void testGetSharedResourceAttributesByIdFailureForNull() throws Exception {

        resourceSharingPolicyHandlerService.getSharedResourceAttributesById(null);
    }

    @Test(priority = 29)
    public void testGetSharedResourceAttributesByTypeAndIdSuccess() throws Exception {

        addAndAssertSharedResourceAttributes();
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesByTypeAndId(
                        SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1);
        Assert.assertNotNull(sharedResourceAttributes,
                "Expected non-null list of shared resource attributes by type and ID.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes by type and ID.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 30)
    public void testGetSharedResourceAttributesByTypeAndIdFailureForNull() throws Exception {

        resourceSharingPolicyHandlerService.getSharedResourceAttributesByTypeAndId(null, null);
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 31)
    public void testGetSharedResourceAttributesByTypeAndIdFailure()
            throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeQuery(anyString(), any(), any());

            resourceSharingPolicyHandlerService.getSharedResourceAttributesByTypeAndId(
                    SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    // Tests: DELETE Shared Resource Attributes Records.
    @Test(priority = 32)
    public void testDeleteSharedResourceAttributesByResourceSharingPolicyIdSuccess() throws Exception {

        resourceSharingPolicyHandlerService.deleteSharedResourceAttributesByResourceSharingPolicyId
                (1, SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_ORGANIZATION_SUPER);
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesBySharingPolicyId(1);
        Assert.assertEquals(sharedResourceAttributes.size(), 0);
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 33)
    public void testDeleteSharedResourceAttributesByResourceSharingPolicyIdFailure()
            throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeUpdate(anyString(), any());

            resourceSharingPolicyHandlerService.deleteSharedResourceAttributesByResourceSharingPolicyId(1,
                    SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_ORGANIZATION_SUPER);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(priority = 34)
    public void testDeleteSharedResourceAttributeByAttributeTypeAndIdSuccess() throws Exception {

        resourceSharingPolicyHandlerService.deleteSharedResourceAttributeByAttributeTypeAndId(
                SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1, UM_ID_ORGANIZATION_SUPER);
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesByTypeAndId(
                        SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1);
        Assert.assertEquals(sharedResourceAttributes.size(), 0);
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 35)
    public void testDeleteSharedResourceAttributeByAttributeTypeAndIdFailure()
            throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeUpdate(anyString(), any());

            resourceSharingPolicyHandlerService.deleteSharedResourceAttributeByAttributeTypeAndId(
                    SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_1, UM_ID_ORGANIZATION_SUPER);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(priority = 36)
    public void testAddResourceSharingPolicyWithAttributesSuccess() throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_ALL)
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN)
                .build();

        List<SharedResourceAttribute> sharedResourceAttributes = Arrays.asList(
                new SharedResourceAttribute.Builder()
                        .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                        .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_1)
                        .build(),
                new SharedResourceAttribute.Builder()
                        .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                        .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_2)
                        .build());

        boolean result =
                resourceSharingPolicyHandlerService.addResourceSharingPolicyWithAttributes(resourceSharingPolicy,
                        sharedResourceAttributes);

        Assert.assertTrue(result, "Expected the method to successfully add valid attributes.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 37)
    public void testAddResourceSharingPolicyWithAttributesFailure() throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_ORG_ALL)
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN)
                .build();

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new TransactionException(MOCKED_TRANSACTION_EXCEPTION)).when(mockJdbcTemplate)
                    .withTransaction(any());

            resourceSharingPolicyHandlerService.addResourceSharingPolicyWithAttributes(resourceSharingPolicy,
                    Collections.singletonList(new SharedResourceAttribute()));

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (TransactionException e) {
            throw new TestException(e);
        }
    }

    @Test(priority = 38)
    public void testGetResourceSharingPoliciesWithSharedAttributesSuccess() throws Exception {
        // Prepare mock data
        List<String> policyHoldingOrganizationIds = Arrays.asList(
                UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_ORG_ALL);

        SharedResourceAttribute attribute1 = new SharedResourceAttribute.Builder()
                .withResourceSharingPolicyId(1)
                .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_1)
                .build();

        SharedResourceAttribute attribute2 = new SharedResourceAttribute.Builder()
                .withResourceSharingPolicyId(2)
                .withSharedAttributeType(SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1)
                .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_2)
                .build();

        List<SharedResourceAttribute> sharedAttributes = Arrays.asList(attribute1, attribute2);
        boolean attributesAdded = resourceSharingPolicyHandlerService.addSharedResourceAttributes(sharedAttributes);
        Assert.assertTrue(attributesAdded, "Shared resource attributes were not added successfully.");

        Map<String, Map<ResourceSharingPolicy, List<SharedResourceAttribute>>> result =
                resourceSharingPolicyHandlerService.getResourceSharingPoliciesWithSharedAttributes(
                        policyHoldingOrganizationIds);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), policyHoldingOrganizationIds.size(),
                "Grouped result size does not match the number of policy holding organizations.");

        for (String policyHoldingOrgId : policyHoldingOrganizationIds) {
            Assert.assertTrue(result.containsKey(policyHoldingOrgId),
                    "Grouped result does not contain expected organization ID: " + policyHoldingOrgId);

            Map<ResourceSharingPolicy, List<SharedResourceAttribute>> policyMap = result.get(policyHoldingOrgId);

            for (Map.Entry<ResourceSharingPolicy, List<SharedResourceAttribute>> entry : policyMap.entrySet()) {
                ResourceSharingPolicy policy = entry.getKey();
                List<SharedResourceAttribute> attributes = entry.getValue();

                Assert.assertNotNull(policy, "Resource sharing policy should not be null.");
                Assert.assertNotNull(attributes, "Shared resource attributes list should not be null.");
            }
        }
    }

    @Test(priority = 39)
    public void testGetResourceSharingPoliciesWithSharedAttributesEmptyResult() throws Exception {

        List<String> policyHoldingOrganizationIds = Collections.singletonList(UM_ID_ORGANIZATION_INVALID);

        Map<String, Map<ResourceSharingPolicy, List<SharedResourceAttribute>>> result =
                resourceSharingPolicyHandlerService.getResourceSharingPoliciesWithSharedAttributes(
                        policyHoldingOrganizationIds);

        Assert.assertEquals(result.size(), 0);
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 40)
    public void testGetResourceSharingPoliciesWithSharedAttributesFailure() throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeQuery(anyString(), any(), any());

            resourceSharingPolicyHandlerService.getResourceSharingPoliciesWithSharedAttributes(
                    Arrays.asList(UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_ORG_ALL));

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(priority = 38)
    public void testDeleteResourceSharingPolicyByResourceTypeAndIdForResourceDeletionSuccess() throws Exception {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withSharingPolicy(PolicyEnum.SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN)
                .build();

        int addedPolicyId = resourceSharingPolicyHandlerService.addResourceSharingPolicy(resourceSharingPolicy);

        // Deleting the added policy
        resourceSharingPolicyHandlerService.deleteResourceSharingPolicyByResourceTypeAndId(
                RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_1);

        // Verifying that the policy is deleted
        Optional<ResourceSharingPolicy> deletedPolicy =
                resourceSharingPolicyHandlerService.getResourceSharingPolicyById(addedPolicyId);
        Assert.assertFalse(deletedPolicy.isPresent());
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 39)
    public void testDeleteResourceSharingPolicyByResourceTypeAndIdForResourceDeletionFailure()
            throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeUpdate(anyString(), any());

            resourceSharingPolicyHandlerService.deleteResourceSharingPolicyByResourceTypeAndId(RESOURCE_TYPE_RESOURCE_1,
                    UM_ID_RESOURCE_1);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(priority = 40)
    public void testDeleteSharedResourceAttributeByAttributeTypeAndIdForAttributeDeletionSuccess() throws Exception {

        resourceSharingPolicyHandlerService.deleteSharedResourceAttributeByAttributeTypeAndId(
                SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1);
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesByTypeAndId(
                        SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1);
        Assert.assertEquals(sharedResourceAttributes.size(), 0);
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 41)
    public void testDeleteSharedResourceAttributeByAttributeTypeAndIdForAttributeDeletionFailure()
            throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);
            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeUpdate(anyString(), any());

            resourceSharingPolicyHandlerService.deleteSharedResourceAttributeByAttributeTypeAndId(
                    SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_1);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    @Test(priority = 42)
    public void testDeleteResourceSharingPoliciesAndAttributesByOrganizationIdSuccess() throws Exception {

        // Fetch the policies before deletion and collect their IDs.
        List<ResourceSharingPolicy> resourceSharingPoliciesBeforeDelete =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(
                        Collections.singletonList(UM_ID_ORGANIZATION_ORG_ALL));
        List<Integer> resourceSharingPolicyIdsBeforeDelete = resourceSharingPoliciesBeforeDelete.stream()
                .map(ResourceSharingPolicy::getResourceSharingPolicyId)
                .collect(Collectors.toList());

        // Call the method to delete policies and attributes for the given orgId.
        resourceSharingPolicyHandlerService.deleteResourceSharingPoliciesAndAttributesByOrganizationId(
                UM_ID_ORGANIZATION_ORG_ALL);

        // Verify that all policies for the orgId are deleted using getResourceSharingPolicyById().
        resourceSharingPolicyIdsBeforeDelete.forEach(policyId -> {
            Optional<ResourceSharingPolicy> resourceSharingPolicy;
            try {
                resourceSharingPolicy = resourceSharingPolicyHandlerService.getResourceSharingPolicyById(policyId);
            } catch (ResourceSharingPolicyMgtException e) {
                throw new RuntimeException(e);
            }
            Assert.assertFalse(resourceSharingPolicy.isPresent(),
                    "Resource sharing policy was not deleted successfully for policy ID: " + policyId);
        });

        // Verify that all attributes related to the deleted policies are also deleted.
        resourceSharingPolicyIdsBeforeDelete.forEach(policyId -> {
            List<SharedResourceAttribute> sharedResourceAttributes;
            try {
                sharedResourceAttributes =
                        resourceSharingPolicyHandlerService.getSharedResourceAttributesBySharingPolicyId(policyId);
            } catch (ResourceSharingPolicyMgtException e) {
                throw new TestException(e);
            }
            Assert.assertTrue(sharedResourceAttributes.isEmpty(),
                    "Shared resource attributes were not deleted successfully for policy ID: " + policyId);
        });
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 43)
    public void testDeleteResourceSharingPoliciesAndAttributesByOrganizationIdFailure()
            throws ResourceSharingPolicyMgtException {

        NamedJdbcTemplate mockJdbcTemplate = mock(NamedJdbcTemplate.class);

        try (MockedStatic<Utils> mockedStatic = mockStatic(Utils.class)) {
            mockedStatic.when(Utils::getNewTemplate).thenReturn(mockJdbcTemplate);

            doThrow(new DataAccessException(MOCKED_DATA_ACCESS_EXCEPTION)).when(mockJdbcTemplate)
                    .executeUpdate(anyString(), any());

            resourceSharingPolicyHandlerService.deleteResourceSharingPoliciesAndAttributesByOrganizationId(
                    UM_ID_ORGANIZATION_ORG_ALL);

            mockedStatic.verify(Utils::getNewTemplate, times(1));
        } catch (DataAccessException e) {
            throw new TestException(e);
        }
    }

    // Helpers: Private helper methods for tests.
    private List<Integer> addAndAssertResourceSharingPolicy(List<String> policyHoldingOrgIds)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicy policy1 = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_1)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
                .withInitiatingOrgId(UM_ID_ORGANIZATION_SUPER)
                .withPolicyHoldingOrgId(policyHoldingOrgIds.get(0))
                .withSharingPolicy(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS)
                .build();

        ResourceSharingPolicy policy2 = new ResourceSharingPolicy.Builder()
                .withResourceId(UM_ID_RESOURCE_2)
                .withResourceType(RESOURCE_TYPE_RESOURCE_1)
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
                    "Expected a positive non-zero integer as the result for a clean record insertion " +
                            "to the database.");
            policyIds.add(resourceSharingPolicyRecord);
        }
        return policyIds;
    }

    private void addAndAssertSharedResourceAttributes()
            throws ResourceSharingPolicyMgtException {

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
        Assert.assertTrue(isAdded, "Added shared resource attributes.");

    }
}

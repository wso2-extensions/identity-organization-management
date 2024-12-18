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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
        ResourceSharingPolicy resource = resourceSharingPolicyHandlerService.getResourceSharingPolicyById(recordId);

        Assert.assertNotNull(resource);
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
    public void testGetResourceSharingPoliciesFailureForNullOrgIdList() throws Exception {

        List<ResourceSharingPolicy> actualPolicies =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(null);

        Assert.assertTrue(actualPolicies.isEmpty());
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 10)
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

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 11)
    public void testGetResourceSharingPoliciesFailureForNullOrgs() throws Exception {

        List<String> policyHoldingOrgIds = new ArrayList<>();
        policyHoldingOrgIds.add(UM_ID_RESOURCE_1);
        policyHoldingOrgIds.add(null);

        List<ResourceSharingPolicy> actualPolicies =
                resourceSharingPolicyHandlerService.getResourceSharingPolicies(policyHoldingOrgIds);

        Assert.assertTrue(actualPolicies.isEmpty());
    }

    // Tests: DELETE Resource Sharing Policy Records.
    @Test(dataProvider = "organizationIdsProvider", priority = 12)
    public void testDeleteResourceSharingPolicyRecordByIdSuccess(List<String> organizationIds) throws Exception {

        List<Integer> addedResourceSharingPolyIds =
                addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        for (int addedResourceSharingPolyId : addedResourceSharingPolyIds) {
            boolean deleteSuccess = resourceSharingPolicyHandlerService.deleteResourceSharingPolicyRecordById(
                    addedResourceSharingPolyId, UM_ID_ORGANIZATION_SUPER);
            Assert.assertTrue(deleteSuccess);
        }
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 13)
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

    @DataProvider(name = "organizationsAndResourceTypesAndIdsProvider")
    public Object[][] organizationsAndResourceTypesAndIdsProvider() {

        return new Object[][]{
                {Arrays.asList(UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_ORG_ALL),
                        RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_1},
                {Arrays.asList(UM_ID_ORGANIZATION_ORG_IMMEDIATE, UM_ID_ORGANIZATION_ORG_IMMEDIATE_CHILD1),
                        RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_2},
                {Arrays.asList(UM_ID_ORGANIZATION_ORG_ALL, UM_ID_ORGANIZATION_ORG_ALL_CHILD1),
                        RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_3},
                {Arrays.asList(UM_ID_ORGANIZATION_SUPER, UM_ID_ORGANIZATION_SUPER),
                        RESOURCE_TYPE_RESOURCE_1, UM_ID_RESOURCE_4},
        };
    }

    @Test(dataProvider = "organizationsAndResourceTypesAndIdsProvider", priority = 14)
    public void testDeleteResourceSharingPolicyByResourceTypeAndIdSuccess(List<String> organizationIds,
                                                                          ResourceType resourceType, String resourceId)
            throws Exception {

        addAndAssertResourceSharingPolicy(Arrays.asList(organizationIds.get(0), organizationIds.get(1)));

        // Delete the resource sharing policy by resource type and ID.
        boolean deleteSuccess =
                resourceSharingPolicyHandlerService.deleteResourceSharingPolicyByResourceTypeAndId(resourceType,
                        resourceId, UM_ID_ORGANIZATION_SUPER);
        Assert.assertTrue(deleteSuccess);
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 15)
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

    // Tests: CREATE Shared Resource Attributes Records.
    @Test(priority = 16)
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

    @Test(priority = 17)
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

    @Test(priority = 18)
    public void testAddSharedResourceAttributesSuccessWithInapplicableAttribute() throws Exception {

        ResourceSharingPolicy resourceSharingPolicyMock = mock(ResourceSharingPolicy.class);
        ResourceType resourceTypeMock = mock(ResourceType.class);

        when(resourceSharingPolicyMock.getResourceType()).thenReturn(resourceTypeMock);
        when(resourceTypeMock.isApplicableAttributeType(any(SharedAttributeType.class))).thenReturn(false);

        SharedResourceAttribute sharedResourceAttribute = new SharedResourceAttribute.Builder()
                .withResourceSharingPolicyId(1)
                .withSharedAttributeType(SharedAttributeType.ROLE)
                .withSharedAttributeId(UM_ID_RESOURCE_ATTRIBUTE_1)
                .build();

        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerServiceMock =
                spy(new ResourceSharingPolicyHandlerServiceImpl());

        doReturn(resourceSharingPolicyMock).when(resourceSharingPolicyHandlerServiceMock)
                .getResourceSharingPolicyById(sharedResourceAttribute.getResourceSharingPolicyId());

        List<SharedResourceAttribute> sharedResourceAttributes = Collections.singletonList(sharedResourceAttribute);

        boolean isAdded = resourceSharingPolicyHandlerServiceMock.addSharedResourceAttributes(sharedResourceAttributes);

        Assert.assertTrue(isAdded, "Skipped the inapplicable attributes and added the rest.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 19)
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
    @Test(priority = 20)
    public void testGetSharedResourceAttributesBySharingPolicyIdSuccess() throws Exception {

        addAndAssertSharedResourceAttributes();
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesBySharingPolicyId(1);
        Assert.assertNotNull(sharedResourceAttributes,
                "Expected non-null list of shared resource attributes.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 21)
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

    @Test(priority = 22)
    public void testGetSharedResourceAttributesByTypeSuccess() throws Exception {

        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesByType(
                        SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1);
        Assert.assertNotNull(sharedResourceAttributes,
                "Expected non-null list of shared resource attributes by type.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes by type.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 23)
    public void testGetSharedResourceAttributesByTypeFailureForNull() throws Exception {

        resourceSharingPolicyHandlerService.getSharedResourceAttributesByType(null);
    }

    @Test(priority = 24)
    public void testGetSharedResourceAttributesByIdSuccess() throws Exception {

        addAndAssertSharedResourceAttributes();
        List<SharedResourceAttribute> sharedResourceAttributes =
                resourceSharingPolicyHandlerService.getSharedResourceAttributesById(UM_ID_RESOURCE_ATTRIBUTE_1);
        Assert.assertNotNull(sharedResourceAttributes,
                "Expected non-null list of shared resource attributes by ID.");
        Assert.assertFalse(sharedResourceAttributes.isEmpty(),
                "Expected non-empty list of shared resource attributes by ID.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 25)
    public void testGetSharedResourceAttributesByIdFailureForNull() throws Exception {

        resourceSharingPolicyHandlerService.getSharedResourceAttributesById(null);
    }

    @Test(priority = 26)
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

    @Test(expectedExceptions = ResourceSharingPolicyMgtClientException.class, priority = 27)
    public void testGetSharedResourceAttributesByTypeAndIdFailureForNull() throws Exception {

        resourceSharingPolicyHandlerService.getSharedResourceAttributesByTypeAndId(null, null);
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 28)
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
    @Test(priority = 29)
    public void testDeleteSharedResourceAttributesByResourceSharingPolicyIdSuccess() throws Exception {

        boolean result = resourceSharingPolicyHandlerService.deleteSharedResourceAttributesByResourceSharingPolicyId
                (1, SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_ORGANIZATION_SUPER);
        Assert.assertTrue(result,
                "Expected successful deletion of shared resource attributes by policy ID and attribute type.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 30)
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

    @Test(priority = 31)
    public void testDeleteSharedResourceAttributeByAttributeTypeAndIdSuccess() throws Exception {

        boolean result = resourceSharingPolicyHandlerService.deleteSharedResourceAttributeByAttributeTypeAndId(
                SHARED_ATTRIBUTE_TYPE_RESOURCE_ATTRIBUTE_1, UM_ID_RESOURCE_ATTRIBUTE_1, UM_ID_ORGANIZATION_SUPER);
        Assert.assertTrue(result,
                "Expected successful deletion of shared resource attribute by attribute type and ID.");
    }

    @Test(expectedExceptions = ResourceSharingPolicyMgtServerException.class, priority = 32)
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

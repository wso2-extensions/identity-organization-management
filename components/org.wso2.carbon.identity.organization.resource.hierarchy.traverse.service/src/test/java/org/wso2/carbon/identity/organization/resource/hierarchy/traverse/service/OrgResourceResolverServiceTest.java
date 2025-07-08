/*
 * Copyright (c) 2024-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service;

import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseServerException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.internal.OrgResourceHierarchyTraverseServiceDataHolder;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.mock.resource.impl.MockResourceManagementService;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.mock.resource.impl.model.MockResource;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.AggregationStrategy;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.FirstFoundAggregationStrategy;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.MergeAllAggregationStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

/**
 * Unit tests for the OrgResourceResolverService.
 */
public class OrgResourceResolverServiceTest {

    private static final String ROOT_ORG_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String L1_ORG_ID = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final String L2_ORG_ID = "30b701c6-e309-4241-b047-0c299c45d1a0";
    private static final String INVALID_ORG_ID = "invalid-org-id";
    private static final String INVALID_APP_ID = "invalid-app-id";

    private OrgResourceResolverService orgResourceResolverService;
    private MockResourceManagementService mockResourceManagementService;
    private AggregationStrategy<MockResource> firstFoundAggregationStrategy;
    private AggregationStrategy<MockResource> mergeAllAggregationStrategy;

    @Mock
    OrganizationManager organizationManager;

    /**
     * Initializes test data and mock services before the test class is run.
     * This method sets up necessary services and aggregation strategies required for the tests.
     */
    @BeforeClass
    public void init() {

        // Open mock objects for the current test instance.
        openMocks(this);

        // Set the OrganizationManager and ApplicationManagementService to the data holder for use in tests
        OrgResourceHierarchyTraverseServiceDataHolder.getInstance().setOrganizationManager(organizationManager);

        // Initialize the aggregation strategies with the appropriate strategy types.
        firstFoundAggregationStrategy = new FirstFoundAggregationStrategy<>();
        mergeAllAggregationStrategy = new MergeAllAggregationStrategy<>(this::resourceMerger);
    }

    /**
     * Sets up mocks for individual test cases by initializing mock services and resource management.
     * This method runs before each test method to ensure the environment is ready for testing specific scenarios.
     */
    @BeforeMethod
    public void setUp() throws Exception {

        // Mock responses for the organization manager with different ancestor organization chains.
        mockAncestorOrganizationRetrieval(Arrays.asList(L2_ORG_ID, L1_ORG_ID, ROOT_ORG_ID));
        mockAncestorOrganizationRetrieval(Arrays.asList(L1_ORG_ID, ROOT_ORG_ID));
        mockAncestorOrganizationRetrieval(Collections.singletonList(ROOT_ORG_ID));

        // Initialize the mock resource management service used in tests.
        mockResourceManagementService = new MockResourceManagementService();

        // Instantiate the OrgResourceResolverService for testing.
        orgResourceResolverService = new OrgResourceResolverServiceImpl();
    }

    /**
     * Resets mock services after each test method to ensure a clean state for subsequent tests.
     */
    @AfterMethod
    public void tearDown() {

        // Reset the mock services to their default state after each test.
        reset(organizationManager);
    }

    @DataProvider(name = "AggregationStrategyDataProvider")
    public Object[][] provideAggregationStrategies() {

        return new Object[][]{
                {firstFoundAggregationStrategy},
                {mergeAllAggregationStrategy}
        };
    }

    /**
     * Tests the behavior of the OrgResourceResolverService when no resources are available in the
     * organization hierarchy.
     * <p>
     * This test ensures that the resolver correctly returns null when no resources are found at any organization level.
     *
     * @param aggregationStrategy The aggregation strategy used to resolve resources.
     */
    @Test(dataProvider = "AggregationStrategyDataProvider")
    public void testGetOrgLevelResourcesFromOrgHierarchyWhenNoResourceAvailable(
            AggregationStrategy<MockResource> aggregationStrategy) throws Exception {

        MockResource resolvedRootResource = invokeOrgLevelResourceResolver(aggregationStrategy, ROOT_ORG_ID);
        assertNull(resolvedRootResource);

        MockResource resolvedL1Resource = invokeOrgLevelResourceResolver(aggregationStrategy, L1_ORG_ID);
        assertNull(resolvedL1Resource);

        MockResource resolvedL2Resource = invokeOrgLevelResourceResolver(aggregationStrategy, L2_ORG_ID);
        assertNull(resolvedL2Resource);
    }

    /**
     * Tests the behavior of the OrgResourceResolverService when an organization-level resource is available at the
     * root organization level.
     * <p>
     * This test ensures that the resolver correctly returns the resource for the root organization and propagates
     * the resolution to its child organizations (L1 and L2) using the given aggregation strategy.
     *
     * @param aggregationStrategy The aggregation strategy used to resolve resources.
     */
    @Test(dataProvider = "AggregationStrategyDataProvider")
    public void testGetOrgLevelResourcesFromOrgHierarchyWhenRootResourceAvailable(
            AggregationStrategy<MockResource> aggregationStrategy) throws Exception {

        // Add org-level resource for root organization into the mock resource management service.
        List<MockResource> createdOrgResource = addOrgResources(Collections.singletonList(ROOT_ORG_ID));

        MockResource resolvedRootResource = invokeOrgLevelResourceResolver(aggregationStrategy, ROOT_ORG_ID);
        assertResolvedResponse(resolvedRootResource, createdOrgResource.get(0));

        MockResource resolvedL1Resource = invokeOrgLevelResourceResolver(aggregationStrategy, L1_ORG_ID);
        assertResolvedResponse(resolvedL1Resource, createdOrgResource.get(0));

        MockResource resolvedL2Resource = invokeOrgLevelResourceResolver(aggregationStrategy, L2_ORG_ID);
        assertResolvedResponse(resolvedL2Resource, createdOrgResource.get(0));
    }

    /**
     * Tests the behavior of the OrgResourceResolverService when an organization-level resource is available at the
     * L1 organization.
     * <p>
     * This test ensures that when a resource is available at the L1 level, it is correctly resolved for
     * L1, and L2 organizations according to the provided aggregation strategy. At the root level,
     * its own resource should be returned.
     *
     * @param aggregationStrategy The aggregation strategy used to resolve resources.
     */
    @Test(dataProvider = "AggregationStrategyDataProvider")
    public void testGetOrgLevelResourcesFromOrgHierarchyWhenL1OrgResourceAvailable(
            AggregationStrategy<MockResource> aggregationStrategy) throws Exception {

        // Add org-level resources for L1 and root organizations into the mock resource management service.
        List<MockResource> createdOrgResources = addOrgResources(Arrays.asList(ROOT_ORG_ID, L1_ORG_ID));

        MockResource resolvedRootResource = invokeOrgLevelResourceResolver(aggregationStrategy, ROOT_ORG_ID);
        assertResolvedResponse(resolvedRootResource, createdOrgResources.get(0));

        MockResource resolvedL1Resource = invokeOrgLevelResourceResolver(aggregationStrategy, L1_ORG_ID);
        assertResolvedResponse(resolvedL1Resource, createdOrgResources.get(1));

        MockResource resolvedL2Resource = invokeOrgLevelResourceResolver(aggregationStrategy, L2_ORG_ID);
        assertResolvedResponse(resolvedL2Resource, createdOrgResources.get(1));
    }

    /**
     * Tests the behavior of the OrgResourceResolverService when an organization-level resource is available at the
     * L2 organization.
     * <p>
     * This test ensures that when a resource is available at the L2 level, it is correctly resolved for the
     * L2 organization based on the provided aggregation strategy.
     * At the root and L1 level, their own resource should be returned.
     *
     * @param aggregationStrategy The aggregation strategy used to resolve resources.
     */
    @Test(dataProvider = "AggregationStrategyDataProvider")
    public void testGetOrgLevelResourcesFromOrgHierarchyWhenL2OrgResourceAvailable(
            AggregationStrategy<MockResource> aggregationStrategy) throws Exception {

        // Add org-level resources for L2, L1 and root organizations into the mock resource management service.
        List<MockResource> createdOrgResources = addOrgResources(Arrays.asList(ROOT_ORG_ID, L1_ORG_ID, L2_ORG_ID));

        MockResource resolvedRootResource = invokeOrgLevelResourceResolver(aggregationStrategy, ROOT_ORG_ID);
        assertResolvedResponse(resolvedRootResource, createdOrgResources.get(0));

        MockResource resolvedL1Resource = invokeOrgLevelResourceResolver(aggregationStrategy, L1_ORG_ID);
        assertResolvedResponse(resolvedL1Resource, createdOrgResources.get(1));

        MockResource resolvedL2Resource = invokeOrgLevelResourceResolver(aggregationStrategy, L2_ORG_ID);
        assertResolvedResponse(resolvedL2Resource, createdOrgResources.get(2));
    }

    @DataProvider(name = "provideAggregationStrategiesForOrgLevelResolverWithInvalidInput")
    public Object[][] provideAggregationStrategiesForOrgLevelResolverWithInvalidInput() {

        List<String> invalidOrgIds = Arrays.asList(null, "", INVALID_ORG_ID);

        return new Object[][]{
                {firstFoundAggregationStrategy, invalidOrgIds},
                {mergeAllAggregationStrategy, invalidOrgIds},
        };
    }

    /**
     * Tests the behavior of the OrgResourceResolverService when provided with invalid organization IDs.
     * <p>
     * This test ensures that when invalid organization IDs are passed to the resolver, it correctly returns null,
     * indicating that no resources are found for the invalid input.
     *
     * @param aggregationStrategy The aggregation strategy used to resolve resources at different levels.
     * @param invalidOrgIds       A list of invalid organization IDs to test the resolver's behavior.
     * @throws Exception If an error occurs during resolution.
     */
    @Test(dataProvider = "provideAggregationStrategiesForOrgLevelResolverWithInvalidInput")
    public void testGetOrgLevelResourcesFromOrgHierarchyForInvalidInput(
            AggregationStrategy<MockResource> aggregationStrategy, List<String> invalidOrgIds) throws Exception {

        for (String orgId : invalidOrgIds) {
            assertThrows(OrgResourceHierarchyTraverseServerException.class,
                    () -> invokeOrgLevelResourceResolver(aggregationStrategy, orgId));
        }
    }

    @DataProvider(name = "provideAggregationStrategiesForAppLevelResolverWithInvalidInput")
    public Object[][] provideAggregationStrategiesForAppLevelResolverWithInvalidInput() {

        List<String> invalidOrgIds = Arrays.asList(null, "", INVALID_ORG_ID);
        List<String> invalidAppIds = Arrays.asList(null, "", INVALID_APP_ID);

        return new Object[][]{
                {firstFoundAggregationStrategy, invalidOrgIds, invalidAppIds},
                {mergeAllAggregationStrategy, invalidOrgIds, invalidAppIds},
        };
    }

    /**
     * Tests the behavior of the OrgResourceResolverService when server-side errors occur during
     * organizational hierarchy traversal.
     * <p>
     * This test simulates server-side exceptions thrown by the `organizationManager` during the following scenarios:
     * - Fetching the depth of an organization within the hierarchy.
     * - Retrieving ancestor organization IDs.
     *
     * @param aggregationStrategy The aggregation strategy used for resolving resources.
     * @throws Exception If an unexpected error occurs.
     */
    @Test(dataProvider = "AggregationStrategyDataProvider")
    public void testGetOrgLevelResourcesFromOrgHierarchyWhenServerErrorOccurs(
            AggregationStrategy<MockResource> aggregationStrategy) throws Exception {

        when(organizationManager.getOrganizationDepthInHierarchy(anyString()))
                .thenThrow(OrganizationManagementServerException.class);
        assertThrows(OrgResourceHierarchyTraverseServerException.class,
                () -> invokeOrgLevelResourceResolver(aggregationStrategy, L1_ORG_ID));
        assertThrows(OrgResourceHierarchyTraverseServerException.class,
                () -> invokeOrgLevelResourceResolver(aggregationStrategy, L2_ORG_ID));

        when(organizationManager.getAncestorOrganizationIds(anyString()))
                .thenThrow(OrganizationManagementServerException.class);
        assertThrows(OrgResourceHierarchyTraverseServerException.class,
                () -> invokeOrgLevelResourceResolver(aggregationStrategy, ROOT_ORG_ID));
        assertThrows(OrgResourceHierarchyTraverseServerException.class,
                () -> invokeOrgLevelResourceResolver(aggregationStrategy, L1_ORG_ID));
        assertThrows(OrgResourceHierarchyTraverseServerException.class,
                () -> invokeOrgLevelResourceResolver(aggregationStrategy, L2_ORG_ID));
    }

    /**
     * Mock the retrieval of ancestor organization IDs.
     *
     * @param orgIds Organization IDs.
     * @throws OrganizationManagementException If an error occurs while retrieving ancestor organization IDs.
     */
    private void mockAncestorOrganizationRetrieval(List<String> orgIds)
            throws OrganizationManagementException {

        List<String> ancestorOrganizationIds = new ArrayList<>(orgIds);
        when(organizationManager.getAncestorOrganizationIds(orgIds.get(0))).thenReturn(ancestorOrganizationIds);
    }

    /**
     * Add organization resources to the mock resource management service.
     *
     * @param orgIds Organization IDs.
     * @return List of created organization resources.
     */
    private List<MockResource> addOrgResources(List<String> orgIds) {

        List<MockResource> createdOrgResources = new ArrayList<>();
        int resourceId = 1;
        for (String orgId : orgIds) {
            MockResource orgResource = new MockResource(resourceId++, orgId + "Org Resource", orgId);
            mockResourceManagementService.addOrgResource(orgResource);
            createdOrgResources.add(orgResource);
        }
        return createdOrgResources;
    }

    /**
     * Resource resolver function used for testing MergeAllAggregationStrategy.
     *
     * @param aggregatedResource Aggregated resource.
     * @param newResource        New resource.
     * @return Merged resource.
     */
    private MockResource resourceMerger(MockResource aggregatedResource, MockResource newResource) {

        if (aggregatedResource == null) {
            return newResource;
        }
        return aggregatedResource;
    }

    /**
     * Invoke the organization level resource resolver.
     *
     * @param aggregationStrategy Aggregation strategy.
     * @param organizationId      Organization ID.
     * @return Resolved resource.
     * @throws OrgResourceHierarchyTraverseException If an error occurs while resolving the resource.
     */
    private MockResource invokeOrgLevelResourceResolver(AggregationStrategy<MockResource> aggregationStrategy,
                                                        String organizationId)
            throws OrgResourceHierarchyTraverseException {

        return orgResourceResolverService.getResourcesFromOrgHierarchy(
                organizationId,
                orgId -> {
                    MockResource resource = mockResourceManagementService.getOrgResource(orgId);
                    return Optional.ofNullable(resource);
                },
                aggregationStrategy);
    }

    /**
     * Assert the resolved resource with the actual resource.
     *
     * @param resolvedResource Resolved resource.
     * @param actualResource   Actual resource.
     */
    private void assertResolvedResponse(MockResource resolvedResource, MockResource actualResource) {

        assertNotNull(resolvedResource);
        assertEquals(resolvedResource.getId(), actualResource.getId());
        assertEquals(resolvedResource.getResourceName(), actualResource.getResourceName());
        assertEquals(resolvedResource.getOrgId(), actualResource.getOrgId());
    }
}

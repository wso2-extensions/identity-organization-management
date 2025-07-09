/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.util;

import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.operation.SelectiveShareApplicationOperation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationNode;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Utility class to process organization shares ordered by hierarchy.
 */
public class OrgApplicationShareProcessor {

    // Helper structure to hold results from BFS traversal
    private static class HierarchyInfo {

        final Map<String, OrganizationNode> allNodesById = new HashMap<>();
        final List<String> bfsOrder = new ArrayList<>();
    }

    /**
     * Performs BFS traversal to get all nodes and their BFS order.
     */
    private static HierarchyInfo getAllNodesAndBfsOrder(List<OrganizationNode> topLevelNodes) {

        HierarchyInfo info = new HierarchyInfo();
        if (topLevelNodes == null || topLevelNodes.isEmpty()) {
            return info;
        }

        Queue<OrganizationNode> queue = new LinkedList<>(topLevelNodes);

        // Add top-level nodes first
        for (OrganizationNode node : topLevelNodes) {
            if (!info.allNodesById.containsKey(node.getId())) {
                info.allNodesById.put(node.getId(), node);
                info.bfsOrder.add(node.getId());
            }
        }

        while (!queue.isEmpty()) {
            OrganizationNode current = queue.poll();

            List<OrganizationNode> children = current.getChildren();
            if (children != null) {
                for (OrganizationNode child : children) {
                    // Check if already processed to handle potential cycles (though unlikely in org structure)
                    if (!info.allNodesById.containsKey(child.getId())) {
                        info.allNodesById.put(child.getId(), child);
                        info.bfsOrder.add(child.getId());
                        queue.offer(child);
                    }
                }
            }
        }
        return info;
    }

    /**
     * Helper to get all descendant IDs using BFS (avoids deep recursion).
     */
    private static Set<String> getDescendantIds(String startOrgId, Map<String, OrganizationNode> allNodesById) {

        Set<String> descendants = new HashSet<>();
        OrganizationNode startNode = allNodesById.get(startOrgId);
        if (startNode == null) {
            return descendants;
        }

        Queue<OrganizationNode> queue = new LinkedList<>(startNode.getChildren());

        while (!queue.isEmpty()) {
            OrganizationNode current = queue.poll();
            if (current != null && descendants.add(current.getId())) { // Add returns true if not already present
                List<OrganizationNode> children = current.getChildren();
                if (children != null) {
                    queue.addAll(children);
                }
            }
        }
        return descendants;
    }

    /**
     * Builds a map of organization IDs to their parent IDs.
     * This allows us to trace ancestry up the hierarchy.
     */
    private static Map<String, String> buildParentMap(Map<String, OrganizationNode> allNodesById) {
        Map<String, String> parentMap = new HashMap<>();

        // For each node, map all its children to itself (as their parent)
        for (OrganizationNode node : allNodesById.values()) {
            List<OrganizationNode> children = node.getChildren();
            if (children != null) {
                for (OrganizationNode child : children) {
                    if (child != null) {
                        parentMap.put(child.getId(), node.getId());
                    }
                }
            }
        }
        return parentMap;
    }

    /**
     * This method prioritizes explicit role attribute configurations provided in the input list.
     * If an organization does not have an explicit configuration, it will inherit the configuration
     * from its nearest ancestor with the 'SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN' policy.
     *
     * @param childOrganizationGraph        The graph of child organizations to process.
     * @param selectiveShareApplicationList Raw list of share configurations.
     * @return A sorted List<SelectiveShareApplicationOperation> reflecting hierarchy and correct prioritization.
     */
    public static List<SelectiveShareApplicationOperation> processAndSortOrganizationShares(
            List<OrganizationNode> childOrganizationGraph,
            List<SelectiveShareApplicationOperation> selectiveShareApplicationList) {

        if (selectiveShareApplicationList == null || selectiveShareApplicationList.isEmpty()) {
            return new ArrayList<>();
        }

        // --- Step 1: Get Full Hierarchy Info ---
        HierarchyInfo hierarchyInfo = getAllNodesAndBfsOrder(childOrganizationGraph);
        Map<String, OrganizationNode> allValidNodes = hierarchyInfo.allNodesById;
        List<String> bfsOrder = hierarchyInfo.bfsOrder;

        if (allValidNodes.isEmpty()) {
            return new ArrayList<>();
        }

        // --- Step 2: Validate Input and Identify PolicyHolders ---
        Map<String, SelectiveShareApplicationOperation> validatedInputConfigs = new HashMap<>();
        for (SelectiveShareApplicationOperation config : selectiveShareApplicationList) {
            if (config != null && allValidNodes.containsKey(config.getOrganizationId())) {
                validatedInputConfigs.put(config.getOrganizationId(), config);
            }
        }

        // --- Step 3: Multi-Pass Processing for Prioritization and Inheritance ---
        Map<String, SelectiveShareApplicationOperation> effectiveConfigs = new HashMap<>();
        Set<String> orgsWithAllChildrenPolicy = new HashSet<>();

        // --- Step 4: Apply Inherited Configs from Nearest Ancestor with Cascading Policy ---
        Map<String, String> parentMap = buildParentMap(allValidNodes);
        for (String orgId : bfsOrder) {
            SelectiveShareApplicationOperation explicitConfig = validatedInputConfigs.get(orgId);
            if (explicitConfig != null) {
                if (!orgsWithAllChildrenPolicy.isEmpty()) {
                    String sourceId = findInheritanceSource(orgId, parentMap, orgsWithAllChildrenPolicy);
                    if (sourceId != null) {
                        SelectiveShareApplicationOperation inheritedConfig = new SelectiveShareApplicationOperation(
                                orgId,
                                PolicyEnum.SELECTED_ORG_ONLY,
                                explicitConfig.getRoleSharing()
                        );
                        effectiveConfigs.put(orgId, inheritedConfig);
                    } else {
                        effectiveConfigs.put(orgId, explicitConfig);
                        if (explicitConfig.getPolicy() == PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN)
                        {
                            orgsWithAllChildrenPolicy.add(orgId);
                        }
                    }
                } else {
                    effectiveConfigs.put(orgId, explicitConfig);
                    if (explicitConfig.getPolicy() == PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN) {
                        orgsWithAllChildrenPolicy.add(orgId);
                    }
                }
            } else {
                if (!orgsWithAllChildrenPolicy.isEmpty()) {
                    String sourceId = findInheritanceSource(orgId, parentMap, orgsWithAllChildrenPolicy);
                    if (sourceId != null) {
                        SelectiveShareApplicationOperation inheritedConfig = new SelectiveShareApplicationOperation(
                                orgId,
                                PolicyEnum.SELECTED_ORG_ONLY,
                                effectiveConfigs.get(sourceId).getRoleSharing()
                        );
                        effectiveConfigs.put(orgId, inheritedConfig);
                    }
                }
            }
        }

        // --- Step 5: Build Final Sorted Result List ---
        List<SelectiveShareApplicationOperation> sortedFinalConfigs = new ArrayList<>(effectiveConfigs.size());
        for (String orgIdInOrder : bfsOrder) {
            SelectiveShareApplicationOperation config = effectiveConfigs.get(orgIdInOrder);
            if (config != null) {
                sortedFinalConfigs.add(config);
            }
        }
        return sortedFinalConfigs;
    }

    /**
     * Traverses up the hierarchy from a given organization to find the nearest ancestor
     * that has the 'ALL_CHILDREN' policy.
     *
     * @param orgId                     The ID of the organization to start the upward search from.
     * @param parentMap                 A map of child IDs to their parent IDs.
     * @param orgsWithAllChildrenPolicy A set of organization IDs that have the cascading policy.
     * @return The ID of the nearest ancestor with the policy, or null if none is found.
     */
    private static String findInheritanceSource(String orgId, Map<String, String> parentMap,
                                                Set<String> orgsWithAllChildrenPolicy) {
        String currentParentId = parentMap.get(orgId);
        while (currentParentId != null) {
            if (orgsWithAllChildrenPolicy.contains(currentParentId)) {
                return currentParentId; // Found the closest ancestor with the policy.
            }
            currentParentId = parentMap.get(currentParentId); // Move up one level.
        }
        return null; // No ancestor with the policy was found.
    }

    /**
     * Retrieves all organization IDs under a given root organization, sorted in a
     * hierarchical, level-wise (BFS) order.
     * Example order: A, B, C, A.1, A.2, B.1, C.1, A.1.1, ...
     * where A, B, C are direct children of the 'mainOrganizationId'.
     *
     * @param mainOrganizationId The ID of the organization that serves as the root
     *                           for fetching the entire hierarchy of interest.
     * @return A List<String> of organization IDs in BFS order.
     * @throws OrganizationManagementException If there's an error fetching organization data.
     */
    public static List<String> getAllOrganizationIdsInBfsOrder(String mainOrganizationId)
            throws OrganizationManagementException {

        // 1. Fetch the hierarchical graph.
        // This call should return the direct children of 'rootOrganizationIdForAllOrgs' (e.g., A, B, C),
        // where each child node is populated with its own children, and so on.
        List<OrganizationNode> effectiveTopLevelNodes = OrgApplicationMgtDataHolder.getInstance()
                .getOrganizationManager()
                .getChildOrganizationGraph(mainOrganizationId, true);

        // 2. Use the helper to get the BFS order of IDs from this graph.
        // The `effectiveTopLevelNodes` (A, B, C in your example) will be the first items
        // in the bfsOrder list, followed by their children level by level.
        HierarchyInfo hierarchyInfo = getAllNodesAndBfsOrder(effectiveTopLevelNodes);

        return hierarchyInfo.bfsOrder;
    }

    /**
     * Filters a given list of organization IDs against the hierarchy under a main organization,
     * includes all descendants of the valid input IDs, and returns the unique, valid IDs
     * sorted in reverse hierarchical (BFS) order.
     * <p>
     * Example Input: mainOrgId="ROOT", listOfOrgIds=["A", "C.1", "INVALID"]
     * Example Hierarchy: ROOT -> [A, B, C], A -> [A.1, A.2], A.1 -> [A.1.1], C -> [C.1]
     * Example Output: ["A.1.1", "C.1", "A.2", "A.1", "A"]
     * (Includes A and its descendants A.1, A.2, A.1.1. Includes C.1. Excludes B. Excludes INVALID. Sorted reverse BFS).
     *
     * @param mainOrganizationId The ID of the organization defining the hierarchy scope.
     * @param listOfOrgIds       A list of organization IDs to be processed. Can contain invalid IDs or be a subset.
     *                           Can be null.
     * @return A List<String> of valid organization IDs (including required descendants) in reverse BFS order.
     * @throws OrganizationManagementException If there's an error fetching organization data.
     */
    public static List<String> getValidOrganizationsInReverseBfsOrder(
            String mainOrganizationId, List<String> listOfOrgIds) throws OrganizationManagementException {

        // --- Step 1: Get Full Hierarchy Info ---
        // Fetch the graph structure starting from the children of mainOrganizationId
        List<OrganizationNode> effectiveTopLevelNodes = OrgApplicationMgtDataHolder.getInstance()
                .getOrganizationManager()
                .getChildOrganizationGraph(mainOrganizationId, true);

        // Get the map of all valid nodes and the standard BFS traversal order
        HierarchyInfo hierarchyInfo = getAllNodesAndBfsOrder(effectiveTopLevelNodes);
        Map<String, OrganizationNode> allValidNodesMap = hierarchyInfo.allNodesById;
        List<String> bfsOrder = hierarchyInfo.bfsOrder;

        // If the resulting hierarchy is empty, there are no valid nodes to return.
        if (allValidNodesMap.isEmpty() || bfsOrder.isEmpty()) {
            return new ArrayList<>();
        }

        // Handle null input list gracefully by treating it as empty
        if (listOfOrgIds == null) {
            listOfOrgIds = new ArrayList<>();
        }

        // --- Step 2: Identify Valid Seed Nodes from the input list ---
        // These are the explicitly listed organizations that exist in the hierarchy.
        Set<String> validSeedOrgIds = new HashSet<>();
        for (String inputId : listOfOrgIds) {
            // Check if the ID from the input list is present in the map of valid nodes
            if (inputId != null && allValidNodesMap.containsKey(inputId)) {
                validSeedOrgIds.add(inputId);
            }
        }

        // If none of the input IDs were valid or the input list was effectively empty, return empty list.
        if (validSeedOrgIds.isEmpty()) {
            return new ArrayList<>();
        }

        // --- Step 3: Expand Seeds to Include All Their Descendants ---
        // Collect all unique IDs that should be in the final list (seeds + their descendants)
        Set<String> finalOrgIdsToInclude = new HashSet<>();
        for (String seedId : validSeedOrgIds) {
            // Add the seed organization itself
            finalOrgIdsToInclude.add(seedId);
            // Find all descendants of this seed within the valid hierarchy
            Set<String> descendants = getDescendantIds(seedId, allValidNodesMap);
            // Add all found descendants to the set (duplicates are handled automatically by Set)
            finalOrgIdsToInclude.addAll(descendants);
        }

        // --- Step 4: Filter the BFS Order and Reverse ---
        // Create the final list which will be sorted in reverse BFS order.
        return getReversedSortedList(bfsOrder, finalOrgIdsToInclude);
    }

    /**
     * Sorts a given list of organization IDs based on their hierarchical order (BFS)
     * under a specified main organization. Only valid organization IDs from the input list
     * that belong to the main organization's hierarchy are included in the output.
     * The output list will contain unique IDs.
     * <p>
     * Example Input: mainOrganizationId="ROOT", organizationIdsToSort=["A.1.1", "INVALID_ID", "A", "C.1", "A"]
     * Example Hierarchy: ROOT -> [A, B, C], A -> [A.1, A.2], A.1 -> [A.1.1], C -> [C.1]
     * Example Output: ["A", "C.1", "A.1.1"]
     * (INVALID_ID and B are excluded as they are not in the input or are invalid. Duplicates from input are handled.)
     *
     * @param mainOrganizationId    The ID of the organization defining the hierarchy scope.
     * @param organizationIdsToSort A list of organization IDs to be sorted. Can be null or contain
     *                              invalid/duplicate IDs.
     * @return A List<String> of unique, valid organization IDs from the input list, sorted in BFS order.
     * @throws OrganizationManagementException If there's an error fetching organization data.
     */
    public static List<String> sortOrganizationsByHierarchy(
            String mainOrganizationId, List<String> organizationIdsToSort) throws OrganizationManagementException {

        // --- Step 1: Get Full Hierarchy Info to establish valid nodes and BFS order ---
        List<OrganizationNode> effectiveTopLevelNodes = OrgApplicationMgtDataHolder.getInstance()
                .getOrganizationManager()
                .getChildOrganizationGraph(mainOrganizationId, true);

        HierarchyInfo hierarchyInfo = getAllNodesAndBfsOrder(effectiveTopLevelNodes);
        Map<String, OrganizationNode> allValidNodesMap = hierarchyInfo.allNodesById; // Used for validation
        List<String> bfsOrder = hierarchyInfo.bfsOrder; // This is our sorting template

        // If the overall hierarchy is empty, no sorting or validation is possible.
        if (allValidNodesMap.isEmpty() || bfsOrder.isEmpty()) {
            return new ArrayList<>();
        }

        // Handle null input list gracefully.
        List<String> inputList = (organizationIdsToSort == null) ? new ArrayList<>() : organizationIdsToSort;

        // --- Step 2: Identify Valid and Unique Organization IDs from the Input List ---
        // We only care about IDs that were *in the input list* AND are *valid within the hierarchy*.
        Set<String> validIdsFromInput = new HashSet<>();
        for (String inputId : inputList) {
            if (inputId != null && allValidNodesMap.containsKey(inputId)) {
                validIdsFromInput.add(inputId); // Using a Set handles duplicates from inputList
            }
        }

        // If no valid IDs were found in the input list, return empty.
        if (validIdsFromInput.isEmpty()) {
            return new ArrayList<>();
        }

        // --- Step 3: Build the Sorted List ---
        // Iterate through the established BFS order of all valid nodes in the hierarchy.
        // If a node from this BFS order is also in our set of `validIdsFromInput`,
        // add it to the final sorted list.
        List<String> sortedResult = new ArrayList<>();
        for (String idInBfsOrder : bfsOrder) {
            if (validIdsFromInput.contains(idInBfsOrder)) {
                sortedResult.add(idInBfsOrder);
            }
        }

        return sortedResult;
    }

    /**
     * Takes a list of organization nodes (potentially representing multiple disconnected sub-graphs or top-level nodes)
     * and returns a single list of all organization IDs from these nodes and their descendants, in BFS order.
     *
     * @param organizationNodes A list of {@link OrganizationNode} objects.
     * @return A {@link List} of {@link String} containing all organization IDs in BFS order.
     *         Returns an empty list if the input list is null or empty.
     */
    public static List<String> getOrganizationIdsInBfsOrder(List<OrganizationNode> organizationNodes) {
        if (organizationNodes == null || organizationNodes.isEmpty()) {
            return new ArrayList<>();
        }
        HierarchyInfo hierarchyInfo = getAllNodesAndBfsOrder(organizationNodes);
        return hierarchyInfo.bfsOrder;
    }

    private static List<String> getReversedSortedList(List<String> bfsOrder, Set<String> finalOrgIdsToInclude) {

        List<String> reversedSortedList = new ArrayList<>();
        // Iterate through the standard BFS order list *backwards*
        for (int i = bfsOrder.size() - 1; i >= 0; i--) {
            String currentIdInBfs = bfsOrder.get(i);
            // Check if this ID (from the standard BFS order) is one of the ones
            // we determined should be included (seeds + descendants).
            if (finalOrgIdsToInclude.contains(currentIdInBfs)) {
                // If it should be included, add it to our reversed list.
                // Since we are iterating backwards through the BFS order,
                // the resulting list will naturally be in reverse BFS order.
                reversedSortedList.add(currentIdInBfs);
            }
        }
        return reversedSortedList;
    }
}

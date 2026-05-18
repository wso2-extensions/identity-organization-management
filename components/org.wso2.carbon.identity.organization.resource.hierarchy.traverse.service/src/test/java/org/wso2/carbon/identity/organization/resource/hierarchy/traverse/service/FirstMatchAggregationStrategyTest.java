/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.FirstMatchAggregationStrategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Unit tests for {@link FirstMatchAggregationStrategy}.
 */
public class FirstMatchAggregationStrategyTest {

    private static final String ROOT = "root";
    private static final String L1 = "l1";
    private static final String L2 = "l2";

    // ── includeSelf = false (ancestor-only traversal) ────────────────────────

    @Test
    public void testIncludeSelfFalse_emptyHierarchy_returnsNull() throws Exception {

        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(false, (r, isFirst) -> true);
        assertNull(strategy.aggregate(Collections.emptyList(), id -> Optional.of("resource")));
    }

    @Test
    public void testIncludeSelfFalse_onlySelfInHierarchy_returnsNull() throws Exception {

        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(false, (r, isFirst) -> true);
        // hierarchy = [L2 only] — no ancestors to check
        assertNull(strategy.aggregate(Collections.singletonList(L2), id -> Optional.of("resource")));
    }

    @Test
    public void testIncludeSelfFalse_resourceAtDirectParent_returnsIt() throws Exception {

        // hierarchy = [L2, L1, ROOT]; resource only at L1 (direct parent of L2)
        Map<String, String> resources = resourceMap(L1, "l1-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(false, (r, isFirst) -> true);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertEquals(result, "l1-resource");
    }

    @Test
    public void testIncludeSelfFalse_resourceOnlyAtRoot_returnsIt() throws Exception {

        // hierarchy = [L2, L1, ROOT]; resource only at ROOT
        Map<String, String> resources = resourceMap(ROOT, "root-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(false, (r, isFirst) -> true);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertEquals(result, "root-resource");
    }

    @Test
    public void testIncludeSelfFalse_predicateRequiresIsFirst_resourceAtDirectParent_returnsIt() throws Exception {

        // isFirst = true only for direct parent (L1); predicate accepts only isFirst entries
        Map<String, String> resources = resourceMap(L1, "l1-resource");
        resources.put(ROOT, "root-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(false, (r, isFirst) -> isFirst);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertEquals(result, "l1-resource");
    }

    @Test
    public void testIncludeSelfFalse_predicateRequiresIsFirst_resourceOnlyAtRoot_returnsNull() throws Exception {

        // isFirst = true only for L1; ROOT has isFirst=false — predicate rejects it
        Map<String, String> resources = resourceMap(ROOT, "root-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(false, (r, isFirst) -> isFirst);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertNull(result);
    }

    @Test
    public void testIncludeSelfFalse_noResourceAnywhere_returnsNull() throws Exception {

        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(false, (r, isFirst) -> true);
        assertNull(strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.empty()));
    }

    @Test
    public void testIncludeSelfFalse_resourceOnlyAtSelf_returnsNull() throws Exception {

        // resource exists at L2 (index 0) but traversal skips self
        Map<String, String> resources = resourceMap(L2, "l2-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(false, (r, isFirst) -> true);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertNull(result);
    }

    // ── includeSelf = true ────────────────────────────────────────────────────

    @Test
    public void testIncludeSelfTrue_emptyHierarchy_returnsNull() throws Exception {

        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(true, (r, isFirst) -> true);
        assertNull(strategy.aggregate(Collections.emptyList(), id -> Optional.of("resource")));
    }

    @Test
    public void testIncludeSelfTrue_resourceAtSelf_returnsIt() throws Exception {

        Map<String, String> resources = resourceMap(L2, "l2-resource");
        resources.put(L1, "l1-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(true, (r, isFirst) -> true);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertEquals(result, "l2-resource");
    }

    @Test
    public void testIncludeSelfTrue_noResourceAtSelf_resourceAtParent_returnsParent() throws Exception {

        Map<String, String> resources = resourceMap(L1, "l1-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(true, (r, isFirst) -> true);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertEquals(result, "l1-resource");
    }

    @Test
    public void testIncludeSelfTrue_predicateRequiresIsFirst_resourceAtSelf_returnsIt() throws Exception {

        // isFirst=true only for L2 (self, index 0); predicate accepts only isFirst entries
        Map<String, String> resources = resourceMap(L2, "l2-resource");
        resources.put(L1, "l1-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(true, (r, isFirst) -> isFirst);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertEquals(result, "l2-resource");
    }

    @Test
    public void testIncludeSelfTrue_predicateRequiresIsFirst_noResourceAtSelf_returnsNull() throws Exception {

        // L1 has a resource but isFirst=false — predicate rejects it
        Map<String, String> resources = resourceMap(L1, "l1-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(true, (r, isFirst) -> isFirst);
        String result = strategy.aggregate(Arrays.asList(L2, L1, ROOT), id -> Optional.ofNullable(resources.get(id)));
        assertNull(result);
    }

    // ── data-provider test covering both modes ────────────────────────────────

    @DataProvider(name = "includeSelfVariants")
    public Object[][] includeSelfVariants() {

        return new Object[][]{{true}, {false}};
    }

    @Test(dataProvider = "includeSelfVariants")
    public void testPredicateRejectsAll_returnsNull(boolean includeSelf) throws Exception {

        Map<String, String> resources = resourceMap(L2, "l2-resource");
        resources.put(L1, "l1-resource");
        resources.put(ROOT, "root-resource");
        FirstMatchAggregationStrategy<String> strategy =
                new FirstMatchAggregationStrategy<>(includeSelf, (r, isFirst) -> false);
        List<String> hierarchy = Arrays.asList(L2, L1, ROOT);
        assertNull(strategy.aggregate(hierarchy, id -> Optional.ofNullable(resources.get(id))));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Map<String, String> resourceMap(String orgId, String resource) {

        Map<String, String> map = new HashMap<>();
        map.put(orgId, resource);
        return map;
    }
}

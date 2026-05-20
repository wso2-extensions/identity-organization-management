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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy;

import org.apache.commons.collections.CollectionUtils;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Aggregation strategy that walks the organization hierarchy and returns the first resource
 * that satisfies the given match predicate.
 *
 * <p>The {@code includeSelf} flag controls the starting point of the traversal:
 * <ul>
 *   <li>{@code true} — start from the target organization itself (index 0).</li>
 *   <li>{@code false} — start from its direct parent (index 1), skipping the org itself.</li>
 * </ul>
 * The predicate receives the resource and a boolean indicating whether the current entry is the
 * first in traversal (i.e., self when {@code includeSelf=true}, or the direct parent when
 * {@code includeSelf=false}), enabling position-aware matching decisions.</p>
 *
 * @param <T> The type of the resource being retrieved from the organization hierarchy.
 */
public class FirstMatchAggregationStrategy<T> implements AggregationStrategy<T> {

    private final boolean includeSelf;
    private final BiPredicate<T, Boolean> matchPredicate;

    /**
     * @param includeSelf    whether to include the target organization itself in the traversal.
     * @param matchPredicate predicate receiving {@code (resource, isFirst)} — {@code isFirst} is
     *                       {@code true} for the first entry in traversal (self or direct parent
     *                       depending on {@code includeSelf}). Returns {@code true} when the
     *                       resource satisfies the match condition.
     */
    public FirstMatchAggregationStrategy(boolean includeSelf, BiPredicate<T, Boolean> matchPredicate) {

        this.includeSelf = includeSelf;
        this.matchPredicate = matchPredicate;
    }

    @Override
    public T aggregate(List<String> organizationHierarchy, Function<String, Optional<T>> resourceRetriever)
            throws OrgResourceHierarchyTraverseException {

        if (CollectionUtils.isEmpty(organizationHierarchy)) {
            return null;
        }

        int startIndex = includeSelf ? 0 : 1;
        for (int i = startIndex; i < organizationHierarchy.size(); i++) {
            boolean isFirst = (i == startIndex);
            Optional<T> resource = resourceRetriever.apply(organizationHierarchy.get(i));
            if (resource.isPresent() && matchPredicate.test(resource.get(), isFirst)) {
                return resource.get();
            }
        }
        return null;
    }
}

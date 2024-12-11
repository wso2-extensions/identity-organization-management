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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy;

import org.apache.commons.collections.CollectionUtils;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.util.OrgResourceHierarchyTraverseUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Aggregation strategy to merge all resources in the organization hierarchy using the specified
 * resource merger function.
 * <p>
 * This strategy traverses the hierarchy and applies the provided function
 * to combine the resources at each level. It ensures that the resources are merged according to the
 * logic defined by the merger function, which could involve combining attributes, performing calculations, or
 * resolving conflicts between resources.
 *
 * @param <T> The type of the resources being merged in the organization/ application hierarchy.
 */
public class MergeAllAggregationStrategy<T> implements AggregationStrategy<T> {

    private final BiFunction<T, T, T> resourceMerger;

    /**
     * Constructor to initialize the aggregation strategy with the resource merger function.
     *
     * @param resourceMerger Resource merger function.
     */
    public MergeAllAggregationStrategy(BiFunction<T, T, T> resourceMerger) {

        this.resourceMerger = resourceMerger;
    }

    @Override
    public T aggregate(List<String> organizationHierarchy, Function<String, Optional<T>> resourceRetriever)
            throws OrgResourceHierarchyTraverseException {

        T aggregatedResource = null;
        if (CollectionUtils.isEmpty(organizationHierarchy)) {
            return aggregatedResource;
        }

        for (String orgId : organizationHierarchy) {
            if (organizationHierarchy.size() != 1 &&
                    OrgResourceHierarchyTraverseUtil.isMinOrgHierarchyDepthReached(orgId)) {
                break;
            }

            Optional<T> resource = resourceRetriever.apply(orgId);
            if (resource.isPresent()) {
                if (aggregatedResource == null) {
                    aggregatedResource = resource.get();
                } else {
                    aggregatedResource = resourceMerger.apply(aggregatedResource, resource.get());
                }
            }
        }
        return aggregatedResource;
    }

    @Override
    public T aggregate(List<String> organizationHierarchy, Map<String, String> applicationHierarchy,
                       BiFunction<String, String, Optional<T>> resourceRetriever)
            throws OrgResourceHierarchyTraverseException {

        T aggregatedResource = null;
        if (CollectionUtils.isEmpty(organizationHierarchy)) {
            return aggregatedResource;
        }

        for (String orgId : organizationHierarchy) {
            if (organizationHierarchy.size() != 1 &&
                    OrgResourceHierarchyTraverseUtil.isMinOrgHierarchyDepthReached(orgId)) {
                break;
            }

            String appId = applicationHierarchy.get(orgId);
            Optional<T> resource = resourceRetriever.apply(orgId, appId);
            if (resource.isPresent()) {
                if (aggregatedResource == null) {
                    aggregatedResource = resource.get();
                } else {
                    aggregatedResource = resourceMerger.apply(aggregatedResource, resource.get());
                }
            }
        }
        return aggregatedResource;
    }
}

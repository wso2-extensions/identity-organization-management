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
 * Aggregation strategy that can be used to traverse the organization hierarchy and retrieve the first resource found.
 * This strategy is commonly applied when multiple resources might exist at different levels of the hierarchy,
 * and only the first one encountered at the bottom of the hierarchy needs to be returned.
 *
 * @param <T> The type of the resource being retrieved from the organization/ application hierarchy.
 */
public class FirstFoundAggregationStrategy<T> implements AggregationStrategy<T> {

    @Override
    public T aggregate(List<String> organizationHierarchy, Function<String, Optional<T>> resourceRetriever)
            throws OrgResourceHierarchyTraverseException {

        if (CollectionUtils.isEmpty(organizationHierarchy) || organizationHierarchy.isEmpty()) {
            return null;
        }

        for (String orgId : organizationHierarchy) {
            if (OrgResourceHierarchyTraverseUtil.isMinOrgHierarchyDepthReached(orgId)) {
                break;
            }

            Optional<T> resource = resourceRetriever.apply(orgId);
            if (resource.isPresent()) {
                return resource.get();
            }
        }
        return null;
    }

    @Override
    public T aggregate(List<String> organizationHierarchy, Map<String, String> applicationHierarchy,
                       BiFunction<String, String, Optional<T>> resourceRetriever)
            throws OrgResourceHierarchyTraverseException {

        if (CollectionUtils.isEmpty(organizationHierarchy) || organizationHierarchy.isEmpty()) {
            return null;
        }

        for (String orgId : organizationHierarchy) {
            if (OrgResourceHierarchyTraverseUtil.isMinOrgHierarchyDepthReached(orgId)) {
                break;
            }

            String appId = applicationHierarchy.get(orgId);
            Optional<T> resource = resourceRetriever.apply(orgId, appId);
            if (resource.isPresent()) {
                return resource.get();
            }
        }
        return null;
    }
}

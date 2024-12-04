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

import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.NotImplementedException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Defines an interface for a strategy used for aggregating resources retrieved from hierarchical structures,
 * such as organization and application hierarchies. This interface provides default
 * methods that can be overridden to implement specific aggregation logic.
 * <p>
 * Implementations should specify how resources are combined and may use functional
 * interfaces for flexible retrieval mechanisms.
 *
 * @param <T> The type of the resource to be aggregated.
 */
public interface AggregationStrategy<T> {

    /**
     * Aggregates resources resolved from an organization's hierarchical structure.
     * <p>
     * This method provides a default implementation that throws a
     * {@link NotImplementedException}. Subclasses must override this method to define
     * specific aggregation logic for organization hierarchies.
     *
     * @param organizationHierarchy A list representing the organization hierarchy,
     *                              where the first element is the root organization
     *                              and subsequent elements represent child organizations.
     * @param resourceRetriever     A function that retrieves a resource given an organization ID.
     *                              Returns an {@link Optional<T>} containing the resource, or empty
     *                              if no resource is found for the given ID.
     * @return The aggregated resource of type <T>.
     * @throws OrgResourceHierarchyTraverseException If any error occurs during resource
     *                                               retrieval or aggregation.
     */
    default T aggregate(List<String> organizationHierarchy, Function<String, Optional<T>> resourceRetriever)
            throws OrgResourceHierarchyTraverseException {

        throw new NotImplementedException("aggregate method is not implemented in " + this.getClass());
    }

    /**
     * Aggregates resources resolved from an organization's and application's hierarchical structure.
     * <p>
     * This method provides a default implementation that throws a
     * {@link NotImplementedException}. Subclasses must override this method to define
     * specific aggregation logic for combined organization and application hierarchies.
     *
     * @param organizationHierarchy A list representing the organization hierarchy,
     *                              where the first element is the root organization
     *                              and subsequent elements represent child organizations.
     * @param applicationHierarchy  A map representing the application hierarchy, where keys
     *                              are organization IDs, and values are application-specific
     *                              details or IDs for each organization.
     * @param resourceRetriever     A bi-function that retrieves a resource based on both an
     *                              organization ID and an application ID. Returns an {@link Optional<T>}
     *                              containing the resource, or empty if no resource is found.
     * @return The aggregated resource of type <T>.
     * @throws OrgResourceHierarchyTraverseException If any error occurs during resource
     *                                               retrieval or aggregation.
     */
    default T aggregate(List<String> organizationHierarchy, Map<String, String> applicationHierarchy,
                        BiFunction<String, String, Optional<T>> resourceRetriever)
            throws OrgResourceHierarchyTraverseException {

        throw new NotImplementedException("aggregate method is not implemented in " + this.getClass());
    }
}

/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.application.resource.hierarchy.traverse.service;

import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.AggregationStrategy;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Service interface for resolving resources from the organization and application hierarchy.
 * This interface provides methods to retrieve resources by traversing the hierarchy of a given
 * organization and application.
 */
public interface OrgAppResourceResolverService {

    /**
     * Retrieves resources by traversing the hierarchy of a given organization and application.
     *
     * @param organizationId      The unique identifier of the organization.
     * @param applicationId       The unique identifier of the application within the organization.
     * @param resourceRetriever   A bi-function that defines how to fetch a resource based on the
     *                            organization and application IDs. The function must return an
     *                            {@link Optional <T>} containing the resource if found,
     *                            or an empty {@link Optional<T>} if not.
     * @param aggregationStrategy A strategy defining how to aggregate resources retrieved from
     *                            different levels of the hierarchy.
     * @param <T>                 The type of the resource being retrieved and aggregated.
     * @return An aggregated resource of type <T> obtained from the organization and application hierarchy.
     * @throws OrgResourceHierarchyTraverseException If any errors occur during resource retrieval
     *                                               or aggregation.
     */
    <T> T getResourcesFromOrgHierarchy(String organizationId, String applicationId,
                                       BiFunction<String, String, Optional<T>> resourceRetriever,
                                       AggregationStrategy<T> aggregationStrategy)
            throws OrgResourceHierarchyTraverseException;

}

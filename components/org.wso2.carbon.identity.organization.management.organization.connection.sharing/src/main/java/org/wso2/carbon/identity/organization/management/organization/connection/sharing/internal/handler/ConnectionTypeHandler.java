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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler;

import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.service.ConnectionSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.util.List;

/**
 * Strategy interface encapsulating the resource-type specific operations required to share a connection across
 * organizations. Each connection type (IDP, identity verification provider, custom authenticator, flow extension)
 * provides its own implementation; the type-agnostic orchestration (validation, organization scoping, async
 * processing and sharing-policy persistence) lives in {@link ConnectionSharingPolicyHandlerService} and dispatches
 * to the handler resolved for the request's {@link ResourceType}.
 */
public interface ConnectionTypeHandler {

    /**
     * Returns the {@link ResourceType} this handler is responsible for, under which its sharing policies are
     * persisted.
     *
     * @return The handled {@link ResourceType}.
     */
    ResourceType getResourceType();

    /**
     * Validates that the given connection is eligible to be shared, before any sharing is initiated. Implementations
     * reject connections that must not be shared (e.g. trusted token issuers or restricted authenticators) with a
     * client error. This is invoked synchronously by the policy handler before the asynchronous sharing process is
     * started, so the caller receives an immediate, actionable error. The default implementation applies no
     * connection-type-specific restrictions.
     *
     * @param connectionId    The ID of the connection to be shared.
     * @param initiatingOrgId The ID of the organization that owns the connection.
     * @throws ConnectionSharingMgtException If the connection is not eligible to be shared, or an error occurs while
     *                                       validating its eligibility.
     */
    default void validateConnectionShareEligibility(String connectionId, String initiatingOrgId)
            throws ConnectionSharingMgtException {

        // No connection-type-specific sharing restrictions by default.
    }

    /**
     * Propagates the connection to a single target organization, creating the shadow resource as required.
     *
     * @param connectionId    The ID of the parent connection being shared.
     * @param targetOrgId     The ID of the organization the connection is shared with.
     * @param policy          The sharing policy applied for the target organization.
     * @param initiatingOrgId The ID of the organization that owns the parent connection.
     * @throws ConnectionSharingMgtException If an error occurs while propagating the connection.
     */
    void shareConnectionToOrg(String connectionId, String targetOrgId, PolicyEnum policy, String initiatingOrgId)
            throws ConnectionSharingMgtException;

    /**
     * Propagates the connection to all organizations under the given general sharing policy, creating shadow
     * resources as required.
     *
     * @param connectionId    The ID of the parent connection being shared.
     * @param policy          The general sharing policy applied.
     * @param initiatingOrgId The ID of the organization that owns the parent connection.
     * @throws ConnectionSharingMgtException If an error occurs while propagating the connection.
     */
    void shareConnectionToAllOrgs(String connectionId, PolicyEnum policy, String initiatingOrgId)
            throws ConnectionSharingMgtException;

    /**
     * Propagates the connection to all existing descendant organizations of the given parent organization, creating
     * shadow resources as required. Used to materialize the "existing children" of a selected organization shared
     * with the {@code SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN} policy (the parent organization itself is
     * shared separately via {@link #shareConnectionToOrg}; future children are handled on organization creation).
     *
     * @param connectionId    The ID of the parent connection being shared.
     * @param parentOrgId     The organization whose existing descendant organizations receive the connection.
     * @param policy          The sharing policy applied.
     * @param initiatingOrgId The ID of the organization that owns the parent connection.
     * @throws ConnectionSharingMgtException If an error occurs while propagating the connection.
     */
    void shareConnectionToExistingChildOrgs(String connectionId, String parentOrgId, PolicyEnum policy,
                                            String initiatingOrgId) throws ConnectionSharingMgtException;

    /**
     * Removes the shadow connection from a target organization and, cascading downward, from all of that
     * organization's descendant organizations that hold the shared connection. A sub-organization must not retain a
     * shared connection that its ancestor no longer has, mirroring the downward fan-out of
     * {@link #shareConnectionToAllOrgs}.
     *
     * @param connectionId    The ID of the parent connection being unshared.
     * @param targetOrgId     The ID of the organization the connection is unshared from (its descendants are
     *                        unshared as well).
     * @param initiatingOrgId The ID of the organization that owns the parent connection.
     * @throws ConnectionSharingMgtException If an error occurs while removing the shadow connection.
     */
    void unshareConnectionFromOrg(String connectionId, String targetOrgId, String initiatingOrgId)
            throws ConnectionSharingMgtException;

    /**
     * Removes the shadow connection from all organizations it has been shared with.
     *
     * @param connectionId    The ID of the parent connection being unshared.
     * @param initiatingOrgId The ID of the organization that owns the parent connection.
     * @throws ConnectionSharingMgtException If an error occurs while removing the shadow connections.
     */
    void unshareConnectionFromAllOrgs(String connectionId, String initiatingOrgId)
            throws ConnectionSharingMgtException;

    /**
     * Retrieves shadow connection associations for a given parent connection, scoped to the provided organization
     * list.
     *
     * @param connectionId    The ID of the parent connection being queried.
     * @param initiatingOrgId The ID of the organization that owns the parent connection.
     * @param orgIdsScope     The list of child organization IDs to scope the search to.
     * @param expressionNodes Expression nodes for in-DAO filtering (id, name, cursor conditions).
     * @param sortOrder       Sort order for keyset pagination (ASC or DESC).
     * @param limit           Maximum number of records to return (0 = no limit).
     * @return A list of {@link ConnectionAssociation}s representing shadow connections.
     * @throws OrganizationManagementException If an error occurs while fetching associations.
     */
    List<ConnectionAssociation> getConnectionAssociations(
            String connectionId, String initiatingOrgId, List<String> orgIdsScope,
            List<ExpressionNode> expressionNodes, String sortOrder, int limit)
            throws OrganizationManagementException;
}

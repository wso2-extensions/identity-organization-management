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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.constant.OrgResourceHierarchyTraverseConstants;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseServerException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.internal.OrgResourceHierarchyTraverseServiceDataHolder;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.AggregationStrategy;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.util.OrgResourceHierarchyTraverseUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementation of the OrgResourceResolverService interface, responsible for resolving resources within the
 * given organization/ application hierarchy.
 */
public class OrgResourceResolverServiceImpl implements OrgResourceResolverService {

    @Override
    public <T> T getResourcesFromOrgHierarchy(String organizationId, Function<String, Optional<T>> resourceRetriever,
                                              AggregationStrategy<T> aggregationStrategy)
            throws OrgResourceHierarchyTraverseException {

        List<String> organizationIds = getAncestorOrganizationsIds(organizationId);
        return aggregationStrategy.aggregate(organizationIds, resourceRetriever);
    }

    @Override
    public <T> T getResourcesFromOrgHierarchy(String organizationId, String applicationId,
                                              BiFunction<String, String, Optional<T>> resourceRetriever,
                                              AggregationStrategy<T> aggregationStrategy)
            throws OrgResourceHierarchyTraverseException {

        try {
            List<String> organizationIds = getAncestorOrganizationsIds(organizationId);

            ApplicationManagementService applicationManagementService = getApplicationManagementService();
            Map<String, String> ancestorAppIds = Collections.emptyMap();
            if (applicationId != null) {
                ancestorAppIds = applicationManagementService.getAncestorAppIds(applicationId, organizationId);
            }

            return aggregationStrategy.aggregate(organizationIds, ancestorAppIds, resourceRetriever);
        } catch (IdentityApplicationManagementException e) {
            throw OrgResourceHierarchyTraverseUtil.handleServerException(
                    OrgResourceHierarchyTraverseConstants.ErrorMessages
                            .ERROR_CODE_SERVER_ERROR_WHILE_RESOLVING_ANCESTOR_APPLICATIONS,
                    e, organizationId, applicationId);
        }
    }

    private List<String> getAncestorOrganizationsIds(String organizationId)
            throws OrgResourceHierarchyTraverseServerException {

        if (StringUtils.isBlank(organizationId)) {
            throw OrgResourceHierarchyTraverseUtil.handleServerException(
                    OrgResourceHierarchyTraverseConstants.ErrorMessages.ERROR_CODE_EMPTY_ORGANIZATION_ID);
        }

        try {
            OrganizationManager organizationManager = OrgResourceHierarchyTraverseUtil.getOrganizationManager();
            List<String> organizationIds = organizationManager.getAncestorOrganizationIds(organizationId);
            if (CollectionUtils.isEmpty(organizationIds) || organizationIds.isEmpty()) {
                throw OrgResourceHierarchyTraverseUtil.handleServerException(OrgResourceHierarchyTraverseConstants
                                .ErrorMessages.ERROR_CODE_INVALID_ANCESTOR_ORGANIZATION_ID_LIST,
                        organizationId);
            }
            return organizationIds;
        } catch (OrganizationManagementServerException e) {
            throw OrgResourceHierarchyTraverseUtil.handleServerException(
                    OrgResourceHierarchyTraverseConstants.ErrorMessages
                            .ERROR_CODE_SERVER_ERROR_WHILE_RESOLVING_ANCESTOR_ORGANIZATIONS, e, organizationId);
        }
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrgResourceHierarchyTraverseServiceDataHolder.getInstance().getApplicationManagementService();
    }
}

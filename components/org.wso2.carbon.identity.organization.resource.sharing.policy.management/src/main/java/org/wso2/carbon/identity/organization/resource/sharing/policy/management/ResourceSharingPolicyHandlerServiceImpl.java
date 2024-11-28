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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.dao.ResourceSharingPolicyHandlerDAO;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.dao.ResourceSharingPolicyHandlerDAOImpl;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implementation of the core service for managing resource sharing policies.
 */
public class ResourceSharingPolicyHandlerServiceImpl implements ResourceSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(ResourceSharingPolicyHandlerServiceImpl.class);
    private static final ResourceSharingPolicyHandlerDAO resourceSharingPolicyHandlerDAO =
            new ResourceSharingPolicyHandlerDAOImpl();
    private static ConcurrentLinkedQueue<String> errorMessages;

    @Override
    public int addResourceSharingPolicy(ResourceSharingPolicy resourceSharingPolicy)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.addResourceSharingPolicyRecord(resourceSharingPolicy);
    }

    @Override
    public List<ResourceSharingPolicy> getResourceSharingPolicies(List<String> organizationIds)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.getResourceSharingPolicies(organizationIds);
    }

    @Override
    public Map<ResourceType, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByResourceType(
            List<String> organizationIds) throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.getResourceSharingPoliciesGroupedByResourceType(organizationIds);
    }

    @Override
    public Map<String, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
            List<String> organizationIds) throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(organizationIds);
    }

    @Override
    public boolean deleteResourceSharingPolicyRecordById(Integer resourceSharingPolicyId, String permittedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.deleteResourceSharingPolicyRecordById(resourceSharingPolicyId,
                permittedOrgId);
    }

    @Override
    public boolean deleteResourceSharingPolicyByResourceTypeAndId(ResourceType resourceType, String resourceId,
                                                                  String permittedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.
                deleteResourceSharingPolicyByResourceTypeAndId(resourceType, resourceId, permittedOrgId);
    }

    @Override
    public boolean addSharedResourceAttributes(List<SharedResourceAttribute> sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.addSharedResourceAttributes(sharedResourceAttributes);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesBySharingPolicyId(Integer resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException, DataAccessException {

        return resourceSharingPolicyHandlerDAO.getSharedResourceAttributesBySharingPolicyId(resourceSharingPolicyId);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesByType(SharedAttributeType attributeType)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.
                getSharedResourceAttributesByType(attributeType);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesById(String attributeId)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.
                getSharedResourceAttributesById(attributeId);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesByTypeAndId
            (SharedAttributeType attributeType, String attributeId) throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.
                getSharedResourceAttributesByTypeAndId(attributeType, attributeId);
    }

    @Override
    public boolean deleteSharedResourceAttributesByResourceSharingPolicyId(Integer resourceSharingPolicyId,
                                                                           SharedAttributeType sharedAttributeType,
                                                                           String permittedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.deleteSharedResourceAttributesByResourceSharingPolicyId(
                resourceSharingPolicyId, sharedAttributeType, permittedOrgId);
    }

    @Override
    public boolean deleteSharedResourceAttributeByAttributeTypeAndId(SharedAttributeType attributeType,
                                                                     String attributeId, String permittedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        return resourceSharingPolicyHandlerDAO.
                deleteSharedResourceAttributeByAttributeTypeAndId(attributeType, attributeId, permittedOrgId);
    }
}

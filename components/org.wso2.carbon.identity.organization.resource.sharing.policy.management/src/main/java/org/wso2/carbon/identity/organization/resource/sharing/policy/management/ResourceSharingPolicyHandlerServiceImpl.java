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
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.dao.ResourceSharingPolicyHandlerDAO;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.dao.ResourceSharingPolicyHandlerDAOImpl;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.SharedResourceAttributes;

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
            throws OrganizationManagementServerException, ResourceSharingPolicyMgtServerException, DataAccessException {

        return resourceSharingPolicyHandlerDAO.addResourceSharingPolicyRecord(resourceSharingPolicy);
    }

    @Override
    public void addSharedResourceAttributes(SharedResourceAttributes sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException {

        resourceSharingPolicyHandlerDAO.addSharedResourceAttributes(sharedResourceAttributes);
    }
}

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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.util;

import org.apache.commons.lang.ArrayUtils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.constant.OrgResourceHierarchyTraverseConstants;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseClientException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseServerException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.internal.OrgResourceHierarchyTraverseServiceDataHolder;

/**
 * Utility class for organization resource hierarchy traverse service.
 */
public class OrgResourceHierarchyTraverseUtil {

    private OrgResourceHierarchyTraverseUtil() {

    }

    /**
     * Get the organization manager.
     *
     * @return Organization manager.
     */
    public static OrganizationManager getOrganizationManager() {

        return OrgResourceHierarchyTraverseServiceDataHolder.getInstance().getOrganizationManager();
    }

    /**
     * Check whether the minimum organization hierarchy depth is reached.
     *
     * @param orgId Organization ID.
     * @return True if the minimum hierarchy depth is reached.
     * @throws OrgResourceHierarchyTraverseServerException If an error occurs while checking the hierarchy depth.
     */
    public static boolean isMinOrgHierarchyDepthReached(String orgId) throws
            OrgResourceHierarchyTraverseServerException {

        int minHierarchyDepth = Utils.getSubOrgStartLevel() - 1;
        try {
            int depthInHierarchy = getOrganizationManager().getOrganizationDepthInHierarchy(orgId);
            return depthInHierarchy < minHierarchyDepth;
        } catch (OrganizationManagementException e) {
            throw new OrgResourceHierarchyTraverseServerException(
                    "Error occurred while getting the hierarchy depth of the organization: " + orgId, e);
        }
    }

    /**
     * Throw an OrgResourceHierarchyTraverseClientException upon client side error while traversing organization
     * resource hierarchy traverse.
     *
     * @param error The error enum.
     * @param data  The error message data.
     * @return OrgResourceHierarchyTraverseClientException
     */
    public static OrgResourceHierarchyTraverseClientException handleClientException(
            OrgResourceHierarchyTraverseConstants.ErrorMessages error, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new OrgResourceHierarchyTraverseClientException(error.getMessage(), description, error.getCode());
    }

    /**
     * Throw an OrgResourceHierarchyTraverseServerException upon server side error while traversing organization
     * resource hierarchy traverse.
     *
     * @param error The error enum.
     * @param e     The error.
     * @param data  The error message data.
     * @return OrgResourceHierarchyTraverseServerException
     */
    public static OrgResourceHierarchyTraverseServerException handleServerException(
            OrgResourceHierarchyTraverseConstants.ErrorMessages error, Throwable e, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new OrgResourceHierarchyTraverseServerException(error.getMessage(), description, error.getCode(), e);
    }
}

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
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.constant.OrgResourceHierarchyTraverseConstants;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseServerException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.internal.OrgResourceHierarchyTraverseServiceDataHolder;

/**
 * Utility class for the Organization Resource Hierarchy Traverse Service.
 * <p>
 * This class provides helper methods to interact with the organization hierarchy and manage traversal logic,
 * such as verifying the hierarchy depth and handling server-side exceptions.
 */
public class OrgResourceHierarchyTraverseUtil {

    /**
     * Private constructor to prevent instantiation of the utility class.
     */
    private OrgResourceHierarchyTraverseUtil() {

    }

    /**
     * Retrieve the organization manager instance.
     *
     * @return The {@link OrganizationManager} instance used to manage organizations.
     */
    public static OrganizationManager getOrganizationManager() {

        return OrgResourceHierarchyTraverseServiceDataHolder.getInstance().getOrganizationManager();
    }

    /**
     * Verify if the organization hierarchy depth has reached the minimum required level.
     * <p>
     * This method obtains the depth of the specified organization in the hierarchy and compares it
     * with the configured minimum depth. An exception is thrown if an error occurs during the depth calculation.
     *
     * @param orgId The ID of the organization to check.
     * @return {@code true} if the hierarchy depth is less than the minimum required depth, {@code false} otherwise.
     * @throws OrgResourceHierarchyTraverseServerException If an error occurs while retrieving the organization's depth.
     */
    public static boolean isMinOrgHierarchyDepthReached(String orgId) throws
            OrgResourceHierarchyTraverseServerException {

        int minHierarchyDepth = Utils.getSubOrgStartLevel() - 1;
        try {
            int depthInHierarchy = getOrganizationManager().getOrganizationDepthInHierarchy(orgId);
            return depthInHierarchy < minHierarchyDepth;
        } catch (OrganizationManagementServerException e) {
            throw new OrgResourceHierarchyTraverseServerException(
                    "Error occurred while getting the hierarchy depth of the organization: " + orgId, e);
        }
    }

    /**
     * Create an {@link OrgResourceHierarchyTraverseServerException} to handle server-side errors.
     * <p>
     * This method formats the error description using the provided data, if applicable, and constructs
     * a custom exception for consistent error handling in the service.
     *
     * @param error The error enumeration containing predefined error messages and codes.
     * @param data  Optional data to format the error message description.
     * @return An {@link OrgResourceHierarchyTraverseServerException} with the formatted error details.
     */
    public static OrgResourceHierarchyTraverseServerException handleServerException(
            OrgResourceHierarchyTraverseConstants.ErrorMessages error, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new OrgResourceHierarchyTraverseServerException(error.getMessage(), description, error.getCode());
    }

    /**
     * Create an {@link OrgResourceHierarchyTraverseServerException} to handle server-side errors.
     * <p>
     * This method formats the error description using the provided data, if applicable, and constructs
     * a custom exception including the underlying cause of the error, for consistent error handling in the service.
     *
     * @param error The error enumeration containing predefined error messages and codes.
     * @param e     The underlying cause of the error.
     * @param data  Optional data to format the error message description.
     * @return An {@link OrgResourceHierarchyTraverseServerException} with the formatted error details.
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

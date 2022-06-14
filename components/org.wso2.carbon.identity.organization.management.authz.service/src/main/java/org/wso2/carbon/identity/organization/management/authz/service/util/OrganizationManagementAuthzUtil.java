/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.authz.service.util;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.core.persistence.UmPersistenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.PERMISSION_SPLITTER;

/**
 * This class provides utility functions for the organization management authorization.
 */
public class OrganizationManagementAuthzUtil {

    /**
     * Get a new Jdbc template.
     *
     * @return a new Jdbc template.
     */
    public static NamedJdbcTemplate getNewTemplate() {

        return new NamedJdbcTemplate(UmPersistenceManager.getInstance().getDataSource());
    }

    public static List<String> getAllowedPermissions(String resourceId) {

        String[] permissionParts = resourceId.split(PERMISSION_SPLITTER);
        List<String> allowedPermissions = new ArrayList<>();
        for (int i = 0; i < permissionParts.length - 1; i++) {
            allowedPermissions.add(String.join(PERMISSION_SPLITTER,
                    subArray(permissionParts, permissionParts.length - i)));
        }
        return allowedPermissions;
    }

    private static <T> T[] subArray(T[] array, int end) {

        return Arrays.copyOfRange(array, 0, end);
    }
}

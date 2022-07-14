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

package org.wso2.carbon.identity.organization.management.oauth2.grant.util;

import org.apache.commons.lang.ArrayUtils;
import org.wso2.carbon.identity.organization.management.oauth2.grant.exception.OrganizationSwitchGrantClientException;
import org.wso2.carbon.identity.organization.management.oauth2.grant.exception.OrganizationSwitchGrantServerException;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;

/**
 * This class provides utility functions for the Organization Switch grant.
 */
public class OrganizationSwitchGrantUtil {

    public static OrganizationSwitchGrantClientException handleClientException(
            OrganizationManagementConstants.ErrorMessages error, String...  data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new OrganizationSwitchGrantClientException(error.getMessage(), description, error.getCode());
    }

    public static OrganizationSwitchGrantServerException handleServerException(
            OrganizationManagementConstants.ErrorMessages error, Throwable  e) {

        return new OrganizationSwitchGrantServerException(error.getMessage(), error.getCode(), e);
    }

    public static OrganizationSwitchGrantServerException handleServerException(
            OrganizationManagementConstants.ErrorMessages error, Throwable  e, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }

        return new OrganizationSwitchGrantServerException(error.getMessage(), description, error.getCode(), e);
    }
}

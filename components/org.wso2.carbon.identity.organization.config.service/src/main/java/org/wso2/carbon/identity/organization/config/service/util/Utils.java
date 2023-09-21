/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.config.service.util;

import org.apache.commons.lang.ArrayUtils;
import org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigClientException;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigServerException;

/**
 * This class provides utility functions for the organization configuration management.
 */
public class Utils {

    /**
     * Throw an OrganizationConfigClientException upon client side error in organization configuration management.
     *
     * @param error The error enum.
     * @param data  The error message data.
     * @return OrganizationConfigClientException
     */
    public static OrganizationConfigClientException handleClientException(
            OrganizationConfigConstants.ErrorMessages error, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new OrganizationConfigClientException(error.getCode(), error.getMessage(), description);
    }

    /**
     * Throw an OrganizationConfigServerException upon server side error in organization management.
     *
     * @param error The error enum.
     * @param e     The error.
     * @param data  The error message data.
     * @return OrganizationConfigServerException
     */
    public static OrganizationConfigServerException handleServerException(
            OrganizationConfigConstants.ErrorMessages error, Throwable e, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new OrganizationConfigServerException(error.getCode(), error.getMessage(), description, e);
    }
}

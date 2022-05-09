/*
 *
 *  * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.organization.valve.util;


import org.apache.catalina.connector.Request;

import static org.wso2.carbon.identity.organization.management.organization.valve.constants.OrganizationManagementOrganizationValveConstants.DEFAULT_ORGANIZATION_DOMAIN;

/**
 * Utility class for Organization Management Organization Valve.
 */
public class Utils {

    /**
     * Getting Organization Domain from the URL.
     * @param request Request that user is invoking.
     * @return  The organization domain.
     */
    public static String getOrganizationDomainFromURLMapping(Request request){

        String requestURI = request.getRequestURI();
        String domain = DEFAULT_ORGANIZATION_DOMAIN;

        if(requestURI.contains("/o/")){
            String temp = requestURI.substring(requestURI.indexOf("/0/") + 3);
            int index = temp.indexOf('/');
            if (index != -1) {
                temp = temp.substring(0, index);
                domain = temp;
            }
        }
        return domain;
    }
}

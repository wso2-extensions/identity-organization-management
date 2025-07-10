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

package org.wso2.carbon.identity.organization.discovery.service.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.CO;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.EQ;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.EW;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SW;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_NAME_COLUMN;

/**
 * This class holds the constants related to organization discovery service.
 */
public class DiscoveryConstants {

    public static final String ENABLE_CONFIG = ".enable";
    public static final String PRE_ADD_USER_EMAIL_DOMAIN_VALIDATE = "PRE_ADD_USER_EMAIL_DOMAIN_VALIDATE";
    public static final String ORGANIZATION_NAME = "organizationName";
    public static final String EMAIL_DOMAIN_DISCOVERY_TYPE = "emailDomain";
    public static final Set<String> SUPPORTED_OPERATIONS;
    public static final Map<String, String> ATTRIBUTE_COLUMN_MAP;

    static {
        Set<String> operations = new HashSet<>(Arrays.asList(SW, EW, CO, EQ));
        SUPPORTED_OPERATIONS = Collections.unmodifiableSet(operations);
    }

    static {
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put(ORGANIZATION_NAME, VIEW_NAME_COLUMN);
        ATTRIBUTE_COLUMN_MAP = Collections.unmodifiableMap(attributeMap);
    }
}

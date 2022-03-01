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

package org.wso2.carbon.identity.organization.management.authz.service.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants related to organization management authorization service.
 */
public class AuthorizationConstants {

    public static final String PERMISSION_SPLITTER = "/";
    public static final String URI_SPLITTER = "/";
    public static final String RESOURCE_PERMISSION_NONE = "none";
    public static final String ORGANIZATION_RESOURCE = "organizations";
    public static final String REGEX_FOR_URLS_WITH_ORG_ID =
            "^(.)*(/api/identity/organization-mgt/v1.0/organizations/)[a-z0-9]{8}(-[a-z0-9]{4}){3}-[a-z0-9]{12}(.)*$";
    public static final String SCIM_ROLE_ID_ATTR_NAME = "urn:ietf:params:scim:schemas:core:2.0:id";

    public static final String HTTP_GET = "GET";
    public static final String HTTP_DELETE = "DELETE";
    public static final String HTTP_PATCH = "PATCH";
    public static final String HTTP_PUT = "PUT";

    private static Map<String, String> scopePermissionMap = new HashMap<>();

    static {

        // todo: read from file.
        scopePermissionMap.put("internal_organization_view",
                "/permission/admin/manage/identity/organizationmgt/view");
        scopePermissionMap.put("internal_organization_update",
                "/permission/admin/manage/identity/organizationmgt/update");
        scopePermissionMap.put("internal_organization_delete",
                "/permission/admin/manage/identity/organizationmgt/delete");
    }

    public static final Map<String, String> SCOPE_PERMISSION_MAP = Collections.unmodifiableMap(scopePermissionMap);
}

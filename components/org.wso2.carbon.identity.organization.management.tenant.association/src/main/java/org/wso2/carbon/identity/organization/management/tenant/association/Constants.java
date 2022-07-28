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

package org.wso2.carbon.identity.organization.management.tenant.association;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Constants related to user-tenant association.
 */
public class Constants {

    public static final String ORG_MGT_PERMISSION = "/permission/admin/manage/identity/organizationmgt";
    public static final String ORG_ROLE_MGT_PERMISSION = "/permission/admin/manage/identity/rolemgt";
    public static final String SESSION_MGT_VIEW_PERMISSION =
            "/permission/admin/manage/identity/authentication/session/view";
    public static final String GROUP_MGT_VIEW_PERMISSION = "/permission/admin/manage/identity/groupmgt/view";
    public static final String GOVERNANCE_VIEW_PERMISSION = "/permission/admin/manage/identity/governance/view";
    public static final String USER_STORE_CONFIG_VIEW_PERMISSION =
            "/permission/admin/manage/identity/userstore/config/view";
    public static final String USER_MGT_VIEW_PERMISSION = "/permission/admin/manage/identity/usermgt/view";
    public static final String USER_MGT_LIST_PERMISSION = "/permission/admin/manage/identity/usermgt/list";
    public static final String APPLICATION_MGT_VIEW_PERMISSION =
            "/permission/admin/manage/identity/applicationmgt/view";
    public static final String CORS_CONFIG_MGT_VIEW_PERMISSION = "/permission/admin/manage/identity/cors/origins/view";
    public static final String IDP_MGT_VIEW_PERMISSION = "/permission/admin/manage/identity/idpmgt/view";
    public static final String CLAIM_META_DATA_MGT_VIEW_PERMISSION =
            "/permission/admin/manage/identity/claimmgt/metadata/view";
    public static final String USER_MGT_CREATE_PERMISSION = "/permission/admin/manage/identity/usermgt/create";


    /*
    Minimum permissions required for org creator to logged in to the console and view user, groups, roles, SP,
    IDP sections.
    */
    public static final List<String> MINIMUM_PERMISSIONS_REQUIRED_FOR_ORG_CREATOR_VIEW =
            Collections.unmodifiableList(Arrays
                    .asList(SESSION_MGT_VIEW_PERMISSION, GROUP_MGT_VIEW_PERMISSION, GOVERNANCE_VIEW_PERMISSION,
                            USER_STORE_CONFIG_VIEW_PERMISSION, USER_MGT_VIEW_PERMISSION, USER_MGT_LIST_PERMISSION,
                            APPLICATION_MGT_VIEW_PERMISSION, CORS_CONFIG_MGT_VIEW_PERMISSION, IDP_MGT_VIEW_PERMISSION,
                            CLAIM_META_DATA_MGT_VIEW_PERMISSION));
}

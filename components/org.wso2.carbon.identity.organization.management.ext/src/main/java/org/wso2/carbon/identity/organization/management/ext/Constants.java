/*
 * Copyright (c) 2022-2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.ext;

/**
 * Organization management extensions constants.
 */
public class Constants {

    public static final String EVENT_PROP_ORGANIZATION = "ORGANIZATION";
    public static final String EVENT_PROP_ORGANIZATION_ID = "ORGANIZATION_ID";
    public static final String EVENT_PROP_ORGANIZATION_DEPTH_IN_HIERARCHY = "ORGANIZATION_DEPTH_IN_HIERARCHY";
    public static final String EVENT_PRE_ADD_ORGANIZATION = "PRE_ADD_ORGANIZATION";
    public static final String EVENT_POST_ADD_ORGANIZATION = "POST_ADD_ORGANIZATION";
    public static final String EVENT_PRE_GET_ORGANIZATION = "PRE_GET_ORGANIZATION";
    public static final String EVENT_POST_GET_ORGANIZATION = "POST_GET_ORGANIZATION";
    public static final String EVENT_PRE_DELETE_ORGANIZATION = "PRE_DELETE_ORGANIZATION";
    public static final String EVENT_POST_DELETE_ORGANIZATION = "POST_DELETE_ORGANIZATION";
    public static final String EVENT_PRE_PATCH_ORGANIZATION = "PRE_PATCH_ORGANIZATION";
    public static final String EVENT_POST_PATCH_ORGANIZATION = "POST_PATCH_ORGANIZATION";
    public static final String EVENT_PRE_UPDATE_ORGANIZATION = "PRE_UPDATE_ORGANIZATION";
    public static final String EVENT_POST_UPDATE_ORGANIZATION = "POST_UPDATE_ORGANIZATION";
    public static final String EVENT_POST_ENABLE_ORGANIZATION = "POST_ENABLE_ORGANIZATION";
    public static final String EVENT_POST_DISABLE_ORGANIZATION = "POST_DISABLE_ORGANIZATION";
    public static final String EVENT_PROP_PATCH_OPERATIONS = "PATCH_OPERATIONS";
    public static final String EVENT_PROP_PREVIOUS_ORGANIZATION = "PREVIOUS_ORGANIZATION";
    public static final String IS_SUBSEQUENT_OPERATION_OF_ADD_ORGANIZATION = "SUBSEQUENT_OPERATION_OF_ADD_ORGANIZATION";
    // Thread-local policy used while creating an organization through the onboarding dispatcher.
    public static final String SKIP_AUTO_CONSOLE_ADMIN_ASSIGNMENT = "SKIP_AUTO_CONSOLE_ADMIN_ASSIGNMENT";

    /*
     Organization user sharing events. Published once per user per organization, when a shared user is created in
     (POST_SHARE_USER) or removed from (POST_UNSHARE_USER) an organization by user sharing. The events carry the
     shared user and the organization it was shared to, and the user it was shared from and that user's organization.
    */
    public static final String EVENT_POST_SHARE_USER = "POST_SHARE_USER";
    public static final String EVENT_POST_UNSHARE_USER = "POST_UNSHARE_USER";
    public static final String EVENT_PROP_SHARED_USER_ID = "SHARED_USER_ID";
    public static final String EVENT_PROP_SHARED_ORGANIZATION_ID = "SHARED_ORGANIZATION_ID";
    public static final String EVENT_PROP_SHARED_TENANT_DOMAIN = "SHARED_TENANT_DOMAIN";
    public static final String EVENT_PROP_ASSOCIATED_USER_ID = "ASSOCIATED_USER_ID";
    public static final String EVENT_PROP_ASSOCIATED_ORGANIZATION_ID = "ASSOCIATED_ORGANIZATION_ID";
    /*
     POST_SHARE_USER only: the roles assigned to the shared user by the sharing, as a List<Map<String, String>> keyed
     by EVENT_PROP_ROLE_ID (the role of the organization the user was shared from) and EVENT_PROP_SHARED_ROLE_ID (the
     role of the organization the user was shared to).
    */
    public static final String EVENT_PROP_ROLES = "ROLES";
    public static final String EVENT_PROP_ROLE_ID = "ROLE_ID";
    public static final String EVENT_PROP_SHARED_ROLE_ID = "SHARED_ROLE_ID";
    // Thread-local flag set while a shared user is created or removed by user sharing.
    public static final String USER_SHARING_OPERATION = "userSharingOperation";
}

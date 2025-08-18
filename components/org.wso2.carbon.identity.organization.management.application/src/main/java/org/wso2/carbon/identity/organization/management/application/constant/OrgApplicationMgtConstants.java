/*
 * Copyright (c) 2022-2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.constant;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains constants related to organization application management.
 */
public class OrgApplicationMgtConstants {

    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String ORGANIZATION_ID = "id";
    public static final String PARENT_ORGANIZATION_ID = "parentId";
    private static final Map<String, String> attributeColumnMap = new HashMap<>();
    public static final Map<String, String> SP_SHARED_ATTRIBUTE_COLUMN_MAP =
            Collections.unmodifiableMap(attributeColumnMap);

    static {
        attributeColumnMap.put(BEFORE, SQLConstants.ID_COLUMN_NAME);
        attributeColumnMap.put(AFTER, SQLConstants.ID_COLUMN_NAME);
        attributeColumnMap.put(ORGANIZATION_ID, SQLConstants.SHARED_ORG_ID_COLUMN_NAME);
    }

    public static final String SP_SHARED_ROLE_EXCLUDED_KEY = "roles";
    public static final String SP_SHARED_SHARING_MODE_INCLUDED_KEY = "sharingMode";
    public static final Set<String> SP_SHARED_SUPPORTED_EXCLUDED_ATTRIBUTES =
            Collections.unmodifiableSet(new HashSet<>(Collections.singleton(SP_SHARED_ROLE_EXCLUDED_KEY)));
    public static final Set<String> SP_SHARED_SUPPORTED_INCLUDED_ATTRIBUTES =
            Collections.unmodifiableSet(new HashSet<>(Collections.singleton(SP_SHARED_SHARING_MODE_INCLUDED_KEY)));

    public static final String TENANT = "TENANT";
    public static final String AUTH_TYPE_OAUTH_2 = "oauth2";
    public static final String IS_FRAGMENT_APP = "isFragmentApp";
    public static final String SHARE_WITH_ALL_CHILDREN = "shareWithAllChildren";
    public static final String ROLE_SHARING_MODE = "roleSharingMode";
    public static final String CORRELATION_ID_MDC = "Correlation-ID";

    public static final String ORGANIZATION_LOGIN_AUTHENTICATOR = "OrganizationAuthenticator";
    public static final String DELETE_FRAGMENT_APPLICATION = "deleteFragmentApplication";
    public static final String DELETE_MAIN_APPLICATION = "deleteMainApplication";
    public static final String UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN = "updateShareWithAllChildren";
    /* This constant is used to skip the organization hierarchy validation when sharing an application. This is just
     keep the backward compatibility for existing application share endpoint. DO NOT use this in any new features. */
    public static final String SKIP_ORGANIZATION_HIERARCHY_VALIDATION = "skipOrganizationHierarchyValidation";
    public static final String DELETE_SHARE_FOR_MAIN_APPLICATION = "deleteShareForMainApp";
    public static final String USER_CUSTOM_ATTRIBUTE_PROPERTY = "USER_CUSTOM_ATTRIBUTE";
    public static final String ORGANIZATION_SSO_IDP_IMAGE_URL = "assets/images/logos/sso.svg";

    public static final String USER_ORGANIZATION_CLAIM_URI = "http://wso2.org/claims/runtime/user_organization";
    public static final String APP_ROLES_CLAIM_URI = "http://wso2.org/claims/applicationRoles";
    public static final String ROLES_CLAIM_URI = "http://wso2.org/claims/roles";
    public static final String USER_ORGANIZATION_CLAIM = "user_organization";
    public static final String OIDC_CLAIM_DIALECT_URI = "http://wso2.org/oidc/claim";
    public static final String RUNTIME_CLAIM_URI_PREFIX = "http://wso2.org/claims/runtime/";

    // Event constants related to shared application management.
    /* Use this to represent the parent organization id of the application being shared. Not the root organization id.
     There maybe places where this has been used to represent the root organization id, but it is not the intended use.
     With new selective role sharing feature, this will be used to represent the parent organization id of
     the application*/
    public static final String EVENT_PROP_PARENT_ORGANIZATION_ID = "PARENT_ORGANIZATION_ID";
    public static final String EVENT_PROP_MAIN_ORGANIZATION_ID = "MAIN_ORGANIZATION_ID";
    public static final String EVENT_PROP_SHARED_ORGANIZATION_ID = "SHARED_ORGANIZATION_ID";
    public static final String EVENT_PROP_PARENT_APPLICATION_ID = "PARENT_APPLICATION_ID";
    public static final String EVENT_PROP_MAIN_APPLICATION_ID = "MAIN_APPLICATION_ID";
    public static final String EVENT_PROP_SHARED_APPLICATION_ID = "SHARED_APPLICATION_ID";
    public static final String EVENT_PROP_SHARED_APPLICATIONS_DATA = "SHARED_APPLICATIONS_DATA";
    public static final String EVENT_PROP_SHARE_WITH_ALL_CHILDREN = "SHARE_WITH_ALL_CHILDREN";
    public static final String EVENT_PROP_SHARED_ORGANIZATIONS = "SHARED_ORGANIZATIONS";
    public static final String EVENT_PROP_SHARED_USER_ATTRIBUTES = "SHARED_USER_ATTRIBUTES";
    public static final String EVENT_PROP_ROLE_SHARING_CONFIG = "ROLE_SHARING_CONFIG";
    public static final String EVENT_PROP_ROLE_AUDIENCES = "ROLE_AUDIENCES";
    public static final String EVENT_PROP_UPDATE_OPERATION = "UPDATE_OPERATION";
    public static final String EVENT_PROP_RESOURCE_SHARING_POLICY_ID = "RESOURCE_SHARING_POLICY_ID";
    public static final String EVENT_PRE_SHARE_APPLICATION = "PRE_SHARE_APPLICATION";
    public static final String EVENT_POST_SHARE_APPLICATION = "POST_SHARE_APPLICATION";
    public static final String PRE_UPDATE_ROLES_OF_SHARED_APPLICATION = "PRE_UPDATE_ROLES_OF_SHARED_APPLICATION";
    public static final String POST_UPDATE_ROLES_OF_SHARED_APPLICATION = "POST_UPDATE_ROLES_OF_SHARED_APPLICATION";
    public static final String EVENT_PRE_DELETE_SHARED_APPLICATION = "PRE_DELETE_SHARED_APPLICATION";
    public static final String EVENT_POST_DELETE_SHARED_APPLICATION = "POST_DELETE_SHARED_APPLICATION";
    public static final String EVENT_PRE_DELETE_ALL_SHARED_APPLICATIONS = "PRE_DELETE_ALL_SHARED_APPLICATIONS";
    public static final String EVENT_POST_DELETE_ALL_SHARED_APPLICATIONS = "POST_DELETE_ALL_SHARED_APPLICATIONS";
    public static final String EVENT_PRE_GET_APPLICATION_SHARED_ORGANIZATIONS =
            "PRE_GET_APPLICATION_SHARED_ORGANIZATIONS";
    public static final String EVENT_POST_GET_APPLICATION_SHARED_ORGANIZATIONS =
            "POST_GET_APPLICATION_SHARED_ORGANIZATIONS";
    public static final String EVENT_PRE_GET_SHARED_APPLICATIONS = "PRE_GET_SHARED_APPLICATIONS";
    public static final String EVENT_POST_GET_SHARED_APPLICATIONS = "POST_GET_SHARED_APPLICATIONS";
    public static final String TENANT_CONTEXT_PATH_COMPONENT = "/t/%s";

    public static final String APPLICATION_ALREADY_EXISTS_ERROR_CODE = "APP-60007";
    public static final String B2B_APPLICATION = "APPLICATION";

    /**
     * Enum representing the types of share policies.
     */
    public enum SharePolicy {

        DO_NOT_SHARE("DO_NOT_SHARE"),
        SELECTIVE_SHARE("SELECTIVE_SHARE"),
        SHARE_WITH_ALL("SHARE_WITH_ALL");

        private final String value;

        SharePolicy(String value) {

            this.value = value;
        }

        public String getValue() {

            return value;
        }
    }

    /**
     * Enum representing the types of share operations.
     */
    public enum ShareOperationType {

        APPLICATION_SHARE("B2B_APPLICATION_SHARE"),
        APPLICATION_UNSHARE("B2B_APPLICATION_UNSHARE");

        private final String value;

        ShareOperationType(String value) {

            this.value = value;
        }

        public String getValue() {

            return value;
        }
    }

    private static final String ORGANIZATION_APPLICATION_MANAGEMENT_ERROR_CODE_PREFIX = "OAM-";

    /**
     * Error messages related to organization application management.
     */
    public enum ErrorMessages {

        ERROR_CODE_INVALID_FILTER_VALUE("60001", "Unable to retrieve app-shared organizations.",
                "An invalid filter value was used for filtering."),
        ERROR_CODE_INVALID_ORGANIZATION_PATH_FILTER("60002", "Invalid organization path filter.",
                "The organization path filter is invalid. It should be in the format: " +
                        "'organizations[orgId eq \"<orgIdValue>\"].roles'."),
        ERROR_CODE_INVALID_SHARING_ORG_ID("60003", "Invalid sharing organization ID.",
                "The organization ID is either missing or invalid. " +
                        "Please ensure a valid organization ID is provided."),
        ERROR_CODE_INVALID_SELECTIVE_SHARING_POLICY("60004", "Invalid or empty sharing policy.",
                "The provided sharing policy is empty or invalid. Ensure that the policy is either " +
                        PolicyEnum.SELECTED_ORG_ONLY.getValue() + " or " +
                        PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN.getValue() + "."),
        ERROR_CODE_INVALID_ORGANIZATION_SHARE_CONFIGURATION("60005",
                "Invalid organization share configuration.",
                "The organization share configuration is invalid. " +
                        "Please ensure a valid configuration with the correct hierarchy."),
        ERROR_CODE_INVALID_ROLE_SHARING_MODE("60006", "Invalid role sharing mode.",
                "The role sharing mode is invalid. " +
                        "Ensure that it is set to one of the following: ALL, NONE, or SELECTED."),
        ERROR_CODE_INVALID_ROLE_SHARING_OPERATION("60007", "Invalid role sharing operation.",
                "The role sharing operation is invalid. " +
                        "Ensure that a valid role sharing mode is specified, along with roles and the audience list."),

        // Server errors.
        ERROR_CODE_ERROR_RETRIEVING_SHARED_APP("65001", "Unable to retrieve shared applications.",
                "An error occurred while retrieving shared applications."),
        ERROR_CODE_ERROR_RETRIEVING_SHARED_APP_ROLES("65002", "Unable to retrieve shared app roles.",
                "An error occurred while retrieving shared application roles."),
        ERROR_CODE_ERROR_RETRIEVING_APP_ROLE_ALLOWED_AUDIENCE("65003",
                "Unable to retrieve application allowed audience for role association.",
                "An error occurred while retrieving allowed audience for role association " +
                        "for the application: %s.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return ORGANIZATION_APPLICATION_MANAGEMENT_ERROR_CODE_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }
}

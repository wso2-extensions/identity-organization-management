/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.role.management.service.constant;

/**
 * Contains the constant of Role Management.
 */
public class RoleManagementConstants {

    public static final String ROLE_ACTION = "ui.execute";

    public static final String GROUPS = "groups";
    public static final String USERS = "users";
    public static final String PERMISSIONS = "permissions";
    public static final String DISPLAY_NAME = "displayName";

    public static final String AND_OPERATOR = "and";
    public static final String OR_OPERATOR = "or";

    public static final String FILTER_ID_PLACEHOLDER = "FILTER_ID_%d";

    public static final String ROLE_NAME_FIELD = "name";

    public static final String COMMA_SEPARATOR = ",";
    public static final String UNION_SEPARATOR = " UNION ALL ";

    public static final String ORG_CREATOR_ROLE = "org-creator";
    public static final String ORG_SWITCHER_ROLE = "org-switcher";

    /**
     * Enum for cursor based pagination direction.
     */
    public enum CursorDirection {
        FORWARD,
        BACKWARD
    }

    /**
     * Enum for Filter Operations.
     */
    public enum FilterOperator {

        EQ("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " = :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        SW("", "%") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        EW("%", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        CO("%", "%") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        GE("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " >= :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        LE("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " <= :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        GT("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " > :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        LT("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " < :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        };

        private final String prefix;
        private final String suffix;

        FilterOperator(String prefix, String suffix) {

            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String getPrefix() {

            return prefix;
        }

        public String getSuffix() {

            return suffix;
        }

        /**
         * Abstract class for filter builder functions.
         *
         * @param count filter amount.
         * @return The filter string.
         */
        public abstract String applyFilterBuilder(int count);
    }
}

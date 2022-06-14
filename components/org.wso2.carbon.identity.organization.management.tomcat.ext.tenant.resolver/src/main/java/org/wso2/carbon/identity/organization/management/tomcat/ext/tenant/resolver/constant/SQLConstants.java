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

package org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.constant;

/**
 * This class contains database queries related to organization management tenant resolving logic.
 */
public class SQLConstants {

    public static final String GET_ORGANIZATION_TENANT_DOMAIN = "SELECT UM_DOMAIN_NAME FROM UM_TENANT WHERE UM_ID IN " +
            "(SELECT UM_TENANT_ID FROM UM_ORG WHERE UM_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_ORG_ID + ";)";

    /**
     * SQL placeholders.
     */
    public static final class SQLPlaceholders {

        public static final String DB_SCHEMA_COLUMN_ORG_ID = "ID";
    }
}

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

import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_USER_ID;

/**
 * This class contains database queries related to organization management authorization.
 */
public class SQLConstants {

    public static final String GET_PERMISSIONS_FOR_USER = "SELECT UM_RESOURCE_ID FROM UM_PERMISSION WHERE UM_ID IN " +
            "(SELECT UM_PERMISSION_ID FROM UM_ROLE_PERMISSION WHERE UM_ROLE_NAME IN " +
            "(SELECT UM_ROLE_NAME FROM UM_HYBRID_ROLE WHERE UM_ID IN " +
            "(SELECT UM_HYBRID_ROLE_ID FROM UM_USER_ROLE_ORG WHERE UM_USER_ID = :" + DB_SCHEMA_COLUMN_USER_ID +
            "; AND ORG_ID = :" + DB_SCHEMA_COLUMN_ORGANIZATION_ID +
            "; AND UM_TENANT_ID = :" + DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; )))";

    /**
     * SQL placeholders.
     */
    public static final class SQLPlaceholders {

        public static final String DB_SCHEMA_COLUMN_USER_ID = "ID";
        public static final String DB_SCHEMA_COLUMN_ORGANIZATION_ID = "NAME";
        public static final String DB_SCHEMA_COLUMN_NAME_TENANT_ID = "TENANT_ID";
    }
}

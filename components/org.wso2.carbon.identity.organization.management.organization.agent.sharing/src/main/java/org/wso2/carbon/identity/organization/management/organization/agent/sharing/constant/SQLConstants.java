/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant;

import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_DOMAIN_NAME;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_EDIT_OPERATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_UUID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_NAME_AGENT_NAMES;

/**
 * SQL constants for organization agent sharing.
 */
public class SQLConstants {

    public static final String ID_COLUMN_NAME = "UM_ID";
    public static final String SHARED_ORG_ID_COLUMN_NAME = "UM_ORG_ID";

    public static final String GET_SHARED_ROLES_OF_SHARED_AGENT =
            "SELECT H.UM_UUID " +
                    "FROM UM_HYBRID_USER_ROLE UHR " +
                    "INNER JOIN UM_HYBRID_ROLE H " +
                    "ON UHR.UM_ROLE_ID = H.UM_ID " +
                    "AND UHR.UM_TENANT_ID = H.UM_TENANT_ID " +
                    "INNER JOIN UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS UHRREP " +
                    "ON UHR.UM_ID = UHRREP.UM_HYBRID_USER_ROLE_ID " +
                    "AND UHR.UM_TENANT_ID = UHRREP.UM_HYBRID_USER_ROLE_TENANT_ID " +
                    "WHERE UHR.UM_USER_NAME = :" + SQLPlaceholders.COLUMN_NAME_UM_AGENT_NAME + "; " +
                    "AND UHR.UM_TENANT_ID = :" + COLUMN_NAME_UM_TENANT_ID + "; " +
                    "AND UHR.UM_DOMAIN_ID = (SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE " +
                    "UM_TENANT_ID = :" + COLUMN_NAME_UM_TENANT_ID + "; " +
                    "AND UM_DOMAIN_NAME = :" + COLUMN_NAME_UM_DOMAIN_NAME + ";);";
    public static final String GET_RESTRICTED_AGENT_NAMES_BY_ROLE_AND_ORG =
            "SELECT r.UM_USER_NAME FROM UM_HYBRID_USER_ROLE r "
                    + "INNER JOIN UM_DOMAIN d "
                    + "ON r.UM_DOMAIN_ID = d.UM_DOMAIN_ID AND r.UM_TENANT_ID = d.UM_TENANT_ID "
                    + "INNER JOIN UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS p "
                    + "ON r.UM_ID = p.UM_HYBRID_USER_ROLE_ID AND r.UM_TENANT_ID = p.UM_HYBRID_USER_ROLE_TENANT_ID "
                    + "INNER JOIN UM_HYBRID_ROLE h "
                    + "ON r.UM_ROLE_ID = h.UM_ID AND r.UM_TENANT_ID = h.UM_TENANT_ID "
                    + "WHERE h.UM_UUID = :" + COLUMN_NAME_UM_UUID + "; "
                    + "AND d.UM_DOMAIN_NAME = :" + COLUMN_NAME_UM_DOMAIN_NAME + "; "
                    + "AND r.UM_USER_NAME IN (" + PLACEHOLDER_NAME_AGENT_NAMES + ") "
                    + "AND r.UM_TENANT_ID = :" + COLUMN_NAME_UM_TENANT_ID + "; "
                    + "AND p.UM_PERMITTED_ORG_ID != :" + COLUMN_NAME_UM_PERMITTED_ORG_ID + "; "
                    + "AND p.UM_EDIT_OPERATION = :" + COLUMN_NAME_UM_EDIT_OPERATION + ";";
    public static final String GET_SHARED_AGENT_ROLES =
            "SELECT DISTINCT hr.UM_UUID FROM UM_HYBRID_ROLE hr "
                    + "INNER JOIN UM_HYBRID_USER_ROLE ur "
                    + "ON hr.UM_ID = ur.UM_ROLE_ID "
                    + "AND hr.UM_TENANT_ID = ur.UM_TENANT_ID "
                    + "INNER JOIN UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS rep "
                    + "ON ur.UM_ID = rep.UM_HYBRID_USER_ROLE_ID "
                    + "AND ur.UM_TENANT_ID = rep.UM_HYBRID_USER_ROLE_TENANT_ID "
                    + "WHERE hr.UM_UUID IN (" + SQLPlaceholders.PLACEHOLDER_ROLE_IDS + ") "
                    + "AND hr.UM_TENANT_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID + ";";
    public static final String GET_AGENT_ROLE_IN_TENANT =
            "SELECT UR.UM_ID FROM UM_HYBRID_USER_ROLE UR " +
                    "INNER JOIN UM_HYBRID_ROLE H " +
                    "ON UR.UM_ROLE_ID = H.UM_ID " +
                    "AND UR.UM_TENANT_ID = H.UM_TENANT_ID " +
                    "INNER JOIN UM_DOMAIN D " +
                    "ON UR.UM_DOMAIN_ID = D.UM_DOMAIN_ID " +
                    "AND UR.UM_TENANT_ID = D.UM_TENANT_ID " +
                    "WHERE UR.UM_USER_NAME = :" + SQLPlaceholders.COLUMN_NAME_UM_AGENT_NAME + "; " +
                    "AND H.UM_UUID = :" + SQLPlaceholders.COLUMN_NAME_UM_UUID + "; " +
                    "AND UR.UM_TENANT_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID + "; " +
                    "AND D.UM_DOMAIN_NAME = :" + SQLPlaceholders.COLUMN_NAME_UM_DOMAIN_NAME + ";";
    public static final String INSERT_RESTRICTED_EDIT_PERMISSION_FOR_AGENT =
            "INSERT INTO UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS (" +
                    "UM_HYBRID_USER_ROLE_ID, UM_HYBRID_USER_ROLE_TENANT_ID, " +
                    "UM_EDIT_OPERATION, UM_PERMITTED_ORG_ID) VALUES (" +
                    ":" + SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_ID + ";, " +
                    ":" + SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID + ";, " +
                    ":" + SQLPlaceholders.COLUMN_NAME_UM_EDIT_OPERATION + ";, " +
                    ":" + SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID + ";)";

    /**
     * SQL placeholders related to organization agent sharing SQL operations.
     */
    public static final class SQLPlaceholders {

        public static final String COLUMN_NAME_UM_AGENT_NAME = "UM_AGENT_NAME";
        public static final String COLUMN_NAME_UM_TENANT_ID = "UM_TENANT_ID";
        public static final String COLUMN_NAME_UM_ID = "UM_ID";
        public static final String COLUMN_NAME_UM_UUID = "UM_UUID";
        public static final String COLUMN_NAME_UM_DOMAIN_NAME = "UM_DOMAIN_NAME";
        public static final String COLUMN_NAME_UM_HYBRID_USER_ROLE_ID = "UM_HYBRID_USER_ROLE_ID";
        public static final String COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID = "UM_HYBRID_USER_ROLE_TENANT_ID";
        public static final String COLUMN_NAME_UM_EDIT_OPERATION = "UM_EDIT_OPERATION";
        public static final String COLUMN_NAME_UM_PERMITTED_ORG_ID = "UM_PERMITTED_ORG_ID";

        public static final String PLACEHOLDER_NAME_AGENT_NAMES = "AGENT_NAMES";
        public static final String PLACEHOLDER_ROLE_IDS = "ROLE_IDS";

        public static final String OR = "OR";
    }
}

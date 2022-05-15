/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.enterprise.login.mgt.core.dao;

/**
 * SQL queries related to data access layer of enterprise login management.
 */
public class SQLQueries {

    private SQLQueries() {

    }

    public static final String ADD_ENTERPRISE_LGN_MAPPING = "INSERT INTO ENTERPRISE_LOGIN_MGT" +
            "(INBOUND_SP_ID, INBOUND_SP_TENANT_ID, OUTBOUND_SP_ID, OUTBOUND_SP_TENANT_ID)" +
            " VALUES (?,?,?,?) ";

    public static final String REMOVE_ENTERPRISE_LGN_MAPPING = "DELETE FROM ENTERPRISE_LOGIN_MGT WHERE " +
            "INBOUND_SP_TENANT_ID = ? AND OUTBOUND_SP_TENANT_ID = ?";

    public static final String REMOVE_ENTERPRISE_LGN_MAPPING_OF_SERVICE = "DELETE FROM ENTERPRISE_LOGIN_MGT WHERE" +
            " INBOUND_SP_TENANT_ID = ? AND OUTBOUND_SP_TENANT_ID = ? AND INBOUND_SP_ID = ?";

    public static final String GET_ENTERPRISE_LGN_MAPPING = "SELECT * FROM ENTERPRISE_LOGIN_MGT WHERE " +
            "OUTBOUND_SP_TENANT_ID = ? AND INBOUND_SP_TENANT_ID = ? ";

    public static final String ADD_EMAIL_DOMAINS = "INSERT INTO ENTERPRISE_LOGIN_EMAIL_DOMAIN " +
            "(OUTBOUND_SP_TENANT_ID, INBOUND_SP_TENANT_ID, EMAIL_DOMAIN) VALUES (?,?,?) ";

    public static final String GET_EMAIL_DOMAINS = "SELECT EMAIL_DOMAIN FROM ENTERPRISE_LOGIN_EMAIL_DOMAIN" +
            " WHERE OUTBOUND_SP_TENANT_ID = ? AND INBOUND_SP_TENANT_ID = ? ";

    public static final String DELETE_EMAIL_DOMAINS = "DELETE FROM ENTERPRISE_LOGIN_EMAIL_DOMAIN" +
            " WHERE INBOUND_SP_TENANT_ID = ? AND OUTBOUND_SP_TENANT_ID = ? ";

    public static final String DELETE_SPECIFIC_EMAIL_DOMAIN = "DELETE FROM ENTERPRISE_LOGIN_EMAIL_DOMAIN" +
            " WHERE INBOUND_SP_TENANT_ID = ? AND OUTBOUND_SP_TENANT_ID = ? AND EMAIL_DOMAIN = ?";

    public static final String GET_ORG_FOR_EMAIL = "SELECT OUTBOUND_SP_TENANT_ID FROM " +
            "ENTERPRISE_LOGIN_EMAIL_DOMAIN WHERE EMAIL_DOMAIN = ?  AND INBOUND_SP_TENANT_ID = ? ";

    public static final String GET_OUTBOUND_SP_RESOURCE_ID = "SELECT OUTBOUND_SP_ID FROM " +
            "ENTERPRISE_LOGIN_MGT WHERE OUTBOUND_SP_TENANT_ID = ? AND INBOUND_SP_TENANT_ID = ?" +
            " AND INBOUND_SP_ID = ? ";

    public static final String UPDATE_ENTERPRISE_LGN_MAPPING_OF_SERVICE = "UPDATE ENTERPRISE_LOGIN_MGT " +
            "SET OUTBOUND_SP_ID = ? WHERE INBOUND_SP_TENANT_ID = ? AND  INBOUND_SP_ID = ? " +
            "AND OUTBOUND_SP_TENANT_ID = ? ";

}

/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */


package org.wso2.carbon.identity.organization.management.authn.core.dao;

/**
 * Creates DAO layer.
 */
public class DAOFactory {

    // Implementation of DAO.
    private EnterpriseLoginMgtDAO enterpriseLoginMgtDAO;

    private DAOFactory() {

        // This factory creates instance of DAOImplementation.
        enterpriseLoginMgtDAO = new EnterpriseLoginMgtDAOImpl();
    }

    private static DAOFactory enterpriseLoginMgtDAOFactoryInstance = new DAOFactory();

    public static DAOFactory getInstance() {

        return enterpriseLoginMgtDAOFactoryInstance;
    }

    /**
     * Returns enterprise login management DAO.
     *
     * @return EnterpriseLoginMgtDAO.
     */
    public EnterpriseLoginMgtDAO getEnterpriseLoginMgtDAO() {

        return enterpriseLoginMgtDAO;
    }
}

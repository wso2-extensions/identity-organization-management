package org.wso2.carbon.identity.organization.management.application.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public class OrgApplicationMgtDAOImpl implements OrgApplicationMgtDAO {

    public static final String INSERT_SHARED_APP = "INSERT INTO SP_SHARED_APP (PARENT_APP_ID, PARENT_TENANT_ID, " +
            "SHARED_APP_ID, SHARED_TENANT_ID, USERNAME) VALUES (?, ?, ?, ?, ?);";

    private static final Log LOG = LogFactory.getLog(OrgApplicationMgtDAOImpl.class);

    public void addSharedApplication(int tenantId, String parentAppId, int sharedTenantId,
                                     String sharedAppId, String username) {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement ps = connection.prepareStatement(INSERT_SHARED_APP)) {

                ps.setString(1, parentAppId);
                ps.setInt(2, tenantId);
                ps.setString(3, sharedAppId);
                ps.setInt(4, sharedTenantId);
                ps.setString(5, username);
                ps.execute();

                IdentityDatabaseUtil.commitTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackTransaction(connection);
                LOG.error(e);
            }
        } catch (SQLException e) {
            LOG.error(e);
        }
    }
}

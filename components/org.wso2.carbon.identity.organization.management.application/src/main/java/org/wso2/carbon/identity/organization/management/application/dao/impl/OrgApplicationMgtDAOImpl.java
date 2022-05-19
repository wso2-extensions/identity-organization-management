package org.wso2.carbon.identity.organization.management.application.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.exception.OrgApplicationMgtException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 *
 */
public class OrgApplicationMgtDAOImpl implements OrgApplicationMgtDAO {

    public static final String INSERT_SHARED_APP = "INSERT INTO SP_SHARED_APP (PARENT_APP_ID, PARENT_TENANT_ID, " +
            "SHARED_APP_ID, SHARED_TENANT_ID, USERNAME) VALUES (?, ?, ?, ?, ?);";

    public static final String GET_SHARED_APP_ID = "SELECT SHARED_APP_ID FROM SP_SHARED_APP WHERE " +
            "PARENT_TENANT_ID = ? AND SHARED_TENANT_ID = ? AND PARENT_APP_ID = ? ;";

    private static final Log LOG = LogFactory.getLog(OrgApplicationMgtDAOImpl.class);

    public void addSharedApplication(int tenantId, String parentAppId, int sharedTenantId,
                                     String sharedAppId, String username) throws OrgApplicationMgtException {

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
                throw new OrgApplicationMgtException(e);
            }
        } catch (SQLException e) {
            LOG.error(e);
            throw new OrgApplicationMgtException(e);
        }
    }

    @Override
    public Optional<String> getSharedApplicationResourceId(int parentTenantId, int sharedTenantId, String parentAppId)
            throws OrgApplicationMgtException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             PreparedStatement ps = connection.prepareStatement(GET_SHARED_APP_ID)) {

                ps.setInt(1, parentTenantId);
                ps.setInt(2, sharedTenantId);
                ps.setString(3, parentAppId);
                ResultSet rs = ps.executeQuery();
                String sharedAppId = "";
                while (rs.next()) {
                    sharedAppId = rs.getString("SHARED_APP_ID");
                }
                rs.close();
                return Optional.ofNullable(sharedAppId);
            } catch (SQLException e) {
                LOG.error(e);
                throw new OrgApplicationMgtException(e);
            }

    }
}

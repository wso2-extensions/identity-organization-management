package org.wso2.carbon.identity.organization.management.role.management.service.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.organization.management.role.management.service.dao.RoleManagementDAO;
import org.wso2.carbon.identity.organization.management.role.management.service.dao.RoleManagementDAOImpl;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.user.core.UserStoreManager;

/**
 * This event listener's main purpose is to listen to user operation events of the users to execute organization
 * level operations.
 */
public class OrganizationUserOperationEventListener extends AbstractIdentityUserOperationEventListener {

    private static final RoleManagementDAO roleManagementDAO = new RoleManagementDAOImpl();
    private static final Log LOG = LogFactory.getLog(OrganizationUserOperationEventListener.class);

    @Override
    public int getExecutionOrderId() {

        return 115;
    }

    @Override
    public boolean doPostDeleteUserWithID(String userID, UserStoreManager userStoreManager) {

        // Check whether the listener is enabled.
        if (!isEnable()) {
            return true;
        }

        try {
            roleManagementDAO.deleteUserRoleByUserId(userID);
            return true;
        } catch (OrganizationManagementServerException e) {
            LOG.error(e.getDescription(), e);
            return false;
        }
    }
}

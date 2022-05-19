package org.wso2.carbon.identity.organization.management.application.dao;

import org.wso2.carbon.identity.organization.management.application.exception.OrgApplicationMgtException;

import java.util.Optional;

/**
 *
 */
public interface OrgApplicationMgtDAO {

    void addSharedApplication(int parentTenantId, String parentAppId, int sharedTenantId,
                              String sharedApplication, String username) throws OrgApplicationMgtException;

    Optional<String> getSharedApplicationResourceId(int parentTenantId, int sharedTenantId, String parentAppId)
            throws OrgApplicationMgtException;
}

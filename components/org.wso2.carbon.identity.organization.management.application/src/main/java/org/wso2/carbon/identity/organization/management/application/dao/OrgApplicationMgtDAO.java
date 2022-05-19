package org.wso2.carbon.identity.organization.management.application.dao;

/**
 *
 */
public interface OrgApplicationMgtDAO {

    void addSharedApplication(int tenantId, String parentAppId, int sharedTenantId,
                              String sharedApplication, String username);

}

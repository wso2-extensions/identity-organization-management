package org.wso2.carbon.identity.organization.management.service.dao;

/**
 *
 */
public interface OrganizationApplicationMgtDAO {

    void addSharedApplication(int tenantId, String parentAppId, int sharedTenantId,
                              String sharedApplication, String username);

}

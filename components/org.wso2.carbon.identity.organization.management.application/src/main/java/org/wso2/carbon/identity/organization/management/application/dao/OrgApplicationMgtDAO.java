package org.wso2.carbon.identity.organization.management.application.dao;

import org.wso2.carbon.identity.organization.management.application.exception.OrgApplicationMgtServerException;

import java.util.Optional;

/**
 * This interface performs CRUD operations for Shared applications.
 */
public interface OrgApplicationMgtDAO {

    /**
     * Creates new entry for shared applications across organizations.
     *
     * @param ownerTenantId  tenant owns the application.
     * @param mainAppId      main application.
     * @param sharedTenantId tenant to whom the application is shared.
     * @param sharedAppId    shared application id.
     * @throws OrgApplicationMgtServerException the server exception is thrown in a failure to create the entry.
     */
    void addSharedApplication(int ownerTenantId, String mainAppId, int sharedTenantId, String sharedAppId)
            throws OrgApplicationMgtServerException;

    /**
     * Returns the Unique identifier of the shared application.
     *
     * @param ownerTenantId  tenant owns the application.
     * @param sharedTenantId tenant to whom the application is shared.
     * @param mainAppId      main application identifier.
     * @return Unique identifier of the shared application.
     * @throws OrgApplicationMgtServerException the server exception is thrown in a failure to retrieve the entry.
     */
    Optional<String> getSharedApplicationResourceId(int ownerTenantId, int sharedTenantId, String mainAppId)
            throws OrgApplicationMgtServerException;
}

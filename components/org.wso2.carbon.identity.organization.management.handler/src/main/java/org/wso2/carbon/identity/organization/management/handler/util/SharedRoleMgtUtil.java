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

package org.wso2.carbon.identity.organization.management.handler.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility methods for handling application audience roles of shared applications. A shared application may hold
 * roles inherited from the main application as well as roles authored by the sub organization itself.
 */
public class SharedRoleMgtUtil {

    private SharedRoleMgtUtil() {

    }

    /**
     * Create an application audience role in a sub organization as a shared role of a main application role.
     *
     * @param roleManagementService Role management service.
     * @param roleName              Name of the role.
     * @param sharedAppId           ID of the shared application.
     * @param sharedAppTenantDomain Tenant domain of the shared application.
     * @return Created role.
     * @throws IdentityRoleManagementException If an error occurs while creating the role.
     */
    public static RoleBasicInfo addSharedApplicationRole(RoleManagementService roleManagementService,
                                                         String roleName, String sharedAppId,
                                                         String sharedAppTenantDomain)
            throws IdentityRoleManagementException {

        IdentityUtil.threadLocalProperties.get().put(RoleConstants.IS_SHARED_ROLE_PROVISIONING_FLOW, true);
        try {
            return roleManagementService.addRole(roleName, Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), RoleConstants.APPLICATION, sharedAppId, sharedAppTenantDomain);
        } finally {
            IdentityUtil.threadLocalProperties.get().remove(RoleConstants.IS_SHARED_ROLE_PROVISIONING_FLOW);
        }
    }

    /**
     * Filter the given sub organization roles to the roles inherited from a main role. Roles authored by the sub
     * organization itself are excluded.
     *
     * @param roleManagementService Role management service.
     * @param subOrgRoles           Roles in the sub organization.
     * @param subOrgTenantDomain    Tenant domain of the sub organization.
     * @return Inherited roles.
     * @throws IdentityRoleManagementException If an error occurs while resolving the main roles.
     */
    public static List<RoleBasicInfo> filterInheritedRoles(RoleManagementService roleManagementService,
                                                           List<RoleBasicInfo> subOrgRoles,
                                                           String subOrgTenantDomain)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(subOrgRoles)) {
            return new ArrayList<>();
        }
        List<String> subOrgRoleIds = subOrgRoles.stream().map(RoleBasicInfo::getId).collect(Collectors.toList());
        Map<String, String> sharedRoleToMainRoleMappings =
                roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(subOrgRoleIds, subOrgTenantDomain);
        if (MapUtils.isEmpty(sharedRoleToMainRoleMappings)) {
            return new ArrayList<>();
        }
        return subOrgRoles.stream()
                .filter(role -> sharedRoleToMainRoleMappings.containsKey(role.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Check whether the main role is already inherited into the sub organization.
     *
     * @param roleManagementService Role management service.
     * @param mainRoleId            ID of the main role.
     * @param subOrgTenantDomain    Tenant domain of the sub organization.
     * @return True if the sub organization has a role inherited from the main role.
     * @throws IdentityRoleManagementException If an error occurs while resolving the shared role.
     */
    public static boolean isInheritedRoleExists(RoleManagementService roleManagementService, String mainRoleId,
                                                String subOrgTenantDomain) throws IdentityRoleManagementException {

        return MapUtils.isNotEmpty(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(mainRoleId), subOrgTenantDomain));
    }
}

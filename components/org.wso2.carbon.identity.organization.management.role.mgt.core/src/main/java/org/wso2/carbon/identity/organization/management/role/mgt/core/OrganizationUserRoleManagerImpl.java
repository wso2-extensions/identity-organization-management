/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.role.mgt.core;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants;
import org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants;
import org.wso2.carbon.identity.organization.management.role.mgt.core.dao.OrganizationUserRoleMgtDAO;
import org.wso2.carbon.identity.organization.management.role.mgt.core.dao.OrganizationUserRoleMgtDAOImpl;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtServerException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.internal.OrganizationUserRoleMgtDataHolder;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.Organization;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.OrganizationUserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.OrganizationUserRoleMappingForEvent;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.Role;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.RoleMember;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserForUserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserRoleOperation;
import org.wso2.carbon.identity.scim2.common.DAO.GroupDAO;
import org.wso2.carbon.identity.scim2.common.exceptions.IdentitySCIMException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.DATA;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.POST_ASSIGN_ORGANIZATION_USER_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.POST_REVOKE_ORGANIZATION_USER_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.PRE_ASSIGN_ORGANIZATION_USER_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.PRE_REVOKE_ORGANIZATION_USER_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.STATUS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.Status;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.TENANT_DOMAIN;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.USER_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.USER_NAME;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ADD_ORG_ROLE_USER_REQUEST_INVALID_ORGANIZATION_PARAM;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ADD_ORG_ROLE_USER_REQUEST_INVALID_USER;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ADD_ORG_ROLE_USER_REQUEST_MAPPING_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.DELETE_ORG_ROLE_USER_REQUEST_INVALID_BOOLEAN_VALUE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.DELETE_ORG_ROLE_USER_REQUEST_INVALID_DIRECT_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_USER_STORE_OPERATIONS_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.INVALID_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.INVALID_ROLE_NON_INTERNAL_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_INVALID_BOOLEAN_VALUE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_INVALID_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_INVALID_OPERATION;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_INVALID_PATH;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_OPERATION_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_PATH_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_TOO_MANY_OPERATIONS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.INCLUDE_SUB_ORGS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.IS_MANDATORY;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getUserIdFromUserName;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getUserStoreManager;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.handleServerException;

/**
 * Impl class of OrganizationUserRoleManager.
 */
public class OrganizationUserRoleManagerImpl implements OrganizationUserRoleManager {

    @Override
    public void addOrganizationUserRoleMappings(String organizationId, UserRoleMapping userRoleMapping)
            throws OrganizationUserRoleMgtException {

        // Fire pre-event
        fireEvent(PRE_ASSIGN_ORGANIZATION_USER_ROLE, organizationId, null,
                OrganizationUserRoleEventConstants.Status.FAILURE);

        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();

        String roleId = userRoleMapping.getRoleId();
        int hybridRoleId = getHybridRoleIdFromSCIMGroupId(roleId);
        userRoleMapping.setHybridRoleId(hybridRoleId);

        // Validation of adding role mappings
        validateAddRoleMappingRequest(organizationId, userRoleMapping);

        // Create lists for mandatory and non-mandatory user role mappings considering their propagations.
        List<UserForUserRoleMapping> usersGetPermissionsForSubOrgsNonMandatory = new ArrayList<>();
        List<UserForUserRoleMapping> usersGetPermissionOnlyToOneOrgNonMandatory = new ArrayList<>();
        List<UserForUserRoleMapping> usersGetPermissionForSubOrgsMandatory = new ArrayList<>();

        AbstractUserStoreManager userStoreManager;
        try {
            userStoreManager = (AbstractUserStoreManager) getUserStoreManager(getTenantId());
            if (userStoreManager == null) {
                throw handleServerException(ERROR_CODE_USER_STORE_OPERATIONS_ERROR, " for tenant Id: " + getTenantId());
            } else {
                for (UserForUserRoleMapping user : userRoleMapping.getUsers()) {
                    boolean userExists = userStoreManager.isExistingUserWithID(user.getUserId());
                    if (!userExists) {
                        throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_INVALID_USER,
                                "No user exists with user Id: " + user.getUserId());
                    }
                    if (user.isMandatoryRole()) {
                        // if it is mandatory then the cascaded property is implied.
                        if (!user.isCascadedRole()) {
                            throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_INVALID_ORGANIZATION_PARAM,
                                    "");
                        }
                        usersGetPermissionForSubOrgsMandatory.add(user);
                    } else if (user.isCascadedRole()) {
                        usersGetPermissionsForSubOrgsNonMandatory.add(user);
                    } else {
                        usersGetPermissionOnlyToOneOrgNonMandatory.add(user);
                    }
                }
            }
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_USER_STORE_OPERATIONS_ERROR, " for tenant id: " + getTenantId());
        }

        List<OrganizationUserRoleMapping> organizationUserRoleMappings = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(usersGetPermissionsForSubOrgsNonMandatory)) {
            List<Organization> organizations = organizationUserRoleMgtDAO
                    .getAllSubOrganizations(organizationId);
            // add starting organization populate role mapping
            organizationUserRoleMappings.addAll(populateOrganizationUserRoleMappings(organizationId, roleId,
                    hybridRoleId, organizationId,
                    usersGetPermissionsForSubOrgsNonMandatory));
            /*
             * Assume we have organizations A,B,C,D and A is the immediate parent of B and B is the immediate parent
             * of C and so on. If we assign a non-mandatory role and if it is assigned at A saying include it to the
             * sub organizations. Then we have to copy that role for all the sub organizations and the assigned
             * level is that organization level. Say we are assigning A, a role R1 to propagate then it will go to B
             * and B's assigned level id will be id of B. And when it propagates to C the assigned level id of it
             * will be the id of C.
             * A -> roleId - R1, assignedLevelId - id(A), orgId - id(A), Mandatory - 0
             *  \
             *   B -> roleId - R1, assignedLevelId - id(B), orgId - id(B), Mandatory - 0
             *    \
             *     C -> roleId - R1, assignedLevelId - id(C), orgId - id(C), Mandatory - 0
             *      \
             *       D -> roleId - R1, assignedLevelId - id(D), orgId - id(D), Mandatory - 0
             * */

            for (Organization organization : organizations) {
                organizationUserRoleMappings.addAll(populateOrganizationUserRoleMappings(organization
                                .getOrganizationId(), roleId, hybridRoleId,
                        organization.getOrganizationId(), usersGetPermissionsForSubOrgsNonMandatory));
            }
        }
        if (CollectionUtils.isNotEmpty(usersGetPermissionForSubOrgsMandatory)) {
            List<Organization> organizations = organizationUserRoleMgtDAO
                    .getAllSubOrganizations(organizationId);
            // Add starting organization to populate role mapping
            organizationUserRoleMappings.addAll(populateOrganizationUserRoleMappings(organizationId, roleId,
                    hybridRoleId, organizationId,
                    usersGetPermissionForSubOrgsMandatory));
            /*
             * Assume we have organizations A,B,C,D and A is the immediate parent of B and B is the immediate parent
             * of C and so on.
             * If we assign a mandatory role and if it is assigned at A saying include it to the sub organizations.
             * Then we have to copy that role for all athe sub organizations, and they only get that from the
             * assignedLevel.
             * Say we are assigning A, a role R1 as mandatory role it will be assigned to B and B's assigned level
             * id will be the id of A. And the assigned level id of C will be the id of A.
             * A -> roleId - R1, assignedLevelId - id(A), orgId - id(A), Mandatory - 1
             *  \
             *   B -> roleId - R1, assignedLevelId - id(A), orgId - id(B), Mandatory - 1
             *    \
             *     C -> roleId - R1, assignedLevelId - id(A), orgId - id(C), Mandatory - 1
             *      \
             *       D -> roleId - R1, assignedLevelId - id(A), orgId - id(D), Mandatory - 1
             * */
            for (Organization organization : organizations) {
                organizationUserRoleMappings.addAll(populateOrganizationUserRoleMappings(
                        organization.getOrganizationId(), roleId, hybridRoleId,
                        organizationId, usersGetPermissionForSubOrgsMandatory));
            }
        }
        if (CollectionUtils.isNotEmpty(usersGetPermissionOnlyToOneOrgNonMandatory)) {
            /*
             * Assume we have organizations A,B,C,D and A is the immediate parent of B and B is the immediate
             * parent of C and so on. If we assign a non-mandatory role that we do not need to propagate to the child
             * organizations, the assigned level id will be the same as the organization id, and it will stop at that
             * level without further propagating.
             * */
            organizationUserRoleMappings
                    .addAll(populateOrganizationUserRoleMappings(organizationId, roleId, hybridRoleId, organizationId,
                            usersGetPermissionOnlyToOneOrgNonMandatory));
        }
        organizationUserRoleMgtDAO
                .addOrganizationUserRoleMappings(organizationUserRoleMappings, getTenantId());

        OrganizationUserRoleMappingForEvent organizationUserRoleMappingForEvent =
                new OrganizationUserRoleMappingForEvent(organizationId, roleId, userRoleMapping.getUsers().stream()
                        .map(m -> new UserForUserRoleMapping(m.getUserId(), m.isMandatoryRole(), m.isCascadedRole()))
                        .collect(Collectors.toList()));
        fireEvent(POST_ASSIGN_ORGANIZATION_USER_ROLE, organizationId, organizationUserRoleMappingForEvent,
                OrganizationUserRoleEventConstants.Status.SUCCESS);
    }

    @Override
    public List<RoleMember> getUsersByOrganizationAndRole(String organizationId, String roleId, int offset, int limit,
                                                          List<String> requestedAttributes, String filter)
            throws OrganizationUserRoleMgtException {

        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        return organizationUserRoleMgtDAO
                .getUserIdsByOrganizationAndRole(organizationId, roleId, offset, limit, requestedAttributes,
                        getTenantId(), filter);
    }

    @Override
    public void patchOrganizationsUserRoleMapping(String organizationId, String roleId, String userId,
                                                  List<UserRoleOperation> userRoleOperations)
            throws OrganizationUserRoleMgtException {

        /*
         * The patchOrganizationUserRoleMapping can have two userRoleOperations.
         * 1. mandatory operation
         * 2. include sub organizations role operation
         * For mandatory role operation, if the operation is mandatory, then includeSubOrganizations is implied.
         * If the mandatory is given then we have to check the equality of the organizationId and the organization
         * id of the assignedLevel.
         * But if only the includeSubOrganization operation is given then, we have to check for non-mandatory
         * organization-user-role mapping for sub organizations too.
         * */
        if (userRoleOperations == null) {
            return;
        }
        if (userRoleOperations.size() == 0) {
            return;
        }
        if (userRoleOperations.size() > 2) {
            throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_TOO_MANY_OPERATIONS, null);
        }
        UserRoleOperation[] userRoleOperationsArr = {userRoleOperations.get(0), userRoleOperations.get(1)};
        validatePatchOperation(userRoleOperations);
        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        int directlyAssignedRoleMappingsInheritance = organizationUserRoleMgtDAO
                .getDirectlyAssignedOrganizationUserRoleMappingInheritance(organizationId, userId, roleId,
                        getTenantId());
        // Check whether directly assigned role mapping exists
        // If role assigned level == organization id
        // then there are no directly assigned user-role mapping exists.
        if (directlyAssignedRoleMappingsInheritance == -1) {
            throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_INVALID_MAPPING, null);
        }

        UserRoleOperation isMandatoryOp = StringUtils.equals(IS_MANDATORY,
                userRoleOperationsArr[0].getPath()) ? userRoleOperationsArr[0] : userRoleOperationsArr[1];
        UserRoleOperation includeSubOrgsOp = StringUtils.equals(INCLUDE_SUB_ORGS,
                userRoleOperationsArr[0].getPath()) ? userRoleOperationsArr[0] : userRoleOperationsArr[1];

        List<OrganizationUserRoleMapping> addOrganizationUserRoleMappings = new ArrayList<>();
        List<OrganizationUserRoleMapping> deleteOrganizationUserRoleMappings = new ArrayList<>();
        List<Organization> organizations = organizationUserRoleMgtDAO
                .getAllSubOrganizations(organizationId);
        int hybridRoleId = getHybridRoleIdFromSCIMGroupId(roleId);
        /*
         * If directlyAssignedRoleMappingsInheritance=1 it means that there is the possibility of having
         * non-mandatory value set as well. Therefore, if the patching of it going to happen we will consider them too.
         * includeSubOrgsOp.getValue() == true and isMandatoryOp.getValue() == true, then we will remove the
         * non-mandatory user-org-role mappings
         * from sub organizations and will only have the mandatory user-org-role mappings.
         * */
        if (directlyAssignedRoleMappingsInheritance == 1) {
            /*
             * check whether non-mandatory value is associated with the params organizationId, userId, roleId,
             * tenantId, assignedAt and mandatory
             * */
            boolean checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(organizationId,
                    userId, roleId, organizationId, false, getTenantId());
            /*
             * If the value is true then there is a non-mandatory value associated with this.
             * If false then there is not.
             * */
            if (checkMapping) {
                deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                        hybridRoleId, roleId, organizationId, false));
            }
            for (Organization organization : organizations) {
                String orgId = organization.getOrganizationId();
                checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                        orgId, false, getTenantId());
                if (checkMapping) {
                    deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId, hybridRoleId,
                            roleId, orgId, false));
                }
            }
            if (isMandatoryOp.getValue()) {
                if (!includeSubOrgsOp.getValue()) {
                    throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_INVALID_BOOLEAN_VALUE, null);
                }
                // else nothing to do.
            } else {
                /*
                 * add new organization-user-role mappings with assignedAt = orgId
                 * remove organization-user-role mappings with mandatory and already
                 * */
                addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                        hybridRoleId, roleId, organizationId, false));
                deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                        hybridRoleId, roleId, organizationId, true));
                for (Organization organization : organizations) {
                    String orgId = organization.getOrganizationId();
                    checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                            organizationId, true, getTenantId());
                    if (checkMapping) {
                        deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId,
                                hybridRoleId, roleId, organizationId, true));
                    }
                }
                if (includeSubOrgsOp.getValue()) {
                    //add non-mandatory organization-user-role mappings
                    for (Organization organization : organizations) {
                        String orgId = organization.getOrganizationId();
                        addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId, hybridRoleId,
                                roleId, orgId, false));
                    }
                }
                // else nothing to do.
            }
        } else { // if directlyAssignedRoleMappingInheritance = 0
            if (isMandatoryOp.getValue()) {
                if (includeSubOrgsOp.getValue()) {
                    /*
                     * add the parent organization-user-role mapping with non-mandatory value to
                     * the deletion list first.
                     * */
                    deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                            hybridRoleId, roleId, organizationId, false));
                    // then check for child organizations with non-mandatory and remove them.
                    for (Organization organization : organizations) {
                        String orgId = organization.getOrganizationId();
                        deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId,
                                hybridRoleId, roleId, orgId, false));
                    }
                    /*
                     * add the parent organization-user-role mapping with mandatory value to the
                     * addOrganizationUserRoleMappings list.
                     * */
                    addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                            hybridRoleId, roleId, organizationId, true));
                    for (Organization organization : organizations) {
                        String orgId = organization.getOrganizationId();
                        addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId, hybridRoleId,
                                roleId, organizationId, true));
                    }
                } else {
                    throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_INVALID_BOOLEAN_VALUE, null);
                }
            } else {
                if (includeSubOrgsOp.getValue()) {
                    /*
                     * we already have a mapping for parent organization. We will check whether there are mappings for
                     * sub organizations and add accordingly.
                     * */
                    for (Organization organization : organizations) {
                        String orgId = organization.getOrganizationId();
                        boolean checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId,
                                userId, roleId, orgId, false, getTenantId());
                        //if there is not a mapping add it.
                        if (!checkMapping) {
                            addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId,
                                    hybridRoleId, roleId, orgId, false));
                        }
                    }
                }
                /*
                 * else we don't need to check it. Since even if we say don't include sub organizations,
                 * the non-mandatory organization-user-role mappings are unique to each organization.
                 * Therefore, we cannot possibly try to remove them here.
                 * If we need to remove them we need to delete them.
                 * */
            }
        }
        organizationUserRoleMgtDAO.updateMandatoryProperty(addOrganizationUserRoleMappings,
                deleteOrganizationUserRoleMappings, getTenantId());
    }

    @Override
    public void deleteOrganizationsUserRoleMapping(String organizationId, String userId, String roleId,
                                                   boolean includeSubOrgs) throws OrganizationUserRoleMgtException {

        // Fire Pre-Event
        fireEvent(PRE_REVOKE_ORGANIZATION_USER_ROLE, organizationId, null, Status.FAILURE);

        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        /*
         * Check whether the role mapping is directly assigned to the particular organization or inherited from the
         * parent level.
         * */
        int directlyAssignedRoleMappingsInheritance = organizationUserRoleMgtDAO
                .getDirectlyAssignedOrganizationUserRoleMappingInheritance(organizationId, userId, roleId,
                        getTenantId());

        if (directlyAssignedRoleMappingsInheritance == -1) {
            throw handleClientException(DELETE_ORG_ROLE_USER_REQUEST_INVALID_DIRECT_MAPPING,
                    String.format("No directly assigned organization user role mapping found for organization: %s, " +
                                    "user: %s, role: %s, directly assigned at organization: %s",
                            organizationId, userId, roleId, organizationId));
        }

        List<Organization> subOrganizations = organizationUserRoleMgtDAO
                .getAllSubOrganizations(organizationId);
        int hybridRoleId = getHybridRoleIdFromSCIMGroupId(roleId);
        List<OrganizationUserRoleMapping> organizationListToBeDeleted = new ArrayList<>();
        if (directlyAssignedRoleMappingsInheritance == 1) {
            if (!includeSubOrgs) {
                throw handleClientException(DELETE_ORG_ROLE_USER_REQUEST_INVALID_BOOLEAN_VALUE, null);
            }
            /*
             * If directlyAssignedRoleMappingsInheritance=1 means we are going to remove a mandatory role.
             * When removing mandatory role, we are removing everything from the sub-organizations including
             * non-mandatory roles as well.
             * */
            organizationListToBeDeleted.add(new OrganizationUserRoleMapping(organizationId, userId, hybridRoleId,
                    roleId, organizationId, true));
            boolean checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(organizationId,
                    userId, roleId, organizationId, false, getTenantId());
            if (checkMapping) {
                organizationListToBeDeleted.add(new OrganizationUserRoleMapping(organizationId, userId, hybridRoleId,
                        roleId, organizationId, false));
            }
            for (Organization organization : subOrganizations) {
                String orgId = organization.getOrganizationId();
                checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                        orgId, false, getTenantId());
                if (checkMapping) {
                    organizationListToBeDeleted.add(new OrganizationUserRoleMapping(orgId, userId, hybridRoleId, roleId,
                            orgId, false));
                }
                checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                        organizationId, true, getTenantId());
                if (checkMapping) {
                    organizationListToBeDeleted.add(new OrganizationUserRoleMapping(orgId, userId, hybridRoleId, roleId,
                            organizationId, true));
                }
            }
        } else { // directlyAssignedRoleMappingsInheritance == 0
            organizationListToBeDeleted.add(new OrganizationUserRoleMapping(organizationId, userId, hybridRoleId,
                    roleId, organizationId, false));
            boolean checkMapping;
            if (includeSubOrgs) {
                for (Organization organization : subOrganizations) {
                    String orgId = organization.getOrganizationId();
                    checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                            orgId, false, getTenantId());
                    if (checkMapping) {
                        organizationListToBeDeleted.add(new OrganizationUserRoleMapping(orgId, userId, hybridRoleId,
                                roleId, orgId, false));
                    }
                }
            }
            // else nothing to do
        }
        organizationUserRoleMgtDAO.deleteOrganizationsUserRoleMapping(organizationListToBeDeleted,
                userId, roleId, getTenantId());
        // Fire post-event.
        OrganizationUserRoleMappingForEvent organizationUserRoleMappingForEvent =
                new OrganizationUserRoleMappingForEvent(organizationId, roleId, userId);
        fireEvent(POST_REVOKE_ORGANIZATION_USER_ROLE, organizationId, organizationUserRoleMappingForEvent,
                OrganizationUserRoleEventConstants.Status.SUCCESS);
    }

    @Override
    public void deleteOrganizationsUserRoleMappings(String userId) throws OrganizationUserRoleMgtException {

        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        organizationUserRoleMgtDAO.deleteOrganizationsUserRoleMappings(userId, getTenantId());
    }

    @Override
    public List<Role> getRolesByOrganizationAndUser(String organizationId, String userId)
            throws OrganizationUserRoleMgtException {

        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        return organizationUserRoleMgtDAO.getRolesByOrganizationAndUser(organizationId, userId, getTenantId());
    }

    @Override
    public boolean isOrganizationUserRoleMappingExists(String organizationId, String userId, String roleId,
                                                       String assignedLevel, boolean mandatory)
            throws OrganizationUserRoleMgtException {

        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        return organizationUserRoleMgtDAO
                .isOrganizationUserRoleMappingExists(organizationId, userId, roleId, assignedLevel, mandatory,
                        getTenantId());
    }

    private void fireEvent(String eventName, String organizationId, Object data,
                           OrganizationUserRoleEventConstants.Status status)
            throws OrganizationUserRoleMgtServerException {

        IdentityEventService eventService = OrganizationUserRoleMgtDataHolder.getInstance().getIdentityEventService();
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(USER_NAME, getAuthenticatedUsername());
        eventProperties.put(USER_ID, getAuthenticatedUserId());
        eventProperties.put(TENANT_DOMAIN, getTenantDomain());
        eventProperties.put(STATUS, status);
        if (data != null) {
            eventProperties.put(DATA, data);
        }
        if (organizationId != null) {
            eventProperties.put(ORGANIZATION_ID, organizationId);
        }
        Event event = new Event(eventName, eventProperties);
        try {
            eventService.handleEvent(event);
        } catch (IdentityEventException e) {
            throw handleServerException(OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_EVENTING_ERROR,
                    eventName, e);
        }
    }


    private String getAuthenticatedUserId() throws OrganizationUserRoleMgtServerException {

        return getUserIdFromUserName(getAuthenticatedUsername(), getTenantId());
    }

    private String getAuthenticatedUsername() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
    }

    private int getHybridRoleIdFromSCIMGroupId(String roleId) throws OrganizationUserRoleMgtException {

        GroupDAO groupDAO = new GroupDAO();
        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        try {
            String groupName = groupDAO.getGroupNameById(getTenantId(), roleId);
            if (groupName == null) {
                throw handleClientException(INVALID_ROLE_ID, "Invalid role ID : " + roleId);
            }
            String[] groupNameParts = groupName.split("/");
            if (groupNameParts.length != 2) {
                throw handleServerException(INVALID_ROLE_ID, "Invalid role ID. Group name : " + groupName);
            }
            String domain = groupNameParts[0];
            if (!"INTERNAL".equalsIgnoreCase(domain)) {
                throw handleClientException(INVALID_ROLE_NON_INTERNAL_ROLE,
                        "Provided role : " + groupName + ", is not an INTERNAL role");
            }
            String roleName = groupNameParts[1];
            return organizationUserRoleMgtDAO.getRoleIdBySCIMGroupName(roleName, getTenantId());
        } catch (IdentitySCIMException e) {
            throw new OrganizationUserRoleMgtServerException(e);
        }
    }

    private void validateAddRoleMappingRequest(String organizationId, UserRoleMapping userRoleMapping)
            throws OrganizationUserRoleMgtException {

        for (UserForUserRoleMapping user : userRoleMapping.getUsers()) {
            boolean isRoleMappingExists = isOrganizationUserRoleMappingExists(organizationId, user.getUserId(),
                    userRoleMapping.getRoleId(),
                    organizationId, user.isMandatoryRole());
            if (isRoleMappingExists) {
                throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_MAPPING_EXISTS, String.format(
                        "Directly assigned role %s to user: %s over the organization: %s is already exists",
                        userRoleMapping.getRoleId(), user.getUserId(), organizationId));
            }
        }
    }

    private List<OrganizationUserRoleMapping> populateOrganizationUserRoleMappings(String organizationId, String roleId,
                                                                                   int hybridRoleId, String assignedAt,
                                                                                   List<UserForUserRoleMapping>
                                                                                           usersList) {

        List<OrganizationUserRoleMapping> organizationUserRoleMappings = new ArrayList<>();
        for (UserForUserRoleMapping user : usersList) {
            OrganizationUserRoleMapping organizationUserRoleMapping = new OrganizationUserRoleMapping();
            organizationUserRoleMapping.setOrganizationId(organizationId);
            organizationUserRoleMapping.setRoleId(roleId);
            organizationUserRoleMapping.setHybridRoleId(hybridRoleId);
            organizationUserRoleMapping.setUserId(user.getUserId());
            organizationUserRoleMapping.setAssignedLevelOrganizationId(assignedAt);
            organizationUserRoleMapping.setMandatory(user.isMandatoryRole());
            organizationUserRoleMappings.add(organizationUserRoleMapping);
        }
        return organizationUserRoleMappings;
    }

    private void validatePatchOperation(List<UserRoleOperation> userRoleOperations)
            throws OrganizationUserRoleMgtException {

        // Validate op.
        for (UserRoleOperation userRoleOperation :
                userRoleOperations) {
            if (StringUtils.isBlank(userRoleOperation.getOp())) {
                throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_OPERATION_UNDEFINED, null);
            }
            String op = userRoleOperation.getOp().trim().toLowerCase(Locale.ENGLISH);
            if (!PATCH_OP_REPLACE.equals(op)) {
                throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_INVALID_OPERATION, null);
            }

            // Validate path.
            if (StringUtils.isBlank(userRoleOperation.getPath())) {
                throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_PATH_UNDEFINED, null);
            }
            // In the path, if there is not includeSubOrgs or isMandatory property throw an error
            if (!(StringUtils.equals(INCLUDE_SUB_ORGS, userRoleOperation.getPath()) ||
                    StringUtils.equals(IS_MANDATORY, userRoleOperation.getPath()))
            ) {
                throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_INVALID_PATH, null);
            }
        }
    }
}

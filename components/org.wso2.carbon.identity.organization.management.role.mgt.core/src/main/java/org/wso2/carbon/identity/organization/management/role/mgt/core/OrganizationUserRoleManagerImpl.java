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
import org.wso2.carbon.identity.organization.management.role.mgt.core.dao.OrganizationUserRoleMgtDAO;
import org.wso2.carbon.identity.organization.management.role.mgt.core.dao.OrganizationUserRoleMgtDAOImpl;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtServerException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.internal.OrganizationUserRoleMgtDataHolder;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.OrganizationUserRoleMapping;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.OrganizationUserRoleMappingForEvent;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.Role;
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
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.DELETE_ORG_ROLE_USER_REQUEST_INVALID_DIRECT_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_EVENTING_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_USER_STORE_OPERATIONS_ERROR;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.INVALID_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.INVALID_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.INVALID_ROLE_NON_INTERNAL_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_INVALID_BOOLEAN_VALUE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_INVALID_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_INVALID_OPERATION;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_INVALID_PATH;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_OPERATION_MISSING;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_OPERATION_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_PATH_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.PATCH_ORG_ROLE_USER_REQUEST_TOO_MANY_OPERATIONS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.INCLUDE_SUB_ORGS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.IS_FORCED;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getUserIdFromUserName;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.getUserStoreManager;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.util.Utils.handleServerException;

/**
 * Implementation of OrganizationUserRoleManager.
 */
public class OrganizationUserRoleManagerImpl implements OrganizationUserRoleManager {

    @Override
    public void addOrganizationUserRoleMappings(String organizationId, UserRoleMapping userRoleMapping)
            throws OrganizationUserRoleMgtException {

        // Fire pre-event
        fireEvent(PRE_ASSIGN_ORGANIZATION_USER_ROLE, organizationId, null,
                OrganizationUserRoleEventConstants.Status.FAILURE);

        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();

        //validate organization
        if (!organizationUserRoleMgtDAO.checkOrganizationIdAvailability(organizationId)) {
            throw handleClientException(INVALID_ORGANIZATION_ID, organizationId);
        }

        // Validation of adding role mappings
        validateAddRoleMappingRequest(organizationId, userRoleMapping);

        //validate roleId
        String roleId = userRoleMapping.getRoleId();
        validateRoleId(roleId);

        // Create list for having organization-user-role mappings.
        List<OrganizationUserRoleMapping> organizationUserRoleMappings = new ArrayList<>();

        AbstractUserStoreManager userStoreManager;
        try {
            userStoreManager = (AbstractUserStoreManager) getUserStoreManager(getTenantId());
            if (userStoreManager == null) {
                throw handleServerException(ERROR_CODE_USER_STORE_OPERATIONS_ERROR, " for tenant Id: " + getTenantId());
            } else {
                for (UserForUserRoleMapping userForUserRoleMapping : userRoleMapping.getUsers()) {
                    boolean userExists = userStoreManager.isExistingUserWithID(userForUserRoleMapping.getUserId());
                    if (!userExists) {
                        throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_INVALID_USER,
                                "No user exists with user Id: " + userForUserRoleMapping.getUserId() +
                                        " for tenant id: " + getTenantId());
                    }
                    if (userForUserRoleMapping.hasForcedPrivilege()) {
                        if (!userForUserRoleMapping.hasIncludeSubOrgsPrivilege()) {
                            throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_INVALID_ORGANIZATION_PARAM, "");
                        }
                        List<String> organizationsList = organizationUserRoleMgtDAO
                                .getAllSubOrganizations(organizationId);
                        addOrganizationUserRoleMappingsFromList(organizationUserRoleMappings, userForUserRoleMapping,
                                organizationId, roleId, organizationsList, true, true);
                    } else if (userForUserRoleMapping.hasIncludeSubOrgsPrivilege()) {
                        List<String> organizationsList = organizationUserRoleMgtDAO
                                .getAllSubOrganizations(organizationId);
                        addOrganizationUserRoleMappingsFromList(organizationUserRoleMappings, userForUserRoleMapping,
                                organizationId, roleId, organizationsList, false, true);
                    } else {
                        addOrganizationUserRoleMappingsFromList(organizationUserRoleMappings, userForUserRoleMapping,
                                organizationId, roleId, null, false, false);
                    }
                }
            }
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_USER_STORE_OPERATIONS_ERROR, " for tenant id: " + getTenantId(), e);
        }

        organizationUserRoleMgtDAO
                .addOrganizationUserRoleMappings(organizationUserRoleMappings, getTenantId());

        OrganizationUserRoleMappingForEvent organizationUserRoleMappingForEvent =
                new OrganizationUserRoleMappingForEvent(organizationId, roleId, userRoleMapping.getUsers().stream()
                        .map(m -> new UserForUserRoleMapping(m.getUserId(), m.hasForcedPrivilege(),
                                m.hasIncludeSubOrgsPrivilege()))
                        .collect(Collectors.toList()));
        fireEvent(POST_ASSIGN_ORGANIZATION_USER_ROLE, organizationId, organizationUserRoleMappingForEvent,
                OrganizationUserRoleEventConstants.Status.SUCCESS);
    }

    @Override
    public List<Map<String, Object>> getUsersByOrganizationAndRole(String organizationId, String roleId, int offset,
                                                                   int limit, List<String> requestedAttributes,
                                                                   String filter)
            throws OrganizationUserRoleMgtException {

        validateRoleId(roleId);
        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        if (!organizationUserRoleMgtDAO.checkOrganizationIdAvailability(organizationId)) {
            throw handleClientException(INVALID_ORGANIZATION_ID, organizationId);
        }
        List<Map<String, Object>> usersWithUserAttributesList = organizationUserRoleMgtDAO
                .getUserIdsByOrganizationAndRole(organizationId, roleId, offset, limit, requestedAttributes,
                        getTenantId(), filter);
        return usersWithUserAttributesList;
    }

    @Override
    public void patchOrganizationsUserRoleMapping(String organizationId, String roleId, String userId,
                                                  List<UserRoleOperation> userRoleOperations)
            throws OrganizationUserRoleMgtException {

        /*
         * The patchOrganizationUserRoleMapping can have two userRoleOperations.
         * 1. forced operation
         * 2. include sub organizations role operation
         * For forced role operation, if the operation is forced, then includeSubOrganizations is implied.
         * If the forced is given then we have to check the equality of the organizationId and the organization
         * id of the assignedLevel.
         * But if only the includeSubOrganization operation is given then, we have to check for non-forced
         * organization-user-role mapping for sub organizations too.
         * */
        if (CollectionUtils.isEmpty(userRoleOperations)) {
            return;
        }
        if (userRoleOperations.size() == 1) {
            throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_OPERATION_MISSING,
                    (StringUtils.equals(userRoleOperations.get(0).getPath(), IS_FORCED)
                            ? INCLUDE_SUB_ORGS : IS_FORCED).substring(1));
        }
        if (userRoleOperations.size() > 2) {
            throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_TOO_MANY_OPERATIONS, null);
        }
        UserRoleOperation[] userRoleOperationsArr = {userRoleOperations.get(0), userRoleOperations.get(1)};
        validatePatchOperation(userRoleOperations);
        validateRoleId(roleId);
        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        int directlyAssignedRoleMappingExist = organizationUserRoleMgtDAO
                .getDirectlyAssignedOrganizationUserRoleMappingInheritance(organizationId, userId, roleId,
                        getTenantId());
        // Check whether directly assigned role mapping exists
        // If role assigned level == organization id
        // then there aren't directly assigned user-role mapping.
        if (directlyAssignedRoleMappingExist == -1) {
            throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_INVALID_MAPPING, null);
        }

        UserRoleOperation isForcedOp = StringUtils.equals(IS_FORCED,
                userRoleOperationsArr[0].getPath()) ? userRoleOperationsArr[0] : userRoleOperationsArr[1];
        UserRoleOperation includeSubOrgsOp = StringUtils.equals(INCLUDE_SUB_ORGS,
                userRoleOperationsArr[0].getPath()) ? userRoleOperationsArr[0] : userRoleOperationsArr[1];

        List<OrganizationUserRoleMapping> addOrganizationUserRoleMappings = new ArrayList<>();
        List<OrganizationUserRoleMapping> deleteOrganizationUserRoleMappings = new ArrayList<>();
        List<String> organizations = organizationUserRoleMgtDAO
                .getAllSubOrganizations(organizationId);
        /*
         * If directlyAssignedRoleMappingExist=1 it means that there is the possibility of having
         * non-forced value set as well. Therefore, if the patching of it going to happen we will consider them too.
         * includeSubOrgsOp.getValue() == true and isForcedOp.getValue() == true, then we will remove the
         * non-forced user-org-role mappings
         * from sub organizations and will only have the forced user-org-role mappings.
         * */
        if (directlyAssignedRoleMappingExist == 1) {
            /*
             * check whether non-forced value is associated with the params organizationId, userId, roleId,
             * tenantId, assignedAt and forced
             * */
            boolean checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(organizationId,
                    userId, roleId, organizationId, false, getTenantId());
            /*
             * If the value is true then there is a non-forced value associated with this.
             * If false then there is not.
             * */
            if (checkMapping) {
                deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                        roleId, organizationId, false));
            }
            for (String orgId : organizations) {
                checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                        orgId, false, getTenantId());
                if (checkMapping) {
                    deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId,
                            roleId, orgId, false));
                }
            }
            if (isForcedOp.getValue()) {
                if (!includeSubOrgsOp.getValue()) {
                    throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_INVALID_BOOLEAN_VALUE, null);
                }
                // else nothing to do.
            } else {
                /*
                 * add new organization-user-role mappings with assignedAt = orgId
                 * remove organization-user-role mappings with forced and already
                 * */
                addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                        roleId, organizationId, false));
                deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                        roleId, organizationId, true));
                for (String orgId : organizations) {
                    checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                            organizationId, true, getTenantId());
                    if (checkMapping) {
                        deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId,
                                roleId, organizationId, true));
                    }
                }
                if (includeSubOrgsOp.getValue()) {
                    //add non-forced organization-user-role mappings
                    for (String orgId : organizations) {
                        addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId, roleId,
                                orgId, false));
                    }
                }
                // else nothing to do.
            }
        } else { // if directlyAssignedRoleMappingExist = 0
            if (isForcedOp.getValue()) {
                if (includeSubOrgsOp.getValue()) {
                    /*
                     * add the parent organization-user-role mapping with non-forced value to
                     * the deletion list first.
                     * */
                    deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                            roleId, organizationId, false));
                    // then check for child organizations with non-forced and remove them.
                    for (String orgId : organizations) {
                        deleteOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId, roleId,
                                orgId, false));
                    }
                    /*
                     * add the parent organization-user-role mapping with forced value to the
                     * addOrganizationUserRoleMappings list.
                     * */
                    addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(organizationId, userId,
                            roleId, organizationId, true));
                    for (String orgId : organizations) {
                        addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId,
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
                    for (String orgId : organizations) {
                        boolean checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId,
                                userId, roleId, orgId, false, getTenantId());
                        //if there is not a mapping add it.
                        if (!checkMapping) {
                            addOrganizationUserRoleMappings.add(new OrganizationUserRoleMapping(orgId, userId,
                                    roleId, orgId, false));
                        }
                    }
                }
                /*
                 * else we don't need to check it. Since even if we say don't include sub organizations,
                 * the non-forced organization-user-role mappings are unique to each organization.
                 * Therefore, we cannot possibly try to remove them here.
                 * If we need to remove them we need to delete them.
                 * */
            }
        }
        organizationUserRoleMgtDAO.updateForcedProperty(addOrganizationUserRoleMappings,
                deleteOrganizationUserRoleMappings, getTenantId());
    }

    @Override
    public void deleteOrganizationsUserRoleMapping(String organizationId, String userId, String roleId,
                                                   boolean includeSubOrgs) throws OrganizationUserRoleMgtException {

        // Fire Pre-Event
        fireEvent(PRE_REVOKE_ORGANIZATION_USER_ROLE, organizationId, null, Status.FAILURE);

        checkUserAvailability(userId);

        //validate roleId
        validateRoleId(roleId);
        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        /*
         * Check whether the role mapping is directly assigned to the particular organization or inherited from the
         * parent level.
         * */
        int directlyAssignedRoleMappingExist = organizationUserRoleMgtDAO
                .getDirectlyAssignedOrganizationUserRoleMappingInheritance(organizationId, userId, roleId,
                        getTenantId());

        if (directlyAssignedRoleMappingExist == -1) {
            throw handleClientException(DELETE_ORG_ROLE_USER_REQUEST_INVALID_DIRECT_MAPPING,
                    String.format("No directly assigned organization user role mapping found for organization: %s, " +
                                    "user: %s, role: %s, assigned at organization: %s.",
                            organizationId, userId, roleId, organizationId));
        }

        List<String> subOrganizations = organizationUserRoleMgtDAO
                .getAllSubOrganizations(organizationId);
        List<OrganizationUserRoleMapping> organizationListToBeDeleted = new ArrayList<>();
        if (directlyAssignedRoleMappingExist == 1) {
            /*
             * If directlyAssignedRoleMappingExist=1 means we are going to remove a forced role.
             * When removing forced role, we are removing everything from the sub-organizations including
             * non-forced roles as well.
             * */
            organizationListToBeDeleted.add(new OrganizationUserRoleMapping(organizationId, userId, roleId,
                    organizationId, true));
            boolean checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(organizationId,
                    userId, roleId, organizationId, false, getTenantId());
            if (checkMapping) {
                organizationListToBeDeleted.add(new OrganizationUserRoleMapping(organizationId, userId, roleId,
                        organizationId, false));
            }
            for (String orgId : subOrganizations) {
                checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                        orgId, false, getTenantId());
                if (checkMapping) {
                    organizationListToBeDeleted.add(new OrganizationUserRoleMapping(orgId, userId, roleId, orgId,
                            false));
                }
                checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                        organizationId, true, getTenantId());
                if (checkMapping) {
                    organizationListToBeDeleted.add(new OrganizationUserRoleMapping(orgId, userId, roleId,
                            organizationId, true));
                }
            }
        } else { // directlyAssignedRoleMappingExist == 0
            organizationListToBeDeleted.add(new OrganizationUserRoleMapping(organizationId, userId, roleId,
                    organizationId, false));
            boolean checkMapping;
            if (includeSubOrgs) {
                for (String orgId : subOrganizations) {
                    checkMapping = organizationUserRoleMgtDAO.isOrganizationUserRoleMappingExists(orgId, userId, roleId,
                            orgId, false, getTenantId());
                    if (checkMapping) {
                        organizationListToBeDeleted.add(new OrganizationUserRoleMapping(orgId, userId, roleId, orgId,
                                false));
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

        checkUserAvailability(userId);

        //validate organizationId
        if (!organizationUserRoleMgtDAO.checkOrganizationIdAvailability(organizationId)) {
            throw handleClientException(INVALID_ORGANIZATION_ID, organizationId);
        }
        return organizationUserRoleMgtDAO.getRolesByOrganizationAndUser(organizationId, userId,
                getTenantId());
    }

    @Override
    public boolean isOrganizationUserRoleMappingExists(String organizationId, String userId, String roleId,
                                                       String assignedLevel, boolean forced)
            throws OrganizationUserRoleMgtException {

        OrganizationUserRoleMgtDAO organizationUserRoleMgtDAO = new OrganizationUserRoleMgtDAOImpl();
        return organizationUserRoleMgtDAO
                .isOrganizationUserRoleMappingExists(organizationId, userId, roleId, assignedLevel, forced,
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
            throw handleServerException(ERROR_CODE_EVENTING_ERROR,
                    eventName, e);
        }
    }

    private void addOrganizationUserRoleMappingsFromList(List<OrganizationUserRoleMapping>
                                                                 organizationUserRoleMappingList,
                                                         UserForUserRoleMapping userForUserRoleMapping,
                                                         String organizationId, String roleId,
                                                         List<String> organizationsList, boolean isForced,
                                                         boolean includeSubOrgs) {

        organizationUserRoleMappingList.add(populateOrganizationUserRoleMappings(organizationId, roleId,
                organizationId, userForUserRoleMapping));
        if (CollectionUtils.isEmpty(organizationsList)) {
            return;
        }
        if (isForced) {
            for (String orgId : organizationsList) {
                organizationUserRoleMappingList.add(populateOrganizationUserRoleMappings(
                        orgId, roleId, organizationId, userForUserRoleMapping));
            }
        } else if (includeSubOrgs) {
            for (String orgId : organizationsList) {
                organizationUserRoleMappingList.add(populateOrganizationUserRoleMappings(orgId, roleId, orgId,
                        userForUserRoleMapping));
            }
        }
    }

    private String getAuthenticatedUserId() throws OrganizationUserRoleMgtServerException {

        return getUserIdFromUserName(getAuthenticatedUsername(), getTenantId());
    }

    private String getAuthenticatedUsername() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
    }

    private void validateRoleId(String roleId) throws OrganizationUserRoleMgtException {

        GroupDAO groupDAO = new GroupDAO();
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
                        "Provided role : " + groupName + ", is not an INTERNAL role.");
            }
        } catch (IdentitySCIMException e) {
            throw new OrganizationUserRoleMgtServerException(e);
        }
    }

    private void validateAddRoleMappingRequest(String organizationId, UserRoleMapping userRoleMapping)
            throws OrganizationUserRoleMgtException {

        for (UserForUserRoleMapping userForUserRoleMapping : userRoleMapping.getUsers()) {
            boolean isRoleMappingExists = isOrganizationUserRoleMappingExists(organizationId,
                    userForUserRoleMapping.getUserId(), userRoleMapping.getRoleId(), organizationId,
                    userForUserRoleMapping.hasForcedPrivilege());
            if (isRoleMappingExists) {
                throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_MAPPING_EXISTS, String.format(
                        "The role %s to user: %s over the organization: %s is already exists.",
                        userRoleMapping.getRoleId(), userForUserRoleMapping.getUserId(), organizationId));
            }
        }
    }

    private OrganizationUserRoleMapping populateOrganizationUserRoleMappings(String organizationId, String roleId,
                                                                             String assignedAt, UserForUserRoleMapping
                                                                                     userForUserRoleMapping) {
        OrganizationUserRoleMapping organizationUserRoleMapping = new OrganizationUserRoleMapping();
        organizationUserRoleMapping.setOrganizationId(organizationId);
        organizationUserRoleMapping.setRoleId(roleId);
        organizationUserRoleMapping.setUserId(userForUserRoleMapping.getUserId());
        organizationUserRoleMapping.setAssignedLevelOrganizationId(assignedAt);
        organizationUserRoleMapping.setForced(userForUserRoleMapping.hasForcedPrivilege());
        return organizationUserRoleMapping;
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
            // In the path, if there is not includeSubOrgs or isForced property throw an error
            if (!(StringUtils.equals(INCLUDE_SUB_ORGS, userRoleOperation.getPath()) ||
                    StringUtils.equals(IS_FORCED, userRoleOperation.getPath()))
            ) {
                throw handleClientException(PATCH_ORG_ROLE_USER_REQUEST_INVALID_PATH, null);
            }
        }
    }

    private void checkUserAvailability(String userId) throws OrganizationUserRoleMgtException {
        AbstractUserStoreManager userStoreManager;
        try {
            userStoreManager = (AbstractUserStoreManager) getUserStoreManager(getTenantId());
            if (userStoreManager == null) {
                throw handleServerException(ERROR_CODE_USER_STORE_OPERATIONS_ERROR, " for tenant Id: " + getTenantId());
            } else {
                boolean userExists = userStoreManager.isExistingUserWithID(userId);
                if (!userExists) {
                    throw handleClientException(ADD_ORG_ROLE_USER_REQUEST_INVALID_USER,
                            "No user exists with user Id: " + userId +
                                    " for tenant id: " + getTenantId());
                }
            }
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_USER_STORE_OPERATIONS_ERROR, " for tenant id: " + getTenantId(), e);
        }
    }
}

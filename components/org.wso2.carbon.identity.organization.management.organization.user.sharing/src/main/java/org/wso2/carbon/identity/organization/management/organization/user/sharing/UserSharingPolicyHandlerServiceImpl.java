package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserShareMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserShareMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserShareMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.GeneralUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareOrgDetailsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserCriteriaType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserIds;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.OrganizationScope;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.APPLICATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_AUDIENCE_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_POLICY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_GENERAL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.NULL_SHARE_INPUT_MESSAGE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.NULL_UNSHARE_INPUT_MESSAGE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_IDS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

public class UserSharingPolicyHandlerServiceImpl implements UserSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(UserSharingPolicyHandlerServiceImpl.class);
    private static ConcurrentLinkedQueue<String> errorMessages;

    @Override
    public void populateSelectiveUserShare(SelectiveUserShareDO selectiveUserShareDO) throws UserShareMgtException {

        LOG.debug("Came in user selective share");
        validateUserShareInput(selectiveUserShareDO);
        LOG.debug("Validated user selective share input");
    }

    @Override
    public void populateGeneralUserShare(GeneralUserShareDO generalUserShareDO) throws UserShareMgtException {

        LOG.debug("Came in user general share");
        validateUserShareInput(generalUserShareDO);
        LOG.debug("Validated user general share input");

        Map<String, UserCriteriaType> userCriteria = generalUserShareDO.getUserCriteria();
        PolicyEnum policy = generalUserShareDO.getPolicy();
        List<String> roleIds = getRoleIds(generalUserShareDO.getRoles());

        for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            UserCriteriaType criterionValues = criterion.getValue();

            switch (criterionKey) {
                case USER_IDS:
                    if (criterionValues instanceof UserIds) {
                        generalUserShareByUserIds((UserIds) criterionValues, policy, roleIds);
                    } else {
                        throw new UserShareMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID.getCode(),
                                ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                                ERROR_CODE_USER_CRITERIA_INVALID.getDescription());
                    }
                    break;
                default:
                    throw new UserShareMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID.getCode(),
                            ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                            ERROR_CODE_USER_CRITERIA_INVALID.getDescription());
            }
        }

    }

    private void generalUserShareByUserIds(UserIds userIds, PolicyEnum policy, List<String> roleIds)
            throws UserShareMgtException {

        String sharingInitiatedOrgId = getOrganizationId();
        for (String associatedUserId : userIds.getIds()) {
            try {
                GeneralUserShare generalUserShare =
                        new GeneralUserShare.Builder().withUserId(associatedUserId).withPolicy(policy)
                                .withRoles(roleIds).build();

                //get the orgs as per policy and then share
                List<String> orgsToShareUserWith =
                        getOrgsToShareUserWith(sharingInitiatedOrgId, generalUserShare.getPolicy());

                if (isUserAlreadyShared(associatedUserId, sharingInitiatedOrgId)) {
                    //Update User Share Path
                    List<UserAssociation> userAssociationsOfGivenUser =
                            getUserAssociationsOfGivenUser(associatedUserId, sharingInitiatedOrgId);
                    List<String> previouslySharedAnsStillInScopeOrgs = new ArrayList<>();

                    //Handle for the previously shared users
                    for (UserAssociation userAssociation : userAssociationsOfGivenUser) {

                        if (!orgsToShareUserWith.contains(userAssociation.getOrganizationId())) {
                            //Unshare from out of scopes organizations for the new policy
                            UserIds associatedUserIdsToBeRemoved =
                                    new UserIds(Collections.singletonList(userAssociation.getAssociatedUserId()));
                            selectiveUserUnshareByUserIds(associatedUserIdsToBeRemoved,
                                    Collections.singletonList(userAssociation.getOrganizationId()));
                        } else {
                            //Update the roles
                            previouslySharedAnsStillInScopeOrgs.add(userAssociation.getOrganizationId());
                            updateRolesIfNecessary(userAssociation, roleIds, sharingInitiatedOrgId);
                        }
                    }

                    //Handle for the newly sharing users
                    List<String> newlySharedOrgs = new ArrayList<>(orgsToShareUserWith);
                    newlySharedOrgs.removeAll(previouslySharedAnsStillInScopeOrgs);

                    for (String orgId : newlySharedOrgs) {
                        shareAndAssignRolesIfPresent(orgId, generalUserShare, sharingInitiatedOrgId);
                    }

                    //Update the sharing policy
                    updateResourceSharingPolicy(generalUserShare, sharingInitiatedOrgId);

                } else {
                    //Create a new user Share Path
                    for (String orgId : orgsToShareUserWith) {
                        shareAndAssignRolesIfPresent(orgId, generalUserShare, sharingInitiatedOrgId);
                    }

                    if (isApplicableOrganizationScopeForSavingPolicy(generalUserShare.getPolicy())) {
                        saveUserSharingPolicy(generalUserShare, sharingInitiatedOrgId);
                    }
                }

            } catch (OrganizationManagementException | IdentityRoleManagementException |
                     ResourceSharingPolicyMgtException e) {
                String errorMessage = String.format(ERROR_GENERAL_SHARE.getMessage(), associatedUserId, e.getMessage());
                throw new UserShareMgtServerException(ERROR_GENERAL_SHARE.getCode(), errorMessage,
                        ERROR_GENERAL_SHARE.getDescription());
            }
        }

        LOG.debug("Completed user general share.");
    }

    private boolean hasRoleChanges(List<String> oldRoleIds, List<String> newRoleIds) {

        return !new HashSet<>(oldRoleIds).equals(new HashSet<>(newRoleIds));
    }

    private void updateRolesIfNecessary(UserAssociation userAssociation, List<String> roleIds,
                                        String sharingInitiatedOrgId)
            throws OrganizationManagementException, IdentityRoleManagementException {
        //todo: get the current roles of the shared user - only the shared roles
        List<String> oldRoleIds = new ArrayList<>(); //new ArrayList<>(get the old roles - only shared roles);
        List<String> newRoleIds = getRolesToBeAddedAfterUpdate(oldRoleIds, roleIds);

        if (hasRoleChanges(oldRoleIds, newRoleIds)) {
            assignRolesIfPresent(userAssociation, sharingInitiatedOrgId, newRoleIds);
        }
    }

    private void updateResourceSharingPolicy(GeneralUserShare generalUserShare, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService =
                getResourceSharingPolicyHandlerService();

        //Delete old sharing policy
        resourceSharingPolicyHandlerService.deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                sharingInitiatedOrgId, ResourceType.USER, generalUserShare.getUserId(), sharingInitiatedOrgId);

        //Create new sharing policy
        if (isApplicableOrganizationScopeForSavingPolicy(generalUserShare.getPolicy())) {
            saveUserSharingPolicy(generalUserShare, sharingInitiatedOrgId);
        }
    }

    private void saveUserSharingPolicy(GeneralUserShare generalUserShare, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService =
                getResourceSharingPolicyHandlerService();

        ResourceSharingPolicy resourceSharingPolicy =
                new ResourceSharingPolicy.Builder().withResourceType(ResourceType.USER)
                        .withResourceId(generalUserShare.getUserId())
                        .withInitiatingOrgId(sharingInitiatedOrgId)
                        .withPolicyHoldingOrgId(sharingInitiatedOrgId)
                        .withSharingPolicy(generalUserShare.getPolicy()).build();

        List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();
        for (String roleId : generalUserShare.getRoles()) {
            SharedResourceAttribute sharedResourceAttribute =
                    new SharedResourceAttribute.Builder().withSharedAttributeType(SharedAttributeType.ROLE)
                            .withSharedAttributeId(roleId).build();
            sharedResourceAttributes.add(sharedResourceAttribute);
        }

        resourceSharingPolicyHandlerService.addResourceSharingPolicyWithAttributes(resourceSharingPolicy,
                sharedResourceAttributes);

    }

    private boolean isApplicableOrganizationScopeForSavingPolicy(PolicyEnum policy) {

        return OrganizationScope.EXISTING_ORGS_AND_FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope()) ||
                OrganizationScope.FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope());
    }

    private void shareAndAssignRolesIfPresent(String orgId, GeneralUserShare generalUserShare,
                                              String sharingInitiatedOrgId)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String associatedUserId = generalUserShare.getUserId();
        List<String> roleIds = generalUserShare.getRoles();

        UserAssociation userAssociation =
                shareUserWithOrganization(orgId, associatedUserId, sharingInitiatedOrgId);

        // Assign roles if any are present
        assignRolesIfPresent(userAssociation, sharingInitiatedOrgId, roleIds);
    }

    private boolean isUserAlreadyShared(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        List<UserAssociation> userAssociationsOfGivenUser =
                getUserAssociationsOfGivenUser(associatedUserId, associatedOrgId);

        return userAssociationsOfGivenUser != null && !userAssociationsOfGivenUser.isEmpty();
    }

    private List<UserAssociation> getUserAssociationsOfGivenUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        return getOrganizationUserSharingService().getUserAssociationsOfGivenUser(associatedUserId, associatedOrgId);
    }

    private List<String> getRolesToBeAddedAfterUpdate(List<String> oldRoleIds, List<String> newRoleIds) {

        List<String> rolesToBeRemoved = new ArrayList<>(oldRoleIds);
        List<String> rolesToBeAdded = new ArrayList<>(newRoleIds);

        rolesToBeRemoved.removeAll(newRoleIds); // Determine roles to be removed.
        rolesToBeAdded.removeAll(oldRoleIds); // Determine roles to be added.

        deleteOldRoles(rolesToBeRemoved); // Handle role deletion.
        return rolesToBeAdded;
    }

    private void deleteOldRoles(List<String> rolesToBeRemoved) {

        //todo: delete the old roles (rolesToBeRemoved)
    }

    private boolean isUserAlreadySharedInGivenOrg(String associatedUserId, String orgId)
            throws OrganizationManagementException {

        return getOrganizationUserSharingService().getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId)
                != null;
    }

    private void assignRolesIfPresent(UserAssociation userAssociation, String sharingInitiatedOrgId,
                                      List<String> roleIds)
            throws OrganizationManagementException, IdentityRoleManagementException {

        if (!roleIds.isEmpty()) {
            assignRolesToTheSharedUser(userAssociation, sharingInitiatedOrgId, roleIds);
        }
    }

    private void assignRolesToTheSharedUser(UserAssociation userAssociation, String sharingInitiatedOrgId,
                                            List<String> roleIds)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        String sharingInitiatedOrgTenantDomain = getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);
        String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

        //TODO: Update the query

        Map<String, String> sharedRoleToMainRoleMappingsBySubOrg =
                getRoleManagementService().getSharedRoleToMainRoleMappingsBySubOrg(roleIds,
                        sharingInitiatedOrgTenantDomain);

        List<String> mainRoles = new ArrayList<>();
        for (String roleId : roleIds) {
            mainRoles.add(sharedRoleToMainRoleMappingsBySubOrg.getOrDefault(roleId, roleId));
        }

        Map<String, String> mainRoleToSharedRoleMappingsBySubOrg =
                getRoleManagementService().getMainRoleToSharedRoleMappingsBySubOrg(mainRoles, targetOrgTenantDomain);

        //TODO: Since we are going only with POST, even for role updates, we have to get the earlier roles and delete it
        //TODO: Handle sub-org role assignments (consider only roles assigned from parents)
        for (String role : mainRoleToSharedRoleMappingsBySubOrg.values()) {
            getRoleManagementService().updateUserListOfRole(role, Collections.singletonList(userId),
                    Collections.emptyList(), targetOrgTenantDomain);
        }

    }

    private UserAssociation shareUserWithOrganization(String orgId, String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        OrganizationUserSharingService organizationUserSharingService = getOrganizationUserSharingService();
        organizationUserSharingService.shareOrganizationUser(orgId, associatedUserId, associatedOrgId);
        return organizationUserSharingService.getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId);
    }

    private List<String> getOrgsToShareUserWith(String policyHoldingOrgId, PolicyEnum policy)
            throws OrganizationManagementException {

        Set<String> orgsToShareUserWith = new HashSet<>();

        switch (policy) {
            case ALL_EXISTING_ORGS_ONLY:
            case ALL_EXISTING_AND_FUTURE_ORGS: //todo: keep only this for general share
                orgsToShareUserWith.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case IMMEDIATE_EXISTING_ORGS_ONLY:
            case IMMEDIATE_EXISTING_AND_FUTURE_ORGS:
                orgsToShareUserWith.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case SELECTED_ORG_ONLY:
                orgsToShareUserWith.add(policyHoldingOrgId);
                break;

            case SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN:
                orgsToShareUserWith.add(policyHoldingOrgId);
                orgsToShareUserWith.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN:
                orgsToShareUserWith.add(policyHoldingOrgId);
                orgsToShareUserWith.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case NO_SHARING:
                break;

            default:
                throw new OrganizationManagementClientException(
                        String.format(ERROR_CODE_INVALID_POLICY.getMessage(), policy.getPolicyName()),
                        ERROR_CODE_INVALID_POLICY.getDescription(),
                        ERROR_CODE_INVALID_POLICY.getCode());
        }

        return new ArrayList<>(orgsToShareUserWith);
    }

    private List<String> getRoleIds(List<RoleWithAudienceDO> rolesWithAudience) throws UserShareMgtException {

        try {
            String sharingInitiatedOrgId = getOrganizationId();
            String sharingInitiatedTenantDomain = getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);

            List<String> list = new ArrayList<>();
            for (RoleWithAudienceDO roleWithAudienceDO : rolesWithAudience) {
                String audienceId =
                        getAudienceId(roleWithAudienceDO, sharingInitiatedOrgId, sharingInitiatedTenantDomain);
                String roleId = getRoleIdFromAudience(
                        roleWithAudienceDO.getRoleName(),
                        roleWithAudienceDO.getAudienceType(),
                        audienceId,
                        sharingInitiatedTenantDomain);
                list.add(roleId);
            }
            return list;
        } catch (OrganizationManagementException | IdentityApplicationManagementException |
                 IdentityRoleManagementException e) {
            throw new UserShareMgtServerException(ERROR_CODE_GET_ROLE_IDS.getCode(),
                    ERROR_CODE_GET_ROLE_IDS.getMessage(),
                    ERROR_CODE_GET_ROLE_IDS.getDescription());
        }

    }

    private String getAudienceId(RoleWithAudienceDO role, String originalOrgId, String tenantDomain)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        switch (role.getAudienceType()) {
            case ORGANIZATION:
                return originalOrgId;
            case APPLICATION:
                return getApplicationManagementService()
                        .getApplicationBasicInfoByName(role.getAudienceName(), tenantDomain)
                        .getApplicationResourceId();
            default:
                throw new OrganizationManagementException(
                        String.format(ERROR_CODE_INVALID_AUDIENCE_TYPE.getMessage(), role.getAudienceType()),
                        ERROR_CODE_INVALID_AUDIENCE_TYPE.getCode());
        }
    }

    private String getRoleIdFromAudience(String roleName, String audienceType, String audienceId, String tenantDomain)
            throws IdentityRoleManagementException {

        return getRoleManagementService().getRoleIdByName(roleName, audienceType, audienceId, tenantDomain);
    }

    @Override
    public void populateSelectiveUserUnshare(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserShareMgtException {

        LOG.debug("Came in user selective unshare");
        validateUserUnshareInput(selectiveUserUnshareDO);
        LOG.debug("Validated user selective unshare input");

        Map<String, UserCriteriaType> userCriteria = selectiveUserUnshareDO.getUserCriteria();
        List<String> organizations = selectiveUserUnshareDO.getOrganizations();

        for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            UserCriteriaType criterionValues = criterion.getValue();

            switch (criterionKey) {
                case USER_IDS:
                    if (criterionValues instanceof UserIds) {
                        selectiveUserUnshareByUserIds((UserIds) criterionValues, organizations);
                    } else {
                        throw new UserShareMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID.getCode(),
                                ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                                ERROR_CODE_USER_CRITERIA_INVALID.getDescription());
                    }
                    break;
                default:
                    throw new UserShareMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID.getCode(),
                            ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                            ERROR_CODE_USER_CRITERIA_INVALID.getDescription());
            }
        }

        LOG.debug("Completed user selective unshare.");
    }

    private void selectiveUserUnshareByUserIds(UserIds userIds, List<String> organizations)
            throws UserShareMgtServerException {

        String unsharingInitiatedOrgId = getOrganizationId();

        for (String associatedUserId : userIds.getIds()) {
            LOG.debug("Deleting user general unshare for associated user id : " + associatedUserId);
            try {
                for (String organizationId : organizations) {

                    getOrganizationUserSharingService().unshareOrganizationUserInSharedOrganization(associatedUserId,
                            organizationId);

                    //Delete resource sharing policy if it has been stored for future shares.
                    getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                            organizationId, ResourceType.USER, associatedUserId, unsharingInitiatedOrgId);

                    LOG.debug("Completed user selective unshare for associated user id : " + associatedUserId +
                            " in shared org id : " + organizationId);

                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new UserShareMgtServerException(ERROR_CODE_USER_UNSHARE.getCode(),
                        ERROR_CODE_USER_UNSHARE.getMessage(), ERROR_CODE_USER_UNSHARE.getDescription());
            }
            LOG.debug("Completed user general unshare for associated user id : " + associatedUserId);
        }
    }

    @Override
    public void populateGeneralUserUnshare(GeneralUserUnshareDO generalUserUnshareDO) throws UserShareMgtException {

        LOG.debug("Came in user general unshare");
        validateUserUnshareInput(generalUserUnshareDO);
        LOG.debug("Validated user general unshare input");

        Map<String, UserCriteriaType> userCriteria = generalUserUnshareDO.getUserCriteria();

        for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            UserCriteriaType criterionValues = criterion.getValue();

            switch (criterionKey) {
                case USER_IDS:
                    if (criterionValues instanceof UserIds) {
                        generalUserUnshareByUserIds((UserIds) criterionValues);
                    } else {
                        throw new UserShareMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID.getCode(),
                                ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                                ERROR_CODE_USER_CRITERIA_INVALID.getDescription());
                    }
                    break;
                default:
                    throw new UserShareMgtClientException(ERROR_CODE_USER_CRITERIA_INVALID.getCode(),
                            ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                            ERROR_CODE_USER_CRITERIA_INVALID.getDescription());
            }
        }

        LOG.debug("Completed user general unshare.");

    }

    private void generalUserUnshareByUserIds(UserIds userIds)
            throws UserShareMgtServerException {

        String unsharingInitiatedOrgId = getOrganizationId();

        for (String associatedUserId : userIds.getIds()) {
            LOG.debug("Deleting user general unshare for associated user id : " + associatedUserId);
            try {

                getOrganizationUserSharingService().unshareOrganizationUsers(associatedUserId, unsharingInitiatedOrgId);

                //Delete resource sharing policy if it has been stored for future shares.
                getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(
                        ResourceType.USER, associatedUserId, unsharingInitiatedOrgId);

                LOG.debug("Completed user general unshare for associated user id : " + associatedUserId);

            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new UserShareMgtServerException(ERROR_CODE_USER_UNSHARE.getCode(),
                        ERROR_CODE_USER_UNSHARE.getMessage(), ERROR_CODE_USER_UNSHARE.getDescription());
            }
            LOG.debug("Completed user general unshare for associated user id : " + associatedUserId);
        }
    }

    //Validation methods

    private <T extends UserCriteriaType> void validateUserShareInput(BaseUserShareDO<T> userShareDO)
            throws UserShareMgtClientException {

        if (userShareDO == null) {
            throwValidationException(NULL_SHARE_INPUT_MESSAGE,
                    UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_INPUT.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_INPUT.getDescription());
        }

        if (userShareDO instanceof SelectiveUserShareDO) {
            validateSelectiveUserShareDO((SelectiveUserShareDO) userShareDO);
        } else if (userShareDO instanceof GeneralUserShareDO) {
            validateGeneralUserShareDO((GeneralUserShareDO) userShareDO);
        }
    }

    private void validateSelectiveUserShareDO(SelectiveUserShareDO selectiveUserShareDO)
            throws UserShareMgtClientException {

        // Validate userCriteria is not null
        validateNotNull(selectiveUserShareDO.getUserCriteria(),
                ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                ERROR_CODE_USER_CRITERIA_INVALID.getCode());

        // Validate that userCriteria contains the required USER_IDS key and is not null
        if (!selectiveUserShareDO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserShareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getDescription());
        }

        // Validate organizations list is not null
        validateNotNull(selectiveUserShareDO.getOrganizations(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL.getCode());

        // Validate each organization in the list
        for (SelectiveUserShareOrgDetailsDO orgDetails : selectiveUserShareDO.getOrganizations()) {
            validateNotNull(orgDetails.getOrganizationId(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL.getCode());

            validateNotNull(orgDetails.getPolicy(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL.getCode());

            // Validate roles list is not null (it can be empty)
            if (orgDetails.getRoles() == null) {
                throwValidationException(UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getMessage(),
                        UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getCode(),
                        UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getDescription());
            } else {
                // Validate each role's properties if present
                for (RoleWithAudienceDO role : orgDetails.getRoles()) {
                    validateNotNull(role.getRoleName(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL.getMessage(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL.getCode());

                    validateNotNull(role.getAudienceName(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL.getMessage(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL.getCode());

                    validateNotNull(role.getAudienceType(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL.getMessage(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL.getCode());
                }
            }
        }
    }

    private void validateGeneralUserShareDO(GeneralUserShareDO generalDO) throws UserShareMgtClientException {

        validateNotNull(generalDO.getUserCriteria(),
                ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                ERROR_CODE_USER_CRITERIA_INVALID.getCode());
        if (!generalDO.getUserCriteria().containsKey(USER_IDS) || generalDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getDescription());
        }

        validateNotNull(generalDO.getPolicy(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL.getCode());

        validateNotNull(generalDO.getRoles(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getCode());

        // Validate each role's properties if present
        for (RoleWithAudienceDO role : generalDO.getRoles()) {
            validateNotNull(role.getRoleName(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL.getCode());

            validateNotNull(role.getAudienceName(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL.getCode());

            validateNotNull(role.getAudienceType(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL.getCode());
        }
    }

    private <T extends UserCriteriaType> void validateUserUnshareInput(BaseUserUnshareDO<T> userUnshareDO)
            throws UserShareMgtClientException {

        if (userUnshareDO == null) {
            throwValidationException(NULL_UNSHARE_INPUT_MESSAGE,
                    UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_INPUT.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_INPUT.getDescription());
        }

        if (userUnshareDO instanceof SelectiveUserUnshareDO) {
            validateSelectiveUserUnshareDO((SelectiveUserUnshareDO) userUnshareDO);
        } else if (userUnshareDO instanceof GeneralUserUnshareDO) {
            validateGeneralUserUnshareDO((GeneralUserUnshareDO) userUnshareDO);
        }
    }

    private void validateSelectiveUserUnshareDO(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserShareMgtClientException {

        // Validate userCriteria is not null
        validateNotNull(selectiveUserUnshareDO.getUserCriteria(),
                ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                ERROR_CODE_USER_CRITERIA_INVALID.getCode());

        // Validate that userCriteria contains the required USER_IDS key and is not null
        if (!selectiveUserUnshareDO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserUnshareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getDescription());
        }

        // Validate organizations list is not null
        validateNotNull(selectiveUserUnshareDO.getOrganizations(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL.getCode());

        for (String organization : selectiveUserUnshareDO.getOrganizations()) {
            validateNotNull(organization,
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL.getCode());
        }
    }

    private void validateGeneralUserUnshareDO(GeneralUserUnshareDO generalUserUnshareDO)
            throws UserShareMgtClientException {

        // Validate userCriteria is not null
        validateNotNull(generalUserUnshareDO.getUserCriteria(),
                ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                ERROR_CODE_USER_CRITERIA_INVALID.getCode());

        // Validate that userCriteria contains the required USER_IDS key and is not null
        if (!generalUserUnshareDO.getUserCriteria().containsKey(USER_IDS) ||
                generalUserUnshareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getDescription());
        }
    }

    private void validateNotNull(Object obj, String errorMessage, String errorCode)
            throws UserShareMgtClientException {

        if (obj == null) {
            throwValidationException(errorMessage, errorCode, errorMessage);
        }
    }

    private void throwValidationException(String message, String errorCode, String description)
            throws UserShareMgtClientException {

        throw new UserShareMgtClientException(errorCode, message, description, new NullPointerException(message));
    }

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationUserSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getApplicationManagementService();
    }
}

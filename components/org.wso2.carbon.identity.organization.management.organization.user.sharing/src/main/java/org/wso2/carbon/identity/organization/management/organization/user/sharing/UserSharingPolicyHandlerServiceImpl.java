package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserShareMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserShareMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserShareMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.NULL_SHARE_INPUT_MESSAGE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.NULL_UNSHARE_INPUT_MESSAGE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_IDS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getUserStoreManager;

public class UserSharingPolicyHandlerServiceImpl implements UserSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(UserSharingPolicyHandlerServiceImpl.class);
    private final OrganizationUserSharingDAO organizationUserSharingDAO = new OrganizationUserSharingDAOImpl();
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
                getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(ResourceType.USER, associatedUserId, unsharingInitiatedOrgId);

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
}

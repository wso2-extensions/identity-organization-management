/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.FilterTreeBuilder;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.core.model.OperationNode;
import org.wso2.carbon.identity.organization.management.authz.service.OrganizationManagementAuthorizationManager;
import org.wso2.carbon.identity.organization.management.authz.service.exception.OrganizationManagementAuthzServiceServerException;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.internal.OrganizationManagementDataHolder;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.ChildOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.AND;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.CREATE_ORGANIZATION_PERMISSION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.CREATE_ROOT_ORGANIZATION_PERMISSION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ATTRIBUTE_KEY_MISSING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ATTRIBUTE_VALUE_MISSING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DUPLICATE_ATTRIBUTE_KEYS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_EVALUATING_ADD_ORGANIZATION_AUTHORIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_EVALUATING_ADD_ROOT_ORGANIZATION_AUTHORIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_PARENT_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_PATCH_OPERATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_HAS_CHILD_ORGANIZATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_ID_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_NAME_CONFLICT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_NAME_RESERVED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_OPERATION_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_REQUEST_ATTRIBUTE_KEY_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_REQUEST_INVALID_PATH;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_REQUEST_MANDATORY_FIELD_INVALID_OPERATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_REQUEST_PATH_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_REQUEST_REMOVE_NON_EXISTING_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_REQUEST_REPLACE_NON_EXISTING_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCH_REQUEST_VALUE_UNDEFINED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_REQUIRED_FIELDS_MISSING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_USER_NOT_AUTHORIZED_TO_CREATE_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_USER_NOT_AUTHORIZED_TO_CREATE_ROOT_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_CREATED_TIME_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_DESCRIPTION_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_LAST_MODIFIED_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_NAME_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_AFTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_BEFORE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PARENT_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ROOT;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.buildURIForBody;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.generateUniqueID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getUserId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * This class implements the {@link OrganizationManager} interface.
 */
public class OrganizationManagerImpl implements OrganizationManager {

    @Override
    public Organization addOrganization(Organization organization) throws
            OrganizationManagementException {

        String tenantDomain = getTenantDomain();
        validateAddOrganizationRequest(organization);
        setParentOrganization(organization, tenantDomain);
        setCreatedAndLastModifiedTime(organization);
        getOrganizationManagementDAO().addOrganization(getTenantId(), tenantDomain, organization);
        return organization;
    }

    @Override
    public boolean isOrganizationExistByName(String organizationName) throws OrganizationManagementException {

        return getOrganizationManagementDAO().isOrganizationExistByName(getTenantId(), organizationName,
                getTenantDomain());
    }

    @Override
    public boolean isOrganizationExistById(String organizationId) throws OrganizationManagementException {

        return getOrganizationManagementDAO().isOrganizationExistById(getTenantId(), organizationId, getTenantDomain());
    }

    @Override
    public String getOrganizationIdByName(String organizationName) throws OrganizationManagementException {

        return getOrganizationManagementDAO().getOrganizationIdByName(getTenantId(), organizationName,
                getTenantDomain());
    }

    @Override
    public Organization getOrganization(String organizationId, Boolean showChildren) throws
            OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_ID_UNDEFINED);
        }
        int tenantId = getTenantId();
        String tenantDomain = getTenantDomain();
        Organization organization = getOrganizationManagementDAO().getOrganization(tenantId, organizationId.trim(),
                tenantDomain);

        if (organization == null) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId, tenantDomain);
        }

        if (!ROOT.equals(organization.getName())) {
            organization.getParent().setSelf(buildURIForBody(organization.getParent().getId()));
        }

        if (showChildren) {
            List<String> childOrganizationIds = getOrganizationManagementDAO().getChildOrganizationIds
                    (tenantId, organizationId, tenantDomain, organization);
            if (CollectionUtils.isNotEmpty(childOrganizationIds)) {
                List<ChildOrganizationDO> childOrganizations = new ArrayList<>();
                for (String childOrganizationId : childOrganizationIds) {
                    ChildOrganizationDO childOrganization = new ChildOrganizationDO();
                    childOrganization.setId(childOrganizationId);
                    childOrganization.setSelf(buildURIForBody(childOrganizationId));
                    childOrganizations.add(childOrganization);
                }
                organization.setChildOrganizations(childOrganizations);
            }
        } else {
            organization.setChildOrganizations(null);
        }

        return organization;
    }

    @Override
    public List<BasicOrganization> getOrganizations(Integer limit, String after, String before, String sortOrder,
                                                    String filter) throws OrganizationManagementException {

        return getOrganizationManagementDAO().getOrganizations(getTenantId(), limit, getTenantDomain(), sortOrder,
                getExpressionNodes(filter, after, before));
    }

    @Override
    public void deleteOrganization(String organizationId, Boolean force) throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_ID_UNDEFINED);
        }
        if (!isOrganizationExistById(organizationId.trim())) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId, getTenantDomain());
        }

        /*
        Organization shouldn't have any child organizations when the organization delete request is not defined as a
        forceful delete.
         */
        if (!force) {
            validateOrganizationDelete(organizationId);
        }
        getOrganizationManagementDAO().deleteOrganization(getTenantId(), organizationId.trim(), getTenantDomain());
    }

    @Override
    public Organization patchOrganization(String organizationId, List<PatchOperation> patchOperations) throws
            OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_ID_UNDEFINED);
        }
        String tenantDomain = getTenantDomain();
        organizationId = organizationId.trim();
        if (!isOrganizationExistById(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId, tenantDomain);
        }
        validateOrganizationPatchOperations(patchOperations, organizationId, tenantDomain);

        getOrganizationManagementDAO().patchOrganization(organizationId, tenantDomain, Instant.now(), patchOperations);

        Organization organization = getOrganizationManagementDAO().getOrganization(getTenantId(), organizationId,
                tenantDomain);
        if (!ROOT.equals(organization.getName())) {
            organization.getParent().setSelf(buildURIForBody(organization.getParent().getId()));
        }

        return organization;
    }

    @Override
    public Organization updateOrganization(String organizationId, String currentOrganizationName,
                                           Organization organization) throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_ID_UNDEFINED);
        }
        organizationId = organizationId.trim();
        if (!isOrganizationExistById(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId, getTenantDomain());
        }

        validateUpdateOrganizationRequest(currentOrganizationName, organization);
        updateLastModifiedTime(organization);
        getOrganizationManagementDAO().updateOrganization(organizationId, getTenantDomain(), organization);

        Organization updatedOrganization = getOrganizationManagementDAO().getOrganization(getTenantId(), organizationId,
                getTenantDomain());
        if (!ROOT.equals(updatedOrganization.getName())) {
            updatedOrganization.getParent().setSelf(buildURIForBody(updatedOrganization.getParent().getId()));
        }

        return updatedOrganization;
    }

    private void updateLastModifiedTime(Organization organization) {

        Instant now = Instant.now();
        organization.setLastModified(now);
    }

    private void validateOrganizationDelete(String organizationId) throws OrganizationManagementException {

        if (getOrganizationManagementDAO().hasChildOrganizations(organizationId, getTenantDomain())) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_HAS_CHILD_ORGANIZATIONS, organizationId,
                    getTenantDomain());
        }
    }

    private void addRootOrganization(String tenantDomain) throws OrganizationManagementException {

        if (!isUserAuthorizedToCreateRootOrganization(tenantDomain)) {
            throw handleClientException(ERROR_CODE_USER_NOT_AUTHORIZED_TO_CREATE_ROOT_ORGANIZATION, tenantDomain);
        }

        Organization organization = new Organization();
        organization.setId(generateUniqueID());
        organization.setName(ROOT);
        setCreatedAndLastModifiedTime(organization);
        getOrganizationManagementDAO().addOrganization(getTenantId(), getTenantDomain(), organization);
    }

    private void setCreatedAndLastModifiedTime(Organization organization) {

        Instant now = Instant.now();
        organization.setCreated(now);
        organization.setLastModified(now);
    }

    private void validateAddOrganizationRequest(Organization organization) throws OrganizationManagementException {

        validateAddOrganizationRequiredFields(organization);
        validateAddOrganizationNameField(organization);
        validateOrganizationAttributes(organization);
        validateAddOrganizationParent(organization);
    }

    private void validateAddOrganizationParent(Organization organization) throws OrganizationManagementException {

        // Check if the parent organization exists.
        String parentId = organization.getParent().getId();
        if (!StringUtils.equals(ROOT, parentId) && !isOrganizationExistById(parentId.trim())) {
            throw handleClientException(ERROR_CODE_INVALID_PARENT_ORGANIZATION, getTenantDomain());
        }
    }

    private void validateAddOrganizationRequiredFields(Organization organization) throws
            OrganizationManagementClientException {

        validateOrganizationRequiredFieldName(organization.getName());
        validateOrganizationRequiredFieldParentId(organization.getParent().getId());
    }

    private void validateUpdateOrganizationRequiredFields(Organization organization) throws
            OrganizationManagementClientException {

        validateOrganizationRequiredFieldName(organization.getName());
    }

    private void validateOrganizationRequiredFieldParentId(String parentId) throws
            OrganizationManagementClientException {

        if (StringUtils.isBlank(parentId)) {
            throw handleClientException(ERROR_CODE_REQUIRED_FIELDS_MISSING, PARENT_ID_FIELD);
        }
    }

    private void validateOrganizationRequiredFieldName(String organizationName) throws
            OrganizationManagementClientException {

        if (StringUtils.isBlank(organizationName)) {
            throw handleClientException(ERROR_CODE_REQUIRED_FIELDS_MISSING, ORGANIZATION_NAME_FIELD);
        }
    }

    private void validateOrganizationAttributes(Organization organization) throws
            OrganizationManagementClientException {

        List<OrganizationAttribute> organizationAttributes = organization.getAttributes();

        for (OrganizationAttribute attribute : organizationAttributes) {
            String attributeKey = attribute.getKey();
            String attributeValue = attribute.getValue();

            if (StringUtils.isBlank(attributeKey)) {
                throw handleClientException(ERROR_CODE_ATTRIBUTE_KEY_MISSING);
            }
            if (attributeValue == null) {
                throw handleClientException(ERROR_CODE_ATTRIBUTE_VALUE_MISSING);
            }
            attribute.setKey(attributeKey.trim());
            attribute.setValue(attributeValue.trim());
        }

        // Check if attribute keys are duplicated.
        Set<String> tempSet = organizationAttributes.stream().map(OrganizationAttribute::getKey)
                .collect(Collectors.toSet());
        if (organization.getAttributes().size() > tempSet.size()) {
            throw handleClientException(ERROR_CODE_DUPLICATE_ATTRIBUTE_KEYS);
        }
    }

    private void validateAddOrganizationNameField(Organization organization) throws OrganizationManagementException {

        organization.setName(organization.getName().trim());
        if (StringUtils.equals(organization.getName(), ROOT)) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_NAME_RESERVED, ROOT);
        }

        // Check if the organization name already exists for the given tenant.
        if (isOrganizationExistByName(organization.getName())) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_NAME_CONFLICT, organization.getName(),
                    getTenantDomain());
        }
    }

    private void setParentOrganization(Organization organization, String tenantDomain)
            throws OrganizationManagementException {

        ParentOrganizationDO parentOrganization = organization.getParent();
        String parentId = parentOrganization.getId().trim();
        boolean createOrganizationAuthorizationRequired = true;

        /*
        For parentId an alias as 'ROOT' is supported. This indicates that the organization should be created as an
        immediate child of the ROOT organization of this tenant. If a ROOT organization is not already available for
        this tenant, a ROOT organization will be created.
         */
        if (StringUtils.equals(ROOT, parentId)) {
            String rootOrganizationId = getOrganizationIdByName(ROOT);
            if (StringUtils.isBlank(rootOrganizationId)) {
                addRootOrganization(tenantDomain);
                createOrganizationAuthorizationRequired = false;
                rootOrganizationId = getOrganizationIdByName(ROOT);
            }
            parentId = rootOrganizationId;
        }

        /*
        For a first time organization creation in a tenant, the evaluation of user's authorization to create an
        organization as a child of the given parent (ROOT organization) will not happen.
        Having '/permission/admin/' assigned to the user would be sufficient in this scenario. This permission implies
        that the user is authorized to create the ROOT organization in the tenant along with the organization that the
        user is requesting to be created in the request.
         */
        if (createOrganizationAuthorizationRequired && !isUserAuthorizedToCreateOrganization(parentId)) {
            throw handleClientException(ERROR_CODE_USER_NOT_AUTHORIZED_TO_CREATE_ORGANIZATION, parentId);
        }
        parentOrganization.setId(parentId);
        parentOrganization.setSelf(buildURIForBody(parentId));
    }

    private boolean isUserAuthorizedToCreateRootOrganization(String tenantDomain) throws
            OrganizationManagementServerException {

        String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            UserRealm tenantUserRealm = getRealmService().getTenantUserRealm(getTenantId());
            AuthorizationManager authorizationManager = tenantUserRealm.getAuthorizationManager();
            return authorizationManager.isUserAuthorized(username, CREATE_ROOT_ORGANIZATION_PERMISSION,
                    CarbonConstants.UI_PERMISSION_ACTION);
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_EVALUATING_ADD_ROOT_ORGANIZATION_AUTHORIZATION, e,
                    tenantDomain);
        }
    }

    private boolean isUserAuthorizedToCreateOrganization(String parentId) throws OrganizationManagementServerException {

        try {
            return OrganizationManagementAuthorizationManager.getInstance().isUserAuthorized(getUserId(),
                    CREATE_ORGANIZATION_PERMISSION, parentId, getTenantId());
        } catch (OrganizationManagementAuthzServiceServerException e) {
            throw handleServerException(ERROR_CODE_ERROR_EVALUATING_ADD_ORGANIZATION_AUTHORIZATION, e, parentId);
        }
    }

    private void validateUpdateOrganizationRequest(String currentOrganizationName, Organization organization)
            throws OrganizationManagementException {

        validateUpdateOrganizationRequiredFields(organization);

        String newOrganizationName = organization.getName().trim();
        // Check if the organization name already exists for the given tenant.
        if (!StringUtils.equals(currentOrganizationName, newOrganizationName) &&
                isOrganizationExistByName(newOrganizationName)) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_NAME_CONFLICT, newOrganizationName, getTenantDomain());
        }
        organization.setName(newOrganizationName);

        validateOrganizationAttributes(organization);
    }

    private void validateOrganizationPatchOperations(List<PatchOperation> patchOperations, String organizationId,
                                                     String tenantDomain) throws OrganizationManagementException {

        for (PatchOperation patchOperation : patchOperations) {
            // Validate requested patch operation.
            if (StringUtils.isBlank(patchOperation.getOp())) {
                throw handleClientException(ERROR_CODE_PATCH_OPERATION_UNDEFINED, organizationId, getTenantDomain());
            }
            String op = patchOperation.getOp().trim();
            if (!(PATCH_OP_ADD.equals(op) || PATCH_OP_REMOVE.equals(op) || PATCH_OP_REPLACE.equals(op))) {
                throw handleClientException(ERROR_CODE_INVALID_PATCH_OPERATION, op);
            }

            // Validate path.
            if (StringUtils.isBlank(patchOperation.getPath())) {
                throw handleClientException(ERROR_CODE_PATCH_REQUEST_PATH_UNDEFINED);
            }
            String path = patchOperation.getPath().trim();

            /*
            Check if it is a supported path for patching.
            Fields such as the parentId can't be modified with the current implementation.
             */
            if (!(path.equals(PATCH_PATH_ORG_NAME) || path.equals(PATCH_PATH_ORG_DESCRIPTION) ||
                    path.startsWith(PATCH_PATH_ORG_ATTRIBUTES))) {
                throw handleClientException(ERROR_CODE_PATCH_REQUEST_INVALID_PATH, path);
            }

            // Validate value.
            String value;
            // Value is mandatory for Add and Replace operations.
            if (StringUtils.isBlank(patchOperation.getValue()) && !PATCH_OP_REMOVE.equals(op)) {
                throw handleClientException(ERROR_CODE_PATCH_REQUEST_VALUE_UNDEFINED);
            } else {
                // Avoid NPEs down the road.
                value = patchOperation.getValue() != null ? patchOperation.getValue().trim() : "";
            }

            // Mandatory fields can only be 'Replaced'.
            if (!op.equals(PATCH_OP_REPLACE) && !(path.equals(PATCH_PATH_ORG_DESCRIPTION) ||
                    path.startsWith(PATCH_PATH_ORG_ATTRIBUTES))) {
                throw handleClientException(ERROR_CODE_PATCH_REQUEST_MANDATORY_FIELD_INVALID_OPERATION, op, path);
            }

            // Check if the new organization name already exists.
            if (path.equals(PATCH_PATH_ORG_NAME) && isOrganizationExistByName(value) &&
                    !StringUtils.equals(getOrganizationIdByName(value), organizationId)) {
                throw handleClientException(ERROR_CODE_ORGANIZATION_NAME_CONFLICT, value, getTenantDomain());
            }

            if (path.startsWith(PATCH_PATH_ORG_ATTRIBUTES)) {
                String attributeKey = path.replace(PATCH_PATH_ORG_ATTRIBUTES, "").trim();
                // Attribute key can not be empty.
                if (StringUtils.isBlank(attributeKey)) {
                    throw handleClientException(ERROR_CODE_PATCH_REQUEST_ATTRIBUTE_KEY_UNDEFINED);
                }
                boolean attributeExist = getOrganizationManagementDAO()
                        .isAttributeExistByKey(tenantDomain, organizationId, attributeKey);
                // If attribute key to be added already exists, update its value.
                if (op.equals(PATCH_OP_ADD) && attributeExist) {
                    op = PATCH_OP_REPLACE;
                }
                if (op.equals(PATCH_OP_REMOVE) && !attributeExist) {
                    throw handleClientException(ERROR_CODE_PATCH_REQUEST_REMOVE_NON_EXISTING_ATTRIBUTE, attributeKey);
                }
                if (op.equals(PATCH_OP_REPLACE) && !attributeExist) {
                    throw handleClientException(ERROR_CODE_PATCH_REQUEST_REPLACE_NON_EXISTING_ATTRIBUTE, attributeKey);
                }
            }

            patchOperation.setOp(op);
            patchOperation.setPath(path);
            patchOperation.setValue(value);
        }
    }

    private List<ExpressionNode> getExpressionNodes(String filter, String after, String before)
            throws OrganizationManagementClientException {

        List<ExpressionNode> expressionNodes = new ArrayList<>();
        if (StringUtils.isBlank(filter)) {
            filter = StringUtils.EMPTY;
        }
        String paginatedFilter = getPaginatedFilter(filter, after, before);
        try {
            if (StringUtils.isNotBlank(paginatedFilter)) {
                FilterTreeBuilder filterTreeBuilder = new FilterTreeBuilder(paginatedFilter);
                Node rootNode = filterTreeBuilder.buildTree();
                setExpressionNodeList(rootNode, expressionNodes);
            }
        } catch (IOException | IdentityException e) {
            throw handleClientException(ERROR_CODE_INVALID_FILTER_FORMAT);
        }
        return expressionNodes;
    }

    private String getPaginatedFilter(String paginatedFilter, String after, String before) throws
            OrganizationManagementClientException {

        try {
            if (StringUtils.isNotBlank(before)) {
                String decodedString = new String(Base64.getDecoder().decode(before), StandardCharsets.UTF_8);
                Timestamp.valueOf(decodedString);
                paginatedFilter += StringUtils.isNotBlank(paginatedFilter) ? " and before gt " + decodedString :
                        "before gt " + decodedString;
            } else if (StringUtils.isNotBlank(after)) {
                String decodedString = new String(Base64.getDecoder().decode(after), StandardCharsets.UTF_8);
                Timestamp.valueOf(decodedString);
                paginatedFilter += StringUtils.isNotBlank(paginatedFilter) ? " and after lt " + decodedString :
                        "after lt " + decodedString;
            }
        } catch (IllegalArgumentException e) {
            throw handleClientException(ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION);
        }
        return paginatedFilter;
    }

    /**
     * Sets the expression nodes required for the retrieval of organizations from the database.
     *
     * @param node       The node.
     * @param expression The list of expression nodes.
     */
    private void setExpressionNodeList(Node node, List<ExpressionNode> expression) throws
            OrganizationManagementClientException {

        if (node instanceof ExpressionNode) {
            ExpressionNode expressionNode = (ExpressionNode) node;
            String attributeValue = expressionNode.getAttributeValue();
            if (StringUtils.isNotBlank(attributeValue)) {
                if (isFilteringAttributeNotSupported(attributeValue)) {
                    throw handleClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE, attributeValue);
                }
                expression.add(expressionNode);
            }
        } else if (node instanceof OperationNode) {
            String operation = ((OperationNode) node).getOperation();
            if (!StringUtils.equalsIgnoreCase(operation, AND)) {
                throw handleClientException(ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER);
            }
            setExpressionNodeList(node.getLeftNode(), expression);
            setExpressionNodeList(node.getRightNode(), expression);
        }
    }

    private boolean isFilteringAttributeNotSupported(String attributeValue) {

        return !attributeValue.equalsIgnoreCase(ORGANIZATION_ID_FIELD) &&
                !attributeValue.equalsIgnoreCase(ORGANIZATION_NAME_FIELD) &&
                !attributeValue.equalsIgnoreCase(ORGANIZATION_DESCRIPTION_FIELD) &&
                !attributeValue.equalsIgnoreCase(ORGANIZATION_CREATED_TIME_FIELD) &&
                !attributeValue.equalsIgnoreCase(ORGANIZATION_LAST_MODIFIED_FIELD) &&
                !attributeValue.equalsIgnoreCase(PAGINATION_AFTER) &&
                !attributeValue.equalsIgnoreCase(PAGINATION_BEFORE);
    }

    /**
     * Returns a OrganizationManagementDAO instance.
     *
     * @return A OrganizationManagementDAO instance.
     */
    private OrganizationManagementDAO getOrganizationManagementDAO() {

        return OrganizationManagementDataHolder.getInstance().getOrganizationManagementDAO();
    }

    private RealmService getRealmService() {

        return OrganizationManagementDataHolder.getInstance().getRealmService();
    }
}

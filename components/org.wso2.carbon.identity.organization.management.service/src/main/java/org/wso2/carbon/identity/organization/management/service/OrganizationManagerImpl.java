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

package org.wso2.carbon.identity.organization.management.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.internal.OrganizationManagementDataHolder;
import org.wso2.carbon.identity.organization.management.service.model.ChildOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ATTRIBUTE_KEY_MISSING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ATTRIBUTE_VALUE_MISSING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DUPLICATE_ATTRIBUTE_KEYS;
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
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_NAME_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PARENT_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ROOT;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.buildURIForBody;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;

/**
 * This class implements the {@link OrganizationManager} interface.
 */
public class OrganizationManagerImpl implements OrganizationManager {

    @Override
    public Organization addOrganization(Organization organization) throws
            OrganizationManagementException {

        validateAddOrganizationRequest(organization);
        setParentOrganization(organization);
        setCreatedAndLastModifiedTime(organization);
        getOrganizationManagementDAO().addOrganization(getTenantId(), getTenantDomain(), organization);
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
    public List<String> getOrganizationIds() throws OrganizationManagementException {

        return getOrganizationManagementDAO().getOrganizationIds(getTenantId(), getTenantDomain());
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

        String tenantDomain = getTenantDomain();
        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_ORGANIZATION_ID_UNDEFINED);
        }
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

    private void addRootOrganization() throws OrganizationManagementServerException {

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

    private String generateUniqueID() {

        return UUID.randomUUID().toString();
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

    private void setParentOrganization(Organization organization) throws OrganizationManagementException {

        ParentOrganizationDO parentOrganization = organization.getParent();
        String parentId = parentOrganization.getId().trim();

        /*
        For parentId an alias as 'ROOT' is supported. This indicates that the organization should be created as an
        immediate child of the ROOT organization of this tenant. If a ROOT organization is not already available for
        this tenant, a ROOT organization will be created.
         */
        if (StringUtils.equals(ROOT, parentId)) {
            String rootOrganizationId = getOrganizationIdByName(ROOT);
            if (StringUtils.isBlank(rootOrganizationId)) {
                addRootOrganization();
                rootOrganizationId = getOrganizationIdByName(ROOT);
            }
            parentId = rootOrganizationId;
        }
        parentOrganization.setId(parentId);
        parentOrganization.setSelf(buildURIForBody(parentId));
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

    /**
     * Returns a OrganizationManagementDAO instance.
     *
     * @return A OrganizationManagementDAO instance.
     */
    private OrganizationManagementDAO getOrganizationManagementDAO() {

        return OrganizationManagementDataHolder.getInstance().getOrganizationManagementDAO();
    }
}

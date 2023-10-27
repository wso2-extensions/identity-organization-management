/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.discovery.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.discovery.service.dao.OrganizationDiscoveryDAO;
import org.wso2.carbon.identity.organization.discovery.service.dao.OrganizationDiscoveryDAOImpl;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.discovery.service.model.DiscoveryOrganizationsResult;
import org.wso2.carbon.identity.organization.discovery.service.model.OrgDiscoveryAttribute;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.filter.ExpressionNode;
import org.wso2.carbon.identity.organization.management.service.filter.FilterTreeBuilder;
import org.wso2.carbon.identity.organization.management.service.filter.Node;
import org.wso2.carbon.identity.organization.management.service.filter.OperationNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.discovery.service.constant.DiscoveryConstants.ORGANIZATION_NAME;
import static org.wso2.carbon.identity.organization.discovery.service.constant.DiscoveryConstants.SUPPORTED_OPERATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.AND;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DISCOVERY_ATTRIBUTE_ALREADY_ADDED_FOR_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DISCOVERY_ATTRIBUTE_TAKEN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_DISABLED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DUPLICATE_DISCOVERY_ATTRIBUTE_TYPES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_EMPTY_DISCOVERY_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_DISCOVERY_ATTRIBUTE_VALUE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_LIMIT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_OFFSET;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNAUTHORIZED_ORG_FOR_DISCOVERY_ATTRIBUTE_MANAGEMENT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_DISCOVERY_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_FILTER_OPERATION_FOR_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;

/**
 * Implementation of Organization Discovery Manager Interface.
 */
public class OrganizationDiscoveryManagerImpl implements OrganizationDiscoveryManager {

    private static final OrganizationDiscoveryDAO organizationDiscoveryDAO = new OrganizationDiscoveryDAOImpl();
    private static final OrganizationManager organizationManager = OrganizationDiscoveryServiceHolder.getInstance()
            .getOrganizationManager();

    @Override
    public List<OrgDiscoveryAttribute> addOrganizationDiscoveryAttributes(String organizationId,
                                                                          List<OrgDiscoveryAttribute>
                                                                                  discoveryAttributes,
                                                                          boolean validateRootOrgAccess)
            throws OrganizationManagementException {

        String rootOrganizationId = organizationManager.getPrimaryOrganizationId(organizationId);
        if (validateRootOrgAccess) {
            validateRootOrganization(rootOrganizationId, organizationId);
        }

        if (organizationDiscoveryDAO.isDiscoveryAttributeAddedToOrganization(organizationId)) {
            throw handleClientException(ERROR_CODE_DISCOVERY_ATTRIBUTE_ALREADY_ADDED_FOR_ORGANIZATION, organizationId);
        }

        validateOrganizationDiscoveryAttributes(false, rootOrganizationId, null, discoveryAttributes);

        organizationDiscoveryDAO.addOrganizationDiscoveryAttributes(organizationId, discoveryAttributes);
        return discoveryAttributes;
    }

    @Override
    public List<OrgDiscoveryAttribute> getOrganizationDiscoveryAttributes(String organizationId, boolean
            validateRootOrgAccess) throws OrganizationManagementException {

        if (validateRootOrgAccess) {
            String rootOrganizationId = organizationManager.getPrimaryOrganizationId(organizationId);
            validateRootOrganization(rootOrganizationId, organizationId);
        }
        return organizationDiscoveryDAO.getOrganizationDiscoveryAttributes(organizationId);
    }

    @Override
    public void deleteOrganizationDiscoveryAttributes(String organizationId, boolean validateRootOrgAccess) throws
            OrganizationManagementException {

        if (validateRootOrgAccess) {
            String rootOrganizationId = organizationManager.getPrimaryOrganizationId(organizationId);
            validateRootOrganization(rootOrganizationId, organizationId);
        }
        organizationDiscoveryDAO.deleteOrganizationDiscoveryAttributes(organizationId);
    }

    @Override
    public List<OrgDiscoveryAttribute> updateOrganizationDiscoveryAttributes(String organizationId,
                                                                             List<OrgDiscoveryAttribute>
                                                                                     discoveryAttributes,
                                                                             boolean validateRootOrgAccess)
            throws OrganizationManagementException {

        String rootOrganizationId = organizationManager.getPrimaryOrganizationId(organizationId);
        if (validateRootOrgAccess) {
            validateRootOrganization(rootOrganizationId, organizationId);
        }
        validateOrganizationDiscoveryAttributes(true, rootOrganizationId, organizationId, discoveryAttributes);
        organizationDiscoveryDAO.updateOrganizationDiscoveryAttributes(organizationId, discoveryAttributes);
        return discoveryAttributes;
    }

    @Override
    public boolean isDiscoveryAttributeValueAvailable(String type, String value) throws
            OrganizationManagementException {

        return !organizationDiscoveryDAO.isDiscoveryAttributeExistInHierarchy(false, getOrganizationId(),
                null, type, Collections.singletonList(value));
    }

    @Override
    public boolean isDiscoveryAttributeValueAvailable(String organizationId, String type, String value)
            throws OrganizationManagementException {

        return !organizationDiscoveryDAO.isDiscoveryAttributeExistInHierarchy(false, organizationId,
                null, type, Collections.singletonList(value));
    }

    @Override
    public DiscoveryOrganizationsResult getOrganizationsDiscoveryAttributes(Integer limit, Integer offset,
                                                                            String filter)
            throws OrganizationManagementException {

        limit = validateLimit(limit);
        offset = validateOffset(offset);
        List<ExpressionNode> expressionNodes = getExpressionNodes(filter);
        return organizationDiscoveryDAO.getOrganizationsDiscoveryAttributes(limit,
                offset, getOrganizationId(), expressionNodes);
    }

    @Override
    public Map<String, AttributeBasedOrganizationDiscoveryHandler> getAttributeBasedOrganizationDiscoveryHandlers() {

        return OrganizationDiscoveryServiceHolder.getInstance().getAttributeBasedOrganizationDiscoveryHandlers();
    }

    @Override
    public String getOrganizationIdByDiscoveryAttribute(String attributeType, String discoveryInput,
                                                        String rootOrganizationId)
            throws OrganizationManagementException {

        AttributeBasedOrganizationDiscoveryHandler handler = getAttributeBasedOrganizationDiscoveryHandlers()
                .get(attributeType);
        String attributeValue = handler.extractAttributeValue(discoveryInput);
        if (StringUtils.isNotBlank(attributeValue)) {
            return organizationDiscoveryDAO.getOrganizationIdByDiscoveryAttribute(attributeType, attributeValue,
                    rootOrganizationId);
        }
        return null;
    }

    private void validateRootOrganization(String rootOrganizationId, String organizationId)
            throws OrganizationManagementClientException {

        // Not having a root organization implies that the organization is not a valid organization.
        if (StringUtils.isBlank(rootOrganizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION, organizationId);
        }
        // Checks if the organization ID present in the context as the root organization of the given organization.
        if (!StringUtils.equals(getOrganizationId(), rootOrganizationId)) {
            throw handleClientException(ERROR_CODE_UNAUTHORIZED_ORG_FOR_DISCOVERY_ATTRIBUTE_MANAGEMENT, organizationId);
        }
    }

    private void validateOrganizationDiscoveryAttributes(boolean excludeCurrentOrganization, String rootOrganizationId,
                                                         String organizationId,
                                                         List<OrgDiscoveryAttribute> discoveryAttributes)
            throws OrganizationManagementException {

        Set<String> uniqueDiscoveryAttributeTypes = new HashSet<>();
        if (CollectionUtils.isEmpty(discoveryAttributes)) {
            throw handleClientException(ERROR_CODE_EMPTY_DISCOVERY_ATTRIBUTES, organizationId);
        }
        for (OrgDiscoveryAttribute attribute : discoveryAttributes) {
            String attributeType = attribute.getType();
            if (!uniqueDiscoveryAttributeTypes.add(attributeType)) {
                throw handleClientException(ERROR_CODE_DUPLICATE_DISCOVERY_ATTRIBUTE_TYPES, attributeType);
            }

            if (!OrganizationDiscoveryServiceHolder.getInstance().getDiscoveryTypes().contains(attributeType)) {
                throw handleClientException(ERROR_CODE_UNSUPPORTED_DISCOVERY_ATTRIBUTE, attributeType);
            }

            AttributeBasedOrganizationDiscoveryHandler discoveryHandler = OrganizationDiscoveryServiceHolder
                    .getInstance().getAttributeBasedOrganizationDiscoveryHandler(attributeType);

            if (!discoveryHandler.isDiscoveryConfigurationEnabled(rootOrganizationId)) {
                throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_DISABLED, getOrganizationId());
            }

            attribute.setValues(attribute.getValues().stream().distinct().collect(Collectors.toList()));
            if (!discoveryHandler.areAttributeValuesInValidFormat(attribute.getValues())) {
                throw handleClientException(ERROR_CODE_INVALID_DISCOVERY_ATTRIBUTE_VALUE, attributeType);
            }
            boolean discoveryAttributeTaken = organizationDiscoveryDAO.isDiscoveryAttributeExistInHierarchy
                    (excludeCurrentOrganization, rootOrganizationId, organizationId, attributeType,
                            attribute.getValues());
            if (discoveryAttributeTaken) {
                throw handleClientException(ERROR_CODE_DISCOVERY_ATTRIBUTE_TAKEN, attributeType);
            }
        }
    }

    private List<ExpressionNode> getExpressionNodes(String filter) throws OrganizationManagementClientException {

        List<ExpressionNode> expressionNodes = new ArrayList<>();
        if (StringUtils.isBlank(filter)) {
            filter = StringUtils.EMPTY;
        }
        try {
            if (StringUtils.isNotBlank(filter)) {
                FilterTreeBuilder filterTreeBuilder = new FilterTreeBuilder(filter);
                Node rootNode = filterTreeBuilder.buildTree();
                setExpressionNodeList(rootNode, expressionNodes);
            }
        } catch (IOException e) {
            throw handleClientException(ERROR_CODE_INVALID_FILTER_FORMAT);
        }
        return expressionNodes;
    }

    private void setExpressionNodeList(Node node, List<ExpressionNode> expression) throws
            OrganizationManagementClientException {

        if (node instanceof ExpressionNode) {
            ExpressionNode expressionNode = (ExpressionNode) node;
            String attributeValue = expressionNode.getAttributeValue();
            String operation = expressionNode.getOperation();

            if (StringUtils.isNotBlank(attributeValue)) {
                if (!isFilteringAttributeSupported(attributeValue)) {
                    throw handleClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE, attributeValue);
                }
                if (!SUPPORTED_OPERATIONS.contains(operation)) {
                    throw handleClientException(ERROR_CODE_UNSUPPORTED_FILTER_OPERATION_FOR_ATTRIBUTE,
                            operation, attributeValue);
                }
                expression.add(expressionNode);
            }
        } else if (node instanceof OperationNode) {
            String operation = ((OperationNode) node).getOperation();
            if (!StringUtils.equalsIgnoreCase(AND, operation)) {
                throw handleClientException(ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER);
            }
            setExpressionNodeList(node.getLeftNode(), expression);
            setExpressionNodeList(node.getRightNode(), expression);
        }
    }

    private boolean isFilteringAttributeSupported(String attributeValue) {

        return ORGANIZATION_NAME.equalsIgnoreCase(attributeValue);
    }

    /**
     * Validate limit.
     *
     * @param limit The given limit value.
     * @return Validated limit.
     * @throws OrganizationManagementClientException Exception thrown for invalid limit.
     */
    private int validateLimit(Integer limit) throws OrganizationManagementClientException {

        if (limit == null) {
            limit = IdentityUtil.getDefaultItemsPerPage();
        }
        if (limit < 0) {
            throw handleClientException(ERROR_CODE_INVALID_LIMIT);
        }
        int maximumItemsPerPage = IdentityUtil.getMaximumItemPerPage();
        if (limit > maximumItemsPerPage) {
            limit = maximumItemsPerPage;
        }
        return limit;
    }

    /**
     * Validate offset.
     *
     * @param offset The given offset value.
     * @return Validated offset value.
     * @throws OrganizationManagementClientException Exception thrown for invalid offset.
     */
    private int validateOffset(Integer offset) throws OrganizationManagementClientException {

        if (offset == null) {
            offset = 0;
        }
        if (offset < 0) {
            throw handleClientException(ERROR_CODE_INVALID_OFFSET);
        }
        return offset;
    }
}

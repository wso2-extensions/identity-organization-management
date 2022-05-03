/*
 *
 *  * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.role.management.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.FilterTreeBuilder;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.core.model.OperationNode;
import org.wso2.carbon.identity.organization.management.role.management.service.dao.RoleManagementDAO;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementClientException;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementException;
import org.wso2.carbon.identity.organization.management.role.management.service.internal.RoleManagementDataHolder;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.AND;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ROLE_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ROLE_NAME_FIELD;

/**
 * Implementation of Role Manager Interface.
 */
public class RoleManagerImpl implements RoleManager {

    private static final Log LOG = LogFactory.getLog(RoleManagerImpl.class);

    @Override
    public Role addRole(String organizationId, Role role) throws
            RoleManagementException {

        getRoleManagementDAO().addRole(organizationId, Utils.getTenantId(), role);
        return new Role(role.getId(), role.getName());
    }

    @Override
    public Role getRoleById(String organizationId, String roleId) throws RoleManagementException {

        return getRoleManagementDAO().getRoleById(roleId, organizationId, Utils.getTenantId());
    }

    @Override
    public List<Role> getOrganizationRoles(int limit, String after, String before, String sortOrder, String filter,
                                           String organizationId) throws RoleManagementException {

        return getRoleManagementDAO().getOrganizationRoles(organizationId, sortOrder, Utils.getTenantId(), limit,
                getExpressionNodes(filter, after, before));
    }

    @Override
    public Role patchRole(String organizationId, String roleId, List<PatchOperation> patchOperations)
            throws RoleManagementException {

        return getRoleManagementDAO().patchRole(organizationId, roleId, Utils.getTenantId(), patchOperations);
    }

    @Override
    public Role putRole(String organizationId, String roleId, Role role) throws RoleManagementException {

        return getRoleManagementDAO().putRole(organizationId, roleId, role, Utils.getTenantId());
    }

    @Override
    public void deleteRole(String organizationId, String roleId) throws RoleManagementException {

        getRoleManagementDAO().deleteRole(organizationId, roleId);
    }

    /**
     * Get an instance of RoleManagementDAO.
     *
     * @return An instance of RoleManagementDAO.
     */
    private RoleManagementDAO getRoleManagementDAO() {

        return RoleManagementDataHolder.getInstance().getRoleManagementDAO();
    }


    private List<ExpressionNode> getExpressionNodes(String filter, String after, String before)
            throws RoleManagementClientException {

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
            throw Utils.handleClientException(ERROR_CODE_INVALID_FILTER_FORMAT);
        }
        return expressionNodes;
    }

    private String getPaginatedFilter(String paginatedFilter, String after, String before) throws
            RoleManagementClientException {

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
            throw Utils.handleClientException(ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION);
        }
        return paginatedFilter;
    }

    private void setExpressionNodeList(Node node, List<ExpressionNode> expression) throws
            RoleManagementClientException {

        if (node instanceof ExpressionNode) {
            ExpressionNode expressionNode = (ExpressionNode) node;
            String attributeValue = expressionNode.getAttributeValue();
            if (StringUtils.isNotBlank(attributeValue)) {
                if (isFilteringAttributeNotSupported(attributeValue)) {
                    throw Utils.handleClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE, attributeValue);
                }
                expression.add(expressionNode);
            }
        } else if (node instanceof OperationNode) {
            String operation = ((OperationNode) node).getOperation();
            if (!StringUtils.equalsIgnoreCase(operation, AND)) {
                throw Utils.handleClientException(ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER);
            }
            setExpressionNodeList(node.getLeftNode(), expression);
            setExpressionNodeList(node.getRightNode(), expression);
        }
    }

    private boolean isFilteringAttributeNotSupported(String attributeValue) {

        return !attributeValue.equalsIgnoreCase(ROLE_ID_FIELD) &&
                !attributeValue.equalsIgnoreCase(ROLE_NAME_FIELD);
    }
}

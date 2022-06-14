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

package org.wso2.carbon.identity.organization.management.role.management.service.util;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.core.model.OperationNode;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;

import java.util.List;

import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.AND_OPERATOR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.OR_OPERATOR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ROLE_NAME_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;

/**
 * Utility class containing utility functions for role management.
 */
public class Utils {

    /**
     * Setting the expression nodes list and operators list for filtering.
     *
     * @param node                    The root node.
     * @param expression              The expression nodes list.
     * @param operators               The operators list.
     * @param checkFilteringAttribute If checking the filtering attribute is necessary its true, else false.
     * @throws OrganizationManagementClientException Throws an exception if the operators passed by client
     *                                       are not valid operators.
     */
    public static void setExpressionNodeAndOperatorLists(Node node, List<ExpressionNode> expression,
                                                         List<String> operators, boolean checkFilteringAttribute)
            throws OrganizationManagementClientException {

        if (node instanceof ExpressionNode) {
            ExpressionNode expressionNode = (ExpressionNode) node;
            String attributeValue = expressionNode.getAttributeValue();
            if (StringUtils.isNotBlank(attributeValue)) {
                if (checkFilteringAttribute) {
                    if (isFilteringAttributeNotSupported(attributeValue)) {
                        throw handleClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE, attributeValue);
                    }
                }
                expression.add(expressionNode);
            }
        } else if (node instanceof OperationNode) {
            String operation = ((OperationNode) node).getOperation();
            if (!StringUtils.equalsIgnoreCase(operation, AND_OPERATOR) &&
                    !StringUtils.equalsIgnoreCase(operation, OR_OPERATOR)) {
                throw handleClientException(ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER);
            }
            operators.add(operation);
            setExpressionNodeAndOperatorLists(node.getLeftNode(), expression, operators, checkFilteringAttribute);
            setExpressionNodeAndOperatorLists(node.getRightNode(), expression, operators, checkFilteringAttribute);
        }
    }

    /**
     * Check whether the filtering can be applied to the attributes.
     *
     * @param attributeValue The attribute value.
     * @return Returns true if the filtering attribute is not the name.
     */
    private static boolean isFilteringAttributeNotSupported(String attributeValue) {

        return !attributeValue.equalsIgnoreCase(ROLE_NAME_FIELD);
    }
}

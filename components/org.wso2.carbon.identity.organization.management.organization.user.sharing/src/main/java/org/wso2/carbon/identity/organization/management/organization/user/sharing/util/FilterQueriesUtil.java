package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.model.FilterQueryBuilder;

import java.util.List;
import java.util.function.Function;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.SP_SHARED_ATTRIBUTE_COLUMN_MAP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.FILTER_PLACEHOLDER_PREFIX;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;

/**
 * FilterQueriesUtil.
 * <p> Utility class for working with filter queries in conjunction with the {@link FilterQueryBuilder}.</p>
 */
public class FilterQueriesUtil {

    /**
     * Get the filter query builder for shared user organizations.
     *
     * @param expressionNodes List of expression nodes.
     * @return FilterQueryBuilder instance containing the filter query and attribute values.
     * @throws OrganizationManagementClientException If an error occurs while building the filter query.
     */
    public static FilterQueryBuilder getSharedUserOrgsFilterQueryBuilder(List<ExpressionNode> expressionNodes)
            throws OrganizationManagementClientException {

        return getFilterQueryBuilder(expressionNodes, SP_SHARED_ATTRIBUTE_COLUMN_MAP::get);
    }

    /**
     * Append the filter query to the query builder.
     *
     * @param expressionNodes       List of expression nodes.
     * @param attributeNameResolver A function that maps attribute values to their corresponding column names.
     * @return FilterQueryBuilder instance containing the filter query and attribute values.
     * @throws OrganizationManagementClientException If an error occurs while appending the filter query.
     */
    public static FilterQueryBuilder getFilterQueryBuilder(List<ExpressionNode> expressionNodes,
                                                           Function<String, String> attributeNameResolver)
            throws OrganizationManagementClientException {

        int count = 1;
        StringBuilder filter = new StringBuilder();
        final FilterQueryBuilder filterQueryBuilder = new FilterQueryBuilder();
        if (CollectionUtils.isEmpty(expressionNodes)) {
            filterQueryBuilder.setFilterQuery(StringUtils.EMPTY);
        } else {
            for (ExpressionNode expressionNode : expressionNodes) {
                String operation = expressionNode.getOperation();
                String filterValue = expressionNode.getValue();
                String requestedAttributeValue = expressionNode.getAttributeValue();
                String attributeName = attributeNameResolver.apply(requestedAttributeValue);

                count = buildFilterBasedOnOperation(filterQueryBuilder, attributeName, requestedAttributeValue,
                        filterValue, operation, count, filter);
            }
            if (StringUtils.isBlank(filter.toString())) {
                filterQueryBuilder.setFilterQuery(StringUtils.EMPTY);
            } else {
                filterQueryBuilder.setFilterQuery(filter.toString());
            }
        }
        return filterQueryBuilder;
    }

    private static int buildFilterBasedOnOperation(FilterQueryBuilder filterQueryBuilder, String attributeName,
                                                   String requestedAttributeValue, String value, String operation,
                                                   int count, StringBuilder filter)
            throws OrganizationManagementClientException {

        if (StringUtils.isNotBlank(attributeName) && StringUtils.isNotBlank(value) && StringUtils
                .isNotBlank(operation)) {
            switch (operation) {
                case OrganizationManagementConstants.EQ: {
                    equalFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                    ++count;
                    break;
                }
                case OrganizationManagementConstants.SW: {
                    startWithFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                    ++count;
                    break;
                }
                case OrganizationManagementConstants.EW: {
                    endWithFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                    ++count;
                    break;
                }
                case OrganizationManagementConstants.CO: {
                    containsFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                    ++count;
                    break;
                }
                case OrganizationManagementConstants.GE: {
                    greaterThanOrEqualFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                    ++count;
                    break;
                }
                case OrganizationManagementConstants.LE: {
                    lessThanOrEqualFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                    ++count;
                    break;
                }
                case OrganizationManagementConstants.GT: {
                    greaterThanFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                    ++count;
                    break;
                }
                case OrganizationManagementConstants.LT: {
                    lessThanFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                    ++count;
                    break;
                }
                default: {
                    throw handleClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE, operation);
                }
            }
        } else {
            throw handleClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE, requestedAttributeValue);
        }
        return count;
    }

    private static void equalFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                           FilterQueryBuilder filterQueryBuilder) {

        String filterString = String.format(" = :%s%s; AND ", FILTER_PLACEHOLDER_PREFIX, count);
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(FILTER_PLACEHOLDER_PREFIX, value);
    }

    private static void startWithFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                               FilterQueryBuilder filterQueryBuilder) {

        String filterString = String.format(" LIKE :%s%s; AND ", FILTER_PLACEHOLDER_PREFIX, count);
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(FILTER_PLACEHOLDER_PREFIX, value + "%");
    }

    private static void endWithFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                             FilterQueryBuilder filterQueryBuilder) {

        String filterString = String.format(" LIKE :%s%s; AND ", FILTER_PLACEHOLDER_PREFIX, count);
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(FILTER_PLACEHOLDER_PREFIX, "%" + value);
    }

    private static void containsFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                              FilterQueryBuilder filterQueryBuilder) {

        String filterString = String.format(" LIKE :%s%s; AND ", FILTER_PLACEHOLDER_PREFIX, count);
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(FILTER_PLACEHOLDER_PREFIX, "%" + value + "%");
    }

    private static void greaterThanOrEqualFilterBuilder(int count, String value, String attributeName,
                                                        StringBuilder filter,
                                                        FilterQueryBuilder filterQueryBuilder) {

        String filterString = String.format(" >= :%s%s; AND ", FILTER_PLACEHOLDER_PREFIX, count);
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(FILTER_PLACEHOLDER_PREFIX, value);
    }

    private static void lessThanOrEqualFilterBuilder(int count, String value, String attributeName,
                                                     StringBuilder filter,
                                                     FilterQueryBuilder filterQueryBuilder) {

        String filterString = String.format(" <= :%s%s; AND ", FILTER_PLACEHOLDER_PREFIX, count);
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(FILTER_PLACEHOLDER_PREFIX, value);
    }

    private static void greaterThanFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                                 FilterQueryBuilder filterQueryBuilder) {

        String filterString = String.format(" > :%s%s; AND ", FILTER_PLACEHOLDER_PREFIX, count);
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(FILTER_PLACEHOLDER_PREFIX, value);
    }

    private static void lessThanFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                              FilterQueryBuilder filterQueryBuilder) {

        String filterString = String.format(" < :%s%s; AND ", FILTER_PLACEHOLDER_PREFIX, count);
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(FILTER_PLACEHOLDER_PREFIX, value);
    }

    private FilterQueriesUtil() {}
}

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

package org.wso2.carbon.identity.organization.management.application.util;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.IS_APP_SHARED;

/**
 * This class provides utility functions for the Organization Application Management.
 */
public class OrgApplicationManagerUtil {

    private static final ThreadLocal<List<String>> b2bApplicationIds = new ThreadLocal<>();

    /**
     * Get a new Jdbc Template.
     *
     * @return a new Jdbc Template.
     */
    public static NamedJdbcTemplate getNewTemplate() {

        return new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
    }

    /**
     * Set property value to service provider indicating if it should be shared with all child organizations.
     *
     * @param serviceProvider The main application.
     * @param value           Property value.
     */
    public static void setShareWithAllChildrenProperty(ServiceProvider serviceProvider, boolean value) {

        Optional<ServiceProviderProperty> shareWithAllChildren = Arrays.stream(serviceProvider.getSpProperties())
                .filter(p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()))
                .findFirst();
        if (shareWithAllChildren.isPresent()) {
            shareWithAllChildren.get().setValue(Boolean.toString(value));
        } else {
            ServiceProviderProperty[] spProperties = serviceProvider.getSpProperties();
            ServiceProviderProperty[] newSpProperties = new ServiceProviderProperty[spProperties.length + 1];
            System.arraycopy(spProperties, 0, newSpProperties, 0, spProperties.length);

            ServiceProviderProperty shareWithAllChildrenProperty = new ServiceProviderProperty();
            shareWithAllChildrenProperty.setName(SHARE_WITH_ALL_CHILDREN);
            shareWithAllChildrenProperty.setValue(Boolean.TRUE.toString());
            newSpProperties[spProperties.length] = shareWithAllChildrenProperty;

            serviceProvider.setSpProperties(newSpProperties);
        }
    }

    /**
     * Set property value to service provider indicating if the app is shared with any child organizations.
     *
     * @param serviceProvider The main application.
     * @param value           The property value.
     */
    public static void setIsAppSharedProperty(ServiceProvider serviceProvider, boolean value) {

        Optional<ServiceProviderProperty> appShared = Arrays.stream(serviceProvider.getSpProperties())
                .filter(p -> IS_APP_SHARED.equals(p.getName()))
                .findFirst();
        if (appShared.isPresent()) {
            appShared.get().setValue(Boolean.toString(value));
        } else {
            ServiceProviderProperty[] spProperties = serviceProvider.getSpProperties();
            ServiceProviderProperty[] newSpProperties = new ServiceProviderProperty[spProperties.length + 1];
            System.arraycopy(spProperties, 0, newSpProperties, 0, spProperties.length);

            ServiceProviderProperty isAppSharedProperty = new ServiceProviderProperty();
            isAppSharedProperty.setName(IS_APP_SHARED);
            isAppSharedProperty.setValue(Boolean.toString(value));
            newSpProperties[spProperties.length] = isAppSharedProperty;

            serviceProvider.setSpProperties(newSpProperties);
        }
    }

    /**
     * Check whether the application is a system application.
     *
     * @param applicationName The name of the application
     * @return True if the provided application is a system application.
     */
    public static boolean isSystemApplication(String applicationName) {

        Set<String> systemApplications = OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService()
                .getSystemApplications();
        return systemApplications != null && systemApplications.stream().anyMatch(applicationName::equalsIgnoreCase);
    }

    /**
     * Retrieve the B2B application IDs.
     *
     * @return B2B application IDs.
     */
    public static List<String> getB2BApplicationIds() {

        if (b2bApplicationIds.get() == null) {
            return null;
        }
        return b2bApplicationIds.get();
    }

    /**
     * Sets the thread local value to persist the B2B application IDs.
     *
     * @param b2BApplicationIds The email verification state to be skipped.
     */
    public static void setB2BApplicationIds(List<String> b2BApplicationIds) {

        OrgApplicationManagerUtil.b2bApplicationIds.set(b2BApplicationIds);
    }

    /**
     * Clear the thread local used to persist the B2B application IDs.
     */
    public static void clearB2BApplicationIds() {

        b2bApplicationIds.remove();
    }
}


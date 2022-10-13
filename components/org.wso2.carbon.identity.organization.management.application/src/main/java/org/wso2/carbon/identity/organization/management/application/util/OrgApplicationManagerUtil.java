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

import java.util.Arrays;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;

/**
 * This class provides utility functions for the Organization Application Management.
 */
public class OrgApplicationManagerUtil {

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
}


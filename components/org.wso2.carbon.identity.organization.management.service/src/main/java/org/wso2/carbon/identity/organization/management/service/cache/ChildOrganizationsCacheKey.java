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

package org.wso2.carbon.identity.organization.management.service.cache;

import org.wso2.carbon.identity.core.cache.CacheKey;

/**
 * Cache key to lookup organization's children from the cache.
 */
public class ChildOrganizationsCacheKey extends CacheKey {

    private final String organizationKey;

    public ChildOrganizationsCacheKey(String organizationKey) {

        this.organizationKey = organizationKey;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ChildOrganizationsCacheKey that = (ChildOrganizationsCacheKey) o;
        return organizationKey.equals(that.organizationKey);
    }

    @Override
    public int hashCode() {

        int result = super.hashCode();
        result = 31 * result + organizationKey.hashCode();
        return result;
    }
}

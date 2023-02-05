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

package org.wso2.carbon.identity.organization.management.application.listener;

import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;

import java.util.List;

/**
 * Organization application management listener.
 */
public interface OrgApplicationManagerListener {

    void preShareApplication(String parentOrganizationId, String parentApplicationId, String sharedOrganizationId,
                             boolean shareWithAllChildren) throws OrganizationManagementException;

    void postShareApplication(String parentOrganizationId, String parentApplicationId, String sharedOrganizationId,
                              String sharedApplicationId, boolean shareWithAllChildren)
            throws OrganizationManagementException;

    void preDeleteSharedApplication(String parentOrganizationId, String parentApplicationId,
                                    String sharedOrganizationId) throws OrganizationManagementException;

    void postDeleteSharedApplication(String parentOrganizationId, String parentApplicationId,
                                     String sharedOrganizationId, String sharedApplicationId)
            throws OrganizationManagementException;

    void preDeleteAllSharedApplications(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException;

    void postDeleteAllSharedApplications(String parentOrganizationId, String parentApplicationId,
                                         List<SharedApplicationDO> sharedApplicationDOList)
            throws OrganizationManagementException;

    void preGetApplicationSharedOrganizations(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException;

    void postGetApplicationSharedOrganizations(String parentOrganizationId, String parentApplicationId,
                                               List<BasicOrganization> sharedOrganizations)
            throws OrganizationManagementException;

    void preGetSharedApplications(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException;

    void postGetSharedApplications(String parentOrganizationId, String parentApplicationId,
                                   List<SharedApplication> sharedApplications) throws OrganizationManagementException;
}

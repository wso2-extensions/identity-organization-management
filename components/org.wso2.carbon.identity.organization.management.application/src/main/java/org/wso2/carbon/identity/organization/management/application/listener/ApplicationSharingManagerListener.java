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

import org.wso2.carbon.identity.organization.management.application.model.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareUpdateOperation;
import org.wso2.carbon.identity.organization.management.service.exception.NotImplementedException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;

import java.util.List;

/**
 * Organization application sharing manager listener.
 */
public interface ApplicationSharingManagerListener {

    /**
     * Use {@link #preShareApplication(String, String, String, ApplicationShareRolePolicy)} instead.
     * Pre listener of sharing an application.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @param sharedOrganizationId Sub-organization id which the application will be shared to.
     * @param shareWithAllChildren Whether the application is shared with all children or not.
     * @throws OrganizationManagementException When error occurred during pre application sharing actions.
     */
    @Deprecated
    void preShareApplication(String parentOrganizationId, String parentApplicationId, String sharedOrganizationId,
                             boolean shareWithAllChildren) throws OrganizationManagementException;


    /**
     * Pre listener of sharing an application with role sharing policy.
     *
     * @param mainOrganizationId         Main application residing organization id.
     * @param mainApplicationId          Main application id.
     * @param sharedOrganizationId       Sub-organization id which the application will be shared to.
     * @param applicationShareRolePolicy Role sharing policy to be applied for the shared application.
     * @throws OrganizationManagementException When error occurred during pre application sharing actions.
     */
    default void preShareApplication(String mainOrganizationId, String mainApplicationId,
                                     String sharedOrganizationId, ApplicationShareRolePolicy applicationShareRolePolicy)
            throws OrganizationManagementException {

        throw new NotImplementedException();
    }

    /**
     * Use@ {@link #postShareApplication(String, String, String, String, ApplicationShareRolePolicy, int)} instead.
     * Post listener of sharing an application.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @param sharedOrganizationId Sub-organization id which the application will be shared to.
     * @param sharedApplicationId  Shared application id.
     * @param shareWithAllChildren Whether the application is shared with all children or not.
     * @throws OrganizationManagementException When error occurred during post application sharing actions.
     */
    @Deprecated
    void postShareApplication(String parentOrganizationId, String parentApplicationId, String sharedOrganizationId,
                              String sharedApplicationId, boolean shareWithAllChildren)
            throws OrganizationManagementException;


    /**
     * Post listener of sharing an application with role sharing policy.
     *
     * @param mainOrganizationId         Main application residing organization id.
     * @param mainApplicationId          Main application id.
     * @param sharedOrganizationId       Sub-organization id which the application will be shared to.
     * @param sharedApplicationId        Shared application id.
     * @param applicationShareRolePolicy Role sharing policy to be applied for the shared application.
     * @param resourceSharingPolicyId    Resource sharing policy id to be applied for the shared application.
     * @throws OrganizationManagementException When error occurred during post application sharing actions.
     */
    default void postShareApplication(String mainOrganizationId, String mainApplicationId, String sharedOrganizationId,
                                      String sharedApplicationId, ApplicationShareRolePolicy applicationShareRolePolicy,
                                      int resourceSharingPolicyId) throws OrganizationManagementException {
        throw new NotImplementedException();
    }


    /**
     * Pre listener of updating roles of a shared application.
     *
     * @param mainOrganizationId   Main application residing organization id.
     * @param mainApplicationId    Main application id.
     * @param sharedOrganizationId Sub-organization id which the shared application belongs to.
     * @param operation            Operation to be performed on the roles of the shared application (ADD/REMOVE).
     * @param roleChanges          List of role changes to be applied on the shared application.
     * @throws OrganizationManagementException When error occurred during pre shared application role update actions.
     */
    default void preUpdateRolesOfSharedApplication(String mainOrganizationId, String mainApplicationId,
                                                   String sharedOrganizationId,
                                                   ApplicationShareUpdateOperation.Operation operation,
                                                   List<RoleWithAudienceDO> roleChanges)
        throws OrganizationManagementException {

        throw new NotImplementedException();
    }

    /**
     * Post listener of updating roles of a shared application.
     *
     * @param mainOrganizationId   Main application residing organization id.
     * @param mainApplicationId    Main application id.
     * @param sharedOrganizationId Sub-organization id which the shared application belongs to.
     * @param sharedApplicationId  Shared application id.
     * @param operation            Operation performed on the roles of the shared application (ADD/REMOVE).
     * @param roleChanges          List of role changes applied on the shared application.
     * @throws OrganizationManagementException When error occurred during post shared application role update actions.
     */
    default void postUpdateRolesOfSharedApplication(String mainOrganizationId, String mainApplicationId,
                                                    String sharedOrganizationId, String sharedApplicationId,
                                                    ApplicationShareUpdateOperation.Operation operation,
                                                    List<RoleWithAudienceDO> roleChanges)
            throws OrganizationManagementException {

        throw new NotImplementedException();
    }

    /**
     * Pre listener of deleting shared application.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @param sharedOrganizationId Sub-organization id which the shared application is deleted from.
     * @throws OrganizationManagementException When error occurred during pre shared application deleting actions.
     */
    void preDeleteSharedApplication(String parentOrganizationId, String parentApplicationId,
                                    String sharedOrganizationId) throws OrganizationManagementException;

    /**
     * Post listener of deleting shared application.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @param sharedOrganizationId Sub-organization id which the shared application is deleted from.
     * @param sharedApplicationId  Deleted shared application id.
     * @throws OrganizationManagementException When error occurred during post shared application deleting actions.
     */
    void postDeleteSharedApplication(String parentOrganizationId, String parentApplicationId,
                                     String sharedOrganizationId, String sharedApplicationId)
            throws OrganizationManagementException;

    /**
     * Pre listener of deleting all shared applications.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @throws OrganizationManagementException When error occurred during pre delete all shared application actions.
     */
    void preDeleteAllSharedApplications(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException;

    /**
     * Post listener of deleting all shared applications.
     *
     * @param parentOrganizationId    Parent application residing organization id.
     * @param parentApplicationId     Parent application id.
     * @param sharedApplicationDOList Deleted shared application app ids and sub-organization ids.
     * @throws OrganizationManagementException When error occurred during post delete all shared application actions.
     */
    void postDeleteAllSharedApplications(String parentOrganizationId, String parentApplicationId,
                                         List<SharedApplicationDO> sharedApplicationDOList)
            throws OrganizationManagementException;

    /**
     * Pre listener of getting shared organizations of an application.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @throws OrganizationManagementException When error occurred during pre actions of
     *                                         getting shared organizations of an application.
     */
    void preGetApplicationSharedOrganizations(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException;

    /**
     * Post listener of getting shared organizations of an application.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @param sharedOrganizations  The application's shared organization list.
     * @throws OrganizationManagementException When error occurred during post actions of
     *                                         getting shared organizations of an application.
     */
    void postGetApplicationSharedOrganizations(String parentOrganizationId, String parentApplicationId,
                                               List<BasicOrganization> sharedOrganizations)
            throws OrganizationManagementException;

    /**
     * Pre listener of get shared applications.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @throws OrganizationManagementException When error occurred during pre actions of getting shared applications.
     */
    void preGetSharedApplications(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException;

    /**
     * Post listener of get shared applications.
     *
     * @param parentOrganizationId Parent application residing organization id.
     * @param parentApplicationId  Parent application id.
     * @param sharedApplications   Shared applications list.
     * @throws OrganizationManagementException When error occurred during post actions of getting shared applications.
     */
    void postGetSharedApplications(String parentOrganizationId, String parentApplicationId,
                                   List<SharedApplication> sharedApplications) throws OrganizationManagementException;
}

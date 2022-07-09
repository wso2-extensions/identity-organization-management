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

package org.wso2.carbon.identity.organization.management.role.management.endpoint;

import org.wso2.carbon.identity.organization.management.role.management.endpoint.*;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.*;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import java.io.InputStream;
import java.util.List;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequest;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutResponse;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolesListResponse;
import javax.ws.rs.core.Response;


public interface OrganizationsApiService {

      public Response createRole(String organizationId, RolePostRequest rolePostRequest);

      public Response organizationsOrganizationIdRolesGet(String organizationId, String filter, Integer count,
                                                          String cursor);

      public Response organizationsOrganizationIdRolesRoleIdDelete(String roleId, String organizationId);

      public Response organizationsOrganizationIdRolesRoleIdGet(String roleId, String organizationId);

      public Response organizationsOrganizationIdRolesRoleIdPatch(String roleId, String organizationId, RolePatchRequest rolePatchRequest);

      public Response organizationsOrganizationIdRolesRoleIdPut(String roleId, String organizationId, RolePutRequest rolePutRequest);
}

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

package org.wso2.carbon.identity.organization.management.service.dao.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.service.cache.ChildOrganizationsCache;
import org.wso2.carbon.identity.organization.management.service.cache.ChildOrganizationsCacheEntry;
import org.wso2.carbon.identity.organization.management.service.cache.ChildOrganizationsCacheKey;
import org.wso2.carbon.identity.organization.management.service.cache.OrganizationByIdCache;
import org.wso2.carbon.identity.organization.management.service.cache.OrganizationByIdCacheKey;
import org.wso2.carbon.identity.organization.management.service.cache.OrganizationByNameCache;
import org.wso2.carbon.identity.organization.management.service.cache.OrganizationByNameCacheKey;
import org.wso2.carbon.identity.organization.management.service.cache.OrganizationCacheEntry;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;

import java.time.Instant;
import java.util.List;

/**
 * Cached DAO layer for the organization management.
 */
public class CachedBackedOrganizationManagementDAO implements OrganizationManagementDAO {

    private static final Log LOG = LogFactory.getLog(CachedBackedOrganizationManagementDAO.class);

    private final OrganizationManagementDAO organizationManagementDAO;
    private final OrganizationByIdCache organizationByIdCache;
    private final OrganizationByNameCache organizationByNameCache;
    private final ChildOrganizationsCache childOrganizationsCache;

    public CachedBackedOrganizationManagementDAO(OrganizationManagementDAO organizationManagementDAO) {

        this.organizationManagementDAO = organizationManagementDAO;
        this.organizationByIdCache = OrganizationByIdCache.getInstance();
        this.organizationByNameCache = OrganizationByNameCache.getInstance();
        this.childOrganizationsCache = ChildOrganizationsCache.getInstance();
    }

    @Override
    public void addOrganization(int tenantId, String tenantDomain, Organization organization)
            throws OrganizationManagementServerException {

        organizationManagementDAO.addOrganization(tenantId, tenantDomain, organization);
        addOrganizationToCache(organization, tenantDomain);
        addOrganizationToParentOrganizationCache(organization, tenantDomain);
    }

    @Override
    public boolean isOrganizationExistByName(String organizationName, String tenantDomain)
            throws OrganizationManagementServerException {

        Organization organization = getOrganizationFromCacheByName(organizationName, tenantDomain);
        if (organization != null) {
            return true;
        }

        return organizationManagementDAO.isOrganizationExistByName(organizationName, tenantDomain);
    }

    @Override
    public boolean isOrganizationExistById(String organizationId, String tenantDomain)
            throws OrganizationManagementServerException {

        Organization organization = getOrganizationFromCacheById(organizationId, tenantDomain);
        if (organization != null) {
            return true;
        }

        return organizationManagementDAO.isOrganizationExistById(organizationId, tenantDomain);
    }

    @Override
    public String getOrganizationIdByName(int tenantId, String organizationName, String tenantDomain)
            throws OrganizationManagementServerException {

        Organization organization = getOrganizationFromCacheByName(organizationName, tenantDomain);
        if (organization != null) {
            return organization.getId();
        }

        return organizationManagementDAO.getOrganizationIdByName(tenantId, organizationName, tenantDomain);
    }

    @Override
    public Organization getOrganization(int tenantId, String organizationId, String tenantDomain)
            throws OrganizationManagementServerException {

        Organization organization = getOrganizationFromCacheById(organizationId, tenantDomain);

        if (organization != null) {
            return organization;
        }
        organization = organizationManagementDAO.getOrganization(tenantId, organizationId, tenantDomain);
        if (organization != null) {
            addOrganizationToCache(organization, tenantDomain);
        }
        return organization;
    }

    @Override
    public List<BasicOrganization> getOrganizations(int tenantId, Integer limit, String tenantDomain, String sortOrder,
                                                    List<ExpressionNode> expressionNodes)
            throws OrganizationManagementServerException {

        return organizationManagementDAO.getOrganizations(tenantId, limit, tenantDomain, sortOrder, expressionNodes);
    }

    @Override
    public void deleteOrganization(int tenantId, String organizationId, String tenantDomain)
            throws OrganizationManagementServerException {

        deleteOrganizationCacheById(organizationId, tenantDomain);
        organizationManagementDAO.deleteOrganization(tenantId, organizationId, tenantDomain);
    }

    @Override
    public boolean hasChildOrganizations(String organizationId, String tenantDomain)
            throws OrganizationManagementServerException {

        return organizationManagementDAO.hasChildOrganizations(organizationId, tenantDomain);
    }

    @Override
    public void patchOrganization(String organizationId, String tenantDomain, Instant lastModifiedInstant,
                                  List<PatchOperation> patchOperations) throws OrganizationManagementServerException {

        deleteOrganizationCacheById(organizationId, tenantDomain);
        organizationManagementDAO.patchOrganization(organizationId, tenantDomain, lastModifiedInstant, patchOperations);
    }

    @Override
    public void updateOrganization(String organizationId, String tenantDomain, Organization organization)
            throws OrganizationManagementServerException {

        deleteOrganizationCacheById(organizationId, tenantDomain);
        organizationManagementDAO.updateOrganization(organizationId, tenantDomain, organization);
    }

    @Override
    public boolean isAttributeExistByKey(String tenantDomain, String organizationId, String attributeKey)
            throws OrganizationManagementServerException {

        return organizationManagementDAO.isAttributeExistByKey(tenantDomain, organizationId, attributeKey);
    }

    @Override
    public List<String> getChildOrganizationIds(int tenantId, String organizationId, String tenantDomain,
                                                Organization organization) throws
            OrganizationManagementServerException {

        List<String> childOrganizationIdsFromCache = getChildOrganizationIdsFromCache(organizationId, tenantDomain);
        if (CollectionUtils.isNotEmpty(childOrganizationIdsFromCache)) {
            return childOrganizationIdsFromCache;
        }

        List<String> childOrganizationIds = organizationManagementDAO.getChildOrganizationIds(tenantId, organizationId,
                tenantDomain, organization);
        if (CollectionUtils.isNotEmpty(childOrganizationIds)) {
            addChildOrganizationsToCache(organization, childOrganizationIds, tenantDomain);
        }
        return childOrganizationIds;
    }

    @Override
    public boolean hasActiveChildOrganizations(String organizationId) throws OrganizationManagementServerException {

        return organizationManagementDAO.hasActiveChildOrganizations(organizationId);
    }

    @Override
    public boolean isParentOrganizationDisabled(String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        Organization organization = getOrganizationFromCacheById(organizationId, tenantDomain);
        if (organization != null) {
            ParentOrganizationDO parent = organization.getParent();
            if (parent != null) {
                String parentId = parent.getId();
                if (StringUtils.isNotBlank(parentId)) {
                    Organization parentOrganization = getOrganizationFromCacheById(parentId, tenantDomain);
                    if (parentOrganization != null) {
                        return StringUtils.equals(parentOrganization.getStatus(),
                                OrganizationManagementConstants.OrganizationStatus.DISABLED.toString());
                    }
                }
            }
        }
        return organizationManagementDAO.isParentOrganizationDisabled(organizationId, tenantDomain);
    }

    @Override
    public String getOrganizationStatus(String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        Organization organization = getOrganizationFromCacheById(organizationId, tenantDomain);
        if (organization != null) {
            return organization.getStatus();
        }
        return organizationManagementDAO.getOrganizationStatus(organizationId, tenantDomain);
    }

    private Organization getOrganizationFromCacheById(String organizationId, String tenantDomain) {

        OrganizationByIdCacheKey cacheKey = new OrganizationByIdCacheKey(organizationId);
        OrganizationCacheEntry cacheEntry = organizationByIdCache.getValueFromCache(cacheKey, tenantDomain);

        if (cacheEntry != null && cacheEntry.getOrganization() != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Cache hit for organization by its name. Organization id: %s, " +
                        "Tenant domain: %s", organizationId, tenantDomain));
            }
            return cacheEntry.getOrganization();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Cache miss for organization by its name. Organization id: %s, " +
                    "Tenant domain: %s", organizationId, tenantDomain));
        }
        return null;
    }

    private Organization getOrganizationFromCacheByName(String organizationName, String tenantDomain) {

        OrganizationByNameCacheKey cacheKey = new OrganizationByNameCacheKey(organizationName);
        OrganizationCacheEntry cacheEntry = organizationByNameCache.getValueFromCache(cacheKey, tenantDomain);

        if (cacheEntry != null && cacheEntry.getOrganization() != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Cache hit for organization by its name. Organization name: %s, " +
                        "Tenant domain: %s", organizationName, tenantDomain));
            }
            return cacheEntry.getOrganization();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Cache miss for organization by its name. Organization name: %s, " +
                    "Tenant domain: %s", organizationName, tenantDomain));
        }
        return null;
    }

    private List<String> getChildOrganizationIdsFromCache(String organizationId, String tenantDomain) {

        ChildOrganizationsCacheKey cacheKey = new ChildOrganizationsCacheKey(organizationId);
        ChildOrganizationsCacheEntry cacheEntry = childOrganizationsCache.getValueFromCache(cacheKey, tenantDomain);

        if (cacheEntry != null && CollectionUtils.isNotEmpty(cacheEntry.getChildOrganizations())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Cache hit for fetch child organizations. Organization id: %s, " +
                        "Tenant domain: %s", organizationId, tenantDomain));
            }
            return cacheEntry.getChildOrganizations();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Cache miss for fetch child organizations. Organization id: %s, " +
                    "Tenant domain: %s", organizationId, tenantDomain));
        }
        return null;
    }

    private void addOrganizationToCache(Organization organization, String tenantDomain) {

        OrganizationByIdCacheKey organizationByIdCacheKey = new OrganizationByIdCacheKey(organization.getId());
        OrganizationByNameCacheKey organizationByNameCacheKey = new OrganizationByNameCacheKey(organization.getName());
        OrganizationCacheEntry organizationCacheEntry = new OrganizationCacheEntry(organization);

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Adding cache for the organization: %s in tenant domain: %s",
                    organization.getName(), tenantDomain));
        }

        organizationByIdCache.addToCache(organizationByIdCacheKey, organizationCacheEntry, tenantDomain);
        organizationByNameCache.addToCache(organizationByNameCacheKey, organizationCacheEntry, tenantDomain);
    }

    private void addChildOrganizationsToCache(Organization organization, List<String> childOrganizationIds,
                                              String tenantDomain) {

        ChildOrganizationsCacheKey organizationByIdCacheKey = new ChildOrganizationsCacheKey(organization.getId());
        ChildOrganizationsCacheEntry entry = new ChildOrganizationsCacheEntry(childOrganizationIds);

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Adding cache for the child organizations of organization: %s in tenant domain: %s",
                    organization.getName(), tenantDomain));
        }

        childOrganizationsCache.addToCache(organizationByIdCacheKey, entry, tenantDomain);
    }

    private void addOrganizationToParentOrganizationCache(Organization organization, String tenantDomain) {

        ParentOrganizationDO parentOrganization = organization.getParent();
        if (parentOrganization != null) {
            String parentOrganizationId = parentOrganization.getId();
            if (StringUtils.isNotBlank(parentOrganizationId)) {
                ChildOrganizationsCacheKey childOrganizationsCacheKey = new ChildOrganizationsCacheKey
                        (parentOrganizationId); // for root organization.getParent().getId() is null

                ChildOrganizationsCacheEntry entry = childOrganizationsCache.getValueFromCache
                        (childOrganizationsCacheKey, tenantDomain);
                if (entry != null) {
                    List<String> childOrganizations = entry.getChildOrganizations();
                    if (CollectionUtils.isNotEmpty(childOrganizations)) {
                        childOrganizations.add(organization.getId());
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Updating cache of parent organization: %s in tenant domain: %s to " +
                                        "add child organization: %s", parentOrganizationId, tenantDomain,
                                organization.getName()));
                    }
                    childOrganizationsCache.clearCacheEntry(childOrganizationsCacheKey, tenantDomain);
                    entry = new ChildOrganizationsCacheEntry(childOrganizations);
                    childOrganizationsCache.addToCache(childOrganizationsCacheKey, entry, tenantDomain);
                }
            }
        }
    }

    private void deleteOrganizationCacheById(String organizationId, String tenantDomain) {

        Organization organization = getOrganizationFromCacheById(organizationId, tenantDomain);
        if (organization == null) {
            return;
        }
        deleteOrganizationFromCache(organization, tenantDomain);
    }

    private void deleteOrganizationFromCache(Organization organization, String tenantDomain) {

        OrganizationByIdCacheKey organizationByIdCacheKey = new OrganizationByIdCacheKey(organization.getId());
        OrganizationByNameCacheKey organizationByNameCacheKey = new OrganizationByNameCacheKey(organization.getName());

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Clearing cache entries of organization with id: %s, name: %s in" +
                    " tenant domain: %s", organization.getId(), organization.getName(), tenantDomain));
        }

        organizationByIdCache.clearCacheEntry(organizationByIdCacheKey, tenantDomain);
        organizationByNameCache.clearCacheEntry(organizationByNameCacheKey, tenantDomain);
    }
}

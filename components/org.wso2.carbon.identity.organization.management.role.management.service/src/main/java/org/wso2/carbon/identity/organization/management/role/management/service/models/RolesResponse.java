package org.wso2.carbon.identity.organization.management.role.management.service.models;

import java.util.List;

/**
 * This class represents all the information related to fetch roles response.
 */
public class RolesResponse {

    private String nextCursor;
    private int totalResults;
    private String previousCursor;
    private int itemsPerPage;
    private List<Role> roles;

    public RolesResponse(String nextCursor, int totalResults, String previousCursor, int itemsPerPage,
                         List<Role> roles) {

        this.nextCursor = nextCursor;
        this.totalResults = totalResults;
        this.previousCursor = previousCursor;
        this.itemsPerPage = itemsPerPage;
        this.roles = roles;
    }

    public String getNextCursor() {

        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {

        this.nextCursor = nextCursor;
    }

    public int getTotalResults() {

        return totalResults;
    }

    public void setTotalResults(int totalResults) {

        this.totalResults = totalResults;
    }

    public String getPreviousCursor() {

        return previousCursor;
    }

    public void setPreviousCursor(String previousCursor) {

        this.previousCursor = previousCursor;
    }

    public int getItemsPerPage() {

        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {

        this.itemsPerPage = itemsPerPage;
    }

    public List<Role> getRoles() {

        return roles;
    }

    public void setRoles(
            List<Role> roles) {

        this.roles = roles;
    }
}

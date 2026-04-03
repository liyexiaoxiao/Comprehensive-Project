package com.donffroodus.user_service.dto;

public class AdminGetUsersRequest {
    private int offset;
    private int limit;

    // Getters and Setters
    public int getOffset() {
        return offset;
    }
    public void setOffset(int offset) {
        this.offset = offset;
    }
    public int getLimit() {
        return limit;
    }
    public void setLimit(int limit) {
        this.limit = limit;
    }
}

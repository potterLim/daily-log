package com.potterlim.daymark.entity;

public enum EUserRole {
    ADMIN,
    USER;

    public String getAuthorityName() {
        return "ROLE_" + name();
    }
}

package com.potterlim.daymark.entity;

public enum EUserRole {
    USER;

    public String getAuthorityName() {
        return "ROLE_" + name();
    }
}

package com.potterlim.daylog.entity;

public enum UserRole {
    USER;

    public String getAuthorityName() {
        return "ROLE_" + name();
    }
}

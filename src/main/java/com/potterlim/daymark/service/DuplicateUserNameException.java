package com.potterlim.daymark.service;

public class DuplicateUserNameException extends RuntimeException {

    public DuplicateUserNameException(String userName) {
        super("Duplicate user name: " + userName);
    }
}

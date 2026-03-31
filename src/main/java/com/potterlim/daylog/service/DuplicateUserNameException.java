package com.potterlim.daylog.service;

public class DuplicateUserNameException extends RuntimeException {

    public DuplicateUserNameException(String userName) {
        super("Duplicate user name: " + userName);
    }
}

package com.potterlim.daylog.service;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String emailAddress) {
        super("User account already exists with email address: " + emailAddress);
    }
}

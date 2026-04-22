package com.potterlim.daylog.service;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Current password is invalid.");
    }
}

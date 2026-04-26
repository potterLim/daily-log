package com.potterlim.daymark.service;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Current password is invalid.");
    }
}

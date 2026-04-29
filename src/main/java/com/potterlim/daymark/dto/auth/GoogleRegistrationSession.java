package com.potterlim.daymark.dto.auth;

import java.io.Serializable;

public record GoogleRegistrationSession(
    String googleSubject,
    String emailAddress,
    String displayName
) implements Serializable {

    public static final String SESSION_ATTRIBUTE_NAME = GoogleRegistrationSession.class.getName();

    public GoogleRegistrationSession {
        if (googleSubject == null || googleSubject.isBlank()) {
            throw new IllegalArgumentException("googleSubject must not be blank.");
        }

        if (emailAddress == null || emailAddress.isBlank()) {
            throw new IllegalArgumentException("emailAddress must not be blank.");
        }
    }
}

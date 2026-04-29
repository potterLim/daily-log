package com.potterlim.daymark.dto.auth;

public final class RegisterGoogleUserAccountCommand {

    private final String mUserName;
    private final String mEmailAddress;
    private final String mGoogleSubject;
    private final String mRawPassword;

    public RegisterGoogleUserAccountCommand(
        String userName,
        String emailAddress,
        String googleSubject,
        String rawPassword
    ) {
        mUserName = userName;
        mEmailAddress = emailAddress;
        mGoogleSubject = googleSubject;
        mRawPassword = rawPassword;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getEmailAddress() {
        return mEmailAddress;
    }

    public String getGoogleSubject() {
        return mGoogleSubject;
    }

    public String getRawPassword() {
        return mRawPassword;
    }
}

package com.potterlim.daylog.dto.auth;

public final class RegisterUserAccountCommand {

    private final String mUserName;
    private final String mEmailAddress;
    private final String mRawPassword;

    public RegisterUserAccountCommand(String userName, String emailAddress, String rawPassword) {
        mUserName = userName;
        mEmailAddress = emailAddress;
        mRawPassword = rawPassword;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getEmailAddress() {
        return mEmailAddress;
    }

    public String getRawPassword() {
        return mRawPassword;
    }
}

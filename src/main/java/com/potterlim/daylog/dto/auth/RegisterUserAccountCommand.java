package com.potterlim.daylog.dto.auth;

public final class RegisterUserAccountCommand {

    private final String mUserName;
    private final String mRawPassword;

    public RegisterUserAccountCommand(String userName, String rawPassword) {
        mUserName = userName;
        mRawPassword = rawPassword;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getRawPassword() {
        return mRawPassword;
    }
}

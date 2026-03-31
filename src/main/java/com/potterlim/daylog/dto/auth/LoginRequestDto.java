package com.potterlim.daylog.dto.auth;

import jakarta.validation.constraints.NotBlank;

public final class LoginRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String mUserName = "";

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String mPassword = "";

    private boolean mRememberMe = false;

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public boolean isRememberMe() {
        return mRememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        mRememberMe = rememberMe;
    }
}

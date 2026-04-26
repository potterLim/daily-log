package com.potterlim.daymark.dto.auth;

import jakarta.validation.constraints.NotBlank;

public final class LoginRequestDto {

    @NotBlank(message = "아이디 또는 이메일을 입력해주세요.")
    private String mLoginIdentifier = "";

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String mPassword = "";

    private boolean mIsRememberMe;

    public String getLoginIdentifier() {
        return mLoginIdentifier;
    }

    public void setLoginIdentifier(String loginIdentifier) {
        mLoginIdentifier = loginIdentifier;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public boolean isRememberMe() {
        return mIsRememberMe;
    }

    public void setRememberMe(boolean isRememberMe) {
        mIsRememberMe = isRememberMe;
    }
}

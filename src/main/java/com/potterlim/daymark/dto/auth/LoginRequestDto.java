package com.potterlim.daymark.dto.auth;

import jakarta.validation.constraints.NotBlank;

public final class LoginRequestDto {

    private String mLoginIdentifier = "";

    private String mPassword = "";

    private String mNextPath = "";

    private boolean mIsRememberMe;

    @NotBlank(message = "워크스페이스 ID 또는 이메일을 입력해주세요.")
    public String getLoginIdentifier() {
        return mLoginIdentifier;
    }

    public void setLoginIdentifier(String loginIdentifier) {
        mLoginIdentifier = loginIdentifier;
    }

    @NotBlank(message = "비밀번호를 입력해주세요.")
    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getNextPath() {
        return mNextPath;
    }

    public void setNextPath(String nextPath) {
        mNextPath = nextPath;
    }

    public boolean isRememberMe() {
        return mIsRememberMe;
    }

    public void setRememberMe(boolean isRememberMe) {
        mIsRememberMe = isRememberMe;
    }
}

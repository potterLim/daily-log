package com.potterlim.daymark.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class RegisterRequestDto {

    private static final int USER_NAME_MAX_LENGTH = 100;

    private String mUserName = "";

    private String mPassword = "";

    private String mConfirmPassword = "";

    @NotBlank(message = "워크스페이스 ID를 입력해주세요.")
    @Size(max = USER_NAME_MAX_LENGTH, message = "워크스페이스 ID는 100자 이하로 입력해주세요.")
    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    public String getConfirmPassword() {
        return mConfirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        mConfirmPassword = confirmPassword;
    }

    public boolean hasMatchingPassword() {
        return mPassword != null && mPassword.equals(mConfirmPassword);
    }
}

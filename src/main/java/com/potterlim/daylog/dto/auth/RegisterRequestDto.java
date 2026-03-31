package com.potterlim.daylog.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class RegisterRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String mUserName = "";

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 4, message = "비밀번호는 최소 4자 이상이어야 합니다.")
    private String mPassword = "";

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String mConfirmPassword = "";

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

    public String getConfirmPassword() {
        return mConfirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        mConfirmPassword = confirmPassword;
    }

    public boolean hasSamePassword() {
        return mPassword != null && mPassword.equals(mConfirmPassword);
    }
}

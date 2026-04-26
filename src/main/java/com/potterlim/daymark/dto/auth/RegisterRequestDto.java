package com.potterlim.daymark.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class RegisterRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String mUserName = "";

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식을 입력해주세요.")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
    private String mEmailAddress = "";

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
    private String mPassword = "";

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String mConfirmPassword = "";

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public String getEmailAddress() {
        return mEmailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        mEmailAddress = emailAddress;
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

    public boolean hasMatchingPassword() {
        return mPassword != null && mPassword.equals(mConfirmPassword);
    }
}

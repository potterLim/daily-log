package com.potterlim.daymark.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ChangePasswordRequestDto {

    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String mCurrentPassword = "";

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
    private String mNewPassword = "";

    @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
    private String mConfirmNewPassword = "";

    public String getCurrentPassword() {
        return mCurrentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        mCurrentPassword = currentPassword;
    }

    public String getNewPassword() {
        return mNewPassword;
    }

    public void setNewPassword(String newPassword) {
        mNewPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return mConfirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        mConfirmNewPassword = confirmNewPassword;
    }

    public boolean hasMatchingNewPassword() {
        return mNewPassword != null && mNewPassword.equals(mConfirmNewPassword);
    }
}

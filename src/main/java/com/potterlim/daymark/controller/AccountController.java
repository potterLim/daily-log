package com.potterlim.daymark.controller;

import com.potterlim.daymark.dto.auth.ChangePasswordRequestDto;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.service.IUserAccountService;
import com.potterlim.daymark.service.InvalidCurrentPasswordException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final IUserAccountService mUserAccountService;

    public AccountController(IUserAccountService userAccountService) {
        mUserAccountService = userAccountService;
    }

    @GetMapping("/account")
    public String showAccountPage(@AuthenticationPrincipal UserAccount userAccount, Model model) {
        UserAccount currentUserAccount = findCurrentUserAccount(userAccount);

        model.addAttribute("accountWorkspaceId", currentUserAccount.getUsername());
        model.addAttribute("accountEmailAddress", currentUserAccount.getEmailAddress());
        model.addAttribute("accountCreatedAt", currentUserAccount.getCreatedAt());
        return "account/index";
    }

    @GetMapping("/account/password")
    public String showPasswordChangePage(Model model) {
        if (!model.containsAttribute("changePasswordRequestDto")) {
            model.addAttribute("changePasswordRequestDto", new ChangePasswordRequestDto());
        }

        return "account/password";
    }

    @PostMapping("/account/password")
    public String changePassword(
        @AuthenticationPrincipal UserAccount userAccount,
        @Valid @ModelAttribute("changePasswordRequestDto") ChangePasswordRequestDto changePasswordRequestDto,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (!changePasswordRequestDto.hasMatchingNewPassword()) {
            bindingResult.rejectValue("confirmNewPassword", "account.confirmNewPassword", "새 비밀번호 확인이 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "account/password";
        }

        try {
            mUserAccountService.changePassword(
                userAccount.getUserAccountId(),
                changePasswordRequestDto.getCurrentPassword(),
                changePasswordRequestDto.getNewPassword()
            );
        } catch (InvalidCurrentPasswordException invalidCurrentPasswordException) {
            bindingResult.rejectValue("currentPassword", "account.currentPassword", "현재 비밀번호가 올바르지 않습니다.");
            return "account/password";
        }

        redirectAttributes.addFlashAttribute("passwordChangeSuccessMessage", "비밀번호가 변경되었습니다.");
        return "redirect:/account/password";
    }

    private UserAccount findCurrentUserAccount(UserAccount userAccount) {
        return mUserAccountService.findUserAccountByLoginIdentifier(userAccount.getUsername())
            .orElseThrow(() -> new IllegalStateException("Authenticated user account not found."));
    }
}

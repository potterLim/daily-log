package com.potterlim.daymark.controller;

import com.potterlim.daymark.dto.auth.ChangePasswordRequestDto;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.service.AuthenticationMailWorkflowService;
import com.potterlim.daymark.service.IUserAccountService;
import com.potterlim.daymark.service.InvalidCurrentPasswordException;
import com.potterlim.daymark.service.OperationUsageEventService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuthenticationMailWorkflowService mAuthenticationMailWorkflowService;
    private final OperationUsageEventService mOperationUsageEventService;

    public AccountController(
        IUserAccountService userAccountService,
        AuthenticationMailWorkflowService authenticationMailWorkflowService,
        OperationUsageEventService operationUsageEventService
    ) {
        mUserAccountService = userAccountService;
        mAuthenticationMailWorkflowService = authenticationMailWorkflowService;
        mOperationUsageEventService = operationUsageEventService;
    }

    @GetMapping("/account")
    public String showAccountPage(@AuthenticationPrincipal UserAccount userAccount, Model model) {
        UserAccount currentUserAccount = findCurrentUserAccount(userAccount);

        model.addAttribute("accountWorkspaceId", currentUserAccount.getUsername());
        model.addAttribute("accountEmailAddress", currentUserAccount.getEmailAddress());
        model.addAttribute("isAccountEmailVerified", currentUserAccount.hasVerifiedEmailAddress());
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

    @PostMapping("/account/email-verification/resend")
    public String resendEmailVerificationMail(
        @AuthenticationPrincipal UserAccount userAccount,
        HttpServletRequest httpServletRequest,
        RedirectAttributes redirectAttributes
    ) {
        UserAccount currentUserAccount = findCurrentUserAccount(userAccount);

        if (currentUserAccount.hasVerifiedEmailAddress()) {
            redirectAttributes.addFlashAttribute(
                "emailVerificationSuccessMessage",
                "이미 이메일 소유 확인이 완료된 계정입니다."
            );
            return "redirect:/";
        }

        boolean wasVerificationMailSent =
            mAuthenticationMailWorkflowService.sendEmailVerificationInstructions(currentUserAccount, httpServletRequest);
        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.EMAIL_VERIFICATION_RESEND_REQUESTED,
            currentUserAccount.getUserAccountId()
        );
        if (wasVerificationMailSent) {
            redirectAttributes.addFlashAttribute(
                "emailVerificationSuccessMessage",
                "이메일 인증 링크를 다시 보냈습니다. 받은 편지함과 스팸함을 함께 확인해 주세요."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                "emailVerificationWarningMessage",
                "인증 메일 전송에 문제가 있어 잠시 후 다시 시도해 주세요."
            );
        }

        return "redirect:/";
    }

    private UserAccount findCurrentUserAccount(UserAccount userAccount) {
        return mUserAccountService.findUserAccountByLoginIdentifier(userAccount.getUsername())
            .orElseThrow(() -> new IllegalStateException("Authenticated user account not found."));
    }
}

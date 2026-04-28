package com.potterlim.daymark.controller;

import java.net.URI;
import java.util.Optional;
import com.potterlim.daymark.dto.auth.ForgotPasswordRequestDto;
import com.potterlim.daymark.dto.auth.LoginRequestDto;
import com.potterlim.daymark.dto.auth.RegisterRequestDto;
import com.potterlim.daymark.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daymark.dto.auth.ResetPasswordRequestDto;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.entity.UserAccountId;
import com.potterlim.daymark.service.AuthenticationMailWorkflowService;
import com.potterlim.daymark.service.DuplicateEmailException;
import com.potterlim.daymark.service.DuplicateUserNameException;
import com.potterlim.daymark.service.IEmailVerificationTokenService;
import com.potterlim.daymark.service.IPasswordResetTokenService;
import com.potterlim.daymark.service.IUserAccountService;
import com.potterlim.daymark.service.OperationUsageEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final IUserAccountService mUserAccountService;
    private final IPasswordResetTokenService mPasswordResetTokenService;
    private final IEmailVerificationTokenService mEmailVerificationTokenService;
    private final AuthenticationMailWorkflowService mAuthenticationMailWorkflowService;
    private final AuthenticationManager mAuthenticationManager;
    private final SecurityContextRepository mSecurityContextRepository;
    private final RememberMeServices mRememberMeServices;
    private final RequestCache mRequestCache;
    private final OperationUsageEventService mOperationUsageEventService;

    public AuthController(
        IUserAccountService userAccountService,
        IPasswordResetTokenService passwordResetTokenService,
        IEmailVerificationTokenService emailVerificationTokenService,
        AuthenticationMailWorkflowService authenticationMailWorkflowService,
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        RememberMeServices rememberMeServices,
        RequestCache requestCache,
        OperationUsageEventService operationUsageEventService
    ) {
        mUserAccountService = userAccountService;
        mPasswordResetTokenService = passwordResetTokenService;
        mEmailVerificationTokenService = emailVerificationTokenService;
        mAuthenticationMailWorkflowService = authenticationMailWorkflowService;
        mAuthenticationManager = authenticationManager;
        mSecurityContextRepository = securityContextRepository;
        mRememberMeServices = rememberMeServices;
        mRequestCache = requestCache;
        mOperationUsageEventService = operationUsageEventService;
    }

    @GetMapping("/login")
    public String showLoginPage(Authentication authentication, HttpServletRequest httpServletRequest, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }

        if (httpServletRequest.getQueryString() != null) {
            return "redirect:/login";
        }

        if (!model.containsAttribute("loginRequestDto")) {
            model.addAttribute("loginRequestDto", new LoginRequestDto());
        }

        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
        @Valid @ModelAttribute("loginRequestDto") LoginRequestDto loginRequestDto,
        BindingResult bindingResult,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        String normalizedLoginIdentifier = loginRequestDto.getLoginIdentifier().trim();
        Authentication authentication;

        try {
            if (loginRequestDto.isRememberMe()) {
                authentication = authenticateUserWithRememberMe(
                    normalizedLoginIdentifier,
                    loginRequestDto.getPassword(),
                    httpServletRequest,
                    httpServletResponse
                );
            } else {
                authentication = authenticateUser(
                    normalizedLoginIdentifier,
                    loginRequestDto.getPassword(),
                    httpServletRequest,
                    httpServletResponse
                );
            }
        } catch (AuthenticationException authenticationException) {
            mRememberMeServices.loginFail(httpServletRequest, httpServletResponse);
            recordFailedSignInEvent(normalizedLoginIdentifier);
            model.addAttribute("loginErrorMessage", "로그인 정보가 올바르지 않습니다.");
            return "auth/login";
        }

        recordAuthenticatedUserEvent(EOperationEventType.SIGN_IN_SUCCEEDED, authentication);
        return "redirect:" + resolveSavedRequestRedirectPath(httpServletRequest, httpServletResponse);
    }

    @GetMapping("/register")
    public String showRegisterPage(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }

        if (!model.containsAttribute("registerRequestDto")) {
            model.addAttribute("registerRequestDto", new RegisterRequestDto());
        }

        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registerRequestDto") RegisterRequestDto registerRequestDto,
        BindingResult bindingResult,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        RedirectAttributes redirectAttributes
    ) {
        if (!registerRequestDto.hasMatchingPassword()) {
            bindingResult.rejectValue(
                "confirmPassword",
                "register.confirmPassword",
                "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        RegisterUserAccountCommand registerUserAccountCommand =
            new RegisterUserAccountCommand(
                registerRequestDto.getUserName().trim(),
                registerRequestDto.getEmailAddress().trim(),
                registerRequestDto.getPassword()
            );

        UserAccount userAccount;

        try {
            userAccount = mUserAccountService.registerUserAccount(registerUserAccountCommand);
        } catch (DuplicateUserNameException duplicateUserNameException) {
            bindingResult.rejectValue(
                "userName",
                "register.userName",
                "이미 사용 중인 워크스페이스 ID입니다."
            );
            return "auth/register";
        } catch (DuplicateEmailException duplicateEmailException) {
            bindingResult.rejectValue(
                "emailAddress",
                "register.emailAddress",
                "이미 사용 중인 이메일입니다."
            );
            return "auth/register";
        }

        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.USER_REGISTERED,
            userAccount.getUserAccountId()
        );
        boolean wasVerificationMailSent =
            mAuthenticationMailWorkflowService.sendEmailVerificationInstructions(userAccount, httpServletRequest);

        authenticateUser(
            registerRequestDto.getUserName().trim(),
            registerRequestDto.getPassword(),
            httpServletRequest,
            httpServletResponse
        );

        if (wasVerificationMailSent) {
            redirectAttributes.addFlashAttribute(
                "emailVerificationSuccessMessage",
                "가입이 완료되었습니다. 인증 메일을 확인해 주세요."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                "emailVerificationWarningMessage",
                "가입은 완료되었습니다. 인증 메일은 계정에서 다시 요청해 주세요."
            );
        }

        return "redirect:/";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordRequestDto")) {
            model.addAttribute("forgotPasswordRequestDto", new ForgotPasswordRequestDto());
        }

        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(
        @Valid @ModelAttribute("forgotPasswordRequestDto") ForgotPasswordRequestDto forgotPasswordRequestDto,
        BindingResult bindingResult,
        HttpServletRequest httpServletRequest,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        Optional<UserAccount> userAccountOrEmpty =
            mUserAccountService.findUserAccountByEmailAddress(forgotPasswordRequestDto.getEmailAddress());
        userAccountOrEmpty.ifPresentOrElse(
            userAccount -> {
                mOperationUsageEventService.recordUserEvent(
                    EOperationEventType.PASSWORD_RESET_REQUESTED,
                    userAccount.getUserAccountId()
                );
                mAuthenticationMailWorkflowService.sendRecoveryInstructions(userAccount, httpServletRequest);
            },
            () -> mOperationUsageEventService.recordAnonymousEvent(EOperationEventType.PASSWORD_RESET_REQUESTED)
        );

        redirectAttributes.addFlashAttribute(
            "forgotPasswordSuccessMessage",
            "안내 메일을 전송했습니다. 메일함을 확인해 주세요."
        );
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(
        @RequestParam(name = "token", required = false) String tokenOrNull,
        Model model
    ) {
        boolean isPasswordResetTokenValid = mPasswordResetTokenService.isPasswordResetTokenValid(tokenOrNull);

        if (!model.containsAttribute("resetPasswordRequestDto")) {
            ResetPasswordRequestDto resetPasswordRequestDto = new ResetPasswordRequestDto();
            if (tokenOrNull != null) {
                resetPasswordRequestDto.setToken(tokenOrNull);
            }
            model.addAttribute("resetPasswordRequestDto", resetPasswordRequestDto);
        }

        model.addAttribute("isPasswordResetTokenValid", isPasswordResetTokenValid);
        if (!isPasswordResetTokenValid) {
            model.addAttribute(
                "passwordResetErrorMessage",
                "재설정 링크가 유효하지 않거나 이미 만료되었습니다."
            );
        }

        return "auth/reset-password";
    }

    @GetMapping("/verify-email")
    public String verifyEmailAddress(
        @RequestParam(name = "token", required = false) String tokenOrNull,
        Authentication authenticationOrNull,
        RedirectAttributes redirectAttributes
    ) {
        Optional<UserAccountId> verifiedUserAccountIdOrEmpty =
            mEmailVerificationTokenService.verifyEmailAddress(tokenOrNull);
        if (verifiedUserAccountIdOrEmpty.isPresent()) {
            mOperationUsageEventService.recordUserEvent(
                EOperationEventType.EMAIL_VERIFIED,
                verifiedUserAccountIdOrEmpty.get()
            );
            redirectAttributes.addFlashAttribute(
                "emailVerificationSuccessMessage",
                "이메일 확인이 완료되었습니다. 복구 메일을 받을 수 있습니다."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                "emailVerificationErrorMessage",
                "이메일 인증 링크가 유효하지 않거나 이미 만료되었습니다."
            );
        }

        return isAuthenticated(authenticationOrNull) ? "redirect:/" : "redirect:/login";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
        @Valid @ModelAttribute("resetPasswordRequestDto") ResetPasswordRequestDto resetPasswordRequestDto,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (!resetPasswordRequestDto.hasMatchingPassword()) {
            bindingResult.rejectValue(
                "confirmPassword",
                "reset.confirmPassword",
                "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                "isPasswordResetTokenValid",
                mPasswordResetTokenService.isPasswordResetTokenValid(resetPasswordRequestDto.getToken())
            );
            return "auth/reset-password";
        }

        UserAccountId userAccountIdOrNull =
            mPasswordResetTokenService.consumePasswordResetToken(resetPasswordRequestDto.getToken()).orElse(null);
        if (userAccountIdOrNull == null) {
            model.addAttribute("isPasswordResetTokenValid", false);
            model.addAttribute(
                "passwordResetErrorMessage",
                "재설정 링크가 유효하지 않거나 이미 만료되었습니다."
            );
            return "auth/reset-password";
        }

        mUserAccountService.resetPassword(userAccountIdOrNull, resetPasswordRequestDto.getPassword());
        mOperationUsageEventService.recordUserEvent(EOperationEventType.PASSWORD_RESET_COMPLETED, userAccountIdOrNull);
        redirectAttributes.addFlashAttribute(
            "passwordResetSuccessMessage",
            "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요."
        );
        return "redirect:/login";
    }

    private static boolean isAuthenticated(Authentication authenticationOrNull) {
        return authenticationOrNull != null
            && authenticationOrNull.isAuthenticated()
            && !(authenticationOrNull instanceof AnonymousAuthenticationToken);
    }

    private Authentication authenticateUserWithRememberMe(
        String userName,
        String rawPassword,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) {
        Authentication authentication = authenticateUser(
            userName,
            rawPassword,
            httpServletRequest,
            httpServletResponse
        );

        mRememberMeServices.loginSuccess(httpServletRequest, httpServletResponse, authentication);
        return authentication;
    }

    /**
     * Saves the authenticated principal into the Spring Security context and the HTTP session.
     *
     * <p>Preconditions: the given login identifier must already exist and the raw password must be the
     * password entered by the user.</p>
     *
     * @return The authenticated principal saved into the current request context.
     */
    private Authentication authenticateUser(
        String userName,
        String rawPassword,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) {
        UsernamePasswordAuthenticationToken authenticationRequest =
            UsernamePasswordAuthenticationToken.unauthenticated(userName, rawPassword);

        Authentication authentication = mAuthenticationManager.authenticate(authenticationRequest);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        mSecurityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);
        return authentication;
    }

    private String resolveSavedRequestRedirectPath(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) {
        SavedRequest savedRequestOrNull = mRequestCache.getRequest(httpServletRequest, httpServletResponse);
        if (savedRequestOrNull == null) {
            return "/";
        }

        mRequestCache.removeRequest(httpServletRequest, httpServletResponse);
        String redirectUrl = savedRequestOrNull.getRedirectUrl();
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return "/";
        }

        try {
            URI redirectUri = URI.create(redirectUrl);
            String redirectPath = redirectUri.getRawPath();
            String redirectQuery = removeInternalSavedRequestQueryParameter(redirectUri.getRawQuery());
            if (!isAllowedPostLoginRedirectPath(redirectPath)) {
                return "/";
            }

            return redirectQuery == null || redirectQuery.isBlank()
                ? redirectPath
                : redirectPath + "?" + redirectQuery;
        } catch (IllegalArgumentException illegalArgumentException) {
            return "/";
        }
    }

    private static String removeInternalSavedRequestQueryParameter(String rawQueryOrNull) {
        if (rawQueryOrNull == null || rawQueryOrNull.isBlank()) {
            return null;
        }

        StringBuilder queryBuilder = new StringBuilder();
        for (String queryParameter : rawQueryOrNull.split("&")) {
            if (queryParameter.equals("continue") || queryParameter.equals("continue=")) {
                continue;
            }

            if (!queryBuilder.isEmpty()) {
                queryBuilder.append('&');
            }
            queryBuilder.append(queryParameter);
        }

        return queryBuilder.isEmpty() ? null : queryBuilder.toString();
    }

    private static boolean isAllowedPostLoginRedirectPath(String redirectPathOrNull) {
        return redirectPathOrNull != null
            && (redirectPathOrNull.equals("/account")
                || redirectPathOrNull.startsWith("/account/")
                || redirectPathOrNull.startsWith("/daymark/"));
    }

    private void recordAuthenticatedUserEvent(EOperationEventType eventType, Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserAccount userAccount) {
            mOperationUsageEventService.recordUserEvent(eventType, userAccount.getUserAccountId());
            return;
        }

        mOperationUsageEventService.recordAnonymousEvent(eventType);
    }

    private void recordFailedSignInEvent(String loginIdentifier) {
        mUserAccountService.findUserAccountByLoginIdentifier(loginIdentifier)
            .ifPresentOrElse(
                userAccount -> mOperationUsageEventService.recordUserEvent(
                    EOperationEventType.SIGN_IN_FAILED,
                    userAccount.getUserAccountId()
                ),
                () -> mOperationUsageEventService.recordAnonymousEvent(EOperationEventType.SIGN_IN_FAILED)
            );
    }

}

package com.potterlim.daymark.controller;

import java.net.URI;
import com.potterlim.daymark.dto.auth.GoogleRegistrationSession;
import com.potterlim.daymark.dto.auth.LoginRequestDto;
import com.potterlim.daymark.dto.auth.RegisterGoogleUserAccountCommand;
import com.potterlim.daymark.dto.auth.RegisterRequestDto;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.service.DuplicateEmailException;
import com.potterlim.daymark.service.DuplicateUserNameException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final IUserAccountService mUserAccountService;
    private final AuthenticationManager mAuthenticationManager;
    private final SecurityContextRepository mSecurityContextRepository;
    private final RememberMeServices mRememberMeServices;
    private final RequestCache mRequestCache;
    private final OperationUsageEventService mOperationUsageEventService;

    public AuthController(
        IUserAccountService userAccountService,
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        RememberMeServices rememberMeServices,
        RequestCache requestCache,
        OperationUsageEventService operationUsageEventService
    ) {
        mUserAccountService = userAccountService;
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

        String googleStateOrNull = httpServletRequest.getParameter("google");
        if (httpServletRequest.getQueryString() != null && googleStateOrNull == null) {
            return "redirect:/login";
        }

        if ("failed".equals(googleStateOrNull)) {
            model.addAttribute("loginErrorMessage", "Google 로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");
        } else if ("unverified".equals(googleStateOrNull)) {
            model.addAttribute("loginErrorMessage", "Google에서 확인된 이메일 계정만 사용할 수 있습니다.");
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
    public String showRegisterPage(
        Authentication authentication,
        HttpServletRequest httpServletRequest,
        Model model
    ) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }

        if (!model.containsAttribute("registerRequestDto")) {
            model.addAttribute("registerRequestDto", new RegisterRequestDto());
        }

        prepareRegisterModel(httpServletRequest, model);
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registerRequestDto") RegisterRequestDto registerRequestDto,
        BindingResult bindingResult,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        GoogleRegistrationSession googleRegistrationSessionOrNull = getPendingGoogleRegistrationSession(httpServletRequest);
        if (googleRegistrationSessionOrNull == null) {
            return "redirect:/register";
        }

        if (!registerRequestDto.hasMatchingPassword()) {
            bindingResult.rejectValue(
                "confirmPassword",
                "register.confirmPassword",
                "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        if (bindingResult.hasErrors()) {
            prepareRegisterModel(httpServletRequest, model);
            return "auth/register";
        }

        RegisterGoogleUserAccountCommand registerGoogleUserAccountCommand =
            new RegisterGoogleUserAccountCommand(
                registerRequestDto.getUserName().trim(),
                googleRegistrationSessionOrNull.emailAddress(),
                googleRegistrationSessionOrNull.googleSubject(),
                registerRequestDto.getPassword()
            );

        UserAccount userAccount;

        try {
            userAccount = mUserAccountService.registerGoogleUserAccount(registerGoogleUserAccountCommand);
        } catch (DuplicateUserNameException duplicateUserNameException) {
            bindingResult.rejectValue(
                "userName",
                "register.userName",
                "이미 사용 중인 워크스페이스 ID입니다."
            );
            prepareRegisterModel(httpServletRequest, model);
            return "auth/register";
        } catch (DuplicateEmailException duplicateEmailException) {
            bindingResult.reject(
                "register.emailAddress",
                "이미 연결된 Google 계정입니다. 로그인으로 계속해주세요."
            );
            prepareRegisterModel(httpServletRequest, model);
            return "auth/register";
        }

        mOperationUsageEventService.recordUserEvent(
            EOperationEventType.USER_REGISTERED,
            userAccount.getUserAccountId()
        );

        authenticateUser(
            registerRequestDto.getUserName().trim(),
            registerRequestDto.getPassword(),
            httpServletRequest,
            httpServletResponse
        );

        httpServletRequest.getSession().removeAttribute(GoogleRegistrationSession.SESSION_ATTRIBUTE_NAME);
        redirectAttributes.addFlashAttribute("accountSuccessMessage", "Workspace가 준비되었습니다.");
        return "redirect:/";
    }

    @GetMapping("/forgot-password")
    public String redirectForgotPasswordPage() {
        return "redirect:/sign-in-help";
    }

    @GetMapping("/sign-in-help")
    public String showSignInHelpPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/account/password";
        }

        return "auth/sign-in-help";
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

    private static void prepareRegisterModel(HttpServletRequest httpServletRequest, Model model) {
        GoogleRegistrationSession googleRegistrationSessionOrNull = getPendingGoogleRegistrationSession(httpServletRequest);
        model.addAttribute("hasPendingGoogleRegistration", googleRegistrationSessionOrNull != null);
        if (googleRegistrationSessionOrNull == null) {
            return;
        }

        model.addAttribute("googleEmailAddress", googleRegistrationSessionOrNull.emailAddress());
        model.addAttribute("googleDisplayName", googleRegistrationSessionOrNull.displayName());
    }

    private static GoogleRegistrationSession getPendingGoogleRegistrationSession(HttpServletRequest httpServletRequest) {
        Object sessionValueOrNull = httpServletRequest.getSession(true)
            .getAttribute(GoogleRegistrationSession.SESSION_ATTRIBUTE_NAME);
        if (sessionValueOrNull instanceof GoogleRegistrationSession googleRegistrationSession) {
            return googleRegistrationSession;
        }

        return null;
    }

}

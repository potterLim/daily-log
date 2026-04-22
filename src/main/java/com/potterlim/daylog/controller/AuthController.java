package com.potterlim.daylog.controller;

import com.potterlim.daylog.dto.auth.LoginRequestDto;
import com.potterlim.daylog.dto.auth.RegisterRequestDto;
import com.potterlim.daylog.dto.auth.RegisterUserAccountCommand;
import com.potterlim.daylog.service.DuplicateUserNameException;
import com.potterlim.daylog.service.IUserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final IUserAccountService mUserAccountService;
    private final AuthenticationManager mAuthenticationManager;
    private final SecurityContextRepository mSecurityContextRepository;
    private final RememberMeServices mRememberMeServices;

    public AuthController(
        IUserAccountService userAccountService,
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        RememberMeServices rememberMeServices
    ) {
        mUserAccountService = userAccountService;
        mAuthenticationManager = authenticationManager;
        mSecurityContextRepository = securityContextRepository;
        mRememberMeServices = rememberMeServices;
    }

    @GetMapping("/login")
    public String showLoginPage(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
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

        if (mUserAccountService.findUserAccountByUserName(loginRequestDto.getUserName()).isEmpty()) {
            bindingResult.rejectValue("userName", "login.userName", "존재하지 않는 아이디입니다.");
            return "auth/login";
        }

        try {
            if (loginRequestDto.isRememberMe()) {
                authenticateUserWithRememberMe(
                    loginRequestDto.getUserName(),
                    loginRequestDto.getPassword(),
                    httpServletRequest,
                    httpServletResponse
                );
            } else {
                authenticateUser(
                    loginRequestDto.getUserName(),
                    loginRequestDto.getPassword(),
                    httpServletRequest,
                    httpServletResponse
                );
            }
        } catch (BadCredentialsException badCredentialsException) {
            mRememberMeServices.loginFail(httpServletRequest, httpServletResponse);
            bindingResult.rejectValue("password", "login.password", "비밀번호가 올바르지 않습니다.");
            return "auth/login";
        } catch (LockedException lockedException) {
            mRememberMeServices.loginFail(httpServletRequest, httpServletResponse);
            model.addAttribute("loginErrorMessage", "계정이 잠겼습니다. 잠시 후 다시 시도해주세요.");
            return "auth/login";
        } catch (DisabledException disabledException) {
            mRememberMeServices.loginFail(httpServletRequest, httpServletResponse);
            model.addAttribute("loginErrorMessage", "계정이 비활성화되어 있습니다.");
            return "auth/login";
        }

        return "redirect:/";
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
        HttpServletResponse httpServletResponse
    ) {
        if (!registerRequestDto.hasMatchingPassword()) {
            bindingResult.rejectValue("confirmPassword", "register.confirmPassword", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        RegisterUserAccountCommand registerUserAccountCommand =
            new RegisterUserAccountCommand(registerRequestDto.getUserName(), registerRequestDto.getPassword());

        try {
            mUserAccountService.registerUserAccount(registerUserAccountCommand);
        } catch (DuplicateUserNameException duplicateUserNameException) {
            bindingResult.rejectValue("userName", "register.userName", "이미 사용 중인 아이디입니다.");
            return "auth/register";
        }

        authenticateUser(
            registerRequestDto.getUserName(),
            registerRequestDto.getPassword(),
            httpServletRequest,
            httpServletResponse
        );

        return "redirect:/";
    }

    private static boolean isAuthenticated(Authentication authenticationOrNull) {
        return authenticationOrNull != null
            && authenticationOrNull.isAuthenticated()
            && !(authenticationOrNull instanceof AnonymousAuthenticationToken);
    }

    private void authenticateUserWithRememberMe(
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
    }

    /**
     * Saves the authenticated principal into the Spring Security context and the HTTP session.
     *
     * <p>Preconditions: the given user name must already exist and the raw password must be the
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
}

package com.potterlim.daylog.controller;

import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.service.IUserAccountService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class AuthenticatedUserModelAttributeControllerAdvice {

    private final IUserAccountService mUserAccountService;

    public AuthenticatedUserModelAttributeControllerAdvice(IUserAccountService userAccountService) {
        mUserAccountService = userAccountService;
    }

    @ModelAttribute
    public void populateAuthenticatedUserState(Authentication authenticationOrNull, Model model) {
        UserAccount userAccountOrNull = findAuthenticatedUserAccount(authenticationOrNull);
        if (userAccountOrNull == null) {
            return;
        }

        boolean isEmailVerificationRequired = !userAccountOrNull.hasVerifiedEmailAddress();
        model.addAttribute("isEmailVerificationRequired", isEmailVerificationRequired);
        model.addAttribute("currentUserEmailAddress", userAccountOrNull.getEmailAddress());
    }

    private UserAccount findAuthenticatedUserAccount(Authentication authenticationOrNull) {
        if (authenticationOrNull == null
            || !authenticationOrNull.isAuthenticated()
            || authenticationOrNull instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return mUserAccountService.findUserAccountByLoginIdentifier(authenticationOrNull.getName()).orElse(null);
    }
}

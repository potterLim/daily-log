package com.potterlim.daymark.security;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import com.potterlim.daymark.dto.auth.GoogleRegistrationSession;
import com.potterlim.daymark.entity.EOperationEventType;
import com.potterlim.daymark.entity.UserAccount;
import com.potterlim.daymark.service.IUserAccountService;
import com.potterlim.daymark.service.OperationUsageEventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final IUserAccountService mUserAccountService;
    private final OperationUsageEventService mOperationUsageEventService;
    private final SecurityContextRepository mSecurityContextRepository;
    private final RequestCache mRequestCache;

    public GoogleOAuth2AuthenticationSuccessHandler(
        IUserAccountService userAccountService,
        OperationUsageEventService operationUsageEventService,
        SecurityContextRepository securityContextRepository,
        RequestCache requestCache
    ) {
        mUserAccountService = userAccountService;
        mOperationUsageEventService = operationUsageEventService;
        mSecurityContextRepository = securityContextRepository;
        mRequestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        Authentication authentication
    ) throws IOException, ServletException {
        GoogleIdentity googleIdentity = resolveGoogleIdentity(authentication);
        if (!googleIdentity.isUsable()) {
            clearSecurityContext(httpServletRequest, httpServletResponse);
            httpServletResponse.sendRedirect("/login?google=unverified");
            return;
        }

        Optional<UserAccount> userAccountOrEmpty =
            mUserAccountService.findUserAccountByGoogleSubject(googleIdentity.subject())
                .or(() -> mUserAccountService.connectGoogleIdentityByEmailAddress(
                    googleIdentity.emailAddress(),
                    googleIdentity.subject()
                ));
        if (userAccountOrEmpty.isPresent()) {
            authenticateApplicationUser(userAccountOrEmpty.get(), httpServletRequest, httpServletResponse);
            mOperationUsageEventService.recordUserEvent(
                EOperationEventType.SIGN_IN_SUCCEEDED,
                userAccountOrEmpty.get().getUserAccountId()
            );
            httpServletResponse.sendRedirect(resolveSavedRequestRedirectPath(httpServletRequest, httpServletResponse));
            return;
        }

        clearSecurityContext(httpServletRequest, httpServletResponse);
        httpServletRequest.getSession(true).setAttribute(
            GoogleRegistrationSession.SESSION_ATTRIBUTE_NAME,
            new GoogleRegistrationSession(
                googleIdentity.subject(),
                googleIdentity.emailAddress(),
                googleIdentity.displayName()
            )
        );
        httpServletResponse.sendRedirect("/register");
    }

    private void authenticateApplicationUser(
        UserAccount userAccount,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) {
        Authentication applicationAuthentication =
            new UsernamePasswordAuthenticationToken(userAccount, userAccount.getPassword(), userAccount.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(applicationAuthentication);
        SecurityContextHolder.setContext(securityContext);
        mSecurityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);
    }

    private void clearSecurityContext(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
        mSecurityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);
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

    private static GoogleIdentity resolveGoogleIdentity(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            return GoogleIdentity.unusable();
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String subject = readString(attributes, "sub");
        String emailAddress = readString(attributes, "email");
        String displayName = readString(attributes, "name");
        boolean isEmailVerified = Boolean.TRUE.equals(attributes.get("email_verified"))
            || "true".equalsIgnoreCase(String.valueOf(attributes.get("email_verified")));

        return new GoogleIdentity(subject, emailAddress, displayName, isEmailVerified);
    }

    private static String readString(Map<String, Object> attributes, String key) {
        Object valueOrNull = attributes.get(key);
        return valueOrNull == null ? "" : valueOrNull.toString().trim();
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

    private record GoogleIdentity(
        String subject,
        String emailAddress,
        String displayName,
        boolean isEmailVerified
    ) {

        private static GoogleIdentity unusable() {
            return new GoogleIdentity("", "", "", false);
        }

        private boolean isUsable() {
            return isEmailVerified
                && subject != null
                && !subject.isBlank()
                && emailAddress != null
                && !emailAddress.isBlank();
        }
    }
}

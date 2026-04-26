package com.potterlim.daymark.support;

import java.util.List;

public final class LoginRedirectSupport {

    private static final String DEFAULT_REDIRECT_PATH = "/";
    private static final List<String> ALLOWED_REDIRECT_PREFIXES = List.of("/account/", "/daymark/");

    private LoginRedirectSupport() {
    }

    public static String resolveLoginRedirectPath(String redirectPathOrNull) {
        if (redirectPathOrNull == null || redirectPathOrNull.isBlank()) {
            return DEFAULT_REDIRECT_PATH;
        }

        String normalizedRedirectPath = redirectPathOrNull.trim();
        if (!isSafeInternalRedirectPath(normalizedRedirectPath)) {
            return DEFAULT_REDIRECT_PATH;
        }

        if (normalizedRedirectPath.equals(DEFAULT_REDIRECT_PATH) || normalizedRedirectPath.equals("/account")) {
            return normalizedRedirectPath;
        }

        for (String allowedRedirectPrefix : ALLOWED_REDIRECT_PREFIXES) {
            if (normalizedRedirectPath.startsWith(allowedRedirectPrefix)) {
                return normalizedRedirectPath;
            }
        }

        return DEFAULT_REDIRECT_PATH;
    }

    private static boolean isSafeInternalRedirectPath(String redirectPath) {
        return redirectPath.startsWith("/")
            && !redirectPath.startsWith("//")
            && !redirectPath.contains("\\")
            && !redirectPath.contains("\r")
            && !redirectPath.contains("\n");
    }
}

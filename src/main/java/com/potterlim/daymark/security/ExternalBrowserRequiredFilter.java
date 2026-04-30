package com.potterlim.daymark.security;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class ExternalBrowserRequiredFilter extends OncePerRequestFilter {

    private static final String GOOGLE_OAUTH_PATH = "/oauth2/authorization/google";
    private static final String DEFAULT_RETURN_PATH = "/login";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest httpServletRequest) {
        String requestPath = httpServletRequest.getRequestURI()
            .substring(httpServletRequest.getContextPath().length());
        return !GOOGLE_OAUTH_PATH.equals(requestPath);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!EmbeddedBrowserDetector.isEmbeddedBrowser(httpServletRequest.getHeader("User-Agent"))) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        String returnPath = resolveReturnPath(httpServletRequest.getHeader("Referer"));
        String encodedReturnPath = URLEncoder.encode(returnPath, StandardCharsets.UTF_8);
        httpServletResponse.sendRedirect(httpServletRequest.getContextPath()
            + "/external-browser-required?returnTo="
            + encodedReturnPath);
    }

    private static String resolveReturnPath(String refererOrNull) {
        if (refererOrNull == null || refererOrNull.isBlank()) {
            return DEFAULT_RETURN_PATH;
        }

        try {
            URI refererUri = URI.create(refererOrNull);
            String path = refererUri.getPath();
            return isAllowedReturnPath(path) ? path : DEFAULT_RETURN_PATH;
        } catch (IllegalArgumentException illegalArgumentException) {
            return DEFAULT_RETURN_PATH;
        }
    }

    private static boolean isAllowedReturnPath(String pathOrNull) {
        return "/login".equals(pathOrNull)
            || "/register".equals(pathOrNull)
            || "/sign-in-help".equals(pathOrNull);
    }
}

package com.potterlim.daymark.controller;

import com.potterlim.daymark.config.DaymarkApplicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ExternalBrowserRequiredController {

    private static final String DEFAULT_RETURN_PATH = "/login";

    private final DaymarkApplicationProperties mDaymarkApplicationProperties;

    public ExternalBrowserRequiredController(DaymarkApplicationProperties daymarkApplicationProperties) {
        mDaymarkApplicationProperties = daymarkApplicationProperties;
    }

    @GetMapping("/external-browser-required")
    public String showExternalBrowserRequiredPage(
        @RequestParam(name = "returnTo", required = false) String returnPathOrNull,
        HttpServletRequest httpServletRequest,
        Model model
    ) {
        String returnPath = isAllowedReturnPath(returnPathOrNull) ? returnPathOrNull : DEFAULT_RETURN_PATH;
        model.addAttribute("externalBrowserCopyUrl", resolvePublicBaseUrl(httpServletRequest) + returnPath);
        model.addAttribute("externalBrowserReturnPath", returnPath);
        return "auth/external-browser-required";
    }

    private String resolvePublicBaseUrl(HttpServletRequest httpServletRequest) {
        String configuredPublicBaseUrl = mDaymarkApplicationProperties.getPublicBaseUrl();
        if (configuredPublicBaseUrl != null && !configuredPublicBaseUrl.isBlank()) {
            return configuredPublicBaseUrl.strip().replaceAll("/+$", "");
        }

        String scheme = httpServletRequest.getScheme();
        String host = httpServletRequest.getServerName();
        int port = httpServletRequest.getServerPort();
        boolean isDefaultPort = ("http".equals(scheme) && port == 80)
            || ("https".equals(scheme) && port == 443);

        return isDefaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }

    private static boolean isAllowedReturnPath(String pathOrNull) {
        return "/login".equals(pathOrNull)
            || "/register".equals(pathOrNull)
            || "/sign-in-help".equals(pathOrNull);
    }
}

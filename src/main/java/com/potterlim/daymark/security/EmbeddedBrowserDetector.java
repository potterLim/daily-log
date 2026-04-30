package com.potterlim.daymark.security;

import java.util.Locale;

public final class EmbeddedBrowserDetector {

    private EmbeddedBrowserDetector() {
    }

    public static boolean isEmbeddedBrowser(String userAgentOrNull) {
        if (userAgentOrNull == null || userAgentOrNull.isBlank()) {
            return false;
        }

        String normalizedUserAgent = userAgentOrNull.toLowerCase(Locale.ROOT);
        return normalizedUserAgent.contains("kakaotalk")
            || normalizedUserAgent.contains("instagram")
            || normalizedUserAgent.contains("fbav")
            || normalizedUserAgent.contains("fban")
            || normalizedUserAgent.contains("line/")
            || normalizedUserAgent.contains("naver")
            || normalizedUserAgent.contains("daumapps")
            || normalizedUserAgent.contains("; wv)");
    }
}

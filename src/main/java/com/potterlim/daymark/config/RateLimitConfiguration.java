package com.potterlim.daymark.config;

import com.potterlim.daymark.security.RateLimitingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitConfiguration implements WebMvcConfigurer {

    private final RateLimitingInterceptor mRateLimitingInterceptor;

    public RateLimitConfiguration(RateLimitingInterceptor rateLimitingInterceptor) {
        mRateLimitingInterceptor = rateLimitingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry interceptorRegistry) {
        interceptorRegistry.addInterceptor(mRateLimitingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/actuator/health",
                "/actuator/health/**",
                "/css/**",
                "/favicon.ico",
                "/images/**",
                "/js/**"
            );
    }
}

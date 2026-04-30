package com.potterlim.daymark.config;

import java.io.IOException;
import com.potterlim.daymark.security.ExternalBrowserRequiredFilter;
import com.potterlim.daymark.security.SecurityUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

@Configuration
public class SecurityConfiguration {

    private final DaymarkApplicationProperties mDaymarkApplicationProperties;

    public SecurityConfiguration(DaymarkApplicationProperties daymarkApplicationProperties) {
        mDaymarkApplicationProperties = daymarkApplicationProperties;
    }

    @Bean
    public SecurityFilterChain buildSecurityFilterChain(
        HttpSecurity httpSecurity,
        RememberMeServices rememberMeServices,
        SecurityContextRepository securityContextRepository,
        RequestCache requestCache,
        ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
        AuthenticationSuccessHandler googleOAuth2AuthenticationSuccessHandler
    ) throws Exception {
        String rememberMeCookieName = mDaymarkApplicationProperties.getSecurity().getRememberMeCookieName();

        httpSecurity
            .authorizeHttpRequests(authorizeHttpRequests ->
                authorizeHttpRequests
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(
                        "/",
                        "/css/**",
                        "/images/**",
                        "/js/**",
                        "/favicon.ico",
                        "/error",
                        "/external-browser-required",
                        "/login",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/register",
                        "/forgot-password",
                        "/sign-in-help"
                    )
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/admin/operations")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET,
                        "/account",
                        "/account/password",
                        "/daymark/morning",
                        "/daymark/morning/edit",
                        "/daymark/evening",
                        "/daymark/evening/edit",
                        "/daymark/week",
                        "/daymark/library",
                        "/daymark/library/export/markdown",
                        "/daymark/library/export/pdf",
                        "/daymark/preview"
                    )
                    .authenticated()
                    .requestMatchers(HttpMethod.POST,
                        "/account/password",
                        "/daymark/morning/save",
                        "/daymark/evening/save",
                        "/logout"
                    )
                    .authenticated()
                    .requestMatchers("/actuator/**")
                    .denyAll()
                    .anyRequest()
                    .permitAll())
            .exceptionHandling(exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                    .accessDeniedHandler(SecurityConfiguration::forwardToNotFoundPage))
            .securityContext(securityContext ->
                securityContext.securityContextRepository(securityContextRepository))
            .requestCache(requestCacheConfigurer -> requestCacheConfigurer.requestCache(requestCache))
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrerPolicy -> referrerPolicy.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
            .addFilterBefore(
                new ExternalBrowserRequiredFilter(),
                OAuth2AuthorizationRequestRedirectFilter.class
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .deleteCookies("JSESSIONID", rememberMeCookieName))
            .csrf(Customizer.withDefaults());

        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            httpSecurity.oauth2Login(oAuth2Login -> oAuth2Login
                .loginPage("/login")
                .successHandler(googleOAuth2AuthenticationSuccessHandler)
                .failureUrl("/login?google=failed"));
        }

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager createAuthenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public RememberMeServices createRememberMeServices(SecurityUserDetailsService securityUserDetailsService) {
        DaymarkApplicationProperties.SecurityProperties securityProperties =
            mDaymarkApplicationProperties.getSecurity();
        TokenBasedRememberMeServices tokenBasedRememberMeServices =
            new TokenBasedRememberMeServices(securityProperties.getRememberMeKey(), securityUserDetailsService);

        tokenBasedRememberMeServices.setParameter("rememberMe");
        tokenBasedRememberMeServices.setCookieName(securityProperties.getRememberMeCookieName());
        tokenBasedRememberMeServices.setUseSecureCookie(securityProperties.isRememberMeCookieSecure());
        tokenBasedRememberMeServices.setTokenValiditySeconds(securityProperties.getRememberMeTokenValiditySeconds());

        return tokenBasedRememberMeServices;
    }

    @Bean
    public SecurityContextRepository createSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public RequestCache createRequestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    public PasswordEncoder createPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void forwardToNotFoundPage(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        AccessDeniedException accessDeniedException
    ) throws ServletException, IOException {
        httpServletResponse.setStatus(HttpStatus.NOT_FOUND.value());
        httpServletRequest.getRequestDispatcher("/error").forward(httpServletRequest, httpServletResponse);
    }
}

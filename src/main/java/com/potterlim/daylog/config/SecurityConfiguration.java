package com.potterlim.daylog.config;

import com.potterlim.daylog.security.SecurityUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SecurityConfiguration {

    private final DayLogApplicationProperties mDayLogApplicationProperties;

    public SecurityConfiguration(DayLogApplicationProperties dayLogApplicationProperties) {
        mDayLogApplicationProperties = dayLogApplicationProperties;
    }

    @Bean
    public SecurityFilterChain buildSecurityFilterChain(
        HttpSecurity httpSecurity,
        RememberMeServices rememberMeServices,
        SecurityContextRepository securityContextRepository
    ) throws Exception {
        String rememberMeCookieName = mDayLogApplicationProperties.getSecurity().getRememberMeCookieName();

        httpSecurity
            .authorizeHttpRequests(authorizeHttpRequests ->
                authorizeHttpRequests
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(
                        "/css/**",
                        "/images/**",
                        "/js/**",
                        "/favicon.ico",
                        "/login",
                        "/register",
                        "/forgot-password",
                        "/reset-password",
                        "/verify-email"
                    )
                    .permitAll()
                    .anyRequest()
                    .authenticated())
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .securityContext(securityContext ->
                securityContext.securityContextRepository(securityContextRepository))
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrerPolicy -> referrerPolicy.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID", rememberMeCookieName))
            .csrf(Customizer.withDefaults());

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
        DayLogApplicationProperties.SecurityProperties securityProperties = mDayLogApplicationProperties.getSecurity();
        TokenBasedRememberMeServices tokenBasedRememberMeServices =
            new TokenBasedRememberMeServices(securityProperties.getRememberMeKey(), securityUserDetailsService);

        tokenBasedRememberMeServices.setParameter("rememberMe");
        tokenBasedRememberMeServices.setCookieName(securityProperties.getRememberMeCookieName());
        tokenBasedRememberMeServices.setTokenValiditySeconds(securityProperties.getRememberMeTokenValiditySeconds());

        return tokenBasedRememberMeServices;
    }

    @Bean
    public SecurityContextRepository createSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder createPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

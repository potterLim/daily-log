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

@Configuration
public class SecurityConfiguration {

    private final DayLogApplicationProperties mDayLogApplicationProperties;

    public SecurityConfiguration(DayLogApplicationProperties dayLogApplicationProperties) {
        mDayLogApplicationProperties = dayLogApplicationProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity,
        RememberMeServices rememberMeServices,
        SecurityContextRepository securityContextRepository
    ) throws Exception {
        String rememberMeCookieName = mDayLogApplicationProperties.getSecurity().getRememberMeCookieName();

        httpSecurity
            .authorizeHttpRequests(authorizeHttpRequests ->
                authorizeHttpRequests
                    .requestMatchers("/css/**", "/js/**", "/favicon.ico", "/login", "/register")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .securityContext(securityContext ->
                securityContext.securityContextRepository(securityContextRepository))
            .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID", rememberMeCookieName))
            .csrf(Customizer.withDefaults());

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        daoAuthenticationProvider.setHideUserNotFoundExceptions(false);

        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public RememberMeServices rememberMeServices(SecurityUserDetailsService securityUserDetailsService) {
        DayLogApplicationProperties.SecurityProperties securityProperties = mDayLogApplicationProperties.getSecurity();
        TokenBasedRememberMeServices tokenBasedRememberMeServices =
            new TokenBasedRememberMeServices(securityProperties.getRememberMeKey(), securityUserDetailsService);

        tokenBasedRememberMeServices.setParameter("rememberMe");
        tokenBasedRememberMeServices.setCookieName(securityProperties.getRememberMeCookieName());
        tokenBasedRememberMeServices.setTokenValiditySeconds(securityProperties.getRememberMeTokenValiditySeconds());

        return tokenBasedRememberMeServices;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

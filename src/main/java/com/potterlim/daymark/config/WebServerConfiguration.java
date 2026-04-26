package com.potterlim.daymark.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class WebServerConfiguration {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> createWebServerFactoryCustomizer() {
        return configurableServletWebServerFactory ->
            configurableServletWebServerFactory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/"));
    }
}

package com.ineos.oxide.base.security.configuration;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import com.ineos.oxide.base.security.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends VaadinWebSecurity {

    /**
     * Require login to access internal pages and configure login form.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/robots.txt",
                                "/icons/**",
                                "/images/**",
                                "/static/**",
                                "/h2-console/**")
                        .permitAll());
        super.configure(http);
        setLoginView(http, LoginView.class);
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }

}

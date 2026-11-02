package dev.guilhermeluan.desafiobackend.desafiobackend_sea.config;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt.AuthAccessDeniedHandler;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt.AuthEntryPointJwt;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt.AuthTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class WebSecurityConfig {

    private final AuthEntryPointJwt authEntryPointJwt;
    private final AuthAccessDeniedHandler authAccessDeniedHandler;

    public WebSecurityConfig(AuthEntryPointJwt authEntryPointJwt, AuthAccessDeniedHandler authAccessDeniedHandler) {
        this.authEntryPointJwt = authEntryPointJwt;
        this.authAccessDeniedHandler = authAccessDeniedHandler;
    }


    @Bean
    public FilterRegistrationBean<AuthTokenFilter> authTokenFilterRegistration(AuthTokenFilter authTokenFilter) {
        FilterRegistrationBean<AuthTokenFilter> registration = new FilterRegistrationBean<>(authTokenFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthTokenFilter authTokenFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authEntryPointJwt)
                        .accessDeniedHandler(authAccessDeniedHandler))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/analyst/**").hasAnyRole("ANALYST", "ADMIN")
                        .requestMatchers("/solicitations/**").hasRole("CLIENT")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

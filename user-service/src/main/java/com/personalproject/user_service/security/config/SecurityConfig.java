package com.personalproject.user_service.security.config;


import com.personalproject.user_service.security.jwt.JwtTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.util.List;

@Configuration
public class SecurityConfig {
    @Autowired
    private JwtTokenFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                        auth -> auth.requestMatchers(HttpMethod.POST,"/api/v1/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/v1/users/logout").permitAll()
                                .requestMatchers(HttpMethod.POST,"/api/v1/users/signup").permitAll()
                                .requestMatchers(HttpMethod.POST,"/api/v1/users/token/refresh").permitAll()
                                .requestMatchers(HttpMethod.POST,"/api/v1/users/token/validate").permitAll()
                                .requestMatchers(HttpMethod.POST,"/api/v1/users/token").permitAll()
                                .requestMatchers(HttpMethod.PUT,"/api/v1/users/profile")
                                .hasAnyAuthority("user")
                                .requestMatchers(HttpMethod.POST,"/api/v1/users/send-verification-code")
                                .hasAnyAuthority("user")
                                .requestMatchers(HttpMethod.POST,"/api/v1/users/change-password")
                                .hasAnyAuthority("user")
                                .requestMatchers(HttpMethod.GET,"/api/v1/users/*")
                                .hasAnyAuthority("user")
//                                .requestMatchers(HttpMethod.GET,"/api/auth/user/patient")
//                                .hasAnyAuthority("service", "bacsi", "nhanvien", "benhnhan")
                                .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .exceptionHandling(exh -> exh.authenticationEntryPoint(
//                        (request, response, exception) -> {
//                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
//                        }))
                .addFilterBefore(jwtFilter, AuthorizationFilter.class)
        ;



        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    UserDetailsService customUserDetailsService() {
        return new CustomUserDetailsService();
    }
//
//    @Bean
//    UserDetailsService customServiceDetailsService() {
//        return new com.doanth.auth_service.security.config.CustomServiceDetailsService();
//    }

    @Bean
    DaoAuthenticationProvider userAuthenticationProvider() {
        DaoAuthenticationProvider userAuthenticationProvider = new DaoAuthenticationProvider(customUserDetailsService());
        userAuthenticationProvider.setPasswordEncoder(passwordEncoder());
//        userAuthenticationProvider.setUserDetailsService(customUserDetailsService());
        return userAuthenticationProvider;
    }
//    @Bean
//    DaoAuthenticationProvider serviceAuthenticationProvider() {
//        DaoAuthenticationProvider userAuthenticationProvider = new DaoAuthenticationProvider(customServiceDetailsService());
//        userAuthenticationProvider.setPasswordEncoder(passwordEncoder());
////        userAuthenticationProvider.setUserDetailsService(customServiceDetailsService());
//        return userAuthenticationProvider;
//    }

    @Bean("userAuthManager")
    @Primary
    AuthenticationManager userAuthManager() {
        return new ProviderManager(List.of(userAuthenticationProvider()));
    }
//    @Bean("serviceAuthManager")
//    AuthenticationManager serviceAuthManager() {
//        return new ProviderManager(List.of(serviceAuthenticationProvider()));
//    }
}

package ru.merezh.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.merezh.gateway.security.JwtFilter;
import ru.merezh.gateway.security.handler.AccessHandler;
import ru.merezh.gateway.security.handler.AuthEntryPointHandler;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AccessHandler accessHandler;
    private final AuthEntryPointHandler authEntryPointHandler;

    @Value("${service.auth.path}")
    private String pathAuthService;

    @Value("${service.users.path}")
    private String pathUserService;

    @Value("${service.wallets.path}")
    private String pathWalletService;

    @Value("${service.orders.path}")
    private String pathOrderService;

    @Value("${service.payments.path}")
    private String pathPaymentService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

         http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                pathAuthService + "/register",
                                pathAuthService + "/login",
                                pathAuthService + "/refresh"
                        ).permitAll()
                        .requestMatchers(pathAuthService + "/logout").authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                pathWalletService + "/balance",
                                pathOrderService + "/get/user",
                                pathPaymentService + "/get/user"
                        ).authenticated()
                        .requestMatchers(HttpMethod.POST,
                                pathWalletService + "/create",
                                pathWalletService + "/balance/sum",
                                pathWalletService + "/balance/sub",
                                pathOrderService + "/create",
                                pathPaymentService + "/pay/**"
                        ).authenticated()
                        .requestMatchers(HttpMethod.GET,
                                pathUserService,
                                pathUserService + "/**",
                                pathOrderService + "/get/**",
                                pathPaymentService + "/get/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, pathAuthService + "/**", pathWalletService + "/**").hasRole("ADMIN")
                )
                 .exceptionHandling(exception -> exception
                         .accessDeniedHandler(accessHandler)
                         .authenticationEntryPoint(authEntryPointHandler)
                 )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                 .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

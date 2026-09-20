package io.github.devang559.authkit.autoconfigure;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.jwt.JwtService;
import io.github.devang559.authkit.security.AuthKitJwtAuthenticationFilter;
import io.github.devang559.authkit.user.AuthKitUserDetailsService;
import io.github.devang559.authkit.user.AuthUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Slf4j
@AutoConfiguration
@AutoConfigureAfter(AuthKitAutoConfiguration.class)
@ConditionalOnClass({SecurityFilterChain.class, HttpSecurity.class, JwtService.class})
@ConditionalOnProperty(name = "spring.authkit.enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSecurity
public class AuthKitSecurityAutoConfiguration {

    @Bean
    @Order(0)
    @ConditionalOnMissingBean
    AuthKitJwtAuthenticationFilter authKitJwtAuthenticationFilter(
            JwtService jwtService, AuthKitUserDetailsService userDetailsService, AuthUserService userService) {
        return new AuthKitJwtAuthenticationFilter(jwtService, userDetailsService, userService);
    }

    @Bean
    @Order(10)
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain authKitSecurityFilterChain(HttpSecurity http,
                                                   AuthKitJwtAuthenticationFilter filter,
                                                   AuthKitConfig config) throws Exception {
        List<String> publicEndpoints = config.getProperties().getSecurity().getPublicEndpoints();
        log.info("SpringAuthKit: registering security filter chain. Public endpoints: {}", publicEndpoints);
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, ex) -> writeJson(response, 401,
                        "UNAUTHENTICATED", "Authentication is required"))
                .accessDeniedHandler((request, response, ex) -> writeJson(response, 403,
                        "ACCESS_DENIED", "Access is denied")))
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(publicEndpoints.toArray(new String[0])).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }

    private static void writeJson(HttpServletResponse response, int status, String code, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"status\":" + status + ",\"code\":\"" + code + "\",\"message\":\"" + message + "\"}";
        response.getWriter().write(body);
    }
}

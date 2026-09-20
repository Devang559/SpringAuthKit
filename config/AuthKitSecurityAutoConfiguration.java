package io.github.devang559.authkit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.devang559.authkit.jwt.JwtService;
import io.github.devang559.authkit.security.AuthKitAuthenticationEntryPoint;
import io.github.devang559.authkit.security.AuthKitAuthenticationToken;
import io.github.devang559.authkit.security.AuthKitJwtAuthenticationFilter;
import io.github.devang559.authkit.user.AuthKitUserDetailsService;
import io.github.devang559.authkit.user.AuthUserService;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = true)
@ConditionalOnClass({SecurityFilterChain.class, AuthKitAuthenticationToken.class})
@ConditionalOnProperty(name = "spring.authkit.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AuthKitSecurityAutoConfiguration {

    private final AuthKitProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public AuthKitJwtAuthenticationFilter authKitJwtAuthenticationFilter(
            JwtService jwtService,
            AuthUserService userService,
            AuthKitUserDetailsService userDetailsService) {
        return new AuthKitJwtAuthenticationFilter(jwtService, userService, userDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthKitAuthenticationEntryPoint authKitAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new AuthKitAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain authKitSecurityFilterChain(
            HttpSecurity http,
            AuthKitJwtAuthenticationFilter filter,
            AuthKitAuthenticationEntryPoint entryPoint) throws Exception {

        String[] publicEndpoints = properties.getSecurity().getPublicEndpoints().toArray(new String[0]);

        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(publicEndpoints).permitAll()
                        .requestMatchers("/auth/**", "/error", "/webjars/**").permitAll()
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout.disable())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

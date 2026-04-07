package com.emeraldgrove.config;

import com.emeraldgrove.security.JwtAuthenticationFilter;
import com.emeraldgrove.security.RateLimitFilter;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter
    ) {
        try {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/health").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build security filter chain", exception);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${emerald-grove.security.allowed-origin-patterns:chrome-extension://*,moz-extension://*}") String allowedOriginPatterns
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOriginPatterns.split(","))
            .map(String::trim)
            .filter(pattern -> !pattern.isBlank())
            .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
        @Value("${emerald-grove.security.extension-token}") String extensionToken
    ) {
        return new JwtAuthenticationFilter(extensionToken);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
        @Value("${emerald-grove.rate-limit.sync-limit:30}") int syncLimit,
        @Value("${emerald-grove.rate-limit.auth-check-limit:10}") int authCheckLimit,
        @Value("${emerald-grove.rate-limit.window-seconds:60}") int windowSeconds
    ) {
        return new RateLimitFilter(syncLimit, authCheckLimit, windowSeconds * 1000L);
    }
}

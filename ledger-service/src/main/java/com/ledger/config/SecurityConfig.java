package com.ledger.config;

import com.ledger.security.ratelimit.RateLimitFilter;
import com.ledger.security.ratelimit.RateLimiter;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 resource-server security, toggleable so existing flows can run without
 * tokens. When enabled, endpoints require scope authorities (RBAC) and writes are
 * rate limited per subject; ABAC (amount and account limits) is enforced in the
 * controller via AbacGate. HS256 is used for local development; production swaps
 * in RS256 via a JWKS URL with no code change.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain securedChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                     RateLimiter rateLimiter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/transactions").hasAuthority("SCOPE_ledger:write")
                        .requestMatchers(HttpMethod.POST, "/v1/accounts").hasAuthority("SCOPE_ledger:write")
                        .requestMatchers(HttpMethod.GET, "/v1/accounts/**").hasAuthority("SCOPE_ledger:read")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(scopeAuthoritiesConverter())))
                .addFilterAfter(new RateLimitFilter(rateLimiter), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "security.enabled", havingValue = "false")
    SecurityFilterChain openChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private JwtAuthenticationConverter scopeAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter granted = new JwtGrantedAuthoritiesConverter();
        granted.setAuthoritiesClaimName("scope");
        granted.setAuthorityPrefix("SCOPE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(granted);
        return converter;
    }
}

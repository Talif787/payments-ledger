package com.ledger.security.token;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mints short-lived HS256 service tokens so the ledger's outbound call to the
 * fraud service is authenticated, not merely trusted by network position. Only
 * present when security is enabled. In production this is a client-credentials
 * grant from the IdP rather than a shared-secret mint.
 */
@Component
@ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
public class ServiceTokenProvider {

    private final byte[] secret;
    private final String scope;

    public ServiceTokenProvider(@Value("${security.jwt.secret}") String secret,
                                @Value("${security.service-token.scope:fraud:evaluate}") String scope) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.scope = scope;
    }

    public String currentToken() {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("ledger-service")
                    .claim("scope", scope)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(60)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mint service token", e);
        }
    }
}

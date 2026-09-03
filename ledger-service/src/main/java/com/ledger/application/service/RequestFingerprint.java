package com.ledger.application.service;

import com.ledger.domain.money.Currency;
import com.ledger.domain.transaction.Posting;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes a stable SHA-256 fingerprint of the meaningful content of a
 * money-movement request (currency, postings, metadata) independent of ordering
 * and of the server-assigned transaction id. Two requests are "the same" for
 * idempotency purposes exactly when their fingerprints match.
 */
final class RequestFingerprint {

    private RequestFingerprint() {}

    static String of(Currency currency, List<Posting> postings, Map<String, String> metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("cur=").append(currency).append(';');

        postings.stream()
                .map(p -> p.accountId() + "|" + p.direction() + "|" + p.amount().minorUnits())
                .sorted()
                .forEach(line -> sb.append(line).append(';'));

        new TreeMap<>(metadata).forEach((k, v) -> sb.append("m:").append(k).append('=').append(v).append(';'));

        return sha256Hex(sb.toString());
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

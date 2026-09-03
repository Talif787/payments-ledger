package com.ledger.domain.account;

import com.ledger.domain.exception.DomainException;
import java.util.Objects;
import java.util.UUID;

/** Opaque, validated identifier for a ledger account. */
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "account id");
    }

    public static AccountId of(String raw) {
        try {
            return new AccountId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new DomainException.InvalidTransaction("Invalid account id: " + raw);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

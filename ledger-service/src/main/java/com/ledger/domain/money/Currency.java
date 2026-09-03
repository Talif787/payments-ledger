package com.ledger.domain.money;

import com.ledger.domain.exception.DomainException;

/**
 * Supported ISO 4217 currencies with their minor-unit scale (number of
 * fractional digits). The scale governs how a decimal amount is converted to an
 * exact integer number of minor units, which is how money is stored and
 * compared. Extending this enum is the single point of change for currency
 * support.
 */
public enum Currency {
    USD(2),
    EUR(2),
    GBP(2),
    INR(2),
    JPY(0);

    private final int fractionDigits;

    Currency(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public int fractionDigits() {
        return fractionDigits;
    }

    public static Currency of(String code) {
        if (code == null) {
            throw new DomainException.InvalidMoney("Currency is required");
        }
        try {
            return Currency.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException.InvalidMoney("Unsupported currency: " + code);
        }
    }
}

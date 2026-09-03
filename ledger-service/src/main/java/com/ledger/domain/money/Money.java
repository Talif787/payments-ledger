package com.ledger.domain.money;

import com.ledger.domain.exception.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable monetary value stored as an exact integer count of minor units (for
 * example cents) in a single currency. Money never uses floating point. Positive,
 * negative, and zero values are representable; balances may be negative, while
 * posting amounts are constrained to be strictly positive by the caller via
 * {@link #requirePositive()}.
 */
public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money ofMinor(long minorUnits, Currency currency) {
        return new Money(minorUnits, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(0L, currency);
    }

    /**
     * Converts an exact decimal amount to minor units for the given currency.
     * Rejects amounts with more fractional digits than the currency permits so
     * that no silent rounding of user-supplied money can occur.
     */
    public static Money of(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.scale() > currency.fractionDigits()) {
            throw new DomainException.InvalidMoney(
                    "Amount " + amount.toPlainString() + " exceeds allowed precision for " + currency);
        }
        BigDecimal scaled = amount.movePointRight(currency.fractionDigits());
        try {
            return new Money(scaled.setScale(0, RoundingMode.UNNECESSARY).longValueExact(), currency);
        } catch (ArithmeticException e) {
            throw new DomainException.InvalidMoney("Amount out of range or not exact: " + amount);
        }
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(this.minorUnits, other.minorUnits), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(this.minorUnits, other.minorUnits), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(minorUnits), currency);
    }

    public boolean isNegative() {
        return minorUnits < 0;
    }

    public boolean isPositive() {
        return minorUnits > 0;
    }

    public boolean isZero() {
        return minorUnits == 0;
    }

    public Money requirePositive() {
        if (!isPositive()) {
            throw new DomainException.InvalidMoney("Amount must be strictly positive: " + this);
        }
        return this;
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(minorUnits).movePointLeft(currency.fractionDigits());
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(this.minorUnits, other.minorUnits);
    }

    private void requireSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new DomainException.CurrencyMismatch(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return toBigDecimal().toPlainString() + " " + currency;
    }
}

package com.ledger.domain.exception;

/**
 * Root of the ledger's domain error hierarchy. Domain exceptions carry a stable
 * machine-readable {@link #code()} used by the presentation layer to map errors
 * to HTTP responses without leaking internal detail. The hierarchy is sealed so
 * the set of failure modes is closed and exhaustively handled.
 */
public sealed class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static final class UnbalancedTransaction extends DomainException {
        public UnbalancedTransaction(String message) { super("UNBALANCED_TRANSACTION", message); }
    }

    public static final class CurrencyMismatch extends DomainException {
        public CurrencyMismatch(String message) { super("CURRENCY_MISMATCH", message); }
    }

    public static final class InvalidMoney extends DomainException {
        public InvalidMoney(String message) { super("INVALID_MONEY", message); }
    }

    public static final class InvalidTransaction extends DomainException {
        public InvalidTransaction(String message) { super("INVALID_TRANSACTION", message); }
    }

    public static final class AccountNotFound extends DomainException {
        public AccountNotFound(String message) { super("ACCOUNT_NOT_FOUND", message); }
    }

    public static final class AccountInactive extends DomainException {
        public AccountInactive(String message) { super("ACCOUNT_INACTIVE", message); }
    }

    public static final class InsufficientFunds extends DomainException {
        public InsufficientFunds(String message) { super("INSUFFICIENT_FUNDS", message); }
    }

    public static final class IdempotencyConflict extends DomainException {
        public IdempotencyConflict(String message) { super("IDEMPOTENCY_CONFLICT", message); }
    }
}

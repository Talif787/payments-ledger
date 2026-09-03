package com.ledger.application.port.out;

import com.ledger.application.model.FraudVerdict;

/**
 * Consults the fraud service for a proposed money movement. Implementations are
 * best-effort: on timeout or error they must return an unscreened ALLOW rather
 * than fail the caller, so a fraud outage never blocks the ledger.
 */
public interface FraudEvaluator {
    FraudVerdict evaluate(String accountId, long amountMinor, String currency, String counterpartyId);
}

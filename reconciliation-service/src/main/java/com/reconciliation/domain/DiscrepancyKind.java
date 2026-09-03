package com.reconciliation.domain;

/**
 * The distinct ways the ledger can fail reconciliation. Each names a specific
 * invariant so an operator sees immediately what broke.
 */
public enum DiscrepancyKind {
    /** An account's stored balance does not equal the sum of its own postings. */
    BALANCE_POSTING_MISMATCH,
    /** The balance re-derived from the event stream disagrees with the ledger. */
    EVENT_LEDGER_MISMATCH,
    /** All ledger balances together do not net to zero. */
    GLOBAL_IMBALANCE
}

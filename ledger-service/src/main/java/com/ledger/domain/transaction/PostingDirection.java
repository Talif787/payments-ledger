package com.ledger.domain.transaction;

/**
 * Direction of a single posting. The ledger uses a credit-positive convention:
 * a CREDIT increases the affected account's balance and a DEBIT decreases it.
 */
public enum PostingDirection {
    DEBIT,
    CREDIT
}

package com.reconciliation.domain;

/**
 * The ledger's authoritative view of one account at reconciliation time: its
 * stored balance and the independently computed sum of its postings. In a
 * correct ledger these are always equal; comparing them catches corruption of
 * the balance projection.
 */
public record AccountSnapshot(String accountId, long balanceMinor, long postingSumMinor) {
}

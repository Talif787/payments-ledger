package com.reconciliation.application.service;

/** A single account balance change derived from a posting in an event. */
public record PostingDelta(String accountId, long deltaMinor) {
}

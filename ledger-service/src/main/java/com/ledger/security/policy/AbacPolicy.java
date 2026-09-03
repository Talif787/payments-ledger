package com.ledger.security.policy;

import java.util.Set;

/**
 * Attribute-based access control for money movement. Beyond "may this caller post
 * transactions" (which RBAC scopes answer), this enforces limits carried in the
 * caller's own token: a maximum amount they may move in one transaction, and an
 * optional allowlist of accounts they may debit. Pure and deterministic.
 */
public final class AbacPolicy {

    /**
     * @param subject          the authenticated caller
     * @param debitAccountIds  accounts money would leave in this transaction
     * @param totalDebitMinor  total minor units leaving those accounts
     */
    public AbacDecision permitTransaction(AccessSubject subject,
                                          Set<String> debitAccountIds,
                                          long totalDebitMinor) {
        if (subject.maxTxnMinor() > 0 && totalDebitMinor > subject.maxTxnMinor()) {
            return AbacDecision.deny(
                    "amount " + totalDebitMinor + " exceeds token limit " + subject.maxTxnMinor());
        }
        if (!subject.allowedAccounts().isEmpty()) {
            for (String account : debitAccountIds) {
                if (!subject.allowedAccounts().contains(account)) {
                    return AbacDecision.deny("account " + account + " not permitted for this token");
                }
            }
        }
        return AbacDecision.permit();
    }
}

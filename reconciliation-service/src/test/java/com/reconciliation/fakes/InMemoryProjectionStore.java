package com.reconciliation.fakes;

import com.reconciliation.application.port.ProjectionStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class InMemoryProjectionStore implements ProjectionStore {
    private final Set<UUID> consumed = new HashSet<>();
    private final Map<String, Long> balances = new HashMap<>();

    @Override
    public boolean markConsumed(UUID eventId) {
        return consumed.add(eventId);
    }

    @Override
    public void applyDelta(String accountId, long deltaMinor) {
        balances.merge(accountId, deltaMinor, Long::sum);
    }

    @Override
    public Map<String, Long> derivedBalances() {
        return Map.copyOf(balances);
    }
}

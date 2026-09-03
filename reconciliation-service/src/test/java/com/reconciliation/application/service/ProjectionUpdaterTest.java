package com.reconciliation.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reconciliation.fakes.InMemoryProjectionStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectionUpdaterTest {

    private final InMemoryProjectionStore store = new InMemoryProjectionStore();
    private final ProjectionUpdater updater = new ProjectionUpdater(store);

    @Test
    void appliesFirstDeliveryAndIgnoresDuplicate() {
        UUID eventId = UUID.randomUUID();
        var deltas = List.of(new PostingDelta("a", -1000), new PostingDelta("b", 1000));

        assertThat(updater.apply(eventId, deltas)).isTrue();
        assertThat(updater.apply(eventId, deltas)).isFalse();

        assertThat(store.derivedBalances()).containsEntry("a", -1000L).containsEntry("b", 1000L);
    }

    @Test
    void accumulatesDistinctEvents() {
        updater.apply(UUID.randomUUID(), List.of(new PostingDelta("a", -1000), new PostingDelta("b", 1000)));
        updater.apply(UUID.randomUUID(), List.of(new PostingDelta("a", -500), new PostingDelta("b", 500)));

        assertThat(store.derivedBalances()).containsEntry("a", -1500L).containsEntry("b", 1500L);
    }
}

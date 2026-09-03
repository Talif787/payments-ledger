package com.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraud.domain.Activity;
import com.fraud.domain.RawFeatures;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class WindowAggregatorTest {

    private final WindowAggregator agg = new WindowAggregator();
    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    @Test
    void computesWindowedCountsSumsAndCounterparties() {
        List<Activity> recent = List.of(
                new Activity(NOW.minusSeconds(30), 1000, "cp1", "t1"),
                new Activity(NOW.minusSeconds(120), 2000, "cp2", "t2"),
                new Activity(NOW.minusSeconds(1800), 3000, "cp1", "t3"),
                new Activity(NOW.minusSeconds(7200), 9999, "cp3", "t4"));

        RawFeatures f = agg.compute(recent, "cpNew", NOW);

        assertThat(f.txnCount1m()).isEqualTo(1);
        assertThat(f.txnCount5m()).isEqualTo(2);
        assertThat(f.txnCount1h()).isEqualTo(3);
        assertThat(f.amountSum1hMinor()).isEqualTo(6000);
        assertThat(f.distinctCounterparties1h()).isEqualTo(2);
        assertThat(f.secondsSinceLastTxn()).isEqualTo(30);
        assertThat(f.newCounterparty()).isTrue();
    }

    @Test
    void knownCounterpartyIsNotNew() {
        List<Activity> recent = List.of(new Activity(NOW.minusSeconds(60), 1000, "cp1", "t1"));
        assertThat(agg.compute(recent, "cp1", NOW).newCounterparty()).isFalse();
    }

    @Test
    void noActivityYieldsMaxRecency() {
        assertThat(agg.compute(List.of(), "cpX", NOW).secondsSinceLastTxn()).isEqualTo(Long.MAX_VALUE);
    }
}

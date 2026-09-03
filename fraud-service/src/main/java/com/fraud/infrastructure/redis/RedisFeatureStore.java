package com.fraud.infrastructure.redis;

import com.fraud.application.WindowAggregator;
import com.fraud.application.port.FeatureStore;
import com.fraud.domain.Activity;
import com.fraud.domain.RawFeatures;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed streaming feature store. Each account has a sorted set of recent
 * activity scored by event time. Reads fetch the retention window and delegate to
 * the pure WindowAggregator, so the feature math is the same code the offline
 * tests verify. Writes are idempotent because the member encodes the transaction
 * id, so re-delivered events do not double count.
 */
@Repository
public class RedisFeatureStore implements FeatureStore {

    private static final String KEY_PREFIX = "fraud:acct:";

    private final StringRedisTemplate redis;
    private final WindowAggregator aggregator;
    private final long retentionSeconds;

    public RedisFeatureStore(StringRedisTemplate redis,
                             WindowAggregator aggregator,
                             @Value("${fraud.feature-retention-seconds:7200}") long retentionSeconds) {
        this.redis = redis;
        this.aggregator = aggregator;
        this.retentionSeconds = retentionSeconds;
    }

    private static String key(String accountId) {
        return KEY_PREFIX + accountId;
    }

    @Override
    public void recordActivity(String accountId, Activity a) {
        String member = encode(a);
        double score = a.at().toEpochMilli();
        String key = key(accountId);
        redis.opsForZSet().add(key, member, score);
        // Trim anything older than the retention window.
        double cutoff = Instant.now().toEpochMilli() - retentionSeconds * 1000.0;
        redis.opsForZSet().removeRangeByScore(key, 0, cutoff);
    }

    @Override
    public RawFeatures computeRawFeatures(String accountId, String proposedCounterparty, Instant now) {
        double min = now.toEpochMilli() - retentionSeconds * 1000.0;
        double max = now.toEpochMilli();
        Set<ZSetOperations.TypedTuple<String>> entries =
                redis.opsForZSet().rangeByScoreWithScores(key(accountId), min, max);

        List<Activity> recent = new ArrayList<>();
        if (entries != null) {
            for (ZSetOperations.TypedTuple<String> e : entries) {
                Double s = e.getScore();
                String v = e.getValue();
                if (s == null || v == null) continue;
                recent.add(decode(v, s.longValue()));
            }
        }
        return aggregator.compute(recent, proposedCounterparty, now);
    }

    // member layout: txnId|amountMinor|counterpartyId  (counterparty may be empty)
    private static String encode(Activity a) {
        String cp = a.counterpartyId() == null ? "" : a.counterpartyId();
        return a.txnId() + "|" + a.amountMinor() + "|" + cp;
    }

    private static Activity decode(String member, long epochMillis) {
        String[] parts = member.split("\\|", 3);
        String txnId = parts.length > 0 ? parts[0] : "";
        long amount = parts.length > 1 ? Long.parseLong(parts[1]) : 0L;
        String cp = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
        return new Activity(Instant.ofEpochMilli(epochMillis), amount, cp, txnId);
    }
}

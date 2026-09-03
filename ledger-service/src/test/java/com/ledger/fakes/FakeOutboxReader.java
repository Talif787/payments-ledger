package com.ledger.fakes;

import com.ledger.application.model.OutboxRecord;
import com.ledger.application.port.out.OutboxReader;
import java.util.ArrayList;
import java.util.List;

public final class FakeOutboxReader implements OutboxReader {
    private final List<OutboxRecord> pending = new ArrayList<>();
    public final List<Long> marked = new ArrayList<>();
    public final List<Long> failed = new ArrayList<>();

    public void seed(OutboxRecord... records) {
        for (OutboxRecord r : records) pending.add(r);
    }

    @Override
    public List<OutboxRecord> claimUnpublished(int batchSize) {
        int n = Math.min(batchSize, pending.size());
        List<OutboxRecord> batch = new ArrayList<>(pending.subList(0, n));
        return batch;
    }

    @Override public void markPublished(long id) { marked.add(id); }
    @Override public void recordFailure(long id, String error) { failed.add(id); }
    @Override public long backlogSize() { return pending.size(); }
}

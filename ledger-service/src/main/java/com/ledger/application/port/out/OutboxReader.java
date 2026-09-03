package com.ledger.application.port.out;

import com.ledger.application.model.OutboxRecord;
import java.util.List;

/**
 * Reads and advances the transactional outbox for the relay. Implementations
 * claim rows with row-level locks that skip already-claimed rows, so multiple
 * relay workers can run without publishing the same event twice.
 */
public interface OutboxReader {

    /** Claims up to {@code batchSize} unpublished rows in id order, locking them. */
    List<OutboxRecord> claimUnpublished(int batchSize);

    /** Marks a row as published after its event has been acknowledged by the log. */
    void markPublished(long id);

    /** Records a failed publish attempt for observability without losing the row. */
    void recordFailure(long id, String error);

    /** Number of rows still awaiting publication (for backlog metrics). */
    long backlogSize();
}

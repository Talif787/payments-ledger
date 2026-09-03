package com.ledger.infrastructure.persistence;

import com.ledger.application.port.out.TransactionRunner;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a unit of work in a SERIALIZABLE database transaction and retries the
 * whole unit on serialization failures (which Spring surfaces as
 * {@link TransientDataAccessException}). Retrying at this boundary is what makes
 * serializable isolation practical: conflicting concurrent transactions abort
 * and are transparently replayed rather than surfacing errors to callers.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringTransactionRunner.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_BACKOFF_MILLIS = 5L;

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    }

    @Override
    public <T> T inSerializableTransaction(Supplier<T> work) {
        TransientDataAccessException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return transactionTemplate.execute(status -> work.get());
            } catch (TransientDataAccessException e) {
                last = e;
                log.warn("Serialization conflict on attempt {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                backoff(attempt);
            }
        }
        throw last;
    }

    private void backoff(int attempt) {
        try {
            long jitter = (long) (Math.random() * BASE_BACKOFF_MILLIS);
            Thread.sleep((BASE_BACKOFF_MILLIS * attempt) + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during transaction backoff", ie);
        }
    }
}

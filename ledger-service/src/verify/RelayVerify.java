package com.ledger.verify;

import com.ledger.application.model.OutboxRecord;
import com.ledger.application.service.OutboxRelayService;
import com.ledger.application.service.TopicResolver;
import com.ledger.fakes.FakeEventPublisher;
import com.ledger.fakes.FakeOutboxReader;
import java.time.Instant;
import java.util.UUID;

public final class RelayVerify {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        TopicResolver topics = new TopicResolver("ledger.accounts.v1", "ledger.transactions.v1");

        check("account event routes to accounts topic",
                topics.resolve("account.opened.v1").equals("ledger.accounts.v1"));
        check("transaction event routes to transactions topic",
                topics.resolve("ledger.transaction.posted.v1").equals("ledger.transactions.v1"));
        check("unknown event type is rejected", throwsIae(() -> topics.resolve("mystery.v1")));

        // Happy path: two events published and marked, keyed by aggregate id
        var reader = new FakeOutboxReader();
        var publisher = new FakeEventPublisher();
        var acct = rec(1, "account.opened.v1", "acc-1");
        var txn = rec(2, "ledger.transaction.posted.v1", "txn-1");
        reader.seed(acct, txn);
        var relay = new OutboxRelayService(reader, publisher, topics);

        int published = relay.publishBatch(10);
        check("publishBatch reports two published", published == 2);
        check("both events sent to the log", publisher.sent.size() == 2);
        check("account event on accounts topic", publisher.sent.get(0).topic().equals("ledger.accounts.v1"));
        check("txn event on transactions topic", publisher.sent.get(1).topic().equals("ledger.transactions.v1"));
        check("events keyed by aggregate id", publisher.sent.get(1).key().equals("txn-1"));
        check("both rows marked published", reader.marked.size() == 2 && reader.marked.contains(1L) && reader.marked.contains(2L));

        // Failure path: publish of the second event fails; it must not be marked
        var reader2 = new FakeOutboxReader();
        var publisher2 = new FakeEventPublisher();
        var a = rec(10, "account.opened.v1", "acc-2");
        var b = rec(11, "ledger.transaction.posted.v1", "txn-2");
        reader2.seed(a, b);
        publisher2.failOnEvent(b.eventId());
        var relay2 = new OutboxRelayService(reader2, publisher2, topics);

        boolean threw = false;
        try { relay2.publishBatch(10); } catch (RuntimeException e) { threw = true; }
        check("publish failure propagates so the transaction can roll back", threw);
        check("failed event was not marked published", !reader2.marked.contains(11L));

        System.out.println();
        System.out.println("RELAY RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static OutboxRecord rec(long id, String type, String aggregate) {
        return new OutboxRecord(id, UUID.randomUUID(), type, aggregate, "{}", Instant.now());
    }

    private static boolean throwsIae(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }
}

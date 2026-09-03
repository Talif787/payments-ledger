package com.reconciliation.application.port;

import com.reconciliation.domain.AccountSnapshot;
import java.util.List;

/**
 * Read-only view into the ledger's authoritative state. Implemented against a
 * read replica of the ledger database; this service never writes to it.
 */
public interface LedgerReadPort {
    List<AccountSnapshot> loadAccountSnapshots();
}

package com.ledger.application.port.out;

import com.ledger.domain.account.AccountId;
import com.ledger.domain.transaction.TransactionId;

public interface IdGenerator {
    AccountId newAccountId();
    TransactionId newTransactionId();
}

package com.ledger.application.port.in;

import java.util.Objects;

public interface CreateAccountUseCase {

    Result create(Command command);

    record Command(String currency, boolean allowOverdraft) {
        public Command {
            Objects.requireNonNull(currency, "currency");
        }
    }

    record Result(String accountId, String currency, String status) {}
}

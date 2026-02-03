package org.graph.domain.application.usecase.provider;

import org.graph.domain.entities.transaction.Transaction;

public interface TransactionsPublished {
    void submitTransaction(Transaction tx);
}

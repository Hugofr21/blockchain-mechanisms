package org.graph.adapter.provider;

import org.graph.domain.application.transaction.Transaction;

public interface TransactionsPublished {
    void submitTransaction(Transaction tx);
}

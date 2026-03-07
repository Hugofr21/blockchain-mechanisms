package org.graph.application.usecase.provider;

import org.graph.domain.entities.transaction.Transaction;

public interface ITransactionsPublished {
    void submitTransaction(Transaction tx);
}

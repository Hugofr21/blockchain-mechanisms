package org.graph.gateway.provider;

import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.transaction.Transaction;

public interface IConsensusEngine {
    void executeConsensus(Block block, int difficulty, int numThreads);
    boolean validateConsensus(Block block, int difficulty);
    boolean validateTransactions(Iterable<Transaction> transactions);
}

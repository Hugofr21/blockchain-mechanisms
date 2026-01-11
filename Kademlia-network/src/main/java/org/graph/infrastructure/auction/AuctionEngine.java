package org.graph.infrastructure.auction;

import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.entities.auctions.AuctionState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionEngine {
    private Map<String, AuctionState> auctions;

    public AuctionEngine() {
        this.auctions = new ConcurrentHashMap<>();
    }

    private void processTransactions(Transaction tx, long timestamp) {


    }
}

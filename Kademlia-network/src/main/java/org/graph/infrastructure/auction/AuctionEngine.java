package org.graph.infrastructure.auction;

import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.infrastructure.network.message.auction.AuctionOpType;
import org.graph.infrastructure.network.message.auction.AuctionPayload;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionEngine {
    private final Map<String, AuctionState> ledger;

    public AuctionEngine() {
        this.ledger = new ConcurrentHashMap<>();
    }

    public void processTransaction(Transaction tx, long blockTimestamp) throws Exception {
        AuctionPayload payload = (AuctionPayload) tx.getData();

        if (payload.operation == AuctionOpType.CREATE) {
            // O ID do leilão é o Hash da Transação (Garante unicidade global)
            String newAuctionId = tx.getTxId();

            long endTime = blockTimestamp + (payload.durationSeconds * 1000);

            AuctionState newState = new AuctionState(
                    newAuctionId,
                    tx.getSenderId(), // Extraído da chave pública
                    payload.startPrice,
                    endTime
            );

            ledger.put(newAuctionId, newState);
            System.out.println("Auction Created: " + newAuctionId);

        } else if (payload.operation == AuctionOpType.BID) {
            applyBid(payload, tx.getSenderId(), blockTimestamp);
        }
    }

    private void applyBid(AuctionPayload payload, BigInteger bidderId, long blockTime) throws Exception {
        AuctionState state = ledger.get(payload.targetAuctionId);

        if (state == null) {
            throw new IllegalStateException("Auction not found: " + payload.targetAuctionId);
        }

        if (blockTime > state.getEndTimestamp()) {
            state.closeAuction();
            throw new IllegalStateException("Auction expired");
        }

        if (payload.bidAmount.compareTo(state.getCurrentHighestBid()) <= 0) {
            throw new IllegalStateException("Bid must be higher than current: " + state.getCurrentHighestBid());
        }

        state.updateState(payload.bidAmount, bidderId);
        System.out.println("New High Bid on " + payload.targetAuctionId + ": " + payload.bidAmount);
    }


    public Map<String, AuctionState> getWorldState() {
        return ledger;
    }
}

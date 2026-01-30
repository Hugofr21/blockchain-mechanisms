package org.graph.adapter.auction;

import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.application.transaction.TransactionType;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.auctions.Bid;
import org.graph.domain.utils.HashUtils;
import org.graph.adapter.network.message.auction.AuctionOpType;
import org.graph.adapter.network.message.auction.AuctionPayload;
import org.graph.adapter.p2p.Peer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionEngine {
    private final Map<String, AuctionState> ledger;

    public AuctionEngine() {
        this.ledger = new ConcurrentHashMap<>();
    }

    public Map<String, AuctionState> getWorldState() {
        return ledger;
    }


    public void createdLocalAuctions(BigDecimal startPrice, Peer myself) {
        long durationMillis = 24L * 60L * 60L * 1000L;
        long endTime = System.currentTimeMillis() + durationMillis;
        String auctionId = HashUtils.calculateSha256(myself.getMyself().getNodeId().value().toString() + startPrice + endTime);

        AuctionState payload = new AuctionState(
                auctionId,
                myself.getMyself().getNodeId().value(),
                startPrice,
                endTime
        );

        Transaction tx = new Transaction(
                TransactionType.AUCTION_CREATED,
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                payload,
                myself.getMyself().getNodeId().value()
        );

        ledger.put(auctionId, payload);

    }

    public void placeBid(String auctionId, BigDecimal bidValue, Peer  peer) {
        AuctionState genesisAuction = ledger.get(auctionId);
        if (genesisAuction.isOpen()) {
            Bid newBid = new Bid(genesisAuction.getOwnerId(), bidValue,  peer.getHybridLogicalClock().getPhysicalClock(),
                    peer.getMyself().getNodeId().value());
            genesisAuction.updateState(bidValue, newBid.newBidderId());
        }
    }

    public void processTransactionRemote(Transaction tx, long blockTimestamp) throws Exception {
        AuctionPayload payload = (AuctionPayload) tx.getData();

        if (payload.operation == AuctionOpType.CREATE) {

            String newAuctionId = tx.getTxId();

            long endTime = blockTimestamp + (payload.durationSeconds * 1000);

            AuctionState newState = new AuctionState(
                    newAuctionId,
                    tx.getSenderId(),
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

}

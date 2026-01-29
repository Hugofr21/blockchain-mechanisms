package org.graph.infrastructure.auction;

import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.auctions.Bid;
import org.graph.domain.entities.p2p.Node;
import org.graph.domain.entities.p2p.NodeId;
import org.graph.domain.utils.HashUtils;
import org.graph.infrastructure.network.message.auction.AuctionOpType;
import org.graph.infrastructure.network.message.auction.AuctionPayload;
import org.graph.infrastructure.networkTime.HybridLogicalClock;
import org.graph.infrastructure.p2p.Peer;

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


    public void createdLocalAuctions(BigDecimal startPrice, NodeId systemOwnerId) {
        long durationMillis = 24L * 60L * 60L * 1000L;
        long endTime = System.currentTimeMillis() + durationMillis;
        String auctionId = HashUtils.calculateSha256(systemOwnerId.toString() + startPrice + endTime);

        AuctionState genesisAuction = new AuctionState(
                auctionId,
                systemOwnerId.value(),
                startPrice,
                endTime
        );
        ledger.put(auctionId, genesisAuction);

    }

    public void placeBid(String auctionId, BigDecimal bidValue, Peer  peer) {
        AuctionState genesisAuction = ledger.get(auctionId);
        if (genesisAuction.isOpen()) {
            Bid newBid = new Bid(genesisAuction.getOwnerId(), bidValue,  peer.getHybridLogicalClock().getPhysicalClock(),
                    peer.getMyself().getNodeId().value());
            genesisAuction.updateState(bidValue, newBid.getNewBidderId());
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

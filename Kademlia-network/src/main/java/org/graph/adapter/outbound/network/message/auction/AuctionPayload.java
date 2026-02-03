package org.graph.adapter.outbound.network.message.auction;

import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.auctions.Bid;

import java.io.Serial;
import java.io.Serializable;

public class AuctionPayload implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private AuctionOpType operation;
    private String itemDescription;
    private AuctionState auctionState;
    private Bid bid;

    public static AuctionPayload create(String desc, AuctionState state) {
        AuctionPayload p = new AuctionPayload();
        p.operation = AuctionOpType.CREATE;
        p.itemDescription = desc;
        p.auctionState  = state;
        return p;
    }

    public static AuctionPayload bid(Bid b) {
        AuctionPayload p = new AuctionPayload();
        p.operation = AuctionOpType.BID;
        p.bid = b;
        return p;
    }

    public AuctionState getAuctionStateRemote() {
        return auctionState;
    }
    public Bid getBidRemote() {
        return bid;
    }
    public AuctionOpType getOperation() {
        return operation;
    }
    public String getItemDescription() {return itemDescription;}


    @Override
    public String toString() {
        if (operation == AuctionOpType.CREATE) {
            String desc = (itemDescription != null) ? itemDescription : "N/A";
            String price = (auctionState != null) ? String.valueOf(auctionState.getMinPrice()) : "?";
            return String.format("[OP: CREATE] Item: '%s' | Start: %s", desc, price);

        } else if (operation == AuctionOpType.BID) {

            if (bid != null) {
                String shortId = (bid.auctionId().length() > 8) ? bid.auctionId().substring(0, 8) + ".." : bid.auctionId();
                return String.format("[OP: BID] Auction: %s | Value: %s", shortId, bid.bidPrice());
            }
        }
        return "UNKNOWN OP";
    }
}

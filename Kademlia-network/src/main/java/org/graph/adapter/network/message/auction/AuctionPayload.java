package org.graph.adapter.network.message.auction;

import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.auctions.Bid;

import java.io.Serializable;
import java.math.BigDecimal;

public class AuctionPayload implements Serializable {
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
}

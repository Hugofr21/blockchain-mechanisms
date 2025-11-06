package org.graph.domain.entities.auctions;

import org.graph.domain.utils.HashUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class Auctions {
    private BigInteger auctionId;
    private List<Bid> bids;
    private Bid currentHighBid;
    private BigInteger ownerId;
    private double bidPriceMin;
    private Long endAuctionTimestamp;

    public Auctions(BigInteger ownerId, double bidPriceMin) {
        this.bidPriceMin = bidPriceMin;
        this.bids = new ArrayList<Bid>();
        this.currentHighBid = null;
        this.ownerId = ownerId;
        this.endAuctionTimestamp = System.currentTimeMillis() + (10 * 60 * 60 * 1000);
        String merge = ownerId.toString() + bidPriceMin + endAuctionTimestamp;
        this.auctionId = HashUtils.sha256(merge);
    }

    public BigInteger getAuctionId() {
        return auctionId;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public Bid getCurrentHighBid() {
        return currentHighBid;
    }


    public BigInteger getOwnerId() {
        return ownerId;
    }

    public Long getEndAuctionTimestamp() {
        return endAuctionTimestamp;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }

    public void setCurrentHighBid(Bid currentHighBid) {
        this.currentHighBid = currentHighBid;
    }


    @Override
    public String toString() {
        return "Auctions{" +
                "auctionId=" + auctionId +
                ", bids=" + bids +
                ", currentHighBid=" + currentHighBid +
                ", ownerId=" + ownerId +
                ", timestamp=" + endAuctionTimestamp +
                '}';
    }

}

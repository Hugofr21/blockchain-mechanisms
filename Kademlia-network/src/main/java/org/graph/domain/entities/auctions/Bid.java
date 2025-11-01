package org.graph.domain.entities.auctions;

import java.math.BigInteger;

public class Bid {
    private BigInteger auctionId;
    private double bidPrice;
    private long throwTimestamp;

    public Bid(BigInteger auctionId, double bidPrice, long throwTimestamp) {
        this.auctionId = auctionId;
        this.bidPrice = bidPrice;
        this.throwTimestamp = throwTimestamp;
    }

    public BigInteger getAuctionId() { return auctionId; }
    public double getBidPrice() { return bidPrice; }
    public long getThrowTimestamp() { return throwTimestamp; }


    @Override
    public String toString() {
        return "Bid{" +
                "auctionId=" + auctionId +
                ", bidPrice=" + bidPrice +
                ", throwTimestamp=" + throwTimestamp +
                '}';
    }

}

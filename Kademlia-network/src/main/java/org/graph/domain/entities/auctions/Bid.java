package org.graph.domain.entities.auctions;

import org.graph.domain.utils.HashUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Bid {
    private final BigInteger newBidderId;
    private final BigInteger auctionId;
    private final BigDecimal bidPrice;
    private final long throwTimestamp;

    public Bid(BigInteger auctionId, BigDecimal bidPrice, long throwTimestamp,  BigInteger newBidderId) {
        this.auctionId = auctionId;
        this.bidPrice = bidPrice;
        this.throwTimestamp = throwTimestamp;
        this.newBidderId = newBidderId;
    }

    public BigInteger getAuctionId() { return auctionId; }
    public BigDecimal getBidPrice() { return bidPrice; }
    public long getThrowTimestamp() { return throwTimestamp; }
    public BigInteger getNewBidderId() { return newBidderId; }


    @Override
    public String toString() {
        return "Bid{" +
                "auctionId=" + auctionId +
                ", bidPrice=" + bidPrice +
                ", throwTimestamp=" + throwTimestamp +
                '}';
    }

}

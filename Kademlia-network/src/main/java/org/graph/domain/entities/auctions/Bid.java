package org.graph.domain.entities.auctions;

import java.math.BigDecimal;
import java.math.BigInteger;

public record Bid(BigInteger auctionId, BigDecimal bidPrice, long throwTimestamp, BigInteger newBidderId) {


    @Override
    public String toString() {
        return "Bid{" +
                "auctionId=" + auctionId +
                ", bidPrice=" + bidPrice +
                ", throwTimestamp=" + throwTimestamp +
                '}';
    }

}

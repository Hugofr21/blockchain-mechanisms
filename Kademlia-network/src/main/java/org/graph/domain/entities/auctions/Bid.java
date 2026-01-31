package org.graph.domain.entities.auctions;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record Bid(String auctionId, BigDecimal bidPrice, long throwTimestamp, BigInteger newBidderId) implements Serializable {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public String toString() {
        String dateString = formatter.format(Instant.ofEpochMilli(throwTimestamp));
        String shortId = newBidderId.toString().substring(0, Math.min(newBidderId.toString().length(), 8)) + "...";

        return String.format("[%s] Bidder: %s | Amount: %s", dateString, shortId, bidPrice);
    }
}
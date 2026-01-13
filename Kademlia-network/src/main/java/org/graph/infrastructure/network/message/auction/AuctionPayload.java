package org.graph.infrastructure.network.message.auction;

import java.io.Serializable;
import java.math.BigDecimal;

public class AuctionPayload implements Serializable {
    public AuctionOpType operation;
    public String itemDescription;
    public BigDecimal startPrice;
    public long durationSeconds;
    public String targetAuctionId;
    public BigDecimal bidAmount;

    // Construtor auxiliar para Criação
    public static AuctionPayload create(String desc, BigDecimal price, long duration) {
        AuctionPayload p = new AuctionPayload();
        p.operation = AuctionOpType.CREATE;
        p.itemDescription = desc;
        p.startPrice = price;
        p.durationSeconds = duration;
        return p;
    }

    // Construtor auxiliar para Lance
    public static AuctionPayload bid(String auctionId, BigDecimal amount) {
        AuctionPayload p = new AuctionPayload();
        p.operation = AuctionOpType.BID;
        p.targetAuctionId = auctionId;
        p.bidAmount = amount;
        return p;
    }
    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public String getTargetAuctionId() {
        return targetAuctionId;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public AuctionOpType getOperation() {
        return operation;
    }
}

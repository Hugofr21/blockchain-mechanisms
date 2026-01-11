package org.graph.domain.entities.auctions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;


public class AuctionState {
    private final String auctionId; // Derivado do Hash da Tx de criação
    private final BigInteger ownerId;
    private final BigDecimal minPrice;
    private final long endTimestamp; // Validado pelo timestamp do BLOCO, não do sistema

    // Estado Mutável (O que muda a cada novo bloco)
    private BigDecimal currentHighestBid;
    private BigInteger currentWinnerId;
    private boolean isOpen;


    public AuctionState(String auctionId, BigInteger ownerId, BigDecimal minPrice, long endTimestamp) {
        this.auctionId = auctionId;
        this.ownerId = ownerId;
        this.minPrice = minPrice;
        this.endTimestamp = endTimestamp;
        this.currentHighestBid = minPrice; // Começa no preço mínimo
        this.currentWinnerId = null;
        this.isOpen = true;
    }

    public boolean isOpen() { return isOpen; }
    public long getEndTimestamp() { return endTimestamp; }
    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public String getAuctionId() { return auctionId; }
    public BigInteger getOwnerId() { return ownerId; }
    public BigDecimal getMinPrice() { return minPrice; }

    public void updateState(BigDecimal newAmount, BigInteger newBidderId) {
        this.currentHighestBid = newAmount;
        this.currentWinnerId = newBidderId;
    }

    public void closeAuction() {
        this.isOpen = false;
    }


    @Override
    public String toString() {
        return "AuctionState{" +
                "auctionId='" + auctionId + '\'' +
                ", ownerId=" + ownerId +
                ", minPrice=" + minPrice +
                ", endTimestamp=" + endTimestamp +
                ", currentHighestBid=" + currentHighestBid +
                ", currentWinnerId=" + currentWinnerId +
                ", isOpen=" + isOpen +
                ", currentWinner=" + currentWinnerId +
                ", currentHighestBid=" + currentHighestBid +
                ", isOpen=" + isOpen +
                "}";
    }
}

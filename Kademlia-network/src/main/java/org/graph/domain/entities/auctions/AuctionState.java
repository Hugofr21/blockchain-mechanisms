package org.graph.domain.entities.auctions;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class AuctionState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String auctionId;
    private final BigInteger ownerId;
    private final BigDecimal minPrice;
    private final long endTimestamp;
    private final List<Bid> bidHistory;
    private BigDecimal currentHighestBid;
    private BigInteger currentWinnerId;
    private boolean isOpen;


    public AuctionState(String auctionId, BigInteger ownerId, BigDecimal minPrice, long endTimestamp) {
        this.auctionId = auctionId;
        this.ownerId = ownerId;
        this.minPrice = minPrice;
        this.endTimestamp = endTimestamp;
        this.currentHighestBid = minPrice;
        this.currentWinnerId = null;
        this.isOpen = true;
        this.bidHistory = new ArrayList<>();
    }

    public boolean isOpen() { return isOpen; }
    public long getEndTimestamp() { return endTimestamp; }
    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public String getAuctionId() { return auctionId; }
    public BigInteger getOwnerId() { return ownerId; }
    public BigDecimal getMinPrice() { return minPrice; }
    public List<Bid> getBidHistory() {return bidHistory; }

    public void addSuccessfulBid(Bid bid) {
        if (bid != null && !this.bidHistory.contains(bid)) {
            this.bidHistory.add(bid);
            this.currentHighestBid = bid.bidPrice();
            this.currentWinnerId = bid.newBidderId();
        }
    }

    public void closeAuction() {
        this.isOpen = false;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append(" AUCTION DETAILS\n");
        sb.append("========================================\n");
        sb.append(" ID:          ").append(auctionId).append("\n");
        sb.append(" Owner:       ").append(ownerId).append("\n");
        sb.append(" Status:      ").append(isOpen ? "OPEN" : "CLOSED").append("\n");
        sb.append(" Price:       ").append(currentHighestBid).append("\n");
        sb.append(" Expires at:  ").append(new java.util.Date(endTimestamp)).append("\n");
        sb.append("----------------------------------------\n");
        sb.append(" BID HISTORY (").append(bidHistory.size()).append(" bids)\n");
        sb.append("----------------------------------------\n");

        if (bidHistory.isEmpty()) {
            sb.append(" No bids yet.\n");
        } else {
            for (Bid bid : bidHistory) {
                sb.append(" ").append(bid.toString()).append("\n");
            }
        }
        sb.append("========================================\n");
        return sb.toString();
    }
}

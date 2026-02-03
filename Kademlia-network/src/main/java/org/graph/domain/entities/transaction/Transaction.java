package org.graph.domain.entities.transaction;

import org.graph.domain.entities.valueobject.utils.HashUtils;
import org.graph.adapter.outbound.network.message.auction.AuctionPayload;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Base64;

public class Transaction implements Serializable {
    private final String txId;
    private final TransactionType type;
    private PublicKey sender;
    private BigInteger ownerId;
    private AuctionPayload data;
    private final long timestamp;
    private byte[] signature;

    public Transaction(TransactionType type, PublicKey sender, AuctionPayload data, BigInteger ownerId) {
        this.type = type;
        this.sender = sender;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.ownerId = ownerId;
        this.txId = HashUtils.calculateSha256(getDataSign());
    }

    public Transaction(TransactionType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        String data = String.format("%s%d",
                type.toString(),
                timestamp
        );
        this.txId = HashUtils.calculateSha256(data);
    }

    public String getTxId() {
        return txId;
    }

    public TransactionType getType() {
        return type;
    }

    public PublicKey getSender() {
        return sender;
    }

    public AuctionPayload getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDataSign(){
        return String.format("%s%s%s%d%s",
                type.toString(),
                Base64.getEncoder().encodeToString(sender.getEncoded()),
                data.toString(),
                timestamp,
                ownerId
        );
    }
    public BigInteger getSenderId() {
        return ownerId;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
    @Override
    public String toString() {
        String timeStr = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(timestamp));
        String shortId = (txId != null && txId.length() > 8) ? txId.substring(0, 8) + "..." : "null";
        String senderStr = (ownerId != null) ? ownerId.toString() : "System/Genesis";
        if (senderStr.length() > 10) senderStr = senderStr.substring(0, 10) + "..";

        String sigStatus = (signature != null && signature.length > 0) ? "[Sig:OK]" : "[Sig:NO]";

        String payloadSummary = "N/A";
        if (data != null) {
            payloadSummary = data.toString();
        }

        return String.format("Tx{%s | %-10s | From:%-12s | Time:%s | %s | Data: %s}",
                shortId,
                type,
                senderStr,
                timeStr,
                sigStatus,
                payloadSummary
        );
    }
}

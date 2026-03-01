package org.graph.domain.entities.transaction;

import org.graph.domain.valueobject.utils.HashUtils;
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
    private long nonce;

    public Transaction(TransactionType type, PublicKey sender, AuctionPayload data, BigInteger ownerId, long nonce, long timestamp) {
        this.type = type;
        this.sender = sender;
        this.data = data;
        this.timestamp =  timestamp ;
        this.ownerId = ownerId;
        this.nonce = nonce;
        this.txId = HashUtils.calculateSha256(getDataSign());
    }

    public Transaction(TransactionType type, long timestamp) {
        this.type = type;
        this.timestamp = timestamp;
        this.nonce = 0;
        this.txId = HashUtils.calculateSha256(getDataSign());
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

    public String getDataSign() {
        String senderStr = (sender != null) ? Base64.getEncoder().encodeToString(sender.getEncoded()) : "SYSTEM";
        String ownerStr = (ownerId != null) ? ownerId.toString() : "0";
        String dataStr = (data != null) ? data.toString() : "";

        return String.format("%s%s%s%d%s%d",
                type.toString(),
                senderStr,
                dataStr,
                timestamp,
                ownerStr,
                nonce
        );
    }

    public BigInteger getSenderId() {
        return ownerId;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public long getNonce() {
        return nonce;
    }

    public byte[] getSignature() {
        return signature;
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

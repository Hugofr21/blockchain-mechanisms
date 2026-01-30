package org.graph.domain.application.transaction;

import org.graph.domain.utils.HashUtils;
import org.graph.adapter.network.message.auction.AuctionPayload;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Base64;

public class Transaction {
    private String txId;
    private TransactionType type;
    private PublicKey sender;
    private BigInteger ownerId;
    private AuctionPayload data;
    private long timestamp;
    private byte[] signature;

    public Transaction(TransactionType type, PublicKey sender, AuctionPayload data, BigInteger ownerId) {
        this.type = type;
        this.sender = sender;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.ownerId = ownerId;
        this.txId = HashUtils.calculateSha256(dataSign());
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


    private String dataSign(){
        return String.format("%s%s%s%d%s",
                type.toString(),
                Base64.getEncoder().encodeToString(sender.getEncoded()),
                data.toString(),
                timestamp,
                ownerId
        );
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

    public byte[] getSignature() {
        return signature;
    }

    public BigInteger getSenderId() {
        return ownerId;
    }
}

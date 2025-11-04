package org.graph.transaction;

import org.graph.utils.HashUtils;

import java.time.Instant;
import java.util.Objects;

public class Transaction {
    private String id;
    private String from;
    private String to;
    private double amount;
    private long timestamp;

    public Transaction(String from, String to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.timestamp = Instant.now().toEpochMilli();
        this.id = generateId();
    }

    private String generateId() {
        return HashUtils.calculateSha256(from + to + amount + timestamp);
    }

    public String toHashString() {
        return from + to + amount + timestamp;
    }

    public String getId() { return id; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return String.format("%s -> %s: %.2f", from, to, amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

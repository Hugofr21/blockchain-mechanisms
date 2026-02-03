package org.graph.domain.application.block;


import java.io.Serializable;

public class BlockHeader implements Serializable {
    private final int version;
    private final String previousBlockHash;
    private final String merkleRoot;
    private final long timestamp;
    private final int difficulty;
    private long nonce;

    public BlockHeader(int version, String previousBlockHash, String merkleRoot, int difficulty) {
        this.version = version;
        this.previousBlockHash = previousBlockHash;
        this.merkleRoot = merkleRoot;
        this.difficulty = difficulty;
        this.timestamp = System.currentTimeMillis();
        this.nonce = 0;
    }

    public String getPayloadForMining() {
        return version + previousBlockHash + merkleRoot + timestamp + difficulty;
    }

    public int getVersion() {
        return version;
    }


    public void setNonce(long nonce) {this.nonce = nonce;}
    public long getNonce() { return nonce; }
    public int getDifficulty() { return difficulty; }
    public String getPreviousBlockHash() { return previousBlockHash; }
    public long getTimestamp() { return timestamp; }
    public String getMerkleRoot() { return merkleRoot; }

    @Override
    public String toString() {
        return "BlockHeader{" +
                "version=" + version +
                ", previousBlockHash='" + previousBlockHash + '\'' +
                ", merkleRoot='" + merkleRoot + '\'' +
                ", timestamp=" + timestamp +
                ", difficulty=" + difficulty +
                ", nonce=" + nonce +
                '}';
    }
}

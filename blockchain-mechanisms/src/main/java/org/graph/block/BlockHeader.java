package org.graph.block;

import org.graph.merkleTree.MerkleTree;
import org.graph.transaction.Transaction;

import java.time.Instant;
import java.util.List;

public class BlockHeader {
    private int version;
    private String previousBlockHash;
    private MerkleTree merkleRoot;

    public BlockHeader(int version, String previousBlockHash, List<Transaction> transactions) {
        this.version = version;
        this.previousBlockHash = previousBlockHash;
        try {
            this.merkleRoot = new MerkleTree(transactions);
        }catch (Exception e){
            System.out.println("[ERROR] Merkle Tree: " + e.getMessage());
        }
    }

    public int getVersion() {
        return version;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public String getMerkleRoot() {
        return merkleRoot.getRootHash();
    }

    public List<Transaction> getAllTransactions() {
        return merkleRoot.getTransactions();
    }

}

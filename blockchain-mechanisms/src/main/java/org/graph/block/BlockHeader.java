package org.graph.block;

import org.graph.merkleTree.MerkleTree;
import org.graph.pow.MinerResult;

public class BlockHeader {
    private int version;
    private String currentBlockHash;
    private String previousBlockHash;
    private MerkleTree tree;
    private long timestamp;
    private long nonce;

    public BlockHeader(String hashPrev) {

    }

    public int getVersion() {
        return version;
    }

    public String getCurrentBlockHash() {
        return currentBlockHash;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public MerkleTree getTree() {
        return tree;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getNonce() {
        return nonce;
    }

    public MinerResult minerHashSystem(int numberThread){
        return null;
    }


}

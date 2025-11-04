package org.graph.merkleTree;

import org.graph.transaction.Transaction;

public class NodeElement {
    NodeElement parentLeaf;
    NodeElement parentRight;
    String hash;
    Transaction transaction;

    public NodeElement(String hash, Transaction transaction ) {
        this.hash = hash;
        this.transaction = transaction;
    }

    public NodeElement(String combined){
        this.hash = combined;
    }

    public NodeElement getParent() {
        return parentLeaf == null ? parentRight : parentLeaf;
    }

    public String getHash() {
        return hash;
    }

}

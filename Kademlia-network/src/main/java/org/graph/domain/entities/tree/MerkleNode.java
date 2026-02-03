package org.graph.domain.entities.tree;

import org.graph.domain.valueobject.utils.HashUtils;

public class MerkleNode {
    private final String hash;
    private final MerkleNode left;
    private final MerkleNode right;
    private MerkleNode parent;

    public MerkleNode(String hash) {
        this.hash = hash;
        this.left = null;
        this.right = null;
        this.parent = null;
    }

    public MerkleNode(MerkleNode left, MerkleNode right) {
        this.left = left;
        this.right = right;
        this.hash = HashUtils.calculateSha256(left.hash + right.hash);
        if (left != null) left.parent = this;
        if (right != null) right.parent = this;
    }

    public String getHash() { return hash; }
    public MerkleNode getLeft() { return left; }
    public MerkleNode getRight() { return right; }
    public MerkleNode getParent() { return parent; }
    public boolean isLeaf() {return left == null && right == null;}

}

package org.graph.merkleTree;

import org.graph.transaction.Transaction;
import org.graph.utils.HashUtils;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MerkleTree {
    private NodeElement root;
    private List<NodeElement> leaves;

    public MerkleTree(List<Transaction> transactions) throws NoSuchAlgorithmException {
        this.leaves = new ArrayList<>();
        buildTree(transactions);
    }

    public String getRootHash() {
        return root.hash;
    }

    public List<Transaction> getTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        for (NodeElement leaf : leaves) {
            if (leaf.transaction != null) {
                transactions.add(leaf.transaction);
            }
        }
        return transactions;
    }

    private void buildTree(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("No transactions found!");
            return;
        }

        List<NodeElement> nodes = new ArrayList<>();
        for (Transaction tx : transactions) {
            String hash = HashUtils.calculateSha256(tx.toHashString());
            NodeElement leaf = new NodeElement(hash, tx);
            leaves.add(leaf);
            nodes.add(leaf);
        }

        if (nodes.size() % 2 != 0) {
            nodes.add(nodes.getLast());
        }

        while (nodes.size() > 1) {
            List<NodeElement> parentNodes = new ArrayList<>();

            for (int i = 0; i < nodes.size(); i += 2) {
                NodeElement left = nodes.get(i);
                NodeElement right = nodes.get(i + 1);

                String combinedHash = HashUtils.calculateSha256(left.hash + right.hash);
                NodeElement parent = new NodeElement(combinedHash);
                parent.parentLeaf = left;
                parent.parentRight = right;

                parentNodes.add(parent);
            }

            if (parentNodes.size() % 2 != 0 && parentNodes.size() > 1) {
                parentNodes.add(parentNodes.getLast());
            }

            nodes = parentNodes;
        }

        this.root = nodes.getFirst();
    }


    public List<String> proofFindNodeLeave(List<NodeElement> nodes, int targetIndex) {
        List<String> proof = new ArrayList<>();
        List<NodeElement> currentNodes = new LinkedList<>(nodes);

        int index = targetIndex;

        while (currentNodes.size() > 1) {

            if (currentNodes.size() % 2 != 0) {
                currentNodes.add(currentNodes.getLast());
            }

            int siblingIndex = (index % 2 == 0) ? index + 1 : index - 1;

            proof.add(currentNodes.get(siblingIndex).getHash());

            List<NodeElement> newLeaves = new ArrayList<>();
            for (int i = 0; i < currentNodes.size(); i+= 2) {
                NodeElement left = currentNodes.get(i);
                NodeElement right = currentNodes.get(i + 1) ;

                System.out.print("left: " + left.hash + " Right:" + right.hash);

                String combined = left.hash + right.hash;

                NodeElement merged = new NodeElement(combined);

                merged.parentLeaf = left;
                merged.parentLeaf = right;

                newLeaves.add(merged);

            }
            index = index / 2;
            currentNodes = newLeaves;

        }
        return proof;
    }

    public NodeElement findClosetNode(NodeElement search, String target){
        if (target != null && search != null) {

            if (search.hash.equals(target)) {
                return search;
            }

            NodeElement left = findClosetNode(search.parentLeaf, target);
            if (left != null) {
                return left;
            }

            NodeElement right = findClosetNode(search.parentRight, target);
            if (right != null) {
                return right;
            }
            return null;
        }
        return null;
    }

    public int depthMerkle(NodeElement node) {
        if (node == null) {
            return 0;
        }
        return Math.max(depthMerkle(node.parentLeaf), depthMerkle(node.parentRight)) + 1 ;
    }

    public void display(NodeElement node, String prefix, boolean isLeft) {
        if (node != null) {
            if (isLeft) {
                display(node.parentLeaf, prefix + "|    ", false);
            }

            System.out.println(prefix + (isLeft ? "└────────── " : "┌──────────── ") + node.hash);
            display(node.parentRight, prefix + (isLeft ? "    " : "│   "), true);

        }

    }

}

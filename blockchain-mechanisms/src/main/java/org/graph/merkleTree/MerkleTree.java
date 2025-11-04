package org.graph.merkleTree;

import org.graph.transaction.Transaction;
import org.graph.utils.HashUtils;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

}

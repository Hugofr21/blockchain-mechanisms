package org.graph;


import org.graph.transaction.Transaction;

import java.util.Arrays;
import java.util.List;

public class Server {
    public static void main(String[] args) {
        Blockchain blockchain = new Blockchain(5);
        blockchain.createGenesisBlock();

        List<Transaction> block1Tx = Arrays.asList(
                new Transaction("Alice", "Bob", 50.0),
                new Transaction("Bob", "Carol", 25.0),
                new Transaction("Carol", "Dave", 10.0)
        );
        blockchain.addBlock(block1Tx);

        List<Transaction> block2Tx = Arrays.asList(
                new Transaction("Dave", "Eve", 5.0),
                new Transaction("Eve", "Frank", 3.0),
                new Transaction("Frank", "Alice", 2.0)
        );
        blockchain.addBlock(block2Tx);

        blockchain.printBlockchain();

    }
}
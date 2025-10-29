package org.graph;

import org.graph.block.Block;

import java.util.concurrent.ConcurrentLinkedDeque;

public class Blockchain {
    private ConcurrentLinkedDeque<Block> blockchain;
    private int currentDifficulty;

    public Blockchain() {
        this.blockchain = new ConcurrentLinkedDeque<>();
    }


    private void createdGenesisBlock() {

    }


    public boolean addBlockchain(int difficulty) {
        String prev = blockchain.getLast().getHeader().getCurrentBlockHash();
        Block newBlock = new Block(blockchain.size() , prev, difficulty);
        newBlock.getHeader().minerHashSystem(difficulty);
        blockchain.addLast(newBlock);
        return true;
    }





}

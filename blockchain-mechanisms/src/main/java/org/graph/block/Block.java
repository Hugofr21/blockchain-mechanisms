package org.graph.block;

import org.graph.merkleTree.MerkleTree;

public class Block {
    private int numberBlock;
    private BlockHeader header;
    private int difficulty;

    public Block(int numberBlock, String hashPrev, int difficulty) {
        this.numberBlock = numberBlock;
        this.header = new BlockHeader(hashPrev);
        this.difficulty = difficulty;
    }

    public int getNumberBlock() { return numberBlock; }
    public BlockHeader getHeader() { return header; }


    private boolean isValidBlock(){
        return true;
    }


}

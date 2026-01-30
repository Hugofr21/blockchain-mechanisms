package org.graph.gateway;

import org.graph.adapter.blockchain.BlockchainEngine;
import org.graph.domain.application.block.Block;
import org.graph.gateway.validator.Validator;

import static org.graph.adapter.utils.Constants.MAX_TRANSACTIONS;
import static org.graph.adapter.utils.Constants.NETWORK_DIFFICULTY;

public class NetworkGateway {
    private final BlockchainEngine blockchainEngine;
    private final Validator validator;

    public NetworkGateway() {
        this.blockchainEngine = new BlockchainEngine(NETWORK_DIFFICULTY , MAX_TRANSACTIONS);
        this.validator = new Validator();
    }

    public void incomingBlock(Block block) {
        if (!validator.validateBlockchain(block, NETWORK_DIFFICULTY)){
            System.out.println("Invalid block");
            return;
        }
        try {
            blockchainEngine.receiveBlockFromPeer(block);
        }catch (Exception e){
            System.err.println("Error while receiving block from the peer : " + e.getMessage());
        }

    }
}

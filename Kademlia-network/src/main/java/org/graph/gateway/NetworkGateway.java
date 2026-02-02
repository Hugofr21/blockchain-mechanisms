package org.graph.gateway;

import org.graph.adapter.auction.AuctionEngine;
import org.graph.adapter.blockchain.BlockchainEngine;
import org.graph.domain.application.block.Block;
import org.graph.gateway.block.BlockStateRemote;
import org.graph.gateway.validator.Validator;

import static org.graph.adapter.utils.Constants.MAX_TRANSACTIONS;
import static org.graph.adapter.utils.Constants.NETWORK_DIFFICULTY;

public class NetworkGateway {
    private final BlockchainEngine blockchainEngine;
    private final AuctionEngine auctionEngine;
    private final Validator validator;

    public NetworkGateway() {
        this.blockchainEngine = new BlockchainEngine(NETWORK_DIFFICULTY , MAX_TRANSACTIONS);
        this.validator = new Validator();
        this.auctionEngine = new AuctionEngine(this.blockchainEngine);
        this.blockchainEngine.addBlockListener(this.auctionEngine);
    }

    public BlockchainEngine getBlockchainEngine() {
        return blockchainEngine;
    }
    public AuctionEngine getAuctionEngine() {return auctionEngine;}

    /**
     * 1- Processa a receção de um bloco remoto e determina o seu estado final após validação e persistência.
     *
     * 2- O fluxo de processamento segue, de forma estrita, as etapas abaixo:
     * Primeiro, o bloco é submetido às validações de segurança obrigatórias, incluindo prova de trabalho
     * (PoW) e verificação criptográfica da assinatura. Falhas nesta fase resultam na rejeição imediata
     * do bloco, sem qualquer efeito colateral no estado interno.
     *
     * 3- Em seguida, verifica-se se o bloco já é conhecido pelo nó, seja na cadeia principal, seja no
     * conjunto de blocos órfãos. Caso o bloco já exista, o processamento é interrompido e o estado
     * apropriado é retornado, evitando duplicação de dados e processamento redundante.
     *
     * 4- Se o bloco for válido e ainda desconhecido, tenta-se a sua inserção. Esta operação é responsável
     * por persistir o bloco, decidindo internamente se ele será anexado à cadeia principal ou armazenado
     * como órfão, dependendo da disponibilidade do seu bloco pai no momento da receção.
     *
     * 5- Apenas após o bloco ter sido efetivamente armazenado é determinado o estado de retorno. Se o bloco
     * não for o bloco Genesis e o seu pai não existir na cadeia principal, o estado retornado indica
     * ausência do bloco pai (MISSING_PARENT), sinalizando que o bloco foi aceite como órfão e que o nó
     * deve solicitar o bloco pai em falta à rede.
     *
     * 6- Se nenhuma das condições anteriores se aplicar, significa que o bloco foi adicionado com sucesso
     * à cadeia principal, seja por já possuir o pai ou por ser o bloco Genesis, e o estado de sucesso
     * correspondente é retornado.
     *
     * @param block bloco recebido remotamente que será validado, armazenado e avaliado quanto ao seu estado
     * @return BlockStateRemote,  estado final do processamento do bloco no contexto da sincronização remota
     */

    public BlockStateRemote processIncomingBlock(Block block) {

        if (!validator.validateBlockchain(block, NETWORK_DIFFICULTY)){
            System.out.println("[GATEWAY] Invalid block (PoW/Incorrect signature).");
            return BlockStateRemote.INVALID;
        }

        if (blockchainEngine.getBlockOrganizer().contains(block.getCurrentBlockHash())) {
            return BlockStateRemote.EXISTS;
        }

        try {
            blockchainEngine.receiveBlockFromPeer(block);
        } catch (Exception e) {
            System.err.println("[GATEWAY] Error processing block: " + e.getMessage());
            return BlockStateRemote.INVALID;
        }

        if (block.getNumberBlock() > 0 &&
                !blockchainEngine.getBlockOrganizer().isParentInChain(block.getHeader().getPreviousBlockHash())) {

            return BlockStateRemote.MISSING_PARENT;
        }

        return BlockStateRemote.ADDED;
    }

}

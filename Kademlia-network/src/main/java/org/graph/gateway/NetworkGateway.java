package org.graph.gateway;

import org.graph.application.usecase.auction.AuctionEngine;
import org.graph.application.usecase.blockchain.BlockchainEngine;
import org.graph.adapter.outbound.network.message.block.InventoryPayload;
import org.graph.adapter.outbound.network.message.block.InventoryType;
import org.graph.adapter.provider.IEventDispatcher;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.gateway.block.BlockStateRemote;
import org.graph.gateway.validator.SecurityValidator;
import org.graph.server.Peer;

import static org.graph.adapter.utils.Constants.MAX_TRANSACTIONS;
import static org.graph.adapter.utils.Constants.NETWORK_DIFFICULTY;

public class NetworkGateway {
    private final BlockchainEngine blockchainEngine;
    private final AuctionEngine auctionEngine;
    private final SecurityValidator securityValidator;
    private IEventDispatcher dispatcher;
    private Peer myself;

    public NetworkGateway(Peer myself) {
        this.myself = myself;
        this.blockchainEngine = new BlockchainEngine(NETWORK_DIFFICULTY , MAX_TRANSACTIONS, myself);
        this.securityValidator = new SecurityValidator();
        this.auctionEngine = new AuctionEngine(this.blockchainEngine);
        this.blockchainEngine.addBlockListener(this.auctionEngine);
        this.blockchainEngine.setNonceProvider(this.auctionEngine::getNextNonce);
    }

    public BlockchainEngine getBlockchainEngine() {
        return blockchainEngine;
    }
    public AuctionEngine getAuctionEngine() {return auctionEngine;}
    public Peer getMyself(){return myself;}

    public void setNetworkDependencies(IEventDispatcher dispatcher, Peer myself) {
        this.dispatcher = dispatcher;
        this.myself = myself;
    }

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

        if (!securityValidator.validateBlockchain(block, NETWORK_DIFFICULTY)){
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

    /**
     * Processa o envio de cada bloco após a sua mineração.
     *
     * Os blocos não são armazenados numa fila de pendentes; no entanto, sempre que
     * exista pelo menos um bloco disponível para envio, este deve ser imediatamente
     * transmitido.
     * @param block que será enviado para a rede.
     */
    public void announceBlockToNetwork(Block block){
        InventoryPayload inv = new InventoryPayload(InventoryType.BLOCK, block.getCurrentBlockHash());
        Message msg = new Message(
                MessageType.INV_DATA,
                inv,
                myself.getHybridLogicalClock()
        );
        dispatcher.broadcastExcept(msg, null);
    }

}

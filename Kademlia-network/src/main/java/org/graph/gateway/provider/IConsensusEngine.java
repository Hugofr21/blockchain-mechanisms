package org.graph.gateway.provider;

import org.graph.domain.entities.block.Block;

public interface IConsensusEngine {

    /**
     * Sela o bloco de acordo com as regras do protocolo instanciado.
     * Em PoW, inicia a força bruta para encontrar o Nonce.
     * Em PoS, assina o bloco e injeta a prova de capital (Stake).
     * Em BFT, submete o bloco para a ronda de propostas do consórcio.
     */
    void sealBlock(Block block);

    /**
     * Valida matematicamente se a prova de consenso anexada ao bloco é legítima.
     * Verifica as hashes contra a dificuldade em PoW, ou as assinaturas do validador em PoS.
     */
    boolean validateProof(Block block, Block parentBlock);

    /**
     * Regra de Escolha de Bifurcação (Fork Choice Rule).
     * Determina deterministicamente qual bloco vence em caso de colisões na rede.
     */
    boolean isWinningChain(Block newTip, Block currentTip);

    /**
     * Numero dificulade da rede para obter determinado prefix compuatacional em uma identificado valido deternimisticamente.
     */
    int difficulty();
}
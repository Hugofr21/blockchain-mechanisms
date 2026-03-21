package org.graph.gateway.provider;

import org.graph.domain.entities.block.Block;

public interface IConsensusEngine {

    /**
     * A bloco conforme as regras do protocolo ativo (ex: miner).
     * <p>Em Proof-of-Work (PoW), inicia a busca exaustiva para encontrar o Nonce.
     * Em Proof-of-Stake (PoS), assina o bloco e anexa a prova de participação (Stake).
     * Em Byzantine Fault Tolerance (BFT), submete o bloco à rodada de propostas do consórcio.</p>
     *
     * @param block a ser selado de acordo com o protocolo
     */
    void sealBlock(Block block);

    /**
     * Valida a prova de consenso anexada a um bloco.
     * <p>Em Proof-of-Work (PoW), verifica se o hash do bloco satisfaz a dificuldade especificada.
     * Em Proof-of-Stake (PoS), valida as assinaturas dos validadores que atestam a legitimidade do bloco.</p>
     *
     * @param block bloco cujo consenso será validado
     * @return verdadeiro se a prova de consenso for válida; falso caso contrário
     */
    boolean validateProof(Block block, Block parentBlock);
    /**
     * Regra de escolha de bifurcação (Fork Choice Rule).
     * Determina de forma determinística qual bloco deve ser considerado válido
     * em caso de concorrência de blocos na rede.
     *
     * @param newTip bloco recém-recebido da rede
     * @param currentTip bloco atualmente considerado como o último da cadeia válida
     * @return bloco escolhido como o novo tip da cadeia
     */

    boolean isWinningChain(Block newTip, Block currentTip);

    /**
     * Calcula a dificuldade computacional necessária para gerar um identificador válido
     * que satisfaça um prefixo específico de acordo com critérios determinísticos da rede.
     * @return valor inteiro correspondente à dificuldade computacional requerida
     */
    int difficulty();
}
package org.graph.adapter.utils;

import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.node.Node;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.policy.EventTypePolicy;
import org.graph.server.Peer;

import java.math.BigInteger;

public class KademliaUtils {
    /**
     * Validação baseada em Content-Addressable Storage (CAS).
     *
     * <p>Quando um objeto é recebido da rede, é calculado o hash criptográfico
     * (ex.: SHA-256) do seu conteúdo. Esse hash deve corresponder exatamente
     * à chave Kademlia ({@code expectedKey}) utilizada no pedido do objeto.</p>
     *
     * <p>Este mecanismo garante a integridade e autenticidade do conteúdo,
     * assegurando que o objeto devolvido corresponde criptograficamente
     * ao identificador solicitado.</p>
     *
     * <p>Se o hash calculado não corresponder à chave esperada, o objeto é
     * considerado inválido ou forjado e deve ser rejeitado. O nó que forneceu
     * o objeto poderá ser penalizado no sistema de confiança.</p>
     *
     * @param expectedKey Chave Kademlia esperada, previamente anunciada na rede
     *                    através da arquitetura PUB/SUB, permitindo que os vizinhos
     *                    DHT saibam qual objeto deve corresponder à chave.
     * @param data Objeto recebido da rede pelos vizinhos mais próximos no k-bucket,
     *             correspondente ao identificador do objeto.
     * @param sender Identificação do nó que enviou o objeto, utilizada para
     *               verificar a sua autenticidade e reputação.
     */
    public static boolean validateDataIntegrity(BigInteger expectedKey, Object data, Class<?> type, Node sender, Peer myself) {
        String expectedHashHex = expectedKey.toString(16);

        try {
            if (type.equals(Block.class)) {
                Block block = (Block) data;

                String recalculatedHash = block.recalculateHash();

                if (!recalculatedHash.equals(expectedHashHex)) {
                    System.err.println("[SECURITY] Poisoned Block received! Recalculated hash does not match requested key from " + sender.getPort());
                    myself.getReputationsManager().reportEvent(sender.getNodeId().value(), EventTypePolicy.INVALID_BLOCK);
                    return false;
                }

            } else if (type.equals(Transaction.class)) {
                Transaction tx = (Transaction) data;

                String recalculatedTxId = tx.calculateTransactionId();

                if (!recalculatedTxId.equals(expectedHashHex)) {
                    System.err.println("[SECURITY] Poisoned Transaction received! ID mismatch from " + sender.getPort());
                    myself.getReputationsManager().reportEvent(sender.getNodeId().value(), EventTypePolicy.MALICIOUS_BEHAVIOR);
                    return false;
                }

                if (tx.getSender() == null || tx.getSignature() == null) {
                    System.err.println("[SECURITY] Unsigned transaction rejected from " + sender.getPort());
                    return false;
                }

                if (!CryptoUtils.verifySignature(tx.getSender(), recalculatedTxId, tx.getSignature())) {
                    System.err.println("[SECURITY] Transaction signature forgery detected from " + sender.getPort());
                    myself.getReputationsManager().reportEvent(sender.getNodeId().value(), EventTypePolicy.MALICIOUS_BEHAVIOR);
                    return false;
                }

            } else if (type.equals(AuctionState.class)) {
                AuctionState state = (AuctionState) data;

                String recalculatedAuctionId = state.calculateStateHash();

                if (!recalculatedAuctionId.equals(expectedHashHex)) {
                    System.err.println("[SECURITY] Poisoned AuctionState received! Hash mismatch from " + sender.getPort());
                    myself.getReputationsManager().reportEvent(sender.getNodeId().value(), EventTypePolicy.MALICIOUS_BEHAVIOR);
                    return false;
                }


            }
            return true;

        } catch (Exception e) {
            System.err.println("[SECURITY] Validation crash caused by malformed data from " + sender.getPort() + ". Rejecting.");
            myself.getReputationsManager().reportEvent(sender.getNodeId().value(), EventTypePolicy.MALICIOUS_BEHAVIOR);
            return false;
        }
    }

}

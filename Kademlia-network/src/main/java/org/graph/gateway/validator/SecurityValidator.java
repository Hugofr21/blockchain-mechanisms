package org.graph.gateway.validator;

import org.graph.adapter.utils.CryptoUtils;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.transaction.Transaction;

import java.util.HashSet;
import java.util.Set;


/**
 * O validator constitui um mecanismo de verificação responsável por assegurar
 * que os blocos e os seus componentes respeitam rigorosamente as regras
 * estruturais, criptográficas e funcionais definidas pelo protocolo do sistema.
 * Esta camada atua como proteção contra modificações maliciosas ou inconsistências
 * que possam comprometer a integridade da blockchain ou violar as regras de
 * negócio estabelecidas.
 *
 * A validação não se limita à verificação da ligação entre blocos. O processo
 * inclui também a confirmação de que os parâmetros de mineração aplicados ao
 * bloco — nomeadamente o nível de dificuldade e os valores utilizados durante
 * o processo de geração do nonce — respeitam as configurações definidas pelo
 * sistema e não foram manipulados após a mineração.
 *
 * O processo de validação envolve múltiplas verificações independentes. Em
 * primeiro lugar, é recalculado o hash criptográfico do bloco a partir do
 * digest dos seus campos estruturais (por exemplo, hash do bloco anterior,
 * conjunto de transações, timestamp e nonce). O resultado obtido deve coincidir
 * exatamente com o hash armazenado no bloco, garantindo que nenhum dos campos
 * foi alterado após a sua criação.
 *
 * Em seguida, é verificada a validade da prova de trabalho (Proof of Work),
 * confirmando que o hash do bloco satisfaz o nível de dificuldade imposto pelo
 * protocolo, tipicamente expresso através da presença de um determinado número
 * de zeros consecutivos no prefixo do hash.
 *
 * Por fim, todas as transações contidas no bloco devem ser individualmente
 * verificadas através da assinatura digital associada ao emissor. A assinatura
 * é validada utilizando a chave pública do remetente, assegurando autenticidade,
 * integridade da mensagem e impossibilidade de repúdio.
 *
 * Caso qualquer uma destas verificações falhe, o bloco deve ser considerado
 * inválido e rejeitado pelo sistema.
 */

public class SecurityValidator {

    public boolean validateBlockchain(Block block, int currentDifficulty) {
        if (block == null) {
            return false;
        }

        if (block.getNumberBlock() == 0) {
            String recalculatedHash = block.recalculateHash();
            if (!recalculatedHash.equals(block.getCurrentBlockHash())) {
                System.err.println("[SECURITY] Genesis hash integrity failed!");
                return false;
            }
            if (isPoWValid(block.getCurrentBlockHash(), currentDifficulty)) {
                System.err.println("[SECURITY] Genesis PoW invalid!");
                return false;
            }
            return true;
        }


        String recalculatedHash = block.recalculateHash();
        if (!recalculatedHash.equals(block.getCurrentBlockHash())) {
            System.err.println("[SECURITY] Hash integrity failed! Fake hash detected.");
            System.err.println("[SECURITY] Declared: " + block.getCurrentBlockHash());
            System.err.println("[SECURITY] Real Hash: " + recalculatedHash);
            return false;
        }

        if (isPoWValid(block.getCurrentBlockHash(), currentDifficulty)) {
            System.err.println("[SECURITY] PoW invalid: " + block.getCurrentBlockHash());
            System.err.println("[SECURITY] Expected to start with: " + getTarget(currentDifficulty));
            return false;
        }

        Set<String> blockIds = new HashSet<>();
        for (Transaction tx : block.getTransactions()) {

            if (CryptoUtils.verifySignature(tx.getSender(), tx.getDataSign(), tx.getSignature())) {
                System.err.println("[SECURITY] Invalid transaction signature for TX: " + tx.getTxId());
                return false;
            }

            if (!blockIds.add(tx.getTxId())){
                System.err.println("[SECURITY] Duplicated transaction ID inside block: " + tx.getTxId());
                return false;
            }
        }

        return true;
    }


    private boolean isPoWValid(String currentBlockHash, int difficulty) {
        if (currentBlockHash == null) return true;
        String target = getTarget(difficulty);
        return !currentBlockHash.startsWith(target);
    }

    private String getTarget(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }
}
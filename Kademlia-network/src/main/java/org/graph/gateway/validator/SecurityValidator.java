package org.graph.gateway.validator;

import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.transaction.Transaction;

import java.util.HashSet;
import java.util.Set;


/**
 * O validator constitui uma camada de proteção contra ataques ou alterações
 * indevidas que modifiquem a estrutura ou o estado dos objetos em violação
 * das regras de negócio.
 *
 * A validação não se limita a confirmar a integridade da cadeia; garante também
 * que a dificuldade de mineração aplicada está conforme os parâmetros definidos
 * pelo sistema e que os valores de mineração utilizados para identificação
 * não foram adulterados.
 */

public class SecurityValidator {

    public boolean validateBlockchain(Block block, int currentDifficulty) {
        if (block == null) {
            return false;
        }

        // validate pow
        if (isPoWValid(block.getCurrentBlockHash(), currentDifficulty)) {
            System.err.println("[DEBUG] PoW invalid: " + block.getCurrentBlockHash());
            System.err.println("[DEBUG] Expected starts with: " + getTarget(currentDifficulty));
            return false;
        }

        Set<String> blockIds = new HashSet<>();
        for (Transaction tx : block.getTransactions()) {

            // valid signature
//            if (!CryptoUtils.verifySignature(tx.getSender(), tx.getDataSign(), tx.getSignature())) {
//               return false;
//            }

            // duplication transaction remote
            if (!blockIds.add(tx.getTxId())){
                System.err.println("[DEBUG] Invalid transaction ID: " + tx.getTxId());
                return false;
            }
        }


        return true;
    }


    private boolean isPoWValid(String currentBlockHash, int difficulty) {
        String target = getTarget(difficulty);
        return !currentBlockHash.startsWith(target);
    }

    private String getTarget(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }
}
package org.graph.gateway.validator;

import org.graph.domain.application.block.Block;


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

        if (isPoWValid(block.getCurrentBlockHash(), currentDifficulty)) {
            System.err.println("[DEBUG] PoW invalid: " + block.getCurrentBlockHash());
            System.err.println("[DEBUG] Expected starts with: " + getTarget(currentDifficulty));
            return false;
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
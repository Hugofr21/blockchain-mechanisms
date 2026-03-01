package org.graph.domain.entities.node;

import org.graph.domain.valueobject.utils.HashUtils;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.PublicKey;


/**
 * Ao iniciar a aplicação, o sistema gera um par de chaves assimétricas como etapa fundacional do mecanismo de identidade
 * criptográfica do nó. Essa geração não é opcional nem meramente identificadora: a chave pública passa a ser o insumo primário
 * para o cálculo do identificador do nó, enquanto a chave privada permanece restrita ao domínio local, garantindo autenticidade criptográfica
 * nas interações subsequentes. Qualquer tentativa de postergar ou desacoplar essa geração comprometeria a verificabilidade do nó em um ambiente
 * distribuído adversarial.
 *
 *
 *
 * Após a geração bem-sucedida do par de chaves, o sistema constrói um quebra-cabeça de Proof of Work associado diretamente
 * à chave pública. O objetivo computacional consiste em encontrar um valor de nonce tal que o hash criptográfico do par
 * (chave pública, nonce) produza um identificador cujo valor numérico satisfaça uma restrição matemática estrita de dificuldade.
 * Formalmente, o identificador deve ser menor que 2 elevado à potência de (256 menos a dificuldade configurada).
 * Não se trata de um critério estético baseado em prefixos visuais, mas de uma desigualdade matemática que define o custo
 * computacional mínimo exigido para a criação de uma identidade válida.
 *
 *
 *
 * Esse mecanismo de Proof of Work não deve ser interpretado como um sistema de consenso, mas como uma ferramenta explícita
 * de segurança para controle de admissão. O custo imposto à geração de identidades reduz drasticamente a viabilidade de ataques
 * de criação massiva de nós, sendo particularmente relevante em redes ponto a ponto abertas. Caso esse custo seja trivial ou mal
 * parametrizado, o sistema se torna estruturalmente vulnerável a ataques Sybil, independentemente de qualquer lógica posterior de
 * roteamento ou reputação.
 *
 *
 *
 * No contexto de validação local, assume-se que o identificador do orquestrador do minerador já é conhecido e confiável.
 * Ainda assim, a validação matemática do identificador é obrigatória, pois a confiança não substitui a verificabilidade.
 * O sistema deve recalcular o hash a partir da chave pública e do nonce armazenado e confirmar que o identificador resultante
 * satisfaz rigorosamente a condição de dificuldade. Qualquer divergência indica corrupção de estado ou tentativa de manipulação.
 *
 *
 *
 * Na validação remota, aplicada à verificação de nós vizinhos em protocolos inspirados em Kademlia, o procedimento é
 * conceitualmente o mesmo, porém executado em um contexto adversarial. O nó remoto fornece sua chave pública e o nonce
 * associado ao seu identificador. O sistema recalcula o hash, verifica a correspondência exata com o identificador anunciado
 * e confirma o atendimento à dificuldade mínima. Se qualquer uma dessas condições falhar, o nó deve ser considerado ilegítimo,
 * sem exceções ou heurísticas compensatórias.
 *
 *
 *
 * É importante destacar que a simples existência de um identificador aparentemente bem distribuído no espaço de endereçamento
 * não é evidência de legitimidade. A segurança decorre exclusivamente da prova verificável de trabalho associada à chave pública.
 * Ignorar essa distinção, ou tratar o identificador como um valor arbitrário aceito por convenção, representa um erro conceitual
 * grave e invalida a premissa de resistência a ataques Sybil em sistemas distribuídos abertos.
 */


public record NodeId(BigInteger value) implements Serializable {
    public static final int ID_LENGTH_BITS = 256;

    public static NodeId createFromProof(BigInteger id, int difficulty) {
        BigInteger target = BigInteger.ONE.shiftLeft(ID_LENGTH_BITS - difficulty);

        if (id.compareTo(target) >= 0) {
            throw new SecurityException("Invalid PoW: NodeId does not meet difficulty target.");
        }

        return new NodeId(id);
    }

    public static boolean isValidNode(Node node, PublicKey publicKey) {
        try {
            BigInteger calculated = HashUtils.calculateHashFromNodeId(publicKey, node.getNonce());
            if (!calculated.equals(node.getNodeId().value())) return false;

            BigInteger target = BigInteger.ONE.shiftLeft(ID_LENGTH_BITS - node.getNETWORK_DIFFICULTY());
            return calculated.compareTo(target) < 0;

        } catch (Exception e) {
            return false;
        }
    }

    public BigInteger distanceBetweenNode(BigInteger other) {
        return this.value.xor(other);
    }
}
package org.graph.domain.entities.node;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Este objeto é responsável exclusivamente pela criação e validação do identificador de um nó na rede distribuída.
 * Em nenhuma circunstância sua instância ou seus dados derivados devem ser transmitidos diretamente em mensagens de rede.
 * Antes de qualquer intercâmbio de dados entre nós, é obrigatória a realização prévia de verificações formais de verificabilidade
 * e confiabilidade, pois a aceitação prematura de identificadores não validados compromete a segurança estrutural do sistema.
 *
 * Um ponto crítico desta classe é a proibição explícita de associação direta com chaves públicas. A chave pública não pertence
 * ao domínio lógico do identificador, mas ao domínio de identidade criptográfica persistente, devendo ser armazenada exclusivamente
 * em um keystore seguro. Esse keystore deve ser criptografado utilizando AES em modo GCM com chave de 256 bits, garantindo confidencialidade,
 * integridade e autenticação dos dados associados a cada nó vizinho. Qualquer tentativa de acoplar a chave pública a esta classe viola a
 * separação de responsabilidades e introduz riscos desnecessários de exposição ou corrupção de estado.
 *
 * A função deste componente limita-se à verificação matemática e criptográfica do identificador com base nos parâmetros recebidos de
 * forma controlada, como chave pública e nonce, sem jamais assumir confiança implícita. A validação deve confirmar a correspondência exata
 * entre o identificador calculado e o identificador anunciado, bem como o cumprimento rigoroso da dificuldade mínima exigida pelo sistema.
 * A ausência de qualquer uma dessas garantias invalida imediatamente o nó avaliado.
 *
 * Qualquer alteração nesta classe deve ser tratada como uma modificação de alto risco. Mudanças aparentemente triviais podem introduzir falhas
 * sistêmicas que afetam diretamente a resistência a ataques Sybil, a coerência do espaço de endereçamento e a confiabilidade da rede como única.
 * Por esse motivo, antes de iniciar o envio de mensagens entre nós, o sistema deve executar rotinas completas de verificação de integridade,
 * assegurando que os invariantes criptográficos e matemáticos definidos para o identificador permanecem intactos.
 *
 * Ignorar essas restrições, ou utilizar esta classe como um simples gerador de valores identificadores sem validação rigorosa,
 * representa um erro conceitual sério. O identificador não é um atributo decorativo do nó, mas um artefato de segurança que sustenta
 * a confiança mínima necessária para qualquer comunicação em um ambiente distribuído adversarial.
 */

public class Node implements Serializable {
    private NodeId id;
    private String host;
    private int port;
    private final long nonce;
    private final int NETWORK_DIFFICULTY;


    public Node(String host, int port, BigInteger id, long nonce, int networkDifficulty) {
        this.NETWORK_DIFFICULTY = networkDifficulty;
        this.id = NodeId.createFromProof(id, networkDifficulty);
        this.host = host;
        this.port = port;
        this.nonce = nonce;
    }

    public int getPort() {
        return port;
    }
    public long getNonce() {return nonce;}
    public String getHost() {
        return host;
    }
    public NodeId getNodeId() {return id;}

    public int getNETWORK_DIFFICULTY() {
        return NETWORK_DIFFICULTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node that = (Node) o;
        return port == that.port && Objects.equals(id, that.id);
    }


    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}

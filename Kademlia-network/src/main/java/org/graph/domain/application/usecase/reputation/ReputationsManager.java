package org.graph.adapter.mechanism;

import org.graph.adapter.provider.IReputationsManager;
import org.graph.domain.entities.policy.EventType;
import org.graph.domain.entities.policy.reputation.ProofOfReputation;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proof-of-Reputation (PoR) — especificação de alto nível e considerações de segurança.
 *
 * Este módulo implementa mecanismos auxiliares para a construção e manutenção de um índice
 * de reputação distribuído que serve como insumo a políticas de seleção de validadores,
 * roteamento e mecanismos de recompensa/penalização. Reputação é definida aqui como uma
 * estimativa estatística do comportamento passado observável de um nó e não deve ser interpretada
 * como uma garantia determinística de comportamento futuro; por ser derivada de histórico, a
 * reputação é intrinsecamente suscetível a estratégias adversariais como comportamento oportunista
 * (“on-off”) e criação de múltiplas identidades (*Sybil*), razão pela qual o PoR integra sinais
 * adicionais como provas de recurso, métricas de atividade verificáveis e monitorização de anomalias.
 *
 * A seleção de nós para funções críticas (por exemplo, inclusão em comitês de consenso ou
 * encaminhamento privilegiado) combina aleatoriedade pseudo-critérios determinísticos verificáveis:
 * aleatoriedade para reduzir previsibilidade e risco de posicionamento adversarial; critérios
 * verificáveis para evitar que nós de baixo custo e identidades fabricadas suplantem nós legítimos.
 * A aplicação de aleatoriedade sem salvaguardas permite ataques de roteamento otimizado por adversários
 * de baixo custo; por isso, políticas de seleção devem impor limites por identidade verificada,
 * utilizar métricas sociais/topológicas quando disponíveis e empregar técnicas de detecção de Sybil.
 *
 * A política de expiração por inatividade utiliza decaimento temporal parametrizado e janelas de
 * reavaliação para distinguir churn legítimo de abandono malicioso. A remoção definitiva de um nó
 * exige múltiplos critérios convergentes: métricas de disponibilidade abaixo de um limiar durante
 * intervalos definidos, falhas verificadas de comportamento e oportunidade de revalidação,
 * evitando exclusões precipitadas que prejudiquem a disponibilidade e a diversidade da rede.
 *
 * Em face a ataques conhecidos, a arquitetura PoR recomenda uma defesa em profundidade: (i)
 * mitigação de Sybil por vínculos sociais e/ou teste de recursos; (ii) aleatoriedade controlada
 * nas funções de seleção/roteamento; (iii) monitorização e detecção de anomalias temporais; (iv)
 * políticas explícitas de penalização e recuperação que preservem liveness. Parâmetros sensíveis
 * (janelas de amostragem, constantes de decaimento, limiares de disponibilidade) devem ser
 * definidos por análise empírica, validados por simulação e sujeitos a governança operativa.
 */

public class ReputationsManager implements IReputationsManager {
    private Map<BigInteger, ProofOfReputation> reputationMap;

    public ReputationsManager(){
        this.reputationMap = new ConcurrentHashMap<>();
    }

    public ProofOfReputation getProofOfReputation(BigInteger nodeId){
        return reputationMap.computeIfAbsent(nodeId, k -> new ProofOfReputation());
    }


    public void reportEvent(BigInteger nodeId, EventType event){
        ProofOfReputation proof = getProofOfReputation(nodeId);
        double newScore = proof.recordEvent(event);
        if (event == EventType.INVALID_BLOCK || event == EventType.PING_FAIL) {
            System.out.printf("[REPUTATION] Node %s penalized (%s). Novo Score: %.2f%n", nodeId, event, newScore);
        }

    }

    public boolean removeProofOfReputation(BigInteger nodeId){
        return reputationMap.remove(nodeId) != null;
    }

    public boolean isTrusted(BigInteger nodeId){
        return getProofOfReputation(nodeId).getCurrentProofOfReputation() > -500.0;
    }



    public void applyDecayAll(BigInteger nodeId){
        reputationMap.values().forEach(ProofOfReputation::applyDecay);
    }


    @Override
    public double getTrustFactor(BigInteger nodeId) {
        if (!reputationMap.containsKey(nodeId)) {
            return 0.1;
        }
        return reputationMap.get(nodeId).getTrustFactor();
    }
}

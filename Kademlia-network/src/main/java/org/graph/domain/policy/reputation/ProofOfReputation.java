package org.graph.domain.policy.reputation;

import org.graph.domain.policy.EventType;

public class ProofOfReputation {
    private double currentProofOfReputation = 0.0;
    private double maxProofOfReputation = 1000.0;
    private double minProofOfReputation = -1000.0;

    private double weightPingSuccess = 1.0;
    private double weightFindNodeUseful = 5.0;
    private double weightPingFail = -10.0;
    private double weightInvalidData = -100.0;
    private double weightInvalidBlock = -500.0;

    private double decayFactor = 0.99;

    public ProofOfReputation() {}

    public synchronized double recordEvent(EventType event) {
        double delta = switch (event) {
            case PING_SUCCESS -> weightPingSuccess;
            case FIND_NODE_USEFUL -> weightFindNodeUseful;
            case PING_FAIL -> weightPingFail;
            case INVALID_DATA -> weightInvalidData;
            case INVALID_BLOCK -> weightInvalidBlock;
            default -> 0.0;
        };


        currentProofOfReputation = clamp(currentProofOfReputation + delta);
        return currentProofOfReputation;
    }

    public synchronized void setWeights(double pingSuccess, double findNodeUseful, double pingFail, double invalidData) {
        this.weightPingSuccess = pingSuccess;
        this.weightFindNodeUseful = findNodeUseful;
        this.weightPingFail = pingFail;
        this.weightInvalidData = invalidData;
    }


    private double clamp(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0;
        }
        if (v > maxProofOfReputation) return maxProofOfReputation;
        return Math.max(v, minProofOfReputation);
    }

    public synchronized void setBounds(double min, double max) {
        if (min >= max) throw new IllegalArgumentException("min must be < max");
        this.minProofOfReputation = min;
        this.maxProofOfReputation = max;
        this.currentProofOfReputation = clamp(this.currentProofOfReputation);
    }

    public double getCurrentProofOfReputation() {
        return currentProofOfReputation;
    }

    public synchronized double applyDecay() {
        currentProofOfReputation = clamp(currentProofOfReputation * decayFactor);
        return currentProofOfReputation;
    }

    // Método crucial para o S-Kademlia: Retorna 't' para a fórmula
    // Deve retornar um valor positivo para evitar problemas na divisão 1/t
    // Normaliza para um valor positivo onde 0.0 (neutro) vira 1.0 ou similar
    // Estratégia: Mapear [-1000, 1000] para (0, 2] ou similar, ou usar sigmoid
    // Abordagem simples: Se score <= 0, trust é muito baixo (ex: 0.1). Se > 0, escala.
    // Trust mínimo para nós desconhecidos ou ruins
    // Exemplo: Score 100 -> Trust 2.0; Score 1000 -> Trust 11.0
    public synchronized double getTrustFactor() {

        if (currentProofOfReputation <= 0) {
            return 0.1;
        }
        return 1.0 + (currentProofOfReputation / 100.0);
    }
}

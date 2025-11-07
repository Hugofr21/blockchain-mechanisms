package org.graph.domain.application.mechanism;


public class ProofOfReputation {
    private double currentProofOfReputation = 0.0;
    private double maxProofOfReputation = 1000.0;
    private double minProofOfReputation = -1000.0;

    private double weightPingSuccess = 1.0;
    private double weightFindNodeUseful = 5.0;
    private double weightPingFail = -10.0;
    private double weightInvalidData = -500.0;

    private double decayFactor = 0.99;

    public ProofOfReputation() {}

    public synchronized double recordEvent(EventType event) {
        double delta = switch (event) {
            case PING_SUCCESS -> weightPingSuccess;
            case FIND_NODE_USEFUL -> weightFindNodeUseful;
            case PING_FAIL -> weightPingFail;
            case INVALID_DATA -> weightInvalidData;
            default -> 0.0;
        };


        currentProofOfReputation = clamp(currentProofOfReputation + delta);
        return currentProofOfReputation;
    }

    private double clamp(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0;
        }
        if (v > maxProofOfReputation) return maxProofOfReputation;
        return Math.max(v, minProofOfReputation);
    }

    public synchronized void setWeights(double pingSuccess, double findNodeUseful, double pingFail, double invalidData) {
        this.weightPingSuccess = pingSuccess;
        this.weightFindNodeUseful = findNodeUseful;
        this.weightPingFail = pingFail;
        this.weightInvalidData = invalidData;
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
}

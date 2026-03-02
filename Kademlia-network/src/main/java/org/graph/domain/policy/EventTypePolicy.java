package org.graph.domain.policy;

public enum EventTypePolicy {
    PING_SUCCESS,       // +1 (Manutenção básica)
    FIND_NODE_USEFUL,   // +5 (Ajudou na Routing Table)
    VALID_TRANSACTION,  // +10 (Enviou transação válida)
    VALID_BLOCK,        // +50 (Minerou/Propagou bloco válido - GRANDE PRÉMIO)
    PING_FAIL,          // -5
    INVALID_BLOCK,      // -100
    INVALID_DATA        // -20
}

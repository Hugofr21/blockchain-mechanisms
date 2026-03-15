# Matriz de Observabilidade – *MetricsLogger*

O *MetricsLogger* expõe métricas de três camadas críticas do seu nó:

1. **Motor de Blockchain e Consenso** – saúde da replicação e do PoW;
2. **Segurança e Confiança (S‑Kademlia)** – detecção de adversários e reputação;
3. **Topologia P2P e Roteamento DHT** – eficiência da sobre‑posição K‑ademlia.

A tabela abaixo resume cada métrica, seu objetivo e como interpretá‑la em um cenário de “pico” ou anomalia.

---

## 1. Motor de Blockchain e Consenso (Estado)

| Métrica                  | Nome Prometheus                             | O que mede                                                       | Diagnóstico em pico/anomalia                                                                           |
| ------------------------- | ------------------------------------------- | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| **Chain Height**    | `blockchain_chain_height_blocks`          | Comprimento absoluto da cadeia principal validada pelo nó local | Estagnação → falha de consenso ou bloqueio de propagação de blocos                                 |
| **Mempool Size**    | `blockchain_mempool_size_transactions`    | Volume de transações pendentes em memória                     | Crescimento indefinido → excesso de taxa de submissão / dificuldade de PoW muito alta                 |
| **Broker Queue**    | `broker_event_queue_size_messages`        | Mensagens assíncronas na fila interna do nó Java               | Fila alta → back‑pressure, CPU insuficiente para processar criptografia/serialização                |
| **Mining Duration** | `blockchain_mine_duration_milliseconds`   | Tempo CPU gasto em cada tentativa de nonce                       | Picos abruptos → vulnerabilidade a spam de blocos (CPU saturada)                                       |
| **Chain Reorgs**    | `blockchain_reorganizations_reorgs_total` | Número de reorganizações de cadeia (forks resolvidos)         | Frequência alta → partição de rede, latência de propagação ou múltiplos mineradores concorrendo |

---

## 2. Segurança e Confiança – *S‑Kademlia* (IDS & Reputation)

| Métrica                                | Nome Prometheus                          | O que mede                                                                                      | Diagnóstico em pico/anomalia                                                                   |
| --------------------------------------- | ---------------------------------------- | ----------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| **Malicious Activities Detected** | `skademlia_malicious_activities_total` | Contagem de violações de protocolo (injeções de rotas falsas, assinaturas inválidas, etc.) | Pico → ataque adversário detectado e mitigado na borda da rede                                |
| **Peer Trust Score**              | `skademlia_peer_trust_score`           | Reputação histórica de cada par (0.0–1.0)                                                   | Queda → desvio de roteamento ou node comprometido; deve ser excluído da árvore de roteamento |

---

## 3. Topologia P2P e Roteamento – *DHT/K‑AdE­Mil­a*

| Métrica                                 | Nome Prometheus                                  | O que mede                                                    | Diagnóstico em pico/anomalia                                                      |
| ---------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| **Network Throughput – Inbound**  | `network_inbound_throughput_bytes_per_second`  | Volume de dados recebidos por segundo                         | Desbalanceamento (inbound >> outbound) → possível DDoS ou ataque de Sybil        |
| **Network Throughput – Outbound** | `network_outbound_throughput_bytes_per_second` | Volume de dados enviados por segundo                          | Desbalanceamento (outbound >> inbound) → disponibilidade de serviço comprometida |
| **Latency (RTT)**                  | `network_latency_rtt_seconds`                  | Tempo de ida e volta em transações RPC                      | Alta latência → falha de pesquisas DHT por timeout                               |
| **Routing Table Size**             | `dht_routing_table_kbucket_size`               | Número de vizinhos ativos em cada k‑bucket                  | Tamanho muito pequeno → Hop‑count elevado, roteamento ineficiente                |
| **Storage Size**                   | `dht_storage_size_entries`                     | Quantidade de blocos/transações armazenados localmente      | Distribuição desigual (`storage_size` homogêneo) → centralização de dados  |
| **Lookup Hops**                    | `kademlia_lookup_hops`                         | Prom. de saltos necessários para resolver chave              | `> O(log n)` → rota linear, indicativo de erro de distância XOR                |
| **Errors & Avg Hops**              | `kademlia_lookup_errors_total`                 | Número de falhas de lookup                                   | Inchaço → conectividade TCP instável ou pings/gaps não calibrados              |
| **Bootstrap Latency**              | `bootstrap_latency_seconds`                    | Tempo para alcançar o ponto de entrada e completar handshake | Latência muito alta → PoW intencional atrasando on‑board de atacantes           |
| **Bootstrap Success Rate**         | `bootstrap_success_total`                      | % de nós que completaram bootstrap                           | Falhas frequentes → handshake ou verificação de identidade falhando             |
| **K‑Bucket Convergence Rate**     | `kademlia_convergence_rate_nodes_per_second`   | Velocidade de estabilização das tabelas de roteamento       | Flutuação contínua → churn excessivo, perda de pacotes TCP                     |

---

## 4. Recomendações Práticas

1. **Alertas de LTV** – Para métricas de throughput e latency, configure alertas que sirvam de referência a longo prazo (percentil 95% ou 99%); picos inesperados indicam ataques ou sobrecarga.
2. **Thresholds Dinâmicos** – Use *persistence* na fila do broker para decidir sobre back‑pressure automático. Se `broker_event_queue_size_messages` > 90% da capacidade, reduza a taxa de gravação de logs.
3. **Segurança em Layer 1** – Unifique as métricas de *Malicious Activities* com logs de rede para correlacionar taxa de injeções falsas com variações de `skademlia_peer_trust_score`.
4. **Visão Holística** – Combine métricas de “Chain Height” com `blockchain_mempool_size_transactions` para detectar “stale blocks”; se a altura escalar não acompanha o backlog de mempool, a rede está estagnada.

---

### Conclusão

A existência de uma matriz de observabilidade detalhada como a acima transformará cada spike num “ponto fisiológico” do seu sistema, permitindo respostas proativas em vez de reativas. O *MetricsLogger* deve ser configurado para expor essas métricas ao seu agente Prometheus (ou outro back‑end), e os dashboards (Grafana, Kibana, etc.) devem refletir a lógica de diagnóstico triplamente: **Estado**, **Segurança** e **Topologia**.

Se precisar de ajuda para integrar o *MetricsLogger* ao seu stack CI/CD, ou se quiser automatizar vervificações sobre esses valores, basta perguntar!

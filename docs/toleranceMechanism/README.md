# Tolerance Mechanism

Este documento descreve, de forma didática e técnica, a distinção entre **criar uma transação** e **minerar um bloco** numa blockchain que utiliza Proof‑of‑Work, detalha quem realiza o trabalho de PoW e apresenta os mecanismos de tolerância que mantêm a rede operável mesmo em condições adversas.
Também são comparados três modelos de consenso (PoW, PoS e PBFT) para que você possa escolher o mais adequado ao seu caso de uso de leilões distribuídos.

## Transação vs. Mineração – O que realmente acontece?

### 1.1 O que acontece quando fazemos uma **Transação**?

| Etapa             | Descrição                                                                                                                                                                                                                                   | Custos computacionais                                 |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| **Ação**  | O nó**A** deseja fazer um lance num leilão. Ele instancia um objeto `Transaction`, preenche os campos (valor, identificador do leilão, timestamp, nonce da *transação*) e assina digitalmente com sua **chave privada**. | Apenas a geração da assinatura ECDSA (≈ µs‑ms). |
| **PoW?**    | **Não**. O usuário que cria a transação não executa Proof‑of‑Work. O esforço está limitado ao cálculo da assinatura.                                                                                                          | Nenhum gasto de energia significativo.                |
| **Destino** | A transação é propagada via**gossip/P2P** (JSON + Base64). Cada nó que a recebe verifica a assinatura, garante que não há *double‑spend* e a coloca na sua **Mempool** (ou `TransactionOrganizer`).                  | Processamento O(1) por nó.                           |

> **Resumo** – A transação é “leve”, gratuita e apenas transporta a intenção do usuário à rede.

---

### 1.2 O que acontece quando **Mineramos um Bloco** (Proof‑of‑Work)?

| Etapa                    | Descrição                                                                                                                                                                                                                                                                               | Custos computacionais                                       |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| **Objetivo**       | Consolidar um conjunto de transações pendentes da Mempool num **bloco** que será adicionado ao ledger.                                                                                                                                                                          |                                                             |
| **Input do bloco** | - Hash do bloco anterior (`prevHash`) `<br>` - Lista de transações selecionadas `<br>` - *Nonce* do bloco (valor aleatório que será variado) `<br>` - Timestamp (HLC)                                                                                                       |                                                             |
| **PoW**            | Repetidamente:`<br>`  1) Calcula `hash = SHA‑256(prevHash                                                                                                                                                                                                                              |                                                             |
| **Resultado**      | Quando o hash satisfaz a dificuldade, o bloco é considerado**valido**. O minerador (nó que encontrou o nonce) o anuncia para a rede.                                                                                                                                              | Apenas um hash final adicional para assinatura e broadcast. |
| **Propagação**   | Os nós vizinhos recebem o bloco, verificam:`<br>`  - Prova de trabalho (hash) `<br>`  - Assinaturas de cada transação `<br>`  - Consistência de `prevHash` `<br>` Se tudo ok, o bloco entra no seu **ledger** e as transações incluídas são removidas da Mempool. | Verificação O(N) onde N = número de txs no bloco.        |

> **Resumo** – A mineração é a “parte pesada” que garante a **finalização** das transações e a segurança da cadeia.

---

### 1.3 Quem faz o PoW? Existe algum “pedido” da rede?

Não há nenhum coordenador central que solicite trabalho. Cada nó que está configurado como **minerador** age de forma autônoma:

1. **Observação local** – O *watchdog* monitora a sua Mempool. Quando há transações suficientes **ou** o tempo de espera ultrapassa um limite, ele inicia a criação de um bloco candidato.
2. Trigger – O nó decide, por iniciativa própria, começar o loop de PoW.
3. Race – Simultaneamente, todos os demais mineradores ao redor do globo fazem exatamente o mesmo, competindo para encontrar o próximo hash válido.
4. **Vencedor** – O primeiro a encontrar o nonce envia o bloco para a rede. Os demais descartam seus blocos candidatos, removem as transações já confirmadas e reiniciam o ciclo.

---



## PoW

A prova de trabalho pura tem duas limitações operacionais que podem comprometer a disponibilidade da blockchain, especialmente em cenários de leilões onde os tempos de resposta são críticos.
A seguir, os **problemas** identificados, seus **impactos** e as **estratégias de mitigação** implementadas.


| Problema                                                                                                           | Impacto                                                                               | Estratégia de tolerância                                                                                                                                                                                                                                                                                                                         |
| ------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Instabilidade de taxa de sucesso** – picos de carga ou DDoS podem fazer > 50 % das tentativas falharem. | A rede para de produzir blocos ⇒ dead‑lock nos leilões distribuídos.              | **Ajuste dinâmico de dificuldade** a cada *epoch* (ex.: 30 s) baseado em `successRate = successes / attempts`.``Redução gradual (`D ← max(MIN_D, D × 0.85)`) quando `successRate < 0.5`.                                                                                                                                |
| **Latência de validação** – cada bloco precisa ser verificado por todos os pares.                        | O tráfego de rede cresce O(N²) e pode saturar a largura de banda.                   | **Quorum de validação**: o bloco é enviado a *k* nós aleatórios (ex.: k = 3). Aceitação requer ≥ k‑1 confirmações válidas.``Cache de provas: nós reutilizam resultados de verificação recentes para evitar re‑cálculo.                                                                                          |
| **Dependência de hardware** – nós modestos podem nunca alcançar a meta de dificuldade.                   | Desincentiva a participação de dispositivos pequenos, reduzindo descentralização. | **Fallback híbrido PoW → PoS‑lite**: se duas épocas consecutivas apresentarem `successRate < 0.5`, a rede ativa um consenso baseado em **reputação** (uptime + assinaturas válidas) ao invés de hash‑rate.``Reward scaling: quem contribui com PoW recebe bônus; validadores de reputação continuam elegíveis. |
| **Ataques de “hash‑rate centralizado”** – um grande minerador pode dominar a produção de blocos.       | Centraliza a cadeia, vulnerabiliza contra censura.                                    | **Limite de contribuição**: cada nó pode submeter no máximo *X* blocos por epoch, independentemente do hash‑rate.``Mix de dificuldade: a dificuldade mínima (`MIN_D`) impede que um minerador ultra‑rápido produza blocos em intervalos menores que o intervalo de consenso configurado.                                  |

---



## PoS VS PoS Vs **PBFT**

| Característica                                | **Proof‑of‑Work (PoW)**                                                                           | **Proof‑of‑Stake (PoS)**                                                                         | **PBFT**                                                                                               |
| ---------------------------------------------- | --------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| **Modelo de segurança**                 | Gasto computacional (hash‑rate) impede Sybil.                                                            | Possuir stake (tokens) cria custo de colapso; slashing penaliza mau‑comportamento.                      | Cada nó tem identidade conhecida (PKI) e requer 2f+1 assinaturas corretas.                                  |
| **Tolerância a falhas**                 | Liveness mantida enquanto > 50 % das tentativas de PoW são bem‑sucedidas (com mecanismo de fallback). | Liveness mantida enquanto > 2/3 dos tokens são online; pode ser suspen­do se a maioria ficar offline. | Liveness garantida até**f < n/3** nós falharem (≈ 33 %); acima disso o consenso pára.            |
| **Escalabilidade (mensagens)**           | Broadcast + quorum de validação (O(N)) → fácil de escalar para centenas de nós.                      | Apenas um líder proposto por algoritmo de seleção (ex.: Round‑Robin) → O(1) mensagens.              | Comunicação quadrática O(n²) para troca de*pre‑prepare/prepare/commit* ; limita escala a < 200 nós. |
| **Consumo de energia**                   | Altíssimo (mineração).                                                                                 | Muito baixo (assinaturas digitais).                                                                      | Baixo, porém requer servidores com alta disponibilidade de rede.                                            |
| **Barreira de entrada**                  | Necessário hardware de hash (GPU/ASIC).                                                                  | Necessário possuir tokens (stake) ou ser eleito por delegação.                                        | Necessário possuir identidade certificada e ser aceito por quorum.                                          |
| **Centralização potencial**            | Mineradores com grande hash‑rate podem dominar blocos.                                                   | Grandes detentores de stake podem concentrar poder de consenso.                                          | Operadores com alta reputação ou controle de rede podem influenciar quorum.                                |
| **Resistência a ataques de eclipse**    | Quorum aleatório de validação impede controle total da rota.                                           | Depende de lista de validadores conhecida; pode ser alvo de isolamento.                                  | Necessita de conectividade entre 2f+1 nós – vulnerável a segmentação de rede.                           |
| **Complexidade de implementação**      | Algoritmo simples (hash + ajuste de dificuldade).                                                         | Mais complexo (staking, slashing, delegação, checkpoint).                                              | Muito complexo (view changes, checkpointing, assinaturas múltiplas).                                        |
| **Tempo de finalização de bloco**      | Tipicamente 10 s – 10 min (dependendo da cadeia).                                                      | Poucos segundos (blocos produzidos por líder).                                                          | Milissegundos‑a‑segundos (latência de consenso entre nós).                                               |
| **Adequação a leilões distribuídos** | Boa para redes altamente descentralizadas; requer*fallback* para evitar dead‑lock.                     | Ótimo quando há token nativo e interesse econômico dos participantes.                                 | Ideal em ambientes controlados (consórcio de bancos, data‑centers).                                        |

# Arquitetura de Segurança de uma Rede P2P Distribuída para Leilões

*(documentação em Português de Portugal – estilo técnico‑profissional)*

---

## 1. Imutabilidade e Proteção contra Falsificação

A garantia de que o histórico da cadeia nunca pode ser alterado **não** depende de uma barreira física; trata‑se de uma **barreira computacional** que, enquanto a maioria dos nós se mantiver honesta, é intransponível.

### 1.1 Encadeamento Criptográfico (Blockchain)

* Cada bloco contém o **hash SHA‑256** do bloco imediatamente anterior (`previousHash`).
* Qualquer modificação no conteúdo de um bloco antigo altera o seu hash, que deixa de corresponder ao valor armazenado no seguinte bloco, quebrando a cadeia.
* Para que um atacante consiga “re‑escrever” a cadeia, teria de **recalcular o Proof‑of‑Work (PoW) de todos os blocos subsequentes**, exigindo mais poder de computação do que a rede inteira combinada – um cenário praticamente impossível.

### 1.2 Armazenamento Endereçável por Conteúdo (Content‑Addressable Storage – CAS)

* Quando um nó solicita um bloco ou uma transação à rede Kademlia, o pedido inclui **o hash original** do objeto.
* O remetente devolve o conteúdo; o nó requisitante **recalcula o hash** do payload recebido.
* Se houver **mesmo um único bit** de divergência, o objeto é descartado como “envenenado” e o remetente sofre penalização no sistema de reputação.

> **Objetivo:** impedir que dados corrompidos ou deliberadamente adulterados sejam aceites por qualquer nó da rede.

---

## 2. Concorrência, Forks e Ordenação Definitiva das Transações

Num ambiente sem relógio global, transações e blocos chegam de forma **assíncrona** e podem colidir.

### 2.1 Gestão da Mem‑Pool

* Todas as transações válidas são armazenadas numa **mem‑pool** (fila de espera).
* A ordem de chegada ao *network* é irrelevante; a única ordem **definitiva** é estabelecida quando um minerador as inclui num bloco, as organiza numa **Árvore de Merkle** e sela o bloco.

### 2.2 Resolução de Forks (Ramos concorrentes)

1. **Deteção da bifurcação** – quando dois blocos diferentes são aceites como filhos do mesmo bloco‑pai.
2. **Regra da cadeia mais longa/pesada** – o ramo que acumular mais blocos (ou maior peso de PoW) torna‑se a cadeia oficial.
3. **Rollback** – os blocos do ramo perdedor são removidos (rollback) e as suas transações são devolvidas à mem‑pool.
4. **Desempate por hash mínimo** – se a altura for exatamente a mesma, o bloco cujo hash tem valor numérico **menor** vence; o critério é determinista e elimina a ambiguidade.

> **Consequência:** a rede converge rapidamente para um único estado consensual, minimizando períodos de incerteza.

---

## 3. Prevenção de Replay Attacks e Gastos Duplicados

Um atacante pode interceptar uma transação legítima (por exemplo, um lance de 500 €) e retransmiti‑la múltiplas vezes.

### 3.1 Identificador Criptográfico Único – TxID

* O **TxID** é o hash SHA‑256 de **todo** o conteúdo da transação, incluindo assinatura e nonce.
* Uma vez que um bloco que contenha o TxID foi confirmado, o hash é registado como **processado**.
* Qualquer bloco futuro que tente incluir um TxID já presente será rejeitado como  *matematicamente inválido* .

### 3.2 Mecanismo de Nonce Sequencial

* Cada utilizador (identificado pela sua **chave pública**) mantém um contador **nonce** que incrementa a cada transação.
* O nonce é incluído no payload e assinado.
* Ao validar uma transação, o nó verifica se o nonce é **estritamente maior** que o último nonce já confirmado para aquela chave.
* Valores iguais ou inferiores são descartados imediatamente, eliminando a possibilidade de **repetição**.

> **Resultado:** um mesmo lance não pode ser incluído duas vezes, nem pode ser reutilizado para “esgotar” fundos.

---

## 4. Autenticação, Não‑Repúdio e Integridade das Transações

Para garantir que um lance provém realmente do utilizador A e não de um impostor B, a rede usa criptografia assimétrica.

### 4.1 Assinaturas Digitais (ECDSA / RSA)

* Cada cliente possui um par de chaves **(pública / privada)**.
* Antes de enviar, a transação (ou bloco) é **hash‑ed** e o hash é assinado com a **chave privada** do remetente.

### 4.2 Verificação P2P

* O bloco ou transação circula acompanhado da **assinatura** e da **chave pública** do remetente.
* Qualquer nó pode:
  1. Recalcular o hash dos dados recebidos.
  2. Desencriptar a assinatura usando a chave pública.
  3. Comparar os dois hashes.
* Se coincidirem, a assinatura prova que **somente o dono da chave privada** poderia ter criado a mensagem, assegurando **autenticidade** e **não‑repúdio**.

> **Proteção contra impersonificação:** um atacante sem a chave privada não consegue forjar transações válidas.

---

## 5. Garantia de Liveness – Todas as Transações Válidas São Eventualmente Registadas

Mesmo com falhas de comunicação ou forks, a rede deve assegurar que nenhuma transação "desapareça".

### 5.1 Propagação Epidémica (Gossip Protocol)

* Ao submeter uma transação, o nó de origem **broadcast**‑a para todos os seus vizinhos.
* Cada vizinho repete o processo, criando um **efeito cascata** que inunda a mem‑pool de todos os nós em segundos.

### 5.2 Ressurreição Pós‑Rollback

* Quando um fork é resolvido, os blocos do ramo rejeitado são **desmontados**.
* Todas as transações contidas nesses blocos são **reintroduzidas imediatamente** na mem‑pool, garantindo que não são perdidas.

> **Liveness** é, portanto, assegurada: toda transação válida tem oportunidade de ser incluída num bloco futuro.

---

## 6. Tolerância a Falhas Bizantinas

A rede assume que *qualquer* nó pode comportar‑se de forma maliciosa (Byzantine). Três camadas de defesa são aplicadas.

| Camada                           | Mecanismo                          | Função                                                                                        |
| -------------------------------- | ---------------------------------- | ----------------------------------------------------------------------------------------------- |
| **Autenticidade**          | Assinaturas digitais               | Impede a criação de mensagens falsificadas.                                                   |
| **Integridade**            | Content‑Addressable Storage (CAS) | Detecta e descarta dados adulterados.                                                           |
| **Isolamento Topológico** | S‑Kademlia (Trust & Reputation)   | Reduz a influência de nós Sybil ou maliciosos ao ajustar a confiança e reroutear o tráfego. |

* **Reputação local:** nós que enviam dados incorretos ou recusam armazenar blocos têm a sua pontuação de confiança diminuída, sendo eventualmente removidos dos  *k‑buckets* .
* **Roteamento adaptativo:** a rede prefere caminhos que passam por nós com alta confiança, isolando efetivamente os ofensores.

> **Resultado:** mesmo que parte da rede se torne maliciosa, a maioria honestă mantém a propriedade de consenso e a integridade dos dados.

## 9. Considerações Finais

* **Imutabilidade** e **integridade** são asseguradas por hashes encadeados e por verificação de conteúdo (CAS).
* **Concorrência** é gerida por mem‑pools, regras de fork (cadeia mais longa, hash‑mínimo) e por rollback que reinjecta transações órfãs.
* **Replay attacks** são neutralizadas por TxIDs únicos e por **nonces sequenciais** por utilizador.
* **Autenticação** e **não‑repúdio** são garantidos por assinaturas digitais verificáveis por qualquer nó.
* **Liveness** (garantia de entrega) provém da propagação epidemicamente rápida (gossip) e da estratégia de “ressurreição” após reorganizações.
* **Byzantine tolerance** combina assinatura, CAS e um mecanismo de reputação (S‑Kademlia) que limita o impacto de nós maliciosos.

Com estas camadas de defesa interligadas, a rede P2P consegue oferecer um **ambiente seguro, consistente e resiliente** para a realização de leilões distribuídos, preservando a confiança dos participantes e a integridade dos registos ao longo do tempo.

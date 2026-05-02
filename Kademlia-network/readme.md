# Arquitetura de Software do Nó Blockchain-Auction com Kademlia, PoW, Consenso e Segurança

## Enquadramento Geral do Projeto

O projeto apresenta uma arquitetura orientada a princípios de **Clean Architecture**, estruturada para garantir separação rigorosa entre regras de negócio (domínio), casos de uso (aplicação) e detalhes tecnológicos (rede, persistência, criptografia e interface). O objetivo é assegurar que a lógica fundamental do sistema permanece estável e independente de frameworks, bibliotecas externas e mecanismos concretos de transporte ou armazenamento.

A organização do código segue um modelo de **packaging por domínio**, aproximando-se de práticas de *Domain-Driven Design (DDD)*, em que entidades como `Block`, `Transaction`, `AuctionState` e `Node` refletem diretamente a linguagem ubíqua do problema. Esta abordagem reduz o acoplamento semântico e melhora a rastreabilidade entre requisitos funcionais e implementação.

O sistema é um nó distribuído que integra simultaneamente um mecanismo de **rede P2P baseado em Kademlia**, uma **blockchain com mineração Proof-of-Work (PoW)**, uma camada de **sincronização e propagação de blocos**, e mecanismos explícitos de **validação de mensagens e segurança criptográfica**.

---

## Estrutura Arquitetural por Camadas (Clean Architecture)

A estrutura do projeto está dividida em módulos principais que refletem os níveis clássicos de Clean Architecture: `domain`, `application`, `adapter`, `infrastructure`, `gateway` e `server`. A regra central respeitada é a **Dependency Rule**, segundo a qual dependências devem apontar sempre para dentro, ou seja, camadas externas podem depender das internas, mas nunca o inverso.

A camada `domain` é completamente independente de detalhes técnicos, e contém apenas conceitos do problema. A camada `application` contém o comportamento executável do sistema através de casos de uso. As camadas externas (`adapter`, `infrastructure`, `gateway` e `server`) implementam detalhes como rede, serialização, armazenamento, relógios lógicos e integração com o ambiente de execução.

Esta estrutura permite que o núcleo do sistema permaneça testável, determinístico e evolutivo, enquanto os detalhes tecnológicos podem ser substituídos sem alteração da lógica de negócio.

---

## Camada de Domínio (`domain`)

A camada `domain` contém o modelo central do sistema e define as entidades fundamentais que caracterizam um nó blockchain com funcionalidade de leilão.

No submódulo `domain.entities` encontram-se as estruturas que representam os conceitos persistentes e relevantes do sistema, incluindo `Block`, `BlockHeader`, `Transaction`, `MerkleTree`, bem como entidades específicas de leilões como `AuctionState` e `Bid`. Estas classes representam o estado do sistema e devem ser consideradas as unidades principais de consistência.

O módulo `domain.valueobject` agrega objetos imutáveis, normalmente usados para encapsular invariantes e propriedades que não fazem sentido existir isoladamente como entidades. O pacote `cryptography` contém representações como `KeyPairPeer` e `PublicKeyPeer`, essenciais para identificação e autenticação de nós.

A presença do pacote `domain.policy` demonstra uma tentativa explícita de encapsular regras invariantes e decisões de validação. Um exemplo relevante é `ProofOfReputationPolicy`, que sugere um mecanismo híbrido ou complementar ao PoW. Isto implica que o sistema pode integrar métricas reputacionais como parte do processo de aceitação ou priorização de eventos, o que pode ser interpretado como uma camada adicional de confiança sobre a rede P2P.

A separação entre entidades, value objects e políticas reforça coerência conceptual e reduz o risco de dispersão de regras críticas por camadas externas, o que seria um erro arquitetural comum.

---

## Camada de Aplicação e Casos de Uso (`application.usecase`)

A camada `application.usecase` é o ponto onde a lógica executável reside. É aqui que se encontram as operações que orquestram regras do domínio, validam transações, coordenam mineração e sincronizam a blockchain.

Os casos de uso estão divididos por áreas funcionais como `auction`, `blockchain`, `mining` e `reputation`. A classe `AuctionCaseUse` sugere a existência de um fluxo completo para gestão de leilões distribuídos, provavelmente baseado em eventos propagados na rede. A existência de `BlockchainUseCase`, `BlockEventUseCase` e `ChainSyncUseCase` indica que a blockchain não é tratada como armazenamento passivo, mas como uma máquina de estados distribuída que precisa de sincronização e gestão de consistência.

A componente de mineração está representada por `MinerThread`, `MinerThreadBlock` e classes de resultados (`MiningResult`, `MiningResultBlock`). Esta abordagem indica que a mineração é tratada como um processo concorrente contínuo, o que é coerente com sistemas PoW tradicionais. Contudo, o uso explícito de *threads* sugere que o projeto deve ser tratado com cautela relativamente a condições de corrida, principalmente na interação com armazenamento e propagação de blocos.

O pacote `application.usecase.blockchain.block` contém `BlockRule` e `TransactionRule`, o que representa um esforço correto para formalizar validações de integridade e consistência ao nível de bloco e transação. Este ponto é crítico, pois em blockchain as regras de validação constituem o verdadeiro mecanismo de consenso: se diferentes nós aplicarem regras diferentes, a rede fragmenta inevitavelmente.

Os providers em `application.usecase.provider` como `IBlockListener`, `IReputationsManager` e `ITransactionsPublished` atuam como contratos (portas), permitindo que os casos de uso comuniquem com infraestrutura sem conhecer implementações concretas.

---

## Portas e Interfaces (Princípio de Inversão de Dependências)

Embora o projeto não tenha explicitamente um diretório `application.port`, o papel de portas é desempenhado por interfaces dentro de `application.usecase.provider` e também em `adapter.provider`. Esta decisão é funcional, mas conceptualmente inconsistente com a nomenclatura declarada no texto introdutório. Se a intenção é manter rigor arquitetural, a separação formal entre portas da aplicação e contratos de adaptadores deveria ser reforçada para evitar erosão estrutural.

O uso de interfaces como `IKademliaIController` e `IEventDispatcher` demonstra que a comunicação com o mundo externo é mediada por abstrações. Isto é alinhado com Clean Architecture, onde a aplicação depende de abstrações e não de implementações.

---

## Adaptadores de Entrada e Saída (`adapter`)

O módulo `adapter` é responsável por traduzir estímulos externos para chamadas internas de casos de uso e converter respostas internas para formatos compreendidos externamente.

Em `adapter.inbound.network`, classes como `Handshake`, `JoinNetwork`, `HeartbeatEvent` e `NetworkEvent` representam o processamento de eventos recebidos da rede. Esta camada é essencial para encapsular a complexidade do protocolo de comunicação e garantir que a aplicação não conhece detalhes de sockets, payloads ou serialização binária.

Em `adapter.outbound.network` encontra-se a implementação concreta da rede baseada em Kademlia. O pacote `kademlia` inclui `RoutingTable`, `KBucket`, `KademliaNetwork` e `NodeMetric`. Estes componentes correspondem diretamente à literatura clássica do protocolo Kademlia, onde a rede é estruturada em buckets organizados por distância XOR, e a tabela de encaminhamento mantém contactos por proximidade lógica.

O pacote `adapter.outbound.network.message` está organizado por domínios funcionais (`auction`, `block`, `network`, `node`) e contém classes como `BlockPayload`, `HandshakePayload`, `FindNodePayload` e `InventoryPayload`. Esta segmentação demonstra uma abordagem coerente para serialização e transporte de mensagens distribuídas.

O uso de payloads dedicados é arquiteturalmente correto, pois evita expor entidades internas diretamente à rede. Isto reduz o risco de acoplamento estrutural e evita que mudanças internas no domínio quebrem compatibilidade de protocolo.

---

## Rede Distribuída Kademlia e Descoberta de Nós

A presença de classes como `FindNodePayload`, `NodeInfoPayload` e `NodeListPayload` sugere que o nó implementa o fluxo típico de descoberta Kademlia, onde um nó consulta iterativamente vizinhos para localizar o conjunto de nós mais próximos de um identificador alvo.

A classe `RoutingTable` e o conceito de `KBucket` indicam que a tabela é organizada por intervalos de distância XOR, o que é consistente com o modelo formal de Kademlia. A métrica de proximidade e manutenção de vizinhos é fundamental para escalabilidade e resiliência, especialmente em redes sujeitas a churn.

O mecanismo de heartbeat (`HeartbeatEvent`) é um elemento prático relevante: Kademlia na sua forma original não especifica necessariamente heartbeat contínuo, mas implementações reais frequentemente introduzem ping/heartbeat para deteção de falhas e limpeza de buckets.

A camada de rede é portanto uma combinação entre protocolo DHT clássico e extensões operacionais para gestão de disponibilidade.

---

## Blockchain: Estruturas e Estado Distribuído

A blockchain está modelada através de entidades `Block` e `BlockHeader`, e mecanismos auxiliares como `MerkleTree`. Isto indica que cada bloco contém um cabeçalho com metadados essenciais e uma raiz Merkle para garantir integridade das transações.

A presença de `InventoryPayload`, `InventoryType`, `ChainStatusPayload` e `GetBlocksBatchStrategy` sugere que o protocolo de sincronização segue um padrão semelhante a redes blockchain conhecidas: os nós anunciam inventário de blocos, solicitam dados em falta e sincronizam por lotes para reduzir overhead de comunicação.

A existência de `ChainSyncUseCase` reforça que a sincronização é tratada como um caso de uso separado, o que é correto, porque a sincronização é um fluxo crítico que envolve validação, armazenamento, consenso e gestão de conflitos.

O pacote `gateway.block` inclui várias estratégias (`GetBlockStrategy`, `GetStatusStrategy`, `BlockBatchStrategy`), o que demonstra um padrão arquitetural de *Strategy* para lidar com diferentes tipos de mensagens e respostas. Este design reduz duplicação de código e permite extensão modular do protocolo.

---

## Mecanismo de Consenso e Proof-of-Work (PoW)

A mineração é explicitamente representada em `application.usecase.mining`. A presença de `MinerThread` indica que o nó executa continuamente tentativas de criação de blocos válidos através de prova computacional. Embora o código não esteja visível, a nomenclatura `MiningResultBlock` sugere que o resultado da mineração inclui o bloco final e possivelmente metadados como nonce, dificuldade e tempo de execução.

Num sistema PoW, o consenso não é um módulo isolado; ele emerge da combinação de três fatores: regras de validação (`BlockRule`), dificuldade ajustada e seleção da cadeia canónica (normalmente a cadeia com maior trabalho acumulado). O projeto contém elementos que suportam este modelo, mas a ausência explícita de um módulo dedicado a "fork choice rule" sugere que essa lógica pode estar dispersa em `BlockchainUseCase` ou `ChainSyncUseCase`. Se for esse o caso, existe risco de mistura entre sincronização e regras fundamentais de consenso, o que pode reduzir clareza e testabilidade.

O módulo `gateway.provider.IConsensusEngine` indica que existe intenção explícita de encapsular o consenso numa abstração. Esta decisão é tecnicamente correta e alinhada com Clean Architecture, pois permite substituir PoW por outros mecanismos (PoS, PBFT, etc.) sem reescrever a aplicação.

No entanto, é importante salientar que consenso PoW não é apenas mineração: a aceitação de blocos recebidos deve seguir regras idênticas às usadas localmente. Se o sistema aceitar blocos com critérios diferentes dos aplicados na mineração interna, ocorrerá divergência de cadeia e fragmentação inevitável.

---

## Gestão de Eventos e Propagação de Mensagens

A existência de `BrokerEvent`, `IEventDispatcher` e `ConnectionHandler` demonstra que o sistema utiliza um modelo de propagação orientado a eventos. Este padrão é adequado para redes P2P, pois desacopla a receção de mensagens da execução de casos de uso e reduz bloqueios de rede.

A arquitetura parece seguir o modelo de "mensagem entra → adaptador traduz → gateway escolhe estratégia → caso de uso processa → resposta é gerada". Este pipeline é consistente com boas práticas para sistemas distribuídos, pois impede que a lógica de rede contamine o núcleo da aplicação.

O risco típico neste modelo está na ausência de garantias de ordenação e na possibilidade de processamento duplicado. A presença de um relógio lógico híbrido (`HybridLogicalClock`) sugere que o sistema tenta mitigar problemas de causalidade e ordenação de eventos, o que é relevante em ambientes onde múltiplos nós produzem blocos e transações concorrentemente.

---

## Persistência e Armazenamento Distribuído (DHT / Cache)

O módulo `infrastructure.storage` contém `StorageDHT` e um cache `LRU`. Isto indica que o nó não depende exclusivamente de armazenamento local tradicional, mas utiliza uma abordagem distribuída para partilha de dados, potencialmente armazenando blocos, transações ou metadados na DHT.

O uso de LRU indica uma preocupação clara com limites de memória e substituição de entradas antigas. Esta decisão é tecnicamente necessária em redes blockchain, pois o volume de dados cresce continuamente e a replicação total pode ser inviável em nós leves.

No entanto, deve ser observado que DHT não substitui a necessidade de persistência confiável para a cadeia local. Uma DHT é adequada para descoberta e distribuição, mas não é um mecanismo forte de consistência. Se o sistema depender da DHT como fonte primária de verdade, corre o risco de aceitar dados incompletos ou inconsistentes, especialmente em cenários de churn ou ataques de envenenamento de routing.

---

## Infraestrutura de Segurança e Criptografia

A segurança é tratada explicitamente em `infrastructure.crypt` com classes como `KeyStorageManager`, `KeysInfrastructure` e `SecureSession`. Isto sugere que o nó possui um mecanismo estruturado para geração, armazenamento e gestão de chaves criptográficas, bem como sessões seguras para comunicação.

A classe `SecurityBootstrapper` na camada `server` indica que o processo de arranque do nó inclui inicialização de mecanismos de segurança, o que é essencial para evitar que o nó opere em estado inseguro por defeito.

No módulo `gateway.validator`, a classe `SecurityValidator` sugere validação ativa de mensagens recebidas antes de serem processadas pelo núcleo. Esta abordagem é crítica em redes P2P, onde o modelo de ameaça assume adversários ativos. A validação deve incluir autenticação, verificação de assinaturas digitais, validação de estrutura de payload e mitigação de ataques de replay.

A presença de utilitários como `CryptoUtils`, `EncapsulationUtils` e `SerializationUtils` sugere que o projeto implementa serialização binária e mecanismos de encapsulamento. Isto é funcional, mas é também um ponto clássico de vulnerabilidade. Serialização customizada e parsing manual são frequentemente origem de falhas graves como execução remota de código, corrupção de memória lógica, e ataques de negação de serviço por payloads malformados.

Assim, a arquitetura está correta ao isolar estes detalhes na periferia (adapters/infrastructure), mas a robustez do sistema depende fortemente da qualidade dessas validações.

---

## Relógio Lógico Híbrido e Consistência Temporal

O módulo `infrastructure.networkTime.HybridLogicalClock` demonstra uma preocupação relevante com consistência temporal em ambiente distribuído. Relógios físicos não são confiáveis em redes P2P, devido a desvio de relógio e ausência de sincronização garantida. Um Hybrid Logical Clock (HLC) combina timestamps físicos com contadores lógicos para preservar causalidade parcial e permitir ordenação mais consistente de eventos.

Em sistemas blockchain, a ordenação é determinante para evitar ambiguidades na aplicação de transações e para garantir reprodutibilidade do estado. Embora a blockchain tradicional use apenas ordem da cadeia, a existência de eventos externos como leilões e reputação pode exigir uma noção adicional de causalidade que não dependa apenas do tempo de mineração.

---

## Gateway e Estratégias de Protocolo

A camada `gateway` atua como mediadora entre a rede e os casos de uso. A classe `NetworkGateway` indica um ponto central para tratamento de mensagens e encaminhamento para estratégias apropriadas.

O pacote `gateway.block` contém múltiplas estratégias como `InvStrategy`, `GetStatusStrategy`, `ChainStatusResponseStrategy` e `GetBlocksBatchStrategy`. Este desenho mostra uma implementação explícita de *Command/Strategy pattern* para processamento de mensagens. Esta abordagem é adequada para protocolos P2P, pois permite que cada tipo de mensagem tenha lógica isolada e testável.

O `gateway.provider.MessageStrategy` sugere que as estratégias seguem uma interface comum, permitindo extensão modular. Isto é um ponto arquitetural forte, pois evita condicionais extensos e torna o protocolo evolutivo.

---

## Camada de Servidor e Bootstrap (`server`)

A camada `server` contém `Launcher`, `LauncherBootstrap`, `Peer` e `SecurityBootstrapper`, bem como utilitários como `MetricsLogger` e `MenuUtils`. Esta camada representa o ponto de entrada e configuração do sistema.

A arquitetura está correta ao manter esta camada fina. Em Clean Architecture, o arranque deve apenas instanciar dependências, configurar implementações concretas e iniciar o ciclo de vida do nó. Qualquer lógica de negócio nesta camada seria uma violação arquitetural.

O ficheiro `boostrapp.neigbours.txt` sugere um mecanismo de bootstrap de rede baseado numa lista inicial de vizinhos, o que é um padrão típico em redes P2P para evitar isolamento inicial. Este mecanismo é funcionalmente necessário porque Kademlia requer pelo menos um nó conhecido para iniciar a descoberta iterativa.

---

## Considerações Críticas e Potenciais Problemas Estruturais

Apesar da arquitetura estar globalmente alinhada com Clean Architecture, existe um ponto evidente de inconsistência: existem múltiplas classes `Constants.java` espalhadas por `adapter.utils`, `infrastructure.utils` e `server.utils`. Isto tende a gerar divergência de configurações e acoplamento indireto. Um sistema distribuído não pode permitir ambiguidades em parâmetros críticos como tamanho de bloco, dificuldade PoW, timeouts de rede e limites de cache. A existência de constantes duplicadas aumenta o risco de que diferentes módulos operem com pressupostos distintos.

Outro problema potencial é a presença de threads de mineração diretamente na camada de casos de uso. A mineração é uma tarefa concorrente e de longa duração, mas em Clean Architecture a execução concorrente deveria ser um detalhe externo controlado por infraestrutura, mantendo a lógica de mineração como serviço puro e determinístico. Se `MinerThread` contiver lógica de negócio e lógica de concorrência misturadas, a testabilidade e previsibilidade diminuem drasticamente.

Adicionalmente, a separação entre `adapter` e `gateway` parece parcialmente redundante. Em algumas arquiteturas, o gateway é um adaptador de saída. Aqui, o gateway é usado como camada intermediária de protocolo e estratégia. Isto é aceitável, mas deve ser rigidamente controlado para que o gateway não se torne um "dumping ground" de lógica que deveria residir em casos de uso.

---


```text
├── main
│   ├── java
│   │   └── org
│   │       └── graph
│   │           ├── adapter
│   │           │   ├── inbound
│   │           │   │   └── network
│   │           │   │       ├── Handshake.java
│   │           │   │       ├── HeartbeatEvent.java
│   │           │   │       ├── JoinNetwork.java
│   │           │   │       └── NetworkEvent.java
│   │           │   ├── outbound
│   │           │   │   └── network
│   │           │   │       ├── kademlia
│   │           │   │       │   ├── KBucket.java
│   │           │   │       │   ├── KademliaNetwork.java
│   │           │   │       │   ├── NodeMetric.java
│   │           │   │       │   └── RoutingTable.java
│   │           │   │       └── message
│   │           │   │           ├── auction
│   │           │   │           │   ├── AuctionOpType.java
│   │           │   │           │   └── AuctionPayload.java
│   │           │   │           ├── block
│   │           │   │           │   ├── BlockPayload.java
│   │           │   │           │   ├── ChainStatusPayload.java
│   │           │   │           │   ├── InventoryPayload.java
│   │           │   │           │   └── InventoryType.java
│   │           │   │           ├── network
│   │           │   │           │   └── HandshakePayload.java
│   │           │   │           └── node
│   │           │   │               ├── FindNodePayload.java
│   │           │   │               ├── NodeInfoPayload.java
│   │           │   │               └── NodeListPayload.java
│   │           │   ├── provider
│   │           │   │   ├── IEventDispatcher.java
│   │           │   │   └── IKademliaIController.java
│   │           │   └── utils
│   │           │       ├── Base64Utils.java
│   │           │       ├── Constants.java
│   │           │       ├── CryptoUtils.java
│   │           │       └── MessageUtils.java
│   │           ├── application
│   │           │   └── usecase
│   │           │       ├── auction
│   │           │       │   └── AuctionCaseUse.java
│   │           │       ├── blockchain
│   │           │       │   ├── block
│   │           │       │   │   ├── BlockRule.java
│   │           │       │   │   └── TransactionRule.java
│   │           │       │   ├── BlockEventUseCase.java
│   │           │       │   ├── BlockchainUseCase.java
│   │           │       │   └── ChainSyncUseCase.java
│   │           │       ├── mining
│   │           │       │   ├── MinerThread.java
│   │           │       │   ├── MinerThreadBlock.java
│   │           │       │   ├── MiningResult.java
│   │           │       │   └── MiningResultBlock.java
│   │           │       ├── provider
│   │           │       │   ├── IBlockListener.java
│   │           │       │   ├── IReputationsManager.java
│   │           │       │   └── ITransactionsPublished.java
│   │           │       └── reputation
│   │           │           └── ReputationsManager.java
│   │           ├── domain
│   │           │   ├── entities
│   │           │   │   ├── auctions
│   │           │   │   │   ├── AuctionState.java
│   │           │   │   │   └── Bid.java
│   │           │   │   ├── block
│   │           │   │   │   ├── Block.java
│   │           │   │   │   └── BlockHeader.java
│   │           │   │   ├── message
│   │           │   │   │   ├── Message.java
│   │           │   │   │   └── MessageType.java
│   │           │   │   ├── node
│   │           │   │   │   ├── Node.java
│   │           │   │   │   └── NodeId.java
│   │           │   │   ├── transaction
│   │           │   │   │   ├── Transaction.java
│   │           │   │   │   └── TransactionType.java
│   │           │   │   └── tree
│   │           │   │       ├── MerkleNode.java
│   │           │   │       └── MerkleTree.java
│   │           │   ├── policy
│   │           │   │   ├── reputation
│   │           │   │   │   └── ProofOfReputationPolicy.java
│   │           │   │   └── EventTypePolicy.java
│   │           │   └── valueobject
│   │           │       ├── cryptography
│   │           │       │   ├── KeyPairPeer.java
│   │           │       │   ├── Pair.java
│   │           │       │   └── PublicKeyPeer.java
│   │           │       └── utils
│   │           │           └── HashUtils.java
│   │           ├── gateway
│   │           │   ├── block
│   │           │   │   ├── BlockBatchStrategy.java
│   │           │   │   ├── BlockStateRemote.java
│   │           │   │   ├── BlockStrategy.java
│   │           │   │   ├── ChainStatusResponseStrategy.java
│   │           │   │   ├── GetBlockStrategy.java
│   │           │   │   ├── GetBlocksBatchStrategy.java
│   │           │   │   ├── GetStatusStrategy.java
│   │           │   │   └── InvStrategy.java
│   │           │   ├── provider
│   │           │   │   ├── IConsensusEngine.java
│   │           │   │   └── MessageStrategy.java
│   │           │   ├── validator
│   │           │   │   └── SecurityValidator.java
│   │           │   └── NetworkGateway.java
│   │           ├── infrastructure
│   │           │   ├── auction
│   │           │   ├── blockhain
│   │           │   ├── crypt
│   │           │   │   ├── KeyStorageManager.java
│   │           │   │   ├── KeysInfrastructure.java
│   │           │   │   └── SecureSession.java
│   │           │   ├── network
│   │           │   │   ├── neighbor
│   │           │   │   │   ├── ConnectionEntry.java
│   │           │   │   │   └── NeighboursConnections.java
│   │           │   │   ├── BrokerEvent.java
│   │           │   │   ├── ConnectionHandler.java
│   │           │   │   ├── MinerOrchestrator.java
│   │           │   │   └── ServerHandle.java
│   │           │   ├── networkTime
│   │           │   │   └── HybridLogicalClock.java
│   │           │   ├── storage
│   │           │   │   ├── povider
│   │           │   │   │   └── LRU.java
│   │           │   │   └── StorageDHT.java
│   │           │   └── utils
│   │           │       ├── Constants.java
│   │           │       ├── EncapsulationUtils.java
│   │           │       └── SerializationUtils.java
│   │           └── server
│   │               ├── utils
│   │               │   ├── Constants.java
│   │               │   ├── FileSystemUtils.java
│   │               │   ├── MenuUtils.java
│   │               │   ├── MetricsLogger.java
│   │               │   └── PrintBlock.java
│   │               ├── Launcher.java
│   │               ├── LauncherBootstrap.java
│   │               ├── Peer.java
│   │               └── SecurityBootstrapper.java
│   └── resources
│       └── boostrapp.neigbours.txt
└── test
    └── java
```



## Diagrama de Componentes

As quatro vistas apresentadas (C1 a C4) seguem uma abordagem coerente com o modelo **C4 Model** (Context, Container, Component e Code/Domain), permitindo descrever o sistema em diferentes níveis de abstração. Esta metodologia é adequada para sistemas distribuídos complexos como um nó blockchain P2P, porque reduz ambiguidade arquitetural e permite separar preocupações entre visão organizacional, infraestrutura lógica, componentes internos e modelo de domínio.

---

### Visão de Sistema – System Context (C1)

A vista **C1** representa o sistema como uma entidade única dentro do seu ecossistema, identificando claramente os seus principais atores externos e sistemas adjacentes. Nesta perspetiva, o nó blockchain é tratado como uma “caixa preta”, onde não se discute a implementação interna, mas sim as interações e responsabilidades globais.

Num contexto de blockchain distribuída baseada em Kademlia, o ator externo mais relevante é a própria **rede P2P**, composta por múltiplos nós equivalentes, sem hierarquia central. O nó em execução comunica com pares para realizar descoberta de vizinhos, troca de blocos, sincronização de estado e propagação de eventos de aplicação (ex.: leilões). Outro ator típico é o utilizador local (operador do nó), que interage através da camada `server` e utilitários CLI, iniciando o nó, monitorizando métricas ou executando operações administrativas.

O objetivo arquitetural desta vista é mostrar que o sistema depende de um ambiente hostil e descentralizado, onde qualquer nó remoto pode ser adversário. Assim, a segurança, autenticação e validação de mensagens são requisitos estruturais e não meros detalhes de implementação.

Esta vista também explicita a fronteira de responsabilidade: tudo o que ocorre fora do nó (rede, peers, utilizador) é externo, e tudo o que ocorre dentro é responsabilidade do sistema.

![Domain Model c1](../docs/diagram/component/c1.png)
---

### Diagrama de Containers – Container Diagram (C2)

A vista **C2** detalha os principais “containers” lógicos do sistema. Um container não significa necessariamente um container Docker, mas sim um agrupamento executável ou lógico com responsabilidade definida. Nesta arquitetura, os containers refletem diretamente a divisão por camadas proposta pela Clean Architecture.

O container **Server / Bootstrap** representa o ponto de entrada (`Launcher`, `Peer`, `SecurityBootstrapper`) e é responsável por inicializar dependências, carregar configurações, ativar serviços e iniciar o ciclo de execução do nó. Esta camada é deliberadamente fina para não violar a regra de dependência.

O container **Adapters** atua como interface entre o mundo externo e o núcleo do sistema. A entrada de eventos vindos da rede é tratada por `adapter.inbound.network`, enquanto a saída e comunicação com a rede Kademlia e codificação de mensagens ocorre em `adapter.outbound.network`. Este container garante que o domínio não conhece protocolos, serialização binária ou bibliotecas externas.

O container **Application / Use Cases** representa o motor comportamental do sistema. É aqui que residem operações como sincronização da cadeia, processamento de blocos recebidos, mineração e gestão de reputação. Este container implementa a lógica transacional e orquestra regras do domínio sem depender diretamente de detalhes externos.

O container **Domain** encapsula as entidades e regras invariantes: blocos, transações, estado de leilões, árvores Merkle e políticas. Este é o núcleo imutável, projetado para ser independente de infraestrutura.

Finalmente, o container **Infrastructure** agrega implementações concretas de persistência, cache, criptografia, relógios lógicos híbridos e gestão de conexões. Esta camada pode ser substituída sem impacto estrutural sobre o domínio e casos de uso, desde que respeite as interfaces.

A relevância desta vista é demonstrar como o sistema mantém modularidade, permitindo substituição de mecanismos como armazenamento DHT, providers criptográficos ou regras de consenso sem reescrever o núcleo.

![Domain Model c2](../docs/diagram/component/c2.png)
---

### Diagrama de Componentes – Component Diagram (C3)

A vista **C3** aprofunda cada container e descreve os seus principais componentes internos, demonstrando como o sistema distribui responsabilidades e como o fluxo de execução ocorre.

No container de **rede e adaptação**, destacam-se os componentes Kademlia (`RoutingTable`, `KBucket`, `KademliaNetwork`), que implementam descoberta e roteamento. Estes componentes são responsáveis por manter a tabela de vizinhos e selecionar nós próximos para queries e propagação de dados. O componente de mensagens (`message.payload`) estrutura os formatos transportados, segmentando-os por domínio (`auction`, `block`, `node`, `network`). Esta separação reduz risco de inconsistência de protocolo e evita acoplamento direto com entidades internas.

No container **Gateway**, os componentes como `NetworkGateway` e `MessageStrategy` implementam a lógica de encaminhamento e decisão de como processar mensagens. Estratégias como `InvStrategy`, `GetBlockStrategy` e `GetBlocksBatchStrategy` sugerem um protocolo semelhante ao modelo “inventory + request”, comum em redes blockchain. Esta abordagem evita envio indiscriminado de blocos e reduz overhead de banda, pois os nós anunciam disponibilidade e apenas transferem dados sob pedido.

No container **Application**, os principais componentes são os casos de uso: `BlockchainUseCase`, `ChainSyncUseCase`, `BlockEventUseCase` e `AuctionCaseUse`. Estes módulos definem a lógica de alto nível: validação de blocos, gestão de forks, sincronização, coordenação de mineração e publicação de eventos. O módulo de mineração (`MinerThread`, `MiningResultBlock`) indica que existe um componente concorrente ativo, responsável por tentar produzir novos blocos válidos segundo PoW.

No container **Infrastructure**, os componentes como `StorageDHT`, `LRU`, `SecureSession`, `KeyStorageManager` e `HybridLogicalClock` implementam requisitos não-funcionais essenciais: armazenamento eficiente, gestão de chaves e sessões seguras, e ordenação temporal consistente.

Esta vista é particularmente importante porque evidencia que o consenso e a rede não são módulos isolados: o consenso depende diretamente do pipeline de receção, validação, sincronização e persistência. O fluxo típico observado é: mensagem recebida → validada → encaminhada por estratégia → processada por caso de uso → persistida/propagada.

![Domain Model c3](../docs/diagram/component/c3.png)
---

### Modelo de Domínio – Domain Model (C4)

A vista **C4** descreve o modelo conceptual interno do sistema através de entidades e value objects. Esta é a vista que define o vocabulário formal do sistema e estabelece os invariantes fundamentais. Em blockchain, esta vista é crítica, porque o consenso depende diretamente do significado exato de “bloco válido”, “transação válida” e “cadeia válida”.

No domínio de blockchain, as entidades centrais são `Block` e `BlockHeader`. O cabeçalho contém metadados necessários para validação e encadeamento, como hash do bloco anterior, timestamp e possivelmente nonce e dificuldade. A entidade `Transaction` representa operações que alteram o estado distribuído, com um `TransactionType` que indica a categoria semântica da operação.

A presença de `MerkleTree` e `MerkleNode` demonstra que o sistema utiliza uma estrutura criptográfica para garantir integridade e permitir prova eficiente de inclusão. Isto é coerente com arquiteturas blockchain tradicionais, pois reduz custo de verificação e suporta sincronização parcial.

O domínio de rede é representado por `Node` e `NodeId`. Em sistemas Kademlia, o identificador do nó é essencial, pois define a distância XOR e a posição lógica no espaço de endereçamento. Um `NodeId` deve ser tratado como value object imutável e resistente a colisões, normalmente derivado de hash criptográfico.

O domínio de leilões surge com `AuctionState` e `Bid`. A existência destas entidades implica que o sistema não é apenas uma blockchain genérica, mas sim uma blockchain orientada a um caso de uso específico: coordenação distribuída de leilões. Neste cenário, `AuctionState` representa a evolução temporal do leilão e `Bid` representa a intenção de compra associada a um participante. Estes elementos podem ser persistidos como transações ou eventos, dependendo do desenho interno do protocolo.

Os value objects criptográficos (`KeyPairPeer`, `PublicKeyPeer`) representam a identidade do nó e são essenciais para autenticação e assinatura digital. Em redes blockchain, a identidade não é um detalhe secundário: é parte estrutural do mecanismo de segurança e do controlo de integridade.

As políticas (`ProofOfReputationPolicy`, `EventTypePolicy`) representam regras que devem ser consistentes em todos os nós. Se estas políticas forem divergentes entre participantes, o consenso quebra. Assim, a presença destas classes no domínio é arquiteturalmente correta, porque garante que regras críticas são parte do núcleo e não detalhes configuráveis em infraestrutura.

Esta vista conclui a decomposição do sistema ao explicitar que o software depende de um conjunto reduzido de conceitos fundamentais: bloco, transação, nó, árvore Merkle, estado de leilão e políticas de validação.

![Domain Model c4](../docs/diagram/component/c4.png)
---

## Referências Técnicas

Satoshi Nakamoto, “Bitcoin: A Peer-to-Peer Electronic Cash System”, 2008.

Robert C. Martin, *Clean Architecture: A Craftsman’s Guide to Software Structure and Design*, Prentice Hall, 2017.

Eric Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software*, Addison-Wesley, 2003.

Petar Maymounkov and David Mazières, “Kademlia: A Peer-to-peer Information System Based on the XOR Metric”, IPTPS 2002.

Leslie Lamport, “Time, Clocks, and the Ordering of Events in a Distributed System”, Communications of the ACM, 1978.

Kulkarni, D. et al., “Logical Physical Clocks and Consistent Snapshots in Globally Distributed Databases”, conceitos aplicáveis a Hybrid Logical Clocks, literatura académica sobre HLC.

Bruce Schneier, *Applied Cryptography: Protocols, Algorithms, and Source Code in C*, Wiley, 1996.
# Strtuc project

## Arquietura objectica: Architetura Clean
- Clean Architecture guarantees that business logic is pure and immutable, isolated from networking, persistence or UI.
- Domain‑driven packaging (entities, valueobject, policy) reflects the ubiquitous language of a blockchain‑auction node.
- Use‑case layer (application.usecase) is the only place where behavior lives; it talks to the outside through ports (application.port).
- Adapters translate between the outside world (Kademlia, DHT, CLI, binary payloads) and ports – they are the only classes that depend on external libraries.
- Infrastructure hosts concrete implementations (caching, hybrid logical clock, crypto providers) that are swappable without touching the core.
- Gateway & Server are thin bootstrap/entry-point layers that wire everything together.

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
````
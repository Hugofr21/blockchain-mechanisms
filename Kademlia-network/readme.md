# Strtuc project

## Arquietura objectica: Architetura Clean
- Clean Architecture guarantees that business logic is pure and immutable, isolated from networking, persistence or UI.
- Domain‑driven packaging (entities, valueobject, policy) reflects the ubiquitous language of a blockchain‑auction node.
- Use‑case layer (application.usecase) is the only place where behavior lives; it talks to the outside through ports (application.port).
- Adapters translate between the outside world (Kademlia, DHT, CLI, binary payloads) and ports – they are the only classes that depend on external libraries.
- Infrastructure hosts concrete implementations (caching, hybrid logical clock, crypto providers) that are swappable without touching the core.
- Gateway & Server are thin bootstrap/entry-point layers that wire everything together.

```text
project-root
│
├─ domain
│  ├─ entities
│  │  ├─ auction
│  │  │  ├─ AuctionState.java
│  │  │  └─ Bid.java
│  │  ├─ block
│  │  │  ├─ Block.java
│  │  │  └─ BlockHeader.java
│  │  ├─ message
│  │  │  ├─ Message.java
│  │  │  └─ MessageType.java        // enum
│  │  └─ p2p
│  │     ├─ Node.java
│  │     └─ NodeId.java
│  │
│  ├─ valueobject
│  │  ├─ cryptography
│  │  │  ├─ PublicKeyPeer.java
│  │  │  └─ KeyPairPeer.java
│  │  ├─ Pair.java
│  │  └─ HashUtils.java
│  │
│  └─ policy
│     ├─ reputation
│     │  └─ ProofOfReputation.java
│     └─ event
│        └─ EventType.java           // enum
│
├─ application
│  ├─ usecase
│  │  ├─ blockchain
│  │  │  ├─ BlockchainEngine.java
│  │  │  ├─ ChainSyncManager.java
│  │  │  └─ BlockEventManager.java
│  │  ├─ mining
│  │  │  ├─ MinerOrchestrator.java
│  │  │  └─ MiningResult.java
│  │  ├─ auction
│  │  │  └─ AuctionsEngine.java
│  │  └─ reputation
│  │     └─ ReputationManager.java
│  │
│  └─ port
│     ├─ inbound
│     │  └─ MessageInputPort.java
│     └─ outbound
│        ├─ BlockchainRepository.java
│        ├─ NetworkPort.java
│        ├─ StoragePort.java
│        └─ ClockPort.java
│
├─ adapter
│  ├─ inbound
│  │  ├─ network
│  │  │  ├─ HandshakeHandler.java
│  │  │  ├─ NetworkEventHandler.java
│  │  │  └─ KademliaJoinHandler.java
│  │  └─ server
│  │     └─ CommandHandler.java
│  │
│  ├─ provider
│  │  ├─ IEventDispatcher.java
│  │  └─ IKademliaIController
│  │
│  ├─ outbound
│  │  ├─ network
│  │  │  ├─ kademlia
│  │  │  │  ├─ KademliaNetwork.java
│  │  │  │  └─ RoutingTable.java
│  │  │  └─ message
│  │  │     ├─ auction
│  │  │     │  ├─ AuctionType.java      // enum
│  │  │     │  └─ AuctionPayload.java
│  │  │     ├─ block
│  │  │     │  ├─ BlockPayload.java     // record
│  │  │     │  ├─ ChainPayload.java     // record
│  │  │     │  ├─ InventoryPayload.java // record
│  │  │     │  └─ InventoryType.java    // enum
│  │  │     ├─ network
│  │  │     │  └─ HandshakePayload.java // record
│  │  │     └─ node
│  │  │        ├─ FindNodePayload.java
│  │  │        ├─ NodeInfoPayload.java
│  │  │        └─ NodeListPayload.java
│  │  └─ storage
│  │     └─ DHTStorageAdapter.java
│  │
│  └─ mapper
│     └─ MessageMapper.java
│
├─ infrastructure
│  ├─ auction
│  ├─ blockchain
│  ├─ network
│  │  ├─ p2p
│  │  │  ├─ ConnectionHandler.java
│  │  │  ├─ ServerHandler.java
│  │  │  └─ NeighbourConnections.java
│  │  └─ time
│  │     └─ HybridLogicalClock.java
│  │
│  ├─ storage
│  │  ├─ cache
│  │  │  ├─ LRUCache.java
│  │  │  └─ NodeCache.java
│  │  └─ dht
│  │     └─ StorageDHT.java
│  │
│  ├─ cryptography
│  │  ├─ KeyInfrastructure.java
│  │  └─ KeyStorageManager.java
│  │
│  └─ provider
│     └─ LRU.java                    // interface
│
├─ gateway
│  ├─ network
│  │  ├─ NetworkGateway.java
│  │  └─ MessageStrategy.java        // interface
│  └─ block
│     ├─ BlockStateRemote.java       // enum
│     ├─ BlockStrategy.java
│     ├─ GetBlockStrategy.java
│     ├─ GetStatusStrategy.java
│     ├─ ChainStatusStrategy.java
│     └─ InventoryStrategy.java
│
└─ server
   ├─ utils
   │  ├─ Constants.java
   │  ├─ FileSystem.java
   │  ├─ PrintBlock.java
   │  └─ MenuUtils.java
   ├─ Launcher.java
   ├─ LauncherBootstrap.java
   └─ Peer.java

````
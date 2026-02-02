# Introduçao
Sistemas distribuídos utilizam mecanismos baseados em blockchain para garantir dados imutáveis.  
Para aplicações com requisitos de segurança, o objetivo deste projeto

## Arquietura objectica: Architetura Clean 
- domain
  - aplication
    - block
      - Block.java
      - BlockcHeader.java
    - kademlia
      - KBucket.java
      - NodeMetric.java
      - RoutingTable.java
    - mecachism
      - pow
       - MinerThread.java
       - MiniResult.java
      - EventType.java
      - ProofOdReputation.java
    - p2p
     - NeighboursConnections.java 
    - trnsactions
     - Trandaction.java
     - TrnasactiondsType
    - tree
    - MerkleNode.java
    - MerkleTree.java
  - commom
    - Pair.java
  - crypto
    - KeyPairPeer
    - PublicKeyPeer
  - entities
    - auctions
      - AuctionState.java
      - Bid.java
    - message
    - p2p
  - utils
- infrastrtucture
- server
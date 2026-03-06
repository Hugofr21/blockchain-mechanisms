package org.graph.server;

import org.graph.application.usecase.blockchain.BlockEventUseCase;
import org.graph.application.usecase.blockchain.ChainSyncUseCase;
import org.graph.application.usecase.reputation.ReputationsManager;
import org.graph.adapter.inbound.network.NetworkEvent;
import org.graph.infrastructure.network.BrokerEvent;
import org.graph.infrastructure.network.MinerOrchestrator;
import org.graph.infrastructure.network.ServerHandle;
import org.graph.infrastructure.network.neighbor.NeighboursConnections;
import org.graph.adapter.outbound.network.kademlia.RoutingTable;
import org.graph.application.usecase.mining.MiningResult;
import org.graph.domain.entities.node.Node;
import org.graph.gateway.NetworkGateway;
import org.graph.infrastructure.crypt.KeysInfrastructure;
import org.graph.adapter.outbound.network.kademlia.KademliaNetwork;
import org.graph.infrastructure.networkTime.HybridLogicalClock;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.graph.adapter.utils.Constants.*;
import static org.graph.server.utils.Constants.HOST;


public class Peer {

    private ServerSocket server;
    private Node myself;
    private RoutingTable routingTable;
    private Logger mLogger;
    private volatile boolean running;
    private KeysInfrastructure keys;
    private NeighboursConnections neighboursManager;
    private KademliaNetwork mkademliaNetwork;
    private HybridLogicalClock hybridLogicalClock;
    private NetworkGateway networkGateway;
    private BlockEventUseCase mBlockEventUseCase;
    private NetworkEvent networkEvent;
    private ChainSyncUseCase mChainSyncManager;
    private ReputationsManager reputationsManager;
    private BrokerEvent mBrokerEvent;

    public Peer(int port, char[] password) {
        this.keys = new KeysInfrastructure(this, port, password);
        this.hybridLogicalClock = new HybridLogicalClock();
        this.networkGateway = new NetworkGateway(this);
        this.mBrokerEvent =  new BrokerEvent();

        MiningResult proofOfWork = null;
        try {
            proofOfWork = MinerOrchestrator.executeMining(keys.getOwnerPublicKey());
        } catch (Exception e) {
            throw new RuntimeException("[CRITICAL] Failed to initialize Peer identity via Mining.", e);
        }

        this.myself = new Node(HOST, port, proofOfWork.nodeId(),   proofOfWork.nonce(), NETWORK_DIFFICULTY);
        this.keys.getOwnerKeyPair().setPeerId(this.myself.getNodeId().value());

        createdFileLog(myself);
        this.reputationsManager = new ReputationsManager();
        this.routingTable = new RoutingTable(myself,reputationsManager);
        this.neighboursManager = new NeighboursConnections(this);
        this.mkademliaNetwork = new KademliaNetwork(this);
        this.networkEvent = new NetworkEvent(this.neighboursManager, this.mLogger);
        this.mBlockEventUseCase = new BlockEventUseCase(this.networkGateway, this.networkEvent);
        this.mChainSyncManager = new ChainSyncUseCase(this.networkGateway, this.networkEvent,this);

        try {
            keys.setOwnPeerIdAndSave(this.myself.getNodeId().value());
            System.out.println("[DEBUG] Peer initialization complete.");
            System.out.println("[DEBUG] - Fingerprint: " + keys.getOwnFingerprint());
            System.out.println("[DEBUG] - PeerId: " + myself.getNodeId().value());
        } catch (Exception e) {
            System.out.println("[ERROR] Save the keys in file: " + e.getMessage());
        }
        this.networkGateway.setNetworkDependencies(this.networkEvent, this);
    }

    public NeighboursConnections getNeighboursManager() {return neighboursManager;}
    public Logger getLogger() {return mLogger;}
    public RoutingTable getRoutingTable() { return routingTable; }
    public HybridLogicalClock getHybridLogicalClock() { return hybridLogicalClock; }
    public NetworkGateway getNetworkGateway() { return networkGateway; }
    public ChainSyncUseCase getmChainSyncController(){ return mChainSyncManager;}
    public BlockEventUseCase getBlockEventManger(){ return mBlockEventUseCase;}
    public NetworkEvent getNetworkEvent(){return networkEvent;}
    public KademliaNetwork getMkademliaNetwork( ) {return mkademliaNetwork;}
    public ReputationsManager getReputationsManager(){return reputationsManager;}
    public BrokerEvent getGlobalScheduler() {return mBrokerEvent;}

    private void createdFileLog(Node myself){
        try {
            mLogger = Logger.getLogger("PeerLogger_" + myself.getHost() + "_" + myself.getPort());
            mLogger.setUseParentHandlers(false);
            mLogger.setLevel(Level.ALL);
            FileHandler handler = new FileHandler(
                    String.format("%s_%d_peer.log", myself.getHost(), myself.getPort()), true);
            handler.setFormatter(new SimpleFormatter());
            mLogger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Node getMyself() { return myself;}
    public boolean getIsRunning() { return running; }
    public KeysInfrastructure getIsKeysInfrastructure() { return keys; }

    public void startPeer(){
      running = true;
      try {

          server = new ServerSocket(myself.getPort());
          System.out.println("[INFO] Peer initialization at " + myself.getHost() + ":" + myself.getPort());
          Thread serverThread = new Thread(new ServerHandle(server, mLogger, this));
          serverThread.start();
      }catch (Exception e){
          System.out.println("[ERROR] in startPeer: " + e.getMessage());
          server = null;
          running = false;
          stopPeer();
      }
    }


    public void stopPeer(){
        if (!running) return;

        running = false;
        try {
            if (server != null && !server.isClosed()) {
                server.close();
            }
            mLogger.info("Peer shutdown: " + myself.getHost() + ":" + myself.getPort());
        }catch (Exception e){
            System.out.println("[ERROR] to the shutdown server: " + e.getMessage());
        }
    }
}

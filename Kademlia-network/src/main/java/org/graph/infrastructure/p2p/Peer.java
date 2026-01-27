package org.graph.infrastructure.p2p;

import org.graph.domain.application.kademlia.RoutingTable;
import org.graph.domain.application.p2p.NeighboursConnections;
import org.graph.domain.application.mechanism.pow.MiningResult;
import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.crypt.KeysInfrastructure;
import org.graph.infrastructure.network.kademlia.KademliaNetwork;
import org.graph.infrastructure.networkTime.HybridLogicalClock;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.graph.infrastructure.utils.Constants.HOST;
import static org.graph.infrastructure.utils.Constants.NETWORK_DIFFICULTY;


public class Peer {
    private ServerSocket server;
    private Node myself;
    private RoutingTable routingTable;
    private Logger mLogger;
    private volatile boolean running;
    private KeysInfrastructure keys;
    private NeighboursConnections neighboursManager;
    private KademliaNetwork kademliaNetwork;
    private HybridLogicalClock hybridLogicalClock;


    public Peer(int port){
        this.keys = new KeysInfrastructure( this, port);
        this.hybridLogicalClock = new HybridLogicalClock();

        MiningResult proofOfWork = null;
        try {
            proofOfWork = MinerOrchestrator.executeMining(keys.getOwnerPublicKey());
        } catch (Exception e) {
            throw new RuntimeException("[CRITICAL] Failed to initialize Peer identity via Mining.", e);
        }

        this.myself = new Node(HOST, port, keys.getOwnerPublicKey(), proofOfWork.nonce(), NETWORK_DIFFICULTY);

        this.keys.getOwnerKeyPair().setPeerId(this.myself.getNodeId().value());
        this.routingTable = new RoutingTable(myself);
        this.neighboursManager = new NeighboursConnections(this);
        this.kademliaNetwork = new KademliaNetwork(this);
        try {
            keys.setOwnPeerIdAndSave(this.myself.getNodeId().value());
            System.out.println("[DEBUG] Peer initialization:");
            System.out.println("[DEBUG] - Fingerprint: " + keys.getOwnFingerprint());
            System.out.println("[DEBUG] - PeerId: " + myself.getNodeId().value());
        } catch (Exception e) {
            System.out.println("[ERROR] Save the keys in file: " + e.getMessage());
        }

        createdFileLog(myself);
    }

    public NeighboursConnections getNeighboursManager() {
        return neighboursManager;
    }
    public Logger getLogger() {return mLogger;}
    public RoutingTable getRoutingTable() { return routingTable; }
    public HybridLogicalClock getHybridLogicalClock() { return hybridLogicalClock; }

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


    private void stopPeer(){
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

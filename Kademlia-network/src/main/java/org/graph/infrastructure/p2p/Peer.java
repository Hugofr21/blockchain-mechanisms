package org.graph.infrastructure.p2p;

import org.graph.domain.application.kademlia.RoutingTable;
import org.graph.domain.application.p2p.NeighboursConnections;
import org.graph.domain.application.pow.MiningResult;
import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.crypt.KeysInfrastructure;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Peer {
    private static String HOST = "localhost";
    private ServerSocket server;
    private Node myself;
    private RoutingTable routingTable;
    private Logger mLogger;
    private volatile boolean running;
    private KeysInfrastructure keys;
    private NeighboursConnections neighboursManager;



    public Peer(int port){
        this.keys = new KeysInfrastructure( this);

        MiningResult proofOfWork = null;
        try {
            // 3. BLOQUEANTE: Executa mineração para obter identidade válida (S/Kademlia)
            // O Peer não existe até provar trabalho.
            System.out.println("[INFO] Starting initialization PoW...");
            // O invokeAny agora garantirá que, se retornar, será um objeto válido ou lançará exceção
            proofOfWork = MinerOrchestrator.executeMining(keys.getOwnerPublicKey());

            // Verificação Defensiva: Jamais confie cegamente no retorno de operações complexas
            if (proofOfWork == null) {
                throw new IllegalStateException("MiningOrchestrator returned null illegally.");
            }

            // Agora é seguro acessar o getNonce()
            System.out.println("[DEBUG] PoW Solved! Nonce: " + proofOfWork.getNonce() +
                    " | Hash: " + proofOfWork.getNodeId().toString(16));


        } catch (Exception e) {
            throw new RuntimeException("[CRITICAL] Failed to initialize Peer identity via Mining.", e);
        }

        this.myself = new Node(HOST, port, keys.getOwnerPublicKey(), proofOfWork);

        this.keys.getOwnerKeyPair().setPeerId(this.myself.getNodeId().getValue());
        this.routingTable = new RoutingTable(myself);
        this.neighboursManager = new NeighboursConnections(this);
        try {
            keys.setOwnPeerIdAndSave(this.myself.getNodeId().getValue());
            System.out.println("[DEBUG] Peer initialization:");
            System.out.println("[DEBUG] - Fingerprint: " + keys.getOwnFingerprint());
            System.out.println("[DEBUG] - PeerId: " + myself.getNodeId().getValue());
        } catch (Exception e) {
            System.out.println("[ERROR] Save the keys in file: " + e.getMessage());
        }

        createdFileLog(myself);
    }

    public NeighboursConnections getNeighboursManager() {
        return neighboursManager;
    }

    public RoutingTable getRoutingTable() { return routingTable; }

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

    public void startPeer(){
      running = true;
      try {
          server = new ServerSocket(myself.getPort());
          System.out.println("[INFO] Peer initialization at " + myself.getHost() + ":" + myself.getPort());
          Thread serverThread = new Thread(new ServerHandle(server, mLogger, this));
          serverThread.start();
      }catch (Exception e){
          System.out.println("[ERROR] in startPeer: " + e.getMessage());
      }finally {
          server = null;
          stopPeer();
      }

    }


    private void stopPeer(){
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

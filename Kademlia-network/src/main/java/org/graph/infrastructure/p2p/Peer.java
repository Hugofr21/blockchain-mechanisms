package org.graph.infrastructure.p2p;

import org.graph.domain.application.ServerHandle;
import org.graph.domain.entities.p2p.Node;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Peer {
    private ServerSocket server;
    private Node myself;
    private Logger mLogger;
    private volatile boolean running;

    public Peer(Node myself) {
        this.myself = myself;
        createdFileLog(myself);
    }

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

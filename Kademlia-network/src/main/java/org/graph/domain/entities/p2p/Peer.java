package org.graph.domain.entities.p2p;

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

    public boolean getIsRunning() { return running; }

    private void startPeer(){
      running = true;
    }

    private void connectToPeer(){

    }


    private void stopPeer(){

    }

}

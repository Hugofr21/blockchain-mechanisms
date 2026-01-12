package org.graph.server;

import org.graph.infrastructure.p2p.Peer;

import static org.graph.server.utils.Constants.BOOTSTRAP_PORT;

public class LauncherBootstrap {
    public static void main(String[] args) {

        Peer peer = new Peer(BOOTSTRAP_PORT);
        peer.startPeer();
        System.out.println("[BOOTSTRAPPING] Initialized bootstrap: " + peer.getMyself().toString());
        System.out.println("\n================== BOOTSTRAPPING ================");

    }
}

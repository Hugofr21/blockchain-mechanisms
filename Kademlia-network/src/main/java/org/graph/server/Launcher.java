package org.graph.server;

import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.p2p.Peer;
import org.graph.server.utils.MenuUtils;

public class Launcher {
    public static void main(String[] args) {
        MenuUtils.printMenu(args);

        int port = Integer.parseInt(args[0]);
        if (port >= 0) {
            Peer peer = new Peer(port);
            peer.startPeer();
            System.out.println("Peer started: " + peer.getMyself().toString());
        }
    }
}

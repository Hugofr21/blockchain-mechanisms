package org.graph.server;


import org.graph.infrastructure.network.kademlia.JoinNetwork;
import org.graph.infrastructure.p2p.Peer;
import org.graph.server.utils.MenuUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.graph.server.utils.Constants.BOOTSTRAP_PORT;

public class Launcher {
    public static void main(String[] args) {
        MenuUtils.printMenu(args);

        int port = Integer.parseInt(args[0]);
        Peer peer;
        if (port >= 0) {
            peer = new Peer(port);
            peer.startPeer();
            System.out.println("Peer started: " + peer.getMyself().toString());
        } else {
            peer = null;
        }

        System.out.println("Peer iniciado: " + peer.getMyself());

        new Thread(() -> {
            try {

                Thread.sleep(1000);

                JoinNetwork joiner = new JoinNetwork(peer);
                joiner.attemptJoin("localhost", BOOTSTRAP_PORT);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

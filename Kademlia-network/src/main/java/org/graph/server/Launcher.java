package org.graph.server;


import org.graph.adapter.inbound.network.JoinNetwork;
import org.graph.server.utils.MenuUtils;

import static org.graph.server.utils.Constants.BOOTSTRAP_PORT;

public class Launcher {
    public static void main(String[] args) {
        MenuUtils.printMenu(args);

        if (args.length < 1) {
            System.err.println("Erro: Porta de execução não fornecida.");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        char[] nodeSecret = SecurityBootstrapper.obtainNodePassword();

        Peer peer = new Peer(port, nodeSecret);
        peer.startPeer();
        System.out.println("Peer started: " + peer.getMyself().toString());

        java.util.Arrays.fill(nodeSecret, '\0');

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                JoinNetwork joiner = new JoinNetwork(peer);
                joiner.attemptJoin("localhost", BOOTSTRAP_PORT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        MenuUtils.showMainMenu(peer);
    }
}
package org.graph.server;


import org.graph.adapter.inbound.network.JoinNetwork;
import org.graph.server.utils.MenuUtils;
import org.graph.server.utils.MetricsLogger;

import static org.graph.server.utils.Constants.BOOTSTRAP_PORT;

public class Launcher {
    public static void main(String[] args) {
        MenuUtils.printMenu(args);

        if (args.length < 3) {
            System.err.println("[ERRO FATAL] Inefficient arguments. Expected <host_local> <porta_local> <host_bootstrap>");
            System.exit(1);
        }


        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String bootstrapHost = args[2];
        int prometheusPort = Integer.parseInt(args[3]);
        char[] nodeSecret = SecurityBootstrapper.obtainNodePassword();

        MetricsLogger.init(prometheusPort);

        Peer peer = new Peer(host, port, nodeSecret);
        peer.startPeer();
        System.out.println("Peer started: " + peer.getMyself().toString());

        java.util.Arrays.fill(nodeSecret, '\0');


        new Thread(() -> {
            try {
                Thread.sleep(1000);
                JoinNetwork joiner = new JoinNetwork(peer);

                joiner.attemptJoin(bootstrapHost, BOOTSTRAP_PORT);
            } catch (InterruptedException e) {
                System.err.println("[ERRO] Synchronization thread failed:: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }).start();

        MenuUtils.showMainMenu(peer);

        peer.startBackgroundTasks();
    }
}
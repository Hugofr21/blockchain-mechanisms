package org.graph.server;

import org.graph.server.http.HttpServer;
import org.graph.server.utils.MenuUtils;
import org.graph.server.utils.MetricsLogger;
import static org.graph.server.utils.Constants.BOOTSTRAP_PORT;

public class LauncherBootstrap {
    public static void main(String[] args)  {
        if (args.length < 4) {
            System.err.println("[ERRO FATAL] Inefficient arguments. Expected: <host_local> <port_p2p> <port_prometheus> <port_http>");
            System.exit(1);
        }

        System.out.println("\n================== BOOTSTRAPPING ================");

        String host = args[0];
        int prometheusPort = Integer.parseInt(args[2]);
        int portHttpServer = Integer.parseInt(args[3]);
        char[] nodeSecret = SecurityBootstrapper.obtainNodePassword();

        MetricsLogger.init(prometheusPort);

        Peer peer = new Peer(host, BOOTSTRAP_PORT, nodeSecret);
        peer.startPeer();

        peer.getNetworkGateway().getBlockchainEngine().createGenesisBlock();
        System.out.println("[BOOTSTRAPPING] Initialized bootstrap: " + peer.getMyself());

        java.util.Arrays.fill(nodeSecret, '\0');

        peer.startBackgroundTasks();

        HttpServer httpServer = new HttpServer(peer, portHttpServer);
        httpServer.start();

        MenuUtils.showMainMenu(peer);
    }
}
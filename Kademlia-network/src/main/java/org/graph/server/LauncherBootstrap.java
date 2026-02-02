package org.graph.server;

import org.graph.server.utils.MenuUtils;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.graph.server.utils.Constants.BOOTSTRAP_PORT;

/**
 * Super peer that acts as a network access point.

 * This component is responsible for initializing the genesis block at system startup and for
 * managing the peer authentication process. After successful peer authentication,
 * the super peer sends you the corresponding block information, allowing for consistent integration
 * into the network.

 * In the context of the case study, it is possible to predefine a set of peers statically, using the file system. For this purpose, this class should be invoked:
 * @implNote Map<String, String> ngs = FileSystemUtils.neighbours();
 * The size of the configured peer set can be checked using:*
 * @implNote  System.out.println("ngs: " + ngs.size());
 * For testing purposes in a local environment (localhost), the execution arguments must
 * exactly match the values defined in the resource file containing the list
 * of peers, ensuring consistency between the configuration and system startup.
 */

public class LauncherBootstrap {

    public static void main(String[] args) throws IOException, URISyntaxException {

        Peer peer = new Peer(BOOTSTRAP_PORT);
        peer.startPeer();
        peer.getNetworkGateway().getBlockchainEngine().createGenesisBlock();
        System.out.println("[BOOTSTRAPPING] Initialized bootstrap: " + peer.getMyself());
        System.out.println("\n================== BOOTSTRAPPING ================");
        MenuUtils.showMainMenu(peer);

    }

}
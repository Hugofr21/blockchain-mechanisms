package org.graph.server;

import org.graph.adapter.p2p.Peer;
import org.graph.server.utils.MenuUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.graph.server.utils.Constants.BOOTSTRAP_PORT;


public class LauncherBootstrap {

    public static void main(String[] args) throws IOException, URISyntaxException {
//        Map<String, String> ngs = neighbours();
//        System.out.println("ngs: " + ngs.size());

        Peer peer = new Peer(BOOTSTRAP_PORT);
        peer.startPeer();
        System.out.println("[BOOTSTRAPPING] Initialized bootstrap: " + peer.getMyself());
        System.out.println("\n================== BOOTSTRAPPING ================");
        MenuUtils.showMainMenu(peer);
    }

//    private static Map<String, String> neighbours() throws IOException, URISyntaxException {
//        Map<String, String> neighbours = new HashMap<>();
//
//        URL resource = LauncherBootstrap.class.getClassLoader()
//                .getResource("boostrapp.neigbours.txt");
//        if (resource == null) {
//            System.err.println("[BOOTSTRAP] Resource 'boostrapp.neigbours.txt' not found.");
//            return neighbours;
//        }
//
//        Path path = Paths.get(resource.toURI());
//
//        if (Files.isRegularFile(path)) {
//            try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
//                stream.map(String::trim)
//                        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
//                        .forEach(line -> {
//                            String[] parts = line.split(":");
//                            if (parts.length == 2) {
//                                String host = parts[0].trim();
//                                String port = parts[1].trim();
//                                String mapKey = host + ":" + port;
//                                neighbours.put(mapKey, host);
//                            }
//                        });
//            }
//        }
//        return neighbours;
//    }
}
package org.graph.server.http;

import io.javalin.Javalin;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.websocket.WsContext;
import org.graph.api.AuctionController;
import org.graph.api.BlockController;
import org.graph.api.EnvironmentController;
import org.graph.api.NetworkController;
import org.graph.server.Peer;
import org.graph.server.dto.ApiResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServer {
    private final Javalin app;
    private final Peer peerContext;
    private final int httpPort;
    private final Map<String, WsContext> activeWebSockets;

    public HttpServer(Peer peer, int httpPort){
        this.peerContext = peer;
        this.httpPort = httpPort;
        this.activeWebSockets = new ConcurrentHashMap<>();
        this.app = Javalin.create(javalinConfig -> {
            javalinConfig.bundledPlugins.enableCors(corsPluginConfig -> {
                corsPluginConfig.addRule(CorsPluginConfig.CorsRule::anyHost);
            });
        });

        this.app.get("/", ctx -> {
            ctx.json(new ApiResponse("ok", "Server running", httpPort));
        });
        new BlockController(this.app, this.peerContext);
        new AuctionController(this.app, this.peerContext);
        new EnvironmentController(this.app, this.peerContext);
        new NetworkController(this.app, this.peerContext);
    }

    public void start() {
        app.start(this.httpPort);
        System.out.println("[HTTP] Server Observability instantiated on port " + this.httpPort);
    }

    public void stop() {
        app.stop();
    }
}
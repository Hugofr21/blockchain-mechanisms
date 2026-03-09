package org.graph.server.utils;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;

import java.util.concurrent.atomic.AtomicLong;

public class MetricsLogger {

    private static OpenTelemetry openTelemetry;
    private static Meter meter;

    private static DoubleHistogram networkLatency;
    private static LongCounter inboundThroughput;
    private static LongCounter outboundThroughput;
    private static final AtomicLong currentChainHeight = new AtomicLong(0);


    private static DoubleHistogram kademliaLookupHops;
    private static LongCounter kademliaCacheHits;
    private static LongCounter kademliaNetworkHits;
    private static LongCounter kademliaRpcErrors;

    private static boolean isInitialized = false;
    private static final AtomicLong routingTableSize = new AtomicLong(0);
    private static final AtomicLong dhtStorageSize = new AtomicLong(0);

    public static synchronized void init(int prometheusPort) {
        if (isInitialized) return;

        try {
            PrometheusHttpServer prometheusServer = PrometheusHttpServer.builder()
                    .setHost("0.0.0.0")
                    .setPort(prometheusPort)
                    .build();

            SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                    .registerMetricReader(prometheusServer)
                    .build();

            openTelemetry = OpenTelemetrySdk.builder()
                    .setMeterProvider(meterProvider)
                    .buildAndRegisterGlobal();

            meter = openTelemetry.getMeter("org.graph.p2p.node");

            networkLatency = meter.histogramBuilder("kademlia.network.latency")
                    .setDescription("Tempo de ida e volta (RTT) das mensagens RPC na DHT")
                    .setUnit("ms")
                    .build();

            inboundThroughput = meter.counterBuilder("network.throughput.inbound")
                    .setDescription("Quantidade de mensagens recebidas pelo ConnectionHandler")
                    .setUnit("messages")
                    .build();

            outboundThroughput = meter.counterBuilder("network.throughput.outbound")
                    .setDescription("Quantidade de mensagens enviadas pela rede")
                    .setUnit("messages")
                    .build();

            meter.gaugeBuilder("blockchain.chain.height")
                    .setDescription("Tamanho atual da cadeia principal consolidada")
                    .setUnit("blocks")
                    .ofLongs()
                    .buildWithCallback(measurement -> measurement.record(currentChainHeight.get()));

            kademliaLookupHops = meter.histogramBuilder("kademlia.lookup.hops")
                    .setDescription("Número de iterações/RPCs necessários num processo FIND_NODE ou FIND_VALUE")
                    .setUnit("hops").build();

            kademliaCacheHits = meter.counterBuilder("kademlia.cache.hits")
                    .setDescription("Valores encontrados na cache local (evitou rede)").build();

            kademliaNetworkHits = meter.counterBuilder("kademlia.network.hits")
                    .setDescription("Valores procurados iterativamente na rede DHT").build();

            kademliaRpcErrors = meter.counterBuilder("kademlia.rpc.errors")
                    .setDescription("Falhas de timeout ou rejeição de nós na rede").build();

            meter.gaugeBuilder("kademlia.routing.table.size")
                    .setDescription("Número de pares ativos na Routing Table")
                    .ofLongs().buildWithCallback(m -> m.record(routingTableSize.get()));

            meter.gaugeBuilder("kademlia.dht.storage.size")
                    .setDescription("Número de objetos atualmente guardados na DHT local")
                    .ofLongs().buildWithCallback(m -> m.record(dhtStorageSize.get()));

            isInitialized = true;
            System.out.println("[TELEMETRIA] Prometheus Exporter iniciado no porto: " + prometheusPort);

        } catch (Exception e) {
            System.err.println("[TELEMETRIA] Erro ao iniciar Prometheus: " + e.getMessage());
        }
    }

    public static void recordLatency(String peerId, double latencyMs) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(AttributeKey.stringKey("peer.id"), peerId);
        networkLatency.record(latencyMs, attrs);
    }

    public static void recordInboundMessage(String peerId) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(AttributeKey.stringKey("peer.id"), peerId);
        inboundThroughput.add(1, attrs);
    }

    public static void recordOutboundMessage(String peerId) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(AttributeKey.stringKey("peer.id"), peerId);
        outboundThroughput.add(1, attrs);
    }

    public static void updateChainHeight(long newHeight) {
        if (!isInitialized) return;
        currentChainHeight.set(newHeight);
    }

    public static void recordLookupHops(String operationType, int hops) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(AttributeKey.stringKey("operation"), operationType);
        kademliaLookupHops.record(hops, attrs);
    }

    public static void recordCacheHit() { if (isInitialized) kademliaCacheHits.add(1); }
    public static void recordNetworkHit() { if (isInitialized) kademliaNetworkHits.add(1); }

    public static void recordRpcError(String errorType) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(AttributeKey.stringKey("type"), errorType);
        kademliaRpcErrors.add(1, attrs);
    }

    public static void updateTopologyMetrics(long currentPeers, long currentStorageItems) {
        routingTableSize.set(currentPeers);
        dhtStorageSize.set(currentStorageItems);
    }
}
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

    private static DoubleHistogram networkLatency;
    private static LongCounter inboundThroughput;
    private static LongCounter outboundThroughput;
    private static final AtomicLong currentChainHeight = new AtomicLong(0);


    private static DoubleHistogram kademliaLookupHops;
    private static LongCounter kademliaCacheHits;
    private static LongCounter kademliaNetworkHits;
    private static LongCounter kademliaRpcErrors;


    private static LongCounter skademliaOperations;
    private static LongCounter skademliaMaliciousActivities;
    private static DoubleHistogram skademliaTrustScore;
    private static DoubleHistogram skademliaKbucketFilling;

    private static boolean isInitialized = false;
    private static final AtomicLong routingTableSize = new AtomicLong(0);
    private static final AtomicLong dhtStorageSize = new AtomicLong(0);

    private static final AtomicLong mempoolSize = new AtomicLong(0);
    private static final AtomicLong brokerQueueSize = new AtomicLong(0);
    private static DoubleHistogram blockMineDuration;
    private static LongCounter blockchainReorgs;

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

            OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                    .setMeterProvider(meterProvider)
                    .buildAndRegisterGlobal();

            Meter meter = openTelemetry.getMeter("org.graph.p2p.node");

            meter.gaugeBuilder("blockchain.mempool.size")
                    .setDescription("Número de transações pendentes a aguardar mineração")
                    .setUnit("transactions")
                    .ofLongs()
                    .buildWithCallback(m -> m.record(mempoolSize.get()));

            meter.gaugeBuilder("broker.event.queue.size")
                    .setDescription("Quantidade de mensagens acumuladas na fila de processamento assíncrono")
                    .setUnit("messages")
                    .ofLongs()
                    .buildWithCallback(m -> m.record(brokerQueueSize.get()));

            blockMineDuration = meter.histogramBuilder("blockchain.mine.duration")
                    .setDescription("Tempo despendido pelo CPU a resolver o Proof-of-Work de um bloco")
                    .setUnit("ms")
                    .build();

            blockchainReorgs = meter.counterBuilder("blockchain.reorganizations.total")
                    .setDescription("Número de vezes que a cadeia principal sofreu um Rollback (Fork resolvido)")
                    .setUnit("reorgs")
                    .build();
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

            skademliaOperations = meter.counterBuilder("skademlia.operations.total")
                    .setDescription("Resultados das operações Kademlia (Successful, Unsuccessful, Unended)")
                    .build();

            skademliaMaliciousActivities = meter.counterBuilder("skademlia.malicious.activities")
                    .setDescription("Comportamentos maliciosos detetados (Null list, Fake resource, Store refusal)")
                    .build();

            skademliaTrustScore = meter.histogramBuilder("skademlia.peer.trust_score")
                    .setDescription("Pontuação de confiança (t) calculada para os peers")
                    .build();

            skademliaKbucketFilling = meter.histogramBuilder("skademlia.kbucket.filling")
                    .setDescription("Percentagem média de preenchimento dos k-buckets")
                    .setUnit("%")
                    .build();

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

    /**
     * Regista o desfecho de uma operação (LOOKUP, GET, PUT)
     * Status esperados: "SUCCESSFUL", "UNSUCCESSFUL", "UNENDED"
     */
    public static void recordOperationStatus(String operationType, String status) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(
                AttributeKey.stringKey("operation"), operationType,
                AttributeKey.stringKey("status"), status
        );
        skademliaOperations.add(1, attrs);
    }

    /**
     * Regista a deteção de um comportamento anómalo/Sibil
     * Tipos esperados: "NULL_LIST", "FAKE_RESOURCE", "STORE_REFUSAL"
     */
    public static void recordMaliciousBehavior(String peerId, String behaviorType) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(
                AttributeKey.stringKey("peer.id"), peerId,
                AttributeKey.stringKey("behavior"), behaviorType
        );
        skademliaMaliciousActivities.add(1, attrs);
    }

    /**
     * Regista o Trust Score atualizado de um Peer específico
     */
    public static void recordTrustScore(String peerId, double trustScore) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(AttributeKey.stringKey("peer.id"), peerId);
        skademliaTrustScore.record(trustScore, attrs);
    }

    /**
     * Regista a percentagem de preenchimento de um K-Bucket
     */
    public static void recordKBucketFilling(String bucketIndex, double fillPercentage) {
        if (!isInitialized) return;
        Attributes attrs = Attributes.of(AttributeKey.stringKey("bucket.index"), bucketIndex);
        skademliaKbucketFilling.record(fillPercentage, attrs);
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

    public static void updateMempoolSize(long currentSize) {
        if (isInitialized) mempoolSize.set(currentSize);
    }

    public static void updateBrokerQueueSize(long currentSize) {
        if (isInitialized) brokerQueueSize.set(currentSize);
    }

    public static void recordBlockMiningTime(double durationMs) {
        if (isInitialized) blockMineDuration.record(durationMs);
    }

    public static void recordChainReorg() {
        if (isInitialized) blockchainReorgs.add(1);
    }

}
package org.graph.server.utils;


import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;

import java.util.concurrent.atomic.AtomicLong;

public class MetricsLogger {

    private static OpenTelemetry openTelemetry;
    private static Meter meter;

    private static DoubleHistogram networkLatency;
    private static LongCounter inboundThroughput;
    private static LongCounter outboundThroughput;

    private static final AtomicLong currentChainHeight = new AtomicLong(0);
    private static boolean isInitialized = false;

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
}
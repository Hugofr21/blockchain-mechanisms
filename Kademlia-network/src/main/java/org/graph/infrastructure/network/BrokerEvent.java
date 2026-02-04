package org.graph.infrastructure.network;

import org.graph.domain.entities.message.Message;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @implSpec BrokerEvent implementa o padrão Mediator com o objetivo de coordenar a
 * publicação e a recepção de eventos de forma ordenada, garantindo a
 * entrega sequencial dentro de cada canal segundo uma lógica de prioridade
 * crescente (menor para maior). O componente administra regras de
 * roteamento lógico-rotacional, assegurando previsibilidade no fluxo de
 * eventos e evitando acoplamento direto entre publishers e subscribers.
 *
 * A principal finalidade é o desacoplamento estrutural entre produtores
 * e consumidores de eventos, permitindo a evolução independente de cada
 * parte e a aplicação centralizada de regras de roteamento, priorização
 * e distribuição. O modelo favorece a escalabilidade horizontal, uma vez
 * que novas instâncias de BrokerEvent podem ser adicionadas para suportar
 * maior volume de notificações ou integração com sistemas externos,
 * mantendo consistência e ordenação no processamento.
 */

public class BrokerEvent {
    private final PriorityBlockingQueue<MessageWrapper> queue;
    private final ExecutorService executor;
    private volatile boolean running = true;

    public BrokerEvent() {
        this.queue = new PriorityBlockingQueue<>();
        this.executor = Executors.newSingleThreadExecutor();
        startConsumer();
    }

    public void submit(Message message, ConnectionHandler source) {
        queue.offer(new MessageWrapper(message, source));
    }

    private void startConsumer() {
        executor.submit(() -> {
            while (running) {
                try {
                    MessageWrapper item = queue.take();
                    item.source.dispatch(item.message);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[ERROR] Fail process to receive message: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private record MessageWrapper(Message message, ConnectionHandler source) implements Comparable<MessageWrapper> {
        @Override
        public int compareTo(MessageWrapper o) {
            return this.message.getTimestamp().compareTo(o.message.getTimestamp());
        }
    }
}
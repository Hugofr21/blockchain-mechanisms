package org.graph.infrastructure.network.kademlia;

import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.p2p.ConnectionHandler;
import org.graph.infrastructure.p2p.Peer;
import org.graph.infrastructure.utils.SerializationUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public record JoinNetwork(Peer myPeer) {

    /**
     * Attempts to connect to the network by accessing the Bootstrap IP:Port.
     * This is the entry point.
     */
    public void attemptJoin(String bootstrapHost, int bootstrapPort) {
        System.out.println("[JOIN] Conectando ao Bootstrap " + bootstrapHost + ":" + bootstrapPort);

        try (Socket socket = new Socket()) {
            // 1. Conexão TCP Física (Raw Socket)
            socket.connect(new InetSocketAddress(bootstrapHost, bootstrapPort), 5000); // 5s timeout

            // 2. Prepara o Handler (Ele vai gerir o protocolo)
            ConnectionHandler handler = new ConnectionHandler(socket, myPeer, myPeer.getLogger());

            // 3. O RITUAL DE SEDUÇÃO (Handshake S/Kademlia)
            // Aqui trocamos as chaves públicas e validamos o PoW.
            // Se retornar true, significa que o Bootstrap é autêntico.
            if (handler.performHandshake()) {

                // Recuperamos o objeto Node completo (com ID e Chaves) que o handler criou
                Node bootstrapNode = handler.getRemoteNode();

                System.out.println("[JOIN] Bootstrap Autenticado! ID: " + bootstrapNode.getNodeId());

                // 4. PERSISTÊNCIA DA CONEXÃO
                // Adicionamos ao gerenciador para manter o TCP aberto (Heartbeat)
//                myPeer.getNeighboursManager().addConnection(handler);

                // Adicionamos à Tabela de Roteamento (Bucket 0 provavelmente)
                myPeer.getRoutingTable().addNode(bootstrapNode);

                // 5. LOOKUP INICIAL (A "Mágica" do Kademlia)
                // Disparamos a busca para popular nossa tabela
                triggerBootstrapLookup(handler);

                // Opcional: Inicia a thread de escuta deste handler para processar a resposta do Lookup
                // Se o ConnectionHandler for Runnable, precisamos iniciá-lo ou usar o do pool
                new Thread(handler).start();

            } else {
                System.err.println("[JOIN] Handshake rejeitado pelo Bootstrap.");
                socket.close();
            }

        } catch (IOException e) {
            System.err.println("[JOIN] Bootstrap offline ou inacessível: " + e.getMessage());
        }
    }

    /**
     * Envia um FIND_NODE procurando pelo MEU PRÓPRIO ID.
     * Isso força o Bootstrap a me devolver os vizinhos mais próximos de mim.
     */
    private void lookUp(ConnectionHandler handler) {
        // Alias para triggerBootstrapLookup
        triggerBootstrapLookup(handler);
    }

    private void triggerBootstrapLookup(ConnectionHandler handler) {
        try {
            System.out.println("[JOIN] Disparando Lookup (FIND_NODE) buscando a mim mesmo...");

            // Kademlia Rule: Para entrar na rede, procure por si mesmo.
            // Isso faz o Bootstrap retornar os nós que deveriam ser meus vizinhos.
            byte[] targetId = SerializationUtils.serialize(
                    myPeer.getMyself().getNodeId().value()
            );

            Message lookupMsg = new Message(MessageType.FIND_NODE, targetId);

            // Envia a mensagem. A resposta (ACK com lista de nós) virá assincronamente
            // e será processada pelo método 'handleMessage' dentro do ConnectionHandler.
            handler.sendMessage(lookupMsg);

        } catch (IOException e) {
            System.err.println("[JOIN] Erro ao enviar Lookup: " + e.getMessage());
        }
    }
}

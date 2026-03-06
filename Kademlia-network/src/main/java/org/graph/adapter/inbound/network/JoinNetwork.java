package org.graph.adapter.inbound.network;

import org.graph.adapter.outbound.network.message.node.FindNodePayload;
import org.graph.adapter.utils.Base64Utils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.server.Peer;
import org.graph.adapter.utils.MessageUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;

/**
 * Quando um nó pretende juntar-se à rede, estabelece ligação a um nó
 * de bootstrap que foi previamente autenticado e autorizado para essa rede.
 *
 * O processo decorre da seguinte forma:
 * <ol>
 *   <li>O nó autentica-se na rede.</li>
 *   <li>Envia uma mensagem {@code FindNode} para localizar nós próximos
 *       do seu identificador.</li>
 *   <li>Valida e confirma, junto do nó de bootstrap, a lista de nós recebida.</li>
 * </ol>
 */

public record JoinNetwork(Peer myPeer) {
    /**
     * O method {@code attemptJoin()} é utilizado para iniciar a entrada de um nó
     * numa rede Kademlia recorrendo a um nó bootstrap que possui uma lista de nós conhecidos.
     *
     * <p>
     * Este method deve ser invocado **após a autenticação e handshake** (modo 3)
     * ter sido concluído com sucesso. A função realiza um pedido ao nó bootstrap
     * para obter a lista de nós mais próximos da identidade do nó que pretende entrar
     * na rede.
     * </p>
     *
     * @param bootstrapHost Endereço do nó bootstrap na sub-rede privada, podendo
     *                      ser, por exemplo, {@code localhost} ou {@code 127.0.0.1}
     *                      (loopback da placa de rede local).
     * @param bootstrapPort Porta do nó bootstrap. Por defeito, a classe
     *                      {@code LauncherBootstrap} utiliza a porta {@code 5001}.
     */

    public void attemptJoin(String bootstrapHost, int bootstrapPort) {
        System.out.println("[JOIN] Connecting to Bootstrap " + bootstrapHost + ":" + bootstrapPort);

        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(bootstrapHost, bootstrapPort), 5_000);

            ConnectionHandler handler = new ConnectionHandler(socket, myPeer, myPeer.getLogger());
            handler.initStreams();


            Optional<Node> optBootstrap = Handshake.doHandshake(
                    myPeer,
                    handler.getInputStream(),
                    handler.getOutputStream()
            );

            if (optBootstrap.isEmpty()) {
                System.err.println("[JOIN] Handshake rejected by Bootstrap.");
                socket.close();
                return;
            }

            Node bootstrapNode = optBootstrap.get();
            System.out.println("[JOIN] Bootstrap authenticated! ID: " + bootstrapNode.getNodeId());


            handler.setRemoteNode(bootstrapNode);
            myPeer.getNeighboursManager().addConnection(bootstrapNode, handler);
            myPeer.getRoutingTable().addNode(bootstrapNode, myPeer);

            new Thread(handler).start();

           myPeer.getmChainSyncController().startInitialSync(handler);
            triggerBootstrapLookup(handler);


        } catch (IOException e) {
            System.err.println("[JOIN] Bootstrap offline or inaccessible: " + e.getMessage());
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException ex) { }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Após verificar a autenticidade e a fiabilidade do nó,
     * é enviado um pedido à rede para localizar o conjunto
     * de nós mais próximos do identificador do nó em questão.
     *
     * @param handler thread ativa responsável por receber todas
     *                as respostas da rede através do socket.
     */
    private void triggerBootstrapLookup(ConnectionHandler handler) {
        try {
            BigInteger targetId = myPeer.getMyself().getNodeId().value();
            FindNodePayload payload = new FindNodePayload(Base64Utils.encode(targetId.toByteArray()));
            Message lookup = new Message(MessageType.FIND_NODE, payload, myPeer.getHybridLogicalClock());
            MessageUtils.sendMessage(handler.getOutputStream(), lookup);

        } catch (IOException e) {
            System.err.println("[JOIN] Error of to send  Lookup: " + e.getMessage());
        }
    }
}
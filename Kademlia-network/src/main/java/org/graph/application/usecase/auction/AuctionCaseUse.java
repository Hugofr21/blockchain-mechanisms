package org.graph.application.usecase.auction;

import org.graph.application.usecase.provider.BlockListener;
import org.graph.application.usecase.provider.TransactionsPublished;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.entities.transaction.TransactionType;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.auctions.Bid;
import org.graph.domain.valueobject.utils.HashUtils;
import org.graph.adapter.outbound.network.message.auction.AuctionOpType;
import org.graph.adapter.outbound.network.message.auction.AuctionPayload;
import org.graph.server.Peer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Este componente é responsável pela gestão de leilões (Auctions) tanto em contexto local
 * quanto remoto, atuando como camada intermediária entre a lógica de negócio dos leilões
 * e o mecanismo de persistência baseado em blockchain. A sua função central é garantir que
 * o estado dos leilões permaneça consistente, mesmo perante atrasos, reordenação de mensagens
 * ou divergências temporárias entre nós distribuídos.
 *
 * É fundamental esclarecer que, neste projeto, o leilão não segue o modelo clássico de leilão
 * inglês, no qual o vencedor é automaticamente o lance de maior valor. Essa decisão de projeto
 * invalida qualquer pressuposto simplista de comparação direta entre bids e exige um critério
 * explícito de ordenação e avaliação que seja coerente com as regras definidas pela aplicação.
 * Qualquer tentativa de assumir implicitamente que o maior lance vence constitui um erro
 * conceitual que compromete a validade do resultado do leilão.
 *
 * Um dos principais problemas identificados é a necessidade de ordenar os bids para determinar
 * corretamente a sua sequência lógica. Em sistemas distribuídos, a ordem de receção não reflete
 * necessariamente a ordem real de emissão, tornando incorreta qualquer lógica que dependa apenas
 * do momento em que o bid chega ao nó. A ordenação deve basear-se em critérios determinísticos,
 * como timestamps validados ou identificadores criptográficos, e não em suposições implícitas
 * sobre a rede.
 *
 * Outro ponto crítico diz respeito ao tratamento de blocos que não chegam na ordem esperada.
 * Rejeitar bids associados a esses blocos é uma abordagem incorreta e tecnicamente frágil, pois
 * resulta em perda de informação válida. A solução adotada consiste em manter esses bids numa
 * lista de pendentes, permitindo a sua integração posterior quando os blocos predecessores
 * forem finalmente recebidos e validados. Essa estratégia preserva a consistência sem violar
 * os princípios de tolerância a falhas.
 *
 * Por fim, é imposto como regra estrutural que auctions só podem ser persistidas quando estiverem
 * efetivamente contidas dentro de um bloco válido. Guardar auctions fora do contexto de um bloco
 * mina o modelo de imutabilidade da blockchain e cria estados locais que não podem ser verificados
 * nem reproduzidos por outros nós. Qualquer abordagem que ignore essa restrição está conceitualmente
 * errada e deve ser descartada.
 */

public class AuctionCaseUse implements BlockListener {
    private final Map<String, AuctionState> ledger;
    private final Map<String, List<Bid>> pendingBids;
    private final TransactionsPublished serviceTransactions;
    private final ConcurrentHashMap<String, Long> userNonces;
    private final ConcurrentHashMap<String, Long> pendingUserNonces;

    public AuctionCaseUse(TransactionsPublished serviceTransactions) {
        this.ledger = new ConcurrentHashMap<>();
        this.pendingBids = new ConcurrentHashMap<>();
        this.serviceTransactions = serviceTransactions;
        this.userNonces = new ConcurrentHashMap<>();
        this.pendingUserNonces = new ConcurrentHashMap<>();
    }

    public Map<String, AuctionState> getWorldState() {
        return ledger;
    }

    @Override
    public void onBlockCommitted(Block block) {
        for (Transaction tx : block.getTransactions()) {
            try {
                // 1. Processa a lógica de negócio (se aplicável)
                processTransactionRemote(tx, block.getHeader().getTimestamp());

                // 2. CORREÇÃO: Só atualiza o Nonce se a transação tiver um remetente (OwnerId)
                if (tx.getSenderId() != null) {
                    String idKey = tx.getSenderId().toString(16);
                    long current = userNonces.getOrDefault(idKey, 0L);
                    if (tx.getNonce() > current) {
                        userNonces.put(idKey, tx.getNonce());
                    }
                }
            } catch (Exception e) {
                System.err.println("[AUCTION ENGINEER ERROR] Tx " +
                        (tx.getTxId() != null ? tx.getTxId().substring(0,8) : "null") +
                        ": " + e.getMessage());
            }
        }
    }

    private void processTransactionRemote(Transaction tx, long blockTimestamp) throws Exception {
        // 1. DIAGNÓSTICO: O payload sobreviveu à viagem pela rede?
        if (tx.getData() == null) {
            System.err.println("[DEBUG-AUCTION] Ignorado: tx.getData() está NULL. (Falta 'implements Serializable' no AuctionPayload?)");
            return;
        }
        if (!(tx.getData() instanceof AuctionPayload)) {
            System.err.println("[DEBUG-AUCTION] Ignorado: Dados não são AuctionPayload. Tipo: " + tx.getData().getClass().getName());
            return;
        }

        AuctionPayload payload = (AuctionPayload) tx.getData();

        if (payload.getOperation() == AuctionOpType.CREATE) {
            AuctionState remoteStateInfo = payload.getAuctionStateRemote();

            // 2. CORREÇÃO CRÍTICA: O ID do leilão é o ID que vem dentro do state, NÃO o ID da transação!
            String newAuctionId = remoteStateInfo.getAuctionId();

            if (ledger.containsKey(newAuctionId)) {
                System.out.println("[AUCTION ENGINEER] Leilão já existe localmente, ignorando duplicação.");
                return;
            }

            AuctionState newState = new AuctionState(
                    newAuctionId,
                    tx.getSenderId(),
                    remoteStateInfo.getMinPrice(),
                    remoteStateInfo.getEndTimestamp()
            );

            ledger.put(newAuctionId, newState);
            System.out.println("[AUCTION ENGINEER] OFFICIAL auction registado no Ledger: " + newAuctionId);

            processOrphanBids(newAuctionId, newState);

        } else if (payload.getOperation() == AuctionOpType.BID) {
            Bid bid = payload.getBidRemote();
            String auctionId = bid.auctionId();

            AuctionState state = ledger.get(auctionId);

            if (state != null) {
                applyBidToState(state, bid, blockTimestamp);
            } else {
                System.out.println("[AUCTION ENGINEER] Bid recebido para leilão desconhecido. Guardado nos pendentes: " + auctionId);
                pendingBids.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(bid);
            }
        }
    }


    private void processOrphanBids(String auctionId, AuctionState state) {
        List<Bid> orphans = pendingBids.remove(auctionId);

        if (orphans != null && !orphans.isEmpty()) {
            System.out.println("[AUCTION ENGINEER] Replay de " + orphans.size() + " pending bids...");
            orphans.sort(Comparator.comparingLong(Bid::throwTimestamp));

            for (Bid orphanBid : orphans) {
                try {
                    applyBidToState(state, orphanBid, orphanBid.throwTimestamp());
                } catch (Exception e) {
                    System.err.println("[AUCTION ENGINEER ERROR] Old bid invalid: " + e.getMessage());
                }
            }
        }
    }

    private void applyBidToState(AuctionState state, Bid bid, long timestampForValidation) {
        synchronized (state) {
            if (timestampForValidation > state.getEndTimestamp()) {
                state.closeAuction();
                throw new IllegalStateException("Auction expired.");
            }
            if (!state.isOpen()) throw new IllegalStateException("Auction closed.");

            if (bid.bidPrice().compareTo(state.getCurrentHighestBid()) <= 0) {
                throw new IllegalStateException("Insufficient bid (" + bid.bidPrice() + " <= " + state.getCurrentHighestBid() + ")");
            }

            state.addSuccessfulBid(bid);
            System.out.println("[AUCTION ENGINEER] Bid Accepted:" + bid.bidPrice());
        }
    }

    /**
     * Obtém o PRÓXIMO nonce válido para um utilizador.
     * Consulta o estado consolidado e soma 1.
     */
    public synchronized long reserveNextNonce(BigInteger ownerId) {
        String idKey = ownerId.toString(16);
        long confirmed = userNonces.getOrDefault(idKey, 0L);
        long pending = pendingUserNonces.getOrDefault(idKey, 0L);
        long nextNonce = Math.max(confirmed, pending) + 1;
        pendingUserNonces.put(idKey, nextNonce);

        return nextNonce;
    }

    public synchronized long getExpectedLedgerNonce(BigInteger ownerId) {
        String idKey = ownerId.toString(16);
        return userNonces.getOrDefault(idKey, 0L) + 1;
    }

    public void createdLocalAuctions(BigDecimal startPrice, Peer myself) {
        long durationMillis = 24L * 60L * 60L * 1000L;
        long endTime = System.currentTimeMillis() + durationMillis;

        String entropyId = HashUtils.calculateSha256(myself.getMyself().getNodeId().value() + startPrice.toString() + endTime);

        AuctionState newAuction = new AuctionState(
                entropyId,
                myself.getMyself().getNodeId().value(),
                startPrice,
                endTime
        );

        AuctionPayload payload = AuctionPayload.create("New Auction", newAuction);
        long nonce = reserveNextNonce(myself.getMyself().getNodeId().value());

        Transaction tx = new Transaction(
                TransactionType.AUCTION_CREATED,
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                payload,
                myself.getMyself().getNodeId().value(),
                nonce,
                myself.getHybridLogicalClock().getPhysicalClock()
        );

        signAndSubmit(tx, myself);

        subscribeToAuction(newAuction.getAuctionId(), myself);

    }

    public void placeBidRequest(String auctionId, BigDecimal bidValue, Peer myself) {
        AuctionState auctionState = ledger.get(auctionId);

        if (auctionState == null) {
            throw new IllegalArgumentException("Error: The specified auction does not exist in the local ledger.");
        }

        if (!auctionState.isOpen() || System.currentTimeMillis() > auctionState.getEndTimestamp()) {
            throw new IllegalStateException("Error: The auction is closed. No new bids are accepted.");
        }

        if (bidValue.compareTo(auctionState.getCurrentHighestBid()) <= 0) {
            throw new IllegalArgumentException("Business error: The bid value (" + bidValue +
                    ") must be strictly greater than the current maximum value (" + auctionState.getCurrentHighestBid() + ").");
        }

        Bid newBid = new Bid(
                auctionId,
                bidValue,
                System.currentTimeMillis(),
                myself.getMyself().getNodeId().value()
        );

        if (auctionState.getBidHistory().contains(newBid)) {
            throw new IllegalStateException("Erro: Este lance exato já se encontra registado.");
        }

        AuctionPayload payload = AuctionPayload.bid(newBid);
        long nonce = reserveNextNonce(myself.getMyself().getNodeId().value());

        Transaction tx = new Transaction(
                TransactionType.BID,
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                payload,
                myself.getMyself().getNodeId().value(),
                nonce,
                myself.getHybridLogicalClock().getPhysicalClock()
        );

        signAndSubmit(tx, myself);
        subscribeToAuction(auctionId, myself);
        notifySubscribersOfNewBid(auctionId, tx, myself);
    }

    private void signAndSubmit(Transaction tx, Peer myself) {
        try {
            String data = tx.getDataSign();
            byte[] signature = myself.getIsKeysInfrastructure().signMessage(data);
            tx.setSignature(signature);
            serviceTransactions.submitTransaction(tx);
            System.out.println("[CLIENT] Tx submitted: " + tx.getTxId());
        } catch (Exception ex) {
            System.err.println("[AUCTION ENGINEER] Signature error: " + ex.getMessage());
        }
    }

    private void subscribeToAuction(String auctionId, Peer myself){
        String topicId = HashUtils.calculateSha256("subscribers_" + auctionId);
        BigInteger topicKey = new BigInteger(topicId, 16);
        Set<Node> subscriberSet = new HashSet<>();
        subscriberSet.add(myself.getMyself());
        System.out.println("[PUB/SUB] Subscribing to auction: " + auctionId);
        myself.getMkademliaNetwork().storage(topicKey, subscriberSet);
    }


    private void notifySubscribersOfNewBid(String auctionId, Transaction tx, Peer myself) {
        new Thread(() -> {
            try {
                String topicId = HashUtils.calculateSha256("subscribers_" + auctionId);
                BigInteger topicKey = new BigInteger(topicId, 16);
                Object result = myself.getMkademliaNetwork().findValue(topicKey, Object.class);

                if (result instanceof Set<?> subscribers) {
                    System.out.println("[PUB/SUB] Encontrados " + subscribers.size() + " subscritores para notificar.");

                    Message gossipMsg = new Message(MessageType.TRANSACTION, tx, myself.getHybridLogicalClock());

                    for (Object obj : subscribers) {
                        if (obj instanceof Node subscriberNode) {
                            if (subscriberNode.getNodeId().equals(myself.getMyself().getNodeId())) {
                                continue;
                            }
                            myself.getMkademliaNetwork().sendRPCAsync(subscriberNode, gossipMsg);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[PUB/SUB] Falha ao notificar subscritores: " + e.getMessage());
            }
        }).start();
    }
}
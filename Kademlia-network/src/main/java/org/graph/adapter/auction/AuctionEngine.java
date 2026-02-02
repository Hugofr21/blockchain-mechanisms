package org.graph.adapter.auction;

import org.graph.adapter.provider.BlockListener;
import org.graph.adapter.provider.TransactionsPublished;
import org.graph.domain.application.block.Block;
import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.application.transaction.TransactionType;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.auctions.Bid;
import org.graph.domain.utils.HashUtils;
import org.graph.adapter.network.message.auction.AuctionOpType;
import org.graph.adapter.network.message.auction.AuctionPayload;
import org.graph.server.Peer;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

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

public class AuctionEngine implements BlockListener {
    private final Map<String, AuctionState> ledger;
    private final Map<String, List<Bid>> pendingBids;
    private final TransactionsPublished serviceTransactions;

    public AuctionEngine(TransactionsPublished serviceTransactions) {
        this.ledger = new ConcurrentHashMap<>();
        this.pendingBids = new ConcurrentHashMap<>();
        this.serviceTransactions = serviceTransactions;
    }

    public Map<String, AuctionState> getWorldState() {
        return ledger;
    }

    @Override
    public void onBlockCommitted(Block block) {
        for (Transaction tx : block.getTransactions()) {
            try {
                processTransactionRemote(tx, block.getHeader().getTimestamp());
            } catch (Exception e) {
                System.err.println("[AUCTION ENGINEER ERROR] Tx " + tx.getTxId().substring(0,8) + ": " + e.getMessage());
            }
        }
    }

    private void processTransactionRemote(Transaction tx, long blockTimestamp) throws Exception {
        if (tx.getData() == null || !(tx.getData() instanceof AuctionPayload)) return;

        AuctionPayload payload = (AuctionPayload) tx.getData();


        if (payload.getOperation() == AuctionOpType.CREATE) {
            String newAuctionId = tx.getTxId();

            if (ledger.containsKey(newAuctionId)) return;

            AuctionState remoteStateInfo = payload.getAuctionStateRemote();
            AuctionState newState = new AuctionState(
                    newAuctionId,
                    tx.getSenderId(),
                    remoteStateInfo.getMinPrice(),
                    remoteStateInfo.getEndTimestamp()
            );


            ledger.put(newAuctionId, newState);
            System.out.println("[AUCTION ENGINEER] OFFICIAL auction: " + newAuctionId);

            processOrphanBids(newAuctionId, newState);

        } else if (payload.getOperation() == AuctionOpType.BID) {
            Bid bid = payload.getBidRemote();
            String auctionId = bid.auctionId();

            AuctionState state = ledger.get(auctionId);

            if (state != null) {

                applyBidToState(state, bid, blockTimestamp);
            } else {

//                System.out.println("[AUCTION ENGINEER] Bid received for unknown auction(" + bid.bidPrice() + "). Saving...");
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


    public void createdLocalAuctions(BigDecimal startPrice, Peer myself) {
        long durationMillis = 24L * 60L * 60L * 1000L;
        long endTime = System.currentTimeMillis() + durationMillis;
        String entropyId = HashUtils.calculateSha256(myself.getMyself().getNodeId().value() + startPrice.toString() + endTime);

        AuctionState tempStateDTO = new AuctionState(
                entropyId,
                myself.getMyself().getNodeId().value(),
                startPrice,
                endTime
        );

        AuctionPayload payload = AuctionPayload.create("New Auction", tempStateDTO);
        Transaction tx = new Transaction(
                TransactionType.AUCTION_CREATED,
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                payload,
                myself.getMyself().getNodeId().value()
        );

        signAndSubmit(tx, myself);
    }

    public void placeBidRequest(String auctionId, BigDecimal bidValue, Peer myself) {
        Bid newBid = new Bid(
                auctionId,
                bidValue,
                System.currentTimeMillis(),
                myself.getMyself().getNodeId().value()
        );

        AuctionPayload payload = AuctionPayload.bid(newBid);
        Transaction tx = new Transaction(
                TransactionType.BID,
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                payload,
                myself.getMyself().getNodeId().value()
        );

        signAndSubmit(tx, myself);
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
}
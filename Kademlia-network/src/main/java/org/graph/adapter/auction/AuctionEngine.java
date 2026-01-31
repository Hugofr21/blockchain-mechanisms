package org.graph.adapter.auction;

import org.graph.adapter.blockchain.BlockchainEngine;
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
import org.graph.adapter.p2p.Peer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionEngine implements BlockListener {
    private final Map<String, AuctionState> ledger;
    private final TransactionsPublished serviceTransactions; // Interface para enviar para Blockchain

    public AuctionEngine(TransactionsPublished serviceTransactions) {
        this.ledger = new ConcurrentHashMap<>();
        this.serviceTransactions = serviceTransactions;
    }

    public Map<String, AuctionState> getWorldState() {
        return ledger;
    }

    // =================================================================================
    // PARTE 1: OUVINTE (Recebe o Bloco Minerado -> Atualiza Ledger)
    // =================================================================================
    @Override
    public void onBlockCommitted(Block block) {
        System.out.println("[AUCTION] Processando novo bloco: " + block.getCurrentBlockHash());

        // Itera sobre todas as transações do bloco
        for (Transaction tx : block.getTransactions()) {
            try {
                // Tenta processar e atualizar o estado
                processTransactionRemote(tx, block.getHeader().getTimestamp());
            } catch (Exception e) {
                System.err.println("[ERROR] Falha ao processar Tx " + tx.getTxId() + ": " + e.getMessage());
            }
        }
    }

    // =================================================================================
    // PARTE 2: CLIENTE (Cria Transação -> Envia para Lista Pendente)
    // =================================================================================

    public void createdLocalAuctions(BigDecimal startPrice, Peer myself) {
        long durationMillis = 24L * 60L * 60L * 1000L;
        long endTime = System.currentTimeMillis() + durationMillis;

        // Gera o ID deterministicamente
        String auctionId = HashUtils.calculateSha256(myself.getMyself().getNodeId().value().toString() + startPrice + endTime);

        // Cria objeto de estado APENAS para transporte (Payload), não salva no ledger ainda
        AuctionState tempState = new AuctionState(
                auctionId,
                myself.getMyself().getNodeId().value(),
                startPrice,
                endTime
        );

        AuctionPayload payload = AuctionPayload.create("Created auctions of cars", tempState);

        Transaction tx = new Transaction(
                TransactionType.AUCTION_CREATED,
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                payload,
                myself.getMyself().getNodeId().value()
        );

        // Assinatura
        try {
            String data = tx.getDataSign();
            byte[] signature = myself.getIsKeysInfrastructure().signMessage(data);
            tx.setSignature(signature);
        } catch (Exception ex) {
            System.err.println("Erro ao assinar: " + ex.getMessage());
            return;
        }

        // CRÍTICO: Envia para a lista de transações da Blockchain
        // NÃO faz ledger.put aqui!
        serviceTransactions.submitTransaction(tx);
        System.out.println("[CLIENT] Requisição de Leilão enviada para Mempool: " + auctionId);
    }

    public void placeBidRequest(String auctionId, BigDecimal bidValue, Peer myself) {
        // Validação leve (apenas visual)
        AuctionState state = ledger.get(auctionId);
        if (state != null && !state.isOpen()) {
            System.out.println("Auction closed, no bidding possible..");
            return;
        }

        Bid newBid = new Bid(
                auctionId,
                bidValue,
                myself.getHybridLogicalClock().getPhysicalClock(),
                myself.getMyself().getNodeId().value()
        );

        AuctionPayload payload = AuctionPayload.bid(newBid);

        Transaction tx = new Transaction(
                TransactionType.BID,
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                payload,
                myself.getMyself().getNodeId().value()
        );

        // Assinatura
        try {
            String data = tx.getDataSign();
            byte[] signature = myself.getIsKeysInfrastructure().signMessage(data);
            tx.setSignature(signature);
        } catch (Exception ex) {
            System.err.println("Erro ao assinar: " + ex.getMessage());
            return;
        }

        // CRÍTICO: Envia para a lista de transações da Blockchain
        // NÃO atualiza o estado aqui!
        serviceTransactions.submitTransaction(tx);
        System.out.println("[CLIENT] Lance enviado para Mempool.");
    }

    // =================================================================================
    // PARTE 3: LÓGICA DE NEGÓCIO (Consenso)
    // =================================================================================

    public void processTransactionRemote(Transaction tx, long blockTimestamp) throws Exception {
        // Verifica se é payload de leilão
        if (!(tx.getData() instanceof AuctionPayload)) return;

        AuctionPayload payload = (AuctionPayload) tx.getData();

        if (payload.getOperation() == AuctionOpType.CREATE) {
            // AQUI O ESTADO É CRIADO OFICIALMENTE
            String newAuctionId = tx.getTxId();

            AuctionState newState = new AuctionState(
                    newAuctionId,
                    tx.getSenderId(),
                    payload.getAuctionStateRemote().getMinPrice(),
                    payload.getAuctionStateRemote().getEndTimestamp()
            );

            ledger.put(newAuctionId, newState);
            System.out.println("[LEDGER] Leilão Criado: " + newAuctionId);

        } else if (payload.getOperation() == AuctionOpType.BID) {
            // AQUI O LANCE É APLICADO OFICIALMENTE
            applyBid(payload, tx.getSenderId(), blockTimestamp);
        }
    }

    private void applyBid(AuctionPayload payload, BigInteger bidderId, long blockTime) throws Exception {
        Bid remoteBid = (Bid) payload.getBidRemote();
        AuctionState state = ledger.get(remoteBid.auctionId());

        if (state == null) throw new IllegalStateException("Auction not found: " + remoteBid.auctionId());

        if (blockTime > state.getEndTimestamp()) {
            state.closeAuction();
            throw new IllegalStateException("Auction expired");
        }

        if (remoteBid.bidPrice().compareTo(state.getCurrentHighestBid()) <= 0) {
            throw new IllegalStateException("Bid must be higher than current: " + state.getCurrentHighestBid());
        }

        // Atualiza o estado
        state.addSuccessfulBid(remoteBid);
        System.out.println("[LEDGER] Novo Lance Vencedor: " + remoteBid.bidPrice());
    }
}
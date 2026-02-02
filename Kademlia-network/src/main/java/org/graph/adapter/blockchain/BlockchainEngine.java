package org.graph.adapter.blockchain;

import org.graph.adapter.auction.AuctionEngine;
import org.graph.adapter.provider.BlockListener;
import org.graph.adapter.provider.TransactionsPublished;
import org.graph.domain.application.block.Block;
import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.application.transaction.TransactionType;
import org.graph.domain.common.Pair;
import org.graph.adapter.blockchain.block.BlockOrganizer;
import org.graph.adapter.blockchain.block.TransactionOrganizer;
import org.graph.server.Peer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * A classe BlockchainEngine constitui o núcleo lógico do sistema de blockchain deste projeto,
 * sendo responsável pela coordenação da criação, validação e encadeamento de blocos, bem como
 * pela gestão do ciclo de vida das transações. Trata-se do componente mais crítico da arquitetura,
 * uma vez que concentra as regras de consenso, os critérios de mineração e os mecanismos de
 * tolerância a falhas inerentes a sistemas distribuídos.
 *  Deve ter atenção que nao neste projecto leilao modo ingles vence lance mais alto.
 * Um dos problemas estruturais identificados reside na impossibilidade de permitir que objetos
 * críticos do sistema permaneçam em estado pendente por tempo indefinido. Em ambientes de
 * blockchain, estados intermediários prolongados introduzem riscos de inconsistência, consumo
 * desnecessário de recursos e potenciais condições de corrida. A BlockchainEngine deve, portanto,
 * impor limites temporais claros e mecanismos determinísticos para a resolução de pendências,
 * garantindo progresso contínuo do sistema.
 *
 * A política de mineração adotada combina dois critérios distintos e concorrentes: a criação de
 * um novo bloco ocorre quando o conjunto de transações atinge um limiar máximo de dez elementos
 * ou quando o tempo de espera na fila de transações excede dois segundos. Essa abordagem híbrida
 * busca equilibrar latência e eficiência, mas introduz complexidade adicional, pois exige controle
 * preciso de temporização e sincronização para evitar mineração prematura ou atrasos excessivos.
 *
 * A ocorrência de hard forks e soft forks representa outro desafio relevante. Forks surgem quando
 * diferentes nós mantêm visões divergentes da cadeia válida, seja por alterações incompatíveis
 * nas regras de consenso ou por atrasos e falhas de comunicação. A BlockchainEngine deve estar
 * preparada para detetar, tratar e, quando possível, resolver essas divergências, assegurando
 * a integridade histórica da cadeia e a convergência eventual do sistema.
 *
 * Adicionalmente, não se pode assumir que os blocos serão recebidos na mesma ordem em que foram
 * originalmente enviados. Em redes distribuídas, mensagens podem ser atrasadas, reordenadas ou
 * até modificadas de forma maliciosa durante o trajeto. Consequentemente, a validação de blocos
 * não pode depender apenas da ordem de receção, devendo basear-se em hashes, referências ao bloco
 * anterior e regras criptográficas estritas que impeçam alterações não autorizadas.
 *
 * Em síntese, a BlockchainEngine deve ser projetada com foco explícito em determinismo, robustez
 * e verificabilidade. Qualquer simplificação excessiva desses aspectos compromete a segurança e
 * a confiabilidade do sistema, tornando a blockchain vulnerável a inconsistências lógicas e
 * falhas de consenso.
**/

public class BlockchainEngine implements TransactionsPublished {
    private final int numThreads;
    private final int currentDifficulty;
    private final TransactionOrganizer mTransactionOrganizer;
    private final BlockOrganizer mBlockOrganizer;
    private final List<BlockListener> listeners;
    private long lastTxTime;
    private static final long MAX_WAIT_TIME_MS = 2000;
    private Peer myself;

    public BlockchainEngine(int difficulty, int maxTx, Peer myself) {
        this.mTransactionOrganizer = new TransactionOrganizer(maxTx);
        this.mBlockOrganizer = new BlockOrganizer(this);
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.currentDifficulty = difficulty;
        this.listeners = new ArrayList<>();
        this.lastTxTime = System.currentTimeMillis();
        this.myself = myself;
        startMiningWatchdog();
        
    }

    private void startMiningWatchdog() {
        Thread watchdog = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);

                    synchronized (this) {
                        int pending = mTransactionOrganizer.getPendingCount();
                        long timeDiff = System.currentTimeMillis() - lastTxTime;

                        if (pending > 0 && timeDiff > MAX_WAIT_TIME_MS) {
                            System.out.println("\n[WATCHDOG] Timeout atingido (" + pending + " txs). Forçando mineração...");
                            createNewBlock();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();
    }


    public TransactionOrganizer getTransactionOrganizer() {
        return mTransactionOrganizer;
    }
    public BlockOrganizer getBlockOrganizer() {
        return mBlockOrganizer;
    }

    public void addBlockListener(BlockListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void submitTransaction(Transaction tx) {
        addTransaction(tx);
    }

    private void notifyListeners(Block block) {
        for (BlockListener listener : listeners) {
            listener.onBlockCommitted(block);
        }
    }

    public void createGenesisBlock() {
        if (mBlockOrganizer.getChainHeight() >= 0) {
            System.out.println("[INIT] Blockchain já inicializada. Genesis ignorado.");
            return;
        }

        List<Transaction> genesisTx = new ArrayList<>();
        genesisTx.add(new Transaction(TransactionType.REGULAR_TRANSFER));
        Block genesis = new Block(1, 0, "0", genesisTx, currentDifficulty);
        genesis.mineBlock(currentDifficulty, numThreads);

        mBlockOrganizer.addLocalBlock(genesis);
    }

    public void addTransaction(Transaction tx) {
        // TODO: verify Signature
        if (tx == null) {
            System.err.println("[SEC] Transação inválida rejeitada.");
            return;
        }

        mTransactionOrganizer.addTransaction(tx);
        this.lastTxTime = System.currentTimeMillis();

        if (mTransactionOrganizer.shouldCreateBlock()) {
            System.out.println("\n[MINER] Mempool cheio. Iniciando mineração...");
            createNewBlock();
        }
    }

    public void createNewBlock() {
        List<Transaction> transactions = mTransactionOrganizer.getTransactionsForBlock();

        if (transactions == null || transactions.isEmpty()) {
            System.out.println("[INFO] No pending transactions");
            return;
        }

        System.out.println("[MINER] Criando Bloco #" + getInfoBlock().key() + " apontando para " + getInfoBlock().value());

        Block newBlock = new Block(1,getInfoBlock().key() , getInfoBlock().value(), transactions, currentDifficulty);
        newBlock.mineBlock(currentDifficulty, numThreads);

        mTransactionOrganizer.cleanPool(transactions);
        mBlockOrganizer.addLocalBlock(newBlock);

        this.lastTxTime = System.currentTimeMillis();
        notifyListeners(newBlock);

        BigInteger keyId = new BigInteger(newBlock.getCurrentBlockHash(), 16);
        myself.getMkademliaNetwork().storage(keyId, newBlock);

    }


    public boolean receiveBlockFromPeer(Block block) throws InterruptedException {
        System.out.println("\n[BLOCKCHAIN] Receiving block from peer...");
        System.out.println("Current Block: " + block);
        Thread.sleep(100);
        notifyListeners(block);
        return mBlockOrganizer.receiveBlock(block);
    }

    private Pair<Integer, String> getInfoBlock (){
        Block parentBlock = mBlockOrganizer.getLastBlock();

        String previousHash;
        int newHeight;


        if (parentBlock != null) {
            previousHash = parentBlock.getCurrentBlockHash();
            newHeight = parentBlock.getNumberBlock() + 1;
        } else {
            previousHash = "0";
            newHeight = 0;
        }

        return new Pair<>(newHeight, previousHash);
    }

}

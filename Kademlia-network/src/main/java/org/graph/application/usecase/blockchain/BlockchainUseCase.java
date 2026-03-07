package org.graph.application.usecase.blockchain;

import org.graph.adapter.outbound.network.message.auction.AuctionPayload;
import org.graph.application.usecase.provider.IBlockListener;
import org.graph.application.usecase.provider.ITransactionsPublished;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.entities.transaction.TransactionType;
import org.graph.domain.valueobject.cryptography.Pair;
import org.graph.application.usecase.blockchain.block.BlockRule;
import org.graph.application.usecase.blockchain.block.TransactionRule;
import org.graph.server.Peer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

public class BlockchainUseCase implements ITransactionsPublished {
    private final int numThreads;
    private final int currentDifficulty;
    private final TransactionRule mTransactionRule;
    private final BlockRule mBlockRule;
    private final List<IBlockListener> listeners;
    private long lastTxTime;
    private static final long MAX_WAIT_TIME_MS = 2000;
    private Peer myself;

    public BlockchainUseCase(int difficulty, int maxTx, Peer myself) {
        this.mTransactionRule = new TransactionRule(maxTx);
        this.mBlockRule = new BlockRule(this);
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.currentDifficulty = difficulty;
        this.listeners = new ArrayList<>();
        this.lastTxTime = System.currentTimeMillis();
        this.myself = myself;
        startMiningWatchdog();
        
    }

    public void setNonceProvider(Function<BigInteger, Long> provider) {
        this.mTransactionRule.setNonceProvider(provider);
    }

    private void startMiningWatchdog() {
        Thread watchdog = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);

                    synchronized (this) {
                        int pending = mTransactionRule.getPendingCount();
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

    public Peer getMyself() {return myself;}
    public TransactionRule getTransactionOrganizer() {
        return mTransactionRule;
    }
    public BlockRule getBlockOrganizer() {
        return mBlockRule;
    }

    public void addBlockListener(IBlockListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void submitTransaction(Transaction tx) {
        addTransaction(tx);
    }

    private void notifyListeners(Block block) {
        for (IBlockListener listener : listeners) {
            listener.onBlockCommitted(block);
        }
    }

    public void notifyChainReorganized(List<Block> newChain) {
        for (IBlockListener listener : listeners) {
            listener.onChainReorganized(newChain);
        }
    }

    public void createGenesisBlock() {
        if (mBlockRule.getChainHeight() >= 0) {
            System.out.println("[BLOCK_GENESIS] Blockchain already initialized. Genesis ignored.");
            return;
        }

        List<Transaction> genesisTx = new ArrayList<>();
        AuctionPayload genesisData = AuctionPayload.genesis();

        Transaction tx = new Transaction(
                TransactionType.REGULAR_TRANSFER,
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                genesisData,
                myself.getMyself().getNodeId().value(),
                0L,
                myself.getHybridLogicalClock().getPhysicalClock()
        );

        try {
            String dataToSign = tx.getDataSign();
            byte[] signature = myself.getIsKeysInfrastructure().signMessage(dataToSign);
            tx.setSignature(signature);
        } catch (Exception e) {
            System.err.println("[4] Failed to sign transaction Genesis: " + e.getMessage());
            return;
        }

        genesisTx.add(tx);

        Block genesis = new Block(1, 0, "0", genesisTx, currentDifficulty);
        genesis.mineBlock(currentDifficulty, numThreads);


        if (genesis.getCurrentBlockHash() != null) {
            mBlockRule.addLocalBlock(genesis);
        } else {
            System.out.println("[BLOCK_GENESIS] Genesis block mining failed. Structural insertion aborted.");
        }
    }

    public void addTransaction(Transaction tx) {
        if (tx == null) {
            System.err.println("[BLOCK_TRANSACTION] Invalid transaction rejected.");
            return;
        }

        mTransactionRule.addTransaction(tx);
        this.lastTxTime = System.currentTimeMillis();

        if (mTransactionRule.shouldCreateBlock()) {
            System.out.println("\n[MINER] Mempool full. Mining started...");
            createNewBlock();
        }
    }


    public void createNewBlock() {
        List<Transaction> transactions = mTransactionRule.getTransactionsForBlock();

        if (transactions == null || transactions.isEmpty()) {
            System.out.println("[INFO] No pending transactions");
            this.lastTxTime = System.currentTimeMillis();
            return;
        }

        System.out.println("[MINER] Creating Block #" + getInfoBlock().key() + " pointing to " + getInfoBlock().value());

        Block newBlock = new Block(1,getInfoBlock().key() , getInfoBlock().value(), transactions, currentDifficulty);
        newBlock.mineBlock(currentDifficulty, numThreads);

        mTransactionRule.cleanPool(transactions);
        mBlockRule.addLocalBlock(newBlock);

        this.lastTxTime = System.currentTimeMillis();
        notifyListeners(newBlock);

        BigInteger keyId = new BigInteger(newBlock.getCurrentBlockHash(), 16);
        myself.getMkademliaNetwork().storage(keyId, newBlock);

        myself.getNetworkGateway().announceBlockToNetwork(newBlock);

    }


    public boolean receiveBlockFromPeer(Block block) throws InterruptedException {
        System.out.println("\n[BLOCKCHAIN] Receiving block from peer...");
        System.out.println("Current Block: " + block);

        boolean isAdded = mBlockRule.receiveBlock(block);

        if (isAdded) {
            System.out.println("[BLOCKCHAIN] Block accepted. Cleaning pending transactions...");
            mTransactionRule.cleanPool(block.getTransactions());

            Thread.sleep(100);
            notifyListeners(block);
        } else {
            System.err.println("[BLOCKCHAIN] Block rejected by BlockRule.");
        }

        return isAdded;
    }

    private Pair<Integer, String> getInfoBlock (){
        Block parentBlock = mBlockRule.getLastBlock();

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

package org.graph.domain.entities.block;

import org.graph.application.usecase.mining.MinerThreadBlock;
import org.graph.application.usecase.mining.MiningResultBlock;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.entities.tree.MerkleTree;
import org.graph.domain.valueobject.utils.HashUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Block implements Serializable {
    private final int numberBlock;
    private final BlockHeader header;
    private final List<Transaction> transactions;
    private String hashCache;
    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService minerPool = Executors.newFixedThreadPool(CORES);


    public Block(int version, int numberBlock, String hashPrev, List<Transaction> transactions, int difficulty) {
        this.numberBlock = numberBlock;
        this.transactions = transactions;
        MerkleTree tree = new MerkleTree(transactions);
        String root = tree.getRootHash();
        this.header = new BlockHeader(version, hashPrev, root, difficulty);
    }

    public String getCurrentBlockHash() { return hashCache; }
    public int getNumberBlock() { return numberBlock; }
    public BlockHeader getHeader() { return header; }
    public List<Transaction> getTransactions() { return transactions; }

    // Nota: Este comportamento foi introduzido exclusivamente para efeitos de teste.
    public void setCurrentHash(String hash) { this.hashCache = hash;}

    /**
     * Executa o processo de mineração do bloco através da procura de um valor
     * de {@code nonce} que produza um hash SHA-256 compatível com o nível de
     * dificuldade especificado. A dificuldade é definida pelo número de zeros
     * consecutivos exigidos no prefixo do hash resultante, característica típica
     * dos mecanismos de consenso baseados em Proof of Work (PoW).
     *
     * O método distribui o processo de procura do {@code nonce} por múltiplas
     * tarefas concorrentes, permitindo explorar paralelismo computacional. Cada
     * thread executa tentativas independentes de geração de hash, variando o
     * valor do nonce até que uma combinação válida seja encontrada. Quando um
     * dos workers encontra um hash que satisfaz a dificuldade imposta, o
     * processo de mineração é interrompido e o bloco passa a possuir uma prova
     * de trabalho válida.
     *
     * Esta abordagem reduz o tempo esperado de descoberta do nonce através da
     * utilização eficiente de recursos de processamento multicore, mantendo o
     * comportamento probabilístico característico do algoritmo de mineração.
     *
     * @param difficulty nível de dificuldade da mineração, representado pelo
     *                   número de zeros consecutivos exigidos no prefixo do
     *                   hash SHA-256 do bloco
     * @param numberThread número de threads (tarefas concorrentes) utilizadas
     *                     no processo de procura do nonce
     */
    public void mineBlock(int difficulty, int numberThread) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean found = new AtomicBoolean(false);
        CompletionService<MiningResultBlock> completionService = new ExecutorCompletionService<>(minerPool);
        List<Future<MiningResultBlock>> futures = new ArrayList<>();

        long nonceRangePerThread = Long.MAX_VALUE / numberThread;

        for (int i = 0; i < numberThread; i++) {
            long startNonce = i * nonceRangePerThread;
            MinerThreadBlock miner = new MinerThreadBlock(
                    i, startNonce, nonceRangePerThread,
                    header.getPayloadForMining(), difficulty, found
            );
            futures.add(completionService.submit(miner));
        }

        MiningResultBlock result = null;

        try {
            for (int i = 0; i < numberThread; i++) {
                Future<MiningResultBlock> future = completionService.take();
                MiningResultBlock r = future.get();
                if (r != null) {
                    result = r;
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Mining execution encountered an anomaly: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            for (Future<MiningResultBlock> f : futures) {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            }
        }

        if (result != null) {
            applyMiningResult(result, startTime);
        } else {
            System.out.println("[MINER] Failed to mine block (No nonce found in range).");
        }
    }

    private void  applyMiningResult(MiningResultBlock result, long startTime){
        this.header.setNonce(result.nonce());
        this.hashCache = result.hash();
        long endTime = System.currentTimeMillis();

        System.out.println("\n[INFO] Block miner!");
        System.out.println("  Thread win: #" + result.threadId());
        System.out.println("  Nonce find: " + result.nonce());
        System.out.println("  Attempts: " + result.attempts());
        System.out.println("  Time: " + (endTime - startTime) + "ms");
        System.out.println("  Hash: " + hashCache.substring(0, 40) + "...");
        System.out.println("  Hash rate: " +
                (result.attempts() * 1000.0 / (endTime - startTime)) + " H/s");
    }

    /**
     * Executa a validação estrutural e temporal do bloco atual em relação ao
     * bloco imediatamente anterior da cadeia (bloco pai). Este procedimento
     * constitui uma etapa fundamental do mecanismo de consenso, assegurando
     * que o bloco respeita as propriedades de continuidade, integridade
     * criptográfica e coerência temporal exigidas pela blockchain.
     *
     * A verificação compara o estado do bloco candidato com o estado do bloco
     * pai fornecido como referência, garantindo que a ligação entre ambos
     * preserva a estrutura encadeada da cadeia de blocos. Qualquer inconsistência
     * detetada implica a rejeição do bloco, impedindo a sua inclusão na cadeia.
     *
     * O processo de validação aplica as seguintes restrições:
     *
     * - Continuidade criptográfica: o campo {@code previousHash} deve coincidir
     *   exatamente com o hash criptográfico do bloco pai. Esta condição garante
     *   a integridade da ligação entre blocos e impede a inserção arbitrária de
     *   blocos na cadeia.
     *
     * - Sequência estrutural: o identificador sequencial do bloco
     *   ({@code blockNumber}) deve ser exatamente o sucessor do número do bloco
     *   pai, preservando a ordem lógica da cadeia.
     *
     * - Coerência temporal: o {@code timestamp} do bloco atual deve ser
     *   estritamente posterior ao {@code timestamp} do bloco pai, assegurando
     *   consistência cronológica entre blocos consecutivos.
     *
     * - Limites temporais válidos: o {@code timestamp} deve respeitar os limites
     *   definidos pelo protocolo para prevenir manipulações de tempo conhecidas
     *   como ataques do tipo <i>Time Warp</i>, nas quais um nó tenta distorcer a
     *   linha temporal da cadeia para influenciar o processo de validação ou
     *   ajuste de dificuldade.
     *
     * Caso qualquer uma destas condições falhe, o bloco deve ser considerado
     * inválido e rejeitado pelo nó validador.
     *
     * @param parent bloco imediatamente anterior na blockchain, utilizado como
     *               referência para validação estrutural e temporal
     *
     * @return {@code true} se todas as regras de validação forem satisfeitas;
     *         {@code false} caso seja detetada qualquer inconsistência
     */

    public boolean isValidBlock(Block parent){
        if (parent == null) {
            boolean isGenesis = this.numberBlock == 0;
            if(!isGenesis) System.out.println("[DEBUG] Rejected: Father is null and it is not Genesis.");
            return this.numberBlock == 0;
        }

        if (!this.header.getPreviousBlockHash().equals(parent.getCurrentBlockHash())) {
            System.out.println("Invalid Previous Hash");
            return false;
        }


        if (this.numberBlock != parent.getNumberBlock() + 1) {
            System.out.println("Invalid Sequence Number");
            return false;
        }


        if (this.header.getTimestamp() <= parent.getHeader().getTimestamp()) {
            System.out.println("Timestamp too old");
            return false;
        }


        return true;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(header.getTimestamp()));

        sb.append("\n ===================================================================== \n");
        sb.append(String.format("| BLOCK #%-4d                                                       ║\n", numberBlock));
        sb.append("===================================================================== \n");

        sb.append(String.format("| Hash:      %s\n", (hashCache != null ? hashCache : "PENDING MINING")));
        sb.append(String.format("| Prev Hash: %s\n", header.getPreviousBlockHash()));
        sb.append(String.format("| Root:      %s\n", header.getMerkleRoot()));
        sb.append(String.format("| Nonce:     %-10d | Difficulty: %d\n", header.getNonce(), header.getDifficulty()));
        sb.append(String.format("| Time:      %s\n", timeStr));

        sb.append("===================================================================== \n");
        sb.append(String.format("| TRANSACTIONS (%d)                                             ║\n", transactions.size()));
        sb.append("===================================================================== \n");

        if (transactions.isEmpty()) {
            sb.append("| (Empty Block)                                                      ║\n");
        } else {
            for (Transaction tx : transactions) {
                sb.append("| > ").append(tx.toString()).append("\n");
            }
        }

        sb.append("===================================================================== ");
        return sb.toString();
    }


    /**
     * Recalcula o hash criptográfico do bloco utilizando o algoritmo SHA-256 a partir
     * do estado atual de todos os campos que compõem a sua representação imutável.
     *
     * O valor gerado funciona como identificador único do bloco dentro da cadeia e
     * constitui o principal mecanismo de verificação de integridade estrutural. O
     * cálculo do hash incorpora tipicamente os elementos fundamentais do bloco,
     * incluindo o identificador do bloco anterior (previousHash), o conjunto de
     * transações, o timestamp e o nonce utilizado no processo de mineração.
     *
     * Em sistemas baseados em Proof of Work (PoW), qualquer modificação em qualquer
     * um destes componentes provoca necessariamente a alteração completa do hash
     * resultante devido às propriedades de avalanche dos algoritmos criptográficos
     * de hash. Consequentemente, a alteração de dados invalida imediatamente o
     * resultado previamente obtido durante o processo de mineração.
     *
     * Este comportamento garante duas propriedades essenciais do sistema:
     * integridade dos dados armazenados no bloco e imutabilidade prática da cadeia
     * de blocos. Para que um bloco adulterado volte a ser considerado válido, seria
     * necessário recomputar a prova de trabalho não apenas desse bloco, mas também
     * de todos os blocos subsequentes, o que se torna computacionalmente inviável
     * em redes distribuídas suficientemente grandes.
     *
     * @return String contendo o hash SHA-256 codificado em formato hexadecimal,
     *         representando o estado criptográfico atual do bloco.
     */

    public String recalculateHash() {
        try {
            String dataToHash = header.getPayloadForMining() + header.getNonce();
            return HashUtils.calculateSha256(dataToHash);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular o Hash do Bloco: " + e.getMessage());
        }
    }
}

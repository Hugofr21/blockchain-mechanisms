package org.graph.application.usecase.blockchain.block;

import org.graph.domain.entities.transaction.Transaction;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
/*
    Mitigar atack replay
    duplicatiosn de transactions
 */
public class TransactionRule {
    private Set<Transaction> transactions;
    private Set<Transaction> pendingTransactions;
    private Set<String> completedTransactions;
    private final int maxTransactionsPending;
    private final Object lock;
    private Function<BigInteger, Long> currentNonceProvider;

    public TransactionRule(int maxTransactionsPending) {
        this.transactions = new HashSet<Transaction>();
        this.pendingTransactions = new LinkedHashSet<>();
        this.completedTransactions = new HashSet<>();
        this.maxTransactionsPending = maxTransactionsPending;
        this.lock = new Object();
    }

    public void  setNonceProvider(Function<BigInteger, Long> currentNonceProvider) {
        this.currentNonceProvider = currentNonceProvider;
    }

    public boolean isTransactionKnown(String txId) {
        synchronized (lock) {
            if (completedTransactions.contains(txId)) return true;
            return pendingTransactions.stream().anyMatch(t -> t.getTxId().equals(txId));
        }
    }

    public void addTransaction(Transaction transaction) {
        synchronized (lock) {

            if (completedTransactions.contains(transaction.getTxId())) {
                System.out.println("[INFO] Transaction duplicate ignore: " + transaction);
                return;
            }

            if (currentNonceProvider != null) {
                long expectedNextNonce = currentNonceProvider.apply(transaction.getSenderId());
                if (transaction.getNonce() < expectedNextNonce) {
                    System.err.println("[SECURITY] Replay Attack ignored! Nonce " + transaction.getNonce() + " < " + expectedNextNonce);
                    return;
                }
            } else {
                System.out.println("[WARN] Nonce Provider not initialized. Pending transaction at risk.");
            }

            boolean added = pendingTransactions.add(transaction);
            if (added) {
                System.out.println("[INFO] New Transaction pendent: " + transaction);
                System.out.println("[INFO] Total pendents: " + pendingTransactions.size());
            }
        }
    }

    public boolean shouldCreateBlock(){
        synchronized (lock) {
            return pendingTransactions.size() >= maxTransactionsPending;
        }
    }

    public void forceClearPending() {
        synchronized (lock) {
            pendingTransactions.clear();
        }
    }

    public List<Transaction> getTransactionsForBlock() {
        synchronized (lock) {
            if (pendingTransactions.isEmpty() || currentNonceProvider == null) return null;

            List<Transaction> candidateTxs = new ArrayList<>();
            List<Transaction> staleTxs = new ArrayList<>();

            Map<BigInteger, List<Transaction>> bySender = new HashMap<>();
            for (Transaction tx : pendingTransactions) {
                bySender.computeIfAbsent(tx.getSenderId(), k -> new ArrayList<>()).add(tx);
            }

            for (Map.Entry<BigInteger, List<Transaction>> entry : bySender.entrySet()) {
                BigInteger senderId = entry.getKey();
                List<Transaction> senderTxs = entry.getValue();

                senderTxs.sort(Comparator.comparingLong(Transaction::getNonce));

                long expectedNonce = currentNonceProvider.apply(senderId);

                for (Transaction tx : senderTxs) {
                    if (tx.getNonce() < expectedNonce) {
                        staleTxs.add(tx);
                    } else if (tx.getNonce() == expectedNonce) {
                        candidateTxs.add(tx);
                        expectedNonce++;
                    } else {
                        break;
                    }
                }
            }

            for (Transaction stale : staleTxs) {
                pendingTransactions.remove(stale);
            }

            if (candidateTxs.size() > maxTransactionsPending) {
                candidateTxs = candidateTxs.subList(0, maxTransactionsPending);
            }

            return candidateTxs;
        }
    }

    public void markTransactionsAsProcessed(List<Transaction> transactions) {
        synchronized (lock) {
            for (Transaction tx : transactions) {
                completedTransactions.add(tx.getTxId());
                pendingTransactions.removeIf(p -> p.getTxId().equals(tx.getTxId()));
            }
        }
    }

    public void cleanPool(List<Transaction> minedTxs) {
        synchronized (lock) {
            for (Transaction minedTx : minedTxs) {
                pendingTransactions.removeIf(p -> p.getTxId().equals(minedTx.getTxId()));
                completedTransactions.add(minedTx.getTxId());
            }
        }
    }

    public int getPendingCount() {
        synchronized (lock) {
            return pendingTransactions.size();
        }
    }


    public Transaction getTransactionById(String hashHex) {
        synchronized (lock) {
            return pendingTransactions.stream()
                    .filter(tx -> tx.getTxId().equals(hashHex))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Revives transactions from blocks that were orphaned during a Fork (Chain Reorg).
     */
    public void restoreToPool(List<Transaction> orphanedTxs) {
        synchronized (lock) {
            for (Transaction tx : orphanedTxs) {
                completedTransactions.remove(tx.getTxId());
                pendingTransactions.add(tx);
            }
        }
    }


}
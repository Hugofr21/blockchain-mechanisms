package org.graph.application.usecase.blockchain.block;

import org.graph.domain.entities.transaction.Transaction;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
/*
    Mitigar atack replay
    duplicatiosn de transactions

 */
public class TransactionOrganizer {
    private Set<Transaction> transactions;
    private Set<Transaction> pendingTransactions;
    private List<String> completedTransactions;
    private int maxTransactionsPending;
    private Object lock;
    private Function<BigInteger, Long> currentNonceProvider;

    public TransactionOrganizer(int maxTransactionsPending) {
        this.transactions = new HashSet<Transaction>();
        this.pendingTransactions = new LinkedHashSet<>();
        this.completedTransactions = new ArrayList<>();
        this.maxTransactionsPending = maxTransactionsPending;
        this.lock = new Object();
    }

    public void  setNonceProvider(Function<BigInteger, Long> currentNonceProvider) {
        this.currentNonceProvider = currentNonceProvider;
    }

    public void addTransaction(Transaction transaction) {
        synchronized (lock) {

            if (completedTransactions.contains(transaction.getTxId())) {
                System.out.println("[INFO] Transaction duplicate ignore: " + transaction);
                return;
            }

            long expectedNextNonce = currentNonceProvider.apply(transaction.getSenderId());
            if (transaction.getNonce() < expectedNextNonce){
                System.out.println("[SECURITY_INFO] Transaction duplicate ignore, possible attack replay: " + transaction);
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


    public List<Transaction> getTransactionsForBlock() {
        synchronized (lock) {
            if (pendingTransactions.isEmpty()) return null;
            List<Transaction> candidateTxs = new ArrayList<>();
            List<Transaction> sortedPool = new ArrayList<>();
            sortedPool.sort(Comparator.comparingLong(Transaction::getNonce));

            for (Transaction tx : sortedPool) {
                long expectedNonce = currentNonceProvider.apply(tx.getSenderId());
                if (tx.getNonce() == expectedNonce) {
                    candidateTxs.add(tx);
                }
                if (candidateTxs.size() >= maxTransactionsPending) break;

            }
            return candidateTxs;
        }
    }

    public void markTransactionsAsProcessed(List<Transaction> transactions) {
        synchronized (lock) {
            for (Transaction tx : transactions) {
                completedTransactions.add(tx.getTxId());
                pendingTransactions.remove(tx);
            }
        }
    }

    public int getPendingCount() {
        synchronized (lock) {
            return pendingTransactions.size();
        }
    }

    public void cleanPool(List<Transaction> minedTxs) {
        synchronized (lock) {
            for (Transaction tx : minedTxs) {

                pendingTransactions.remove(tx);
                completedTransactions.add(tx.getTxId());
            }
            System.out.println("[POOL] Cleaning completed. Remaining tasks: " + pendingTransactions.size());
        }
    }


    public Transaction getTransactionById(String hashHex) {
        return pendingTransactions.stream()
                .filter(tx -> tx.getTxId().equals(hashHex))
                .findFirst()
                .orElse(null);
    }
}
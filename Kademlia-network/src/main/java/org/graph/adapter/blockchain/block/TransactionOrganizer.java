package org.graph.adapter.blockchain.block;

import org.graph.domain.application.transaction.Transaction;

import java.util.*;

public class TransactionOrganizer {
    private Set<Transaction> transactions;
    private Set<Transaction> pendingTransactions;
    private List<String> completedTransactions;
    private int maxTransactionsPending;
    private Object lock;

    public TransactionOrganizer(int maxTransactionsPending) {
        this.transactions = new HashSet<Transaction>();
        this.pendingTransactions = new LinkedHashSet<>();
        this.completedTransactions = new ArrayList<>();
        this.maxTransactionsPending = maxTransactionsPending;
        this.lock = new Object();
    }

    public void addTransaction(Transaction transaction) {
        synchronized (lock) {

            if (completedTransactions.contains(transaction.getTxId())) {
                System.out.println("[INFO] Transaction duplicate ignore: " + transaction);
                return;
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
            Iterator<Transaction> iterator = pendingTransactions.iterator();
            int count = 0;

            while (iterator.hasNext() && count < maxTransactionsPending) {
                Transaction tx = iterator.next();
                candidateTxs.add(tx);
                count++;
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
}
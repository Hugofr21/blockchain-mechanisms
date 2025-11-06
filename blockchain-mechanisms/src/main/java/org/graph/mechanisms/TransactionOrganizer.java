package org.graph.mechanisms;

import org.graph.transaction.Transaction;

import java.util.*;

public class TransactionOrganizer {
    private Set<Transaction> transactions;
    private Set<Transaction> pendingTransactions;
    private List<String> completedTransactions;
    private int maxTransactionsPending;
    private Object lock;

    public TransactionOrganizer(int maxTransactionsPending) {
        this.transactions = new HashSet<Transaction>();
        this.pendingTransactions = new HashSet<>();
        this.completedTransactions = new ArrayList<>();
        this.maxTransactionsPending = maxTransactionsPending;
        this.lock = new Object();
    }

    public boolean addTransaction(Transaction transaction) {
        synchronized (lock) {

            if (completedTransactions.contains(transaction.getId())) {
                System.out.println("[INFO] Transaction duplicate ignore: " + transaction);
                return false;
            }

            boolean added = pendingTransactions.add(transaction);
            if (added) {
                System.out.println("[INFO] New Transaction pendent: " + transaction);
                System.out.println("[INFO] Total pendents: " + pendingTransactions.size());
            }
            return added;
        }
    }

    public boolean shouldCreateBlock(){
        synchronized (lock) {
            return pendingTransactions.size() >= maxTransactionsPending;
        }
    }


    public List<Transaction> getTransactionsForBlock() {
        synchronized (lock) {
            if (pendingTransactions.isEmpty()) {
                return null;
            }

            List<Transaction> transactions = new ArrayList<>();
            Iterator<Transaction> iterator = pendingTransactions.iterator();

            int count = 0;
            while (iterator.hasNext() && count < maxTransactionsPending) {
                Transaction tx = iterator.next();
                transactions.add(tx);
                iterator.remove();
                completedTransactions.add(tx.getId());
                count++;
            }

            System.out.println("[INFO] " + transactions.size() +
                    " transactions selections for new block");
            System.out.println("[INFO] Pendents restates: " + pendingTransactions.size());

            return transactions;
        }
    }

    public void markTransactionsAsProcessed(List<Transaction> transactions) {
        synchronized (lock) {
            for (Transaction tx : transactions) {
                completedTransactions.add(tx.getId());
                pendingTransactions.remove(tx);
            }
        }
    }

    public int getPendingCount() {
        synchronized (lock) {
            return pendingTransactions.size();
        }
    }


}

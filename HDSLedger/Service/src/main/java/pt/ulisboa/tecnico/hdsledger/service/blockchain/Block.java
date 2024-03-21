package pt.ulisboa.tecnico.hdsledger.service.blockchain;

import java.util.ArrayList;
import java.util.List;

public class Block {
    
    private String prevHash;
    private List<Transaction> transactions;
    private final int maxTransactions;

    public Block(String prevHash) {
        this.prevHash = prevHash;
        this.transactions = new ArrayList<>();
        this.maxTransactions = 5;
    }

    public boolean addTransaction(Transaction transaction) {
        // Check if the maximum number of transactions has been reached
        if (transactions.size() >= maxTransactions) {
            return false; // Cannot add transaction, reached maximum limit
        }
        // Add the transaction and return true
        this.transactions.add(transaction);
        return true;
    }
}
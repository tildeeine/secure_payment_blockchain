package pt.ulisboa.tecnico.hdsledger.service.blockchain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Block {
    
    private String prevHash;
    private List<Transaction> transactions;
    private final int maxTransactions;

    public Block() {
        this.transactions = new ArrayList<>();
        this.maxTransactions = 5;
    }

    public void setPrevHash(String prevHash){
        this.prevHash = prevHash;
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

    public boolean isFull(){
        if (transactions.size() >= maxTransactions) {
            return false; // Cannot add transaction, reached maximum limit
        }
        return true;
    }

    public List<Transaction> getTransactions(){
        return this.transactions;
    }

    public String getPrevHash() {
        return prevHash;
    }
    
}
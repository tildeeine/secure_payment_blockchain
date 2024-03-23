package pt.ulisboa.tecnico.hdsledger.service.blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import pt.ulisboa.tecnico.hdsledger.communication.ClientData;

public class Block {
    
    private String prevHash;
    private List<ClientData> transactions;
    private final int maxTransactions;

    public Block() {
        this.transactions = new ArrayList<>();
        this.maxTransactions = 5;
    }

    public void setPrevHash(String prevHash){
        this.prevHash = prevHash;
    }

    public boolean addTransaction(ClientData transaction) {
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

    public List<ClientData> getTransactions(){
        return this.transactions;
    }

    public String getPrevHash() {
        return prevHash;
    }

    // Method to execute a function on each transaction
    public void forEachTransaction(Consumer<ClientData> action) {
        transactions.forEach(action);
    }

    // Method to remove a specific instance of ClientData from the list of transactions
    public boolean removeTransaction(ClientData transaction) {
        return transactions.remove(transaction);
    }
    
}
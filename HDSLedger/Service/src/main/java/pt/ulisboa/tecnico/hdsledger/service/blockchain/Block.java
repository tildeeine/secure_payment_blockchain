package pt.ulisboa.tecnico.hdsledger.service.blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import pt.ulisboa.tecnico.hdsledger.communication.ClientData;

public class Block implements Serializable{
    
    private final int BLOCK_ID;
    private String prevHash;
    private List<ClientData> transactions;
    private final int maxTransactions;

    public Block(int BLOCK_ID) {
        this.transactions = new ArrayList<>();
        this.BLOCK_ID = BLOCK_ID;
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
    public void removeTransaction(ClientData transaction) {
        if (this.transactions.contains(transaction)){
            transactions.remove(transaction);
        }
        else{
            System.out.println("Can't remove non existant transaction from block.");
        }
    }

    // Method to sort transactions by request ID and client ID
    public void sortTransactions() {
        transactions.sort(Comparator.comparing(ClientData::getRequestID)
                                    .thenComparing(ClientData::getClientID));
    }



    public int getBLOCK_ID() {
        return BLOCK_ID;
    }



    public int getMaxTransactions() {
        return maxTransactions;
    }
    
}
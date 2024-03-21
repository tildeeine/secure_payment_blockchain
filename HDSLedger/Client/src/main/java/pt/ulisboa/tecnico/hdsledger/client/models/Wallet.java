package pt.ulisboa.tecnico.hdsledger.client.models;

import java.security.PrivateKey;
import java.security.PublicKey;

public class Wallet {

    private String id;
    private float balance;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public Wallet(String id, float balance, PrivateKey privateKey, PublicKey publicKey) {
        this.id = id;
        this.balance = balance;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String getId() {
        return id;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    public void adjustBalance(int amount) { // ! Might remove
        this.balance += amount;
        // Throw exception if balance is negative
        if (this.balance < 0) {
            this.balance -= amount; // Undo
            throw new IllegalArgumentException("Balance cannot be negative");
        }
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "id='" + id + '\'' +
                ", balance=" + balance +
                '}';
    }

}

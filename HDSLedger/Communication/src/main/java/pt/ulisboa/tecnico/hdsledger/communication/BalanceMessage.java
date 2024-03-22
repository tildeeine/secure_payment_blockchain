package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.Message;

public class BalanceMessage extends Message {

    private float balance;
    private int requestID;
    private String clientID;
    private Message.Type type;

    public BalanceMessage(float balance, int requestID, String clientID, String senderID, Type type) {
        super(senderID, type);
        this.balance = balance;
        this.requestID = requestID;
        this.clientID = clientID;
    }

    public float getBalance() {
        return balance;
    }

    public Integer getRequestID() {
        return requestID;
    }

    public String getClientID() {
        return clientID;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

}

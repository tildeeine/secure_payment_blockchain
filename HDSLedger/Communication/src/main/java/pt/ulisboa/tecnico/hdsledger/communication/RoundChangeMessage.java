package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.HashSet;

import com.google.gson.Gson;

public class RoundChangeMessage extends Message {

    // Consensus instance
    private int consensusInstance;
    // Round
    private int round;

    private int prepared_round;

    //private String prepared_value;
    private ClientData prepared_data;

    private HashSet<ConsensusMessage> prepareMessages;
    
    public RoundChangeMessage(String senderId, Type type, int consensusInstance, int round, int prepared_round, ClientData prepared_data) {
        super(senderId, type);
        this.consensusInstance = consensusInstance;
        this.round = round;
        this.prepared_round = prepared_round;
        this.prepared_data = prepared_data;
        this.prepareMessages = new HashSet<>();
    }

    public void addPrepareMessage(ConsensusMessage message) {
        prepareMessages.add(message);
    }

    public HashSet<ConsensusMessage> getPrepareMessages() {
        return this.prepareMessages;
    }

    public int getConsensusInstance() {
        return consensusInstance;
    }

    public void setConsensusInstance(int consensusInstance) {
        this.consensusInstance = consensusInstance;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getPreparedRound() {
        return prepared_round;
    }

    public void setPreparedRound(int prepared_round) {
        this.prepared_round = prepared_round;
    }

    // Getter and setter for prepared_value
    public ClientData getClientData() {
        return this.prepared_data;
    }

    public void setPreparedValue(ClientData prepared_data) {
        this.prepared_data = prepared_data;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

}

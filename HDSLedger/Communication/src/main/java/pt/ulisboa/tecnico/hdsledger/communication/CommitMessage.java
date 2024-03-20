package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class CommitMessage {

    // Value
    private ClientData clientData;

    public CommitMessage(ClientData clientData) {
        this.clientData = clientData;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}

package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class CommitMessage {

    // Value
    private ClientData value;

    public CommitMessage(ClientData value) {
        this.value = value;
    }

    public ClientData getData() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}

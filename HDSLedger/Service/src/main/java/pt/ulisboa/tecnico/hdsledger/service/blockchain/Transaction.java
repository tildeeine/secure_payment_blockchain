package pt.ulisboa.tecnico.hdsledger.service.blockchain;

import pt.ulisboa.tecnico.hdsledger.communication.ClientData;

public class Transaction {
    
    private String senderID;

    private String recieverID;

    private ClientData clientData;

    public Transaction(String senderID, String recieverID, ClientData clientData){
        this.senderID = senderID;
        this.recieverID = recieverID;
        this.clientData = clientData;
    }


    public String getSenderID(){
        return this.senderID;
    }

    public String getRecieverID(){
        return this.recieverID;
    }

    public ClientData getclientData(){
        return this.clientData;
    }
}

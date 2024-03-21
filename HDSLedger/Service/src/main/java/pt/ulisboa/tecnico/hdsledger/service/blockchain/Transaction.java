package pt.ulisboa.tecnico.hdsledger.service.blockchain;

public class Transaction {
    
    private String senderID;

    private String recieverID;

    private ClientData clientData;

    public Transaction(senderID, recieverID, amount){
        this.senderID = senderID;
        this.recieverID = recieverID;
        this.amount = amount;
    }


    public String getSenderID(){
        return this.senderID;
    }

    public String getRecieverID(){
        return this.senderID;
    }

    public int getAmount(){
        return this.senderID;
    }
}

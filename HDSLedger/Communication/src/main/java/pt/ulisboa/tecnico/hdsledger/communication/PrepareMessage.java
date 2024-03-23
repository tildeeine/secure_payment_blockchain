package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.service.blockchain.Block;

public class PrepareMessage {
    
    // Value
    private Block block;

    public PrepareMessage(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return this.block;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   

package pt.ulisboa.tecnico.hdsledger.service.blockchain;

import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Blockchain {
    
    private List<Block> blocks;

    public Blockchain() {
        this.blocks = new ArrayList<>();
        // Genesis block (initial block)
        Block genesisBlock = new Block("0"); // Assuming "0" as the initial hash
        this.blocks.add(genesisBlock);
    }

    // Add a new block to the blockchain
    public void addBlock(Block block) throws NoSuchAlgorithmException, IOException {
        Block previousBlock = getLatestBlock();
        String prevHash = calculateHash(previousBlock);
        block.setPrevHash(prevHash);
        blocks.add(block);
    }

    public static boolean verifyBlock(Block block, String hash) throws NoSuchAlgorithmException, IOException{
        if (calculateHash(block).equals(hash)){
            return true;
        }
        return false;
    }

    public Block getLatestBlock() {
        return blocks.get(blocks.size() - 1);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public static byte[] serializeBlock(Block block) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(block);
        objectOutputStream.flush();
        return byteArrayOutputStream.toByteArray();
    }

    public static String calculateHash(Block block) throws IOException, NoSuchAlgorithmException {
        byte[] serializedObj = serializeBlock(block);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(serializedObj));
    }

    // Helper method to convert byte array to hexadecimal string
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

package pg.projekt.sockets.messages;


import lombok.*;
import pg.projekt.CipherMode;
import pg.projekt.EncryptionManager;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A class representing sent message
 */
@Getter
@Setter
@EqualsAndHashCode
public class Message implements Serializable {
    private byte[] payload;
    private byte[] iv;
    private MessageType type;
    private String sender;
    private UUID uuid;
    private static Integer counter = 0; // TODO CAN BE CHANGED BACK TO UUID

    /**
     * Baisc constructor used for internal messages
     * @param content - a string content of message
     */
    public Message(String content){
        this(content, "", MessageType.INFO);
        this.iv = EncryptionManager.generateRandomBytes(16);
    }
    public Message(String content, String sender){
        this(content, sender, MessageType.TEXT);
        this.iv = EncryptionManager.generateRandomBytes(16);
    }

    public Message(byte[] content, String sender, MessageType type){
        this.payload = content;
        this.sender = sender;
        this.type = type;
        this.uuid = UUID.randomUUID();
        this.iv = EncryptionManager.generateRandomBytes(16);
    }

    public Message(String content, String sender, MessageType type){
        this.payload = (content).getBytes(StandardCharsets.UTF_8);
        this.sender = sender;
        this.type = type;
        this.uuid = UUID.randomUUID();
        this.iv = EncryptionManager.generateRandomBytes(16);
    }
    public Message(String content, String sender, MessageType type, UUID uuid){
        this.payload = (content).getBytes(StandardCharsets.UTF_8);
        this.sender = sender;
        this.type = type;
        this.uuid = uuid;
        this.iv = EncryptionManager.generateRandomBytes(16);
    }
    public String getContent(){
        String payloadString = new String(this.payload, StandardCharsets.UTF_8);
        return payloadString;

    }

    public void encryptPayload(byte[] key, CipherMode mode){
        byte[] encryptedPayload = EncryptionManager.encryptAES(payload, key, iv, mode);
        this.payload = encryptedPayload;

    }

    public void decryptPayload(byte[] key, CipherMode mode) {
        byte[] decryptedPayload = EncryptionManager.decryptAES(payload, key, iv, mode);
        this.payload = decryptedPayload;
    }

    @Override
    public String toString(){
        return getContent();
    }

}

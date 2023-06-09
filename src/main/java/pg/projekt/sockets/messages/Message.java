package pg.projekt.sockets.messages;


import lombok.*;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
public class Message implements Serializable {
    private byte[] payload;
    private MessageType type;
    private String sender;
    private UUID uuid;
    private static Integer counter = 0; // TODO CAN BE CHANGED BACK TO UUID

    public Message(String content){
        this(content, "", MessageType.INFO);
    }
    public Message(String content, String sender){
        this(content, sender, MessageType.TEXT);
    }

    public Message(byte[] content, String sender, MessageType type){
        this.payload = content;
        this.sender = sender;
        this.type = type;
        this.uuid = UUID.randomUUID();
    }

    public Message(String content, String sender, MessageType type){
        this.payload = (content).getBytes(StandardCharsets.UTF_8);
        this.sender = sender;
        this.type = type;
        this.uuid = UUID.randomUUID();
    }
    public Message(String content, String sender, MessageType type, UUID uuid){
        this.payload = (content).getBytes(StandardCharsets.UTF_8);
        this.sender = sender;
        this.type = type;
        this.uuid = uuid;
    }
    public String getContent(){
        String payloadString = new String(this.payload, StandardCharsets.UTF_8);
        return payloadString;

    }

    @Override
    public String toString(){
        return getContent();
    }

}

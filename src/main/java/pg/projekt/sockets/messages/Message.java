package pg.projekt.sockets.messages;


import lombok.*;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@EqualsAndHashCode
public class Message implements Serializable {
    private byte[] payload;
    private MessageType type;
    private String sender;

    public Message(String content){
        this(content, "", MessageType.INFO);
    }
    public Message(String content, String sender){
        this(content, sender, MessageType.TEXT);
    }
    public Message(String content, String sender, MessageType type){
        this.payload = (content).getBytes(StandardCharsets.UTF_8);
        this.sender = sender;
        this.type = type;
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
